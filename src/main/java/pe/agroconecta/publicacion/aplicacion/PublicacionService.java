package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.catalogo.aplicacion.VariedadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.precio.aplicacion.ReferenciaPrecioService;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.publicacion.dominio.EstadoPublicacion;
import pe.agroconecta.publicacion.dominio.MarcaRevision;
import pe.agroconecta.publicacion.dominio.Publicacion;
import pe.agroconecta.publicacion.dominio.PublicacionDisponible;
import pe.agroconecta.publicacion.infraestructura.EstadisticaCantidad;
import pe.agroconecta.publicacion.infraestructura.PublicacionDisponibleRepository;
import pe.agroconecta.publicacion.infraestructura.PublicacionRepository;

/**
 * Publicación de cosechas (RF-01, UC-01). Valida con mensajes amigables lo mismo que {@code trg_publicaciones_bi}
 * (la base de datos es la última barrera) y refresca la entidad para leer lo que calcula la base de datos:
 * peso de las unidades fijas, fecha límite, marca de moderación y columnas generadas.
 *
 * <p>También calcula las marcas de la moderación posterior que no pone la base de datos (RF-09) y ofrece a
 * {@code administracion} las consultas de la cola de moderación.</p>
 */
@Service
public class PublicacionService {

    static final String MENSAJE_VARIEDAD = "La variedad elegida no existe.";
    static final String MENSAJE_UNIDAD = "La unidad elegida no existe.";
    static final String MENSAJE_DISTRITO = "El distrito elegido no existe.";
    static final String MENSAJE_PESO = "Indique cuántos kilos pesa cada jaba o saco.";
    static final String MENSAJE_LIMITE_PASADO = "La fecha límite no puede ser anterior a hoy.";
    static final String MENSAJE_LIMITE_ANTES_COSECHA = "La fecha límite no puede ser anterior a la fecha de cosecha.";
    static final String MENSAJE_COSECHA_VENCIDA =
            "Con esa fecha de cosecha, el producto ya pasó su tiempo de venta. Revise la fecha.";
    static final String MENSAJE_NO_EXISTE = "La publicación no existe.";

    /** CANTIDAD_INUSUAL: los kilos superan este múltiplo del promedio de la zona (umbral aprobado en el slice 3). */
    static final double FACTOR_CANTIDAD_INUSUAL = 3.0;
    /** CANTIDAD_INUSUAL: publicaciones mínimas del mismo cultivo y distrito para poder comparar. */
    static final long MINIMO_PARA_COMPARAR = 3;
    /** CANTIDAD_INUSUAL: días hacia atrás que se consideran. */
    static final int DIAS_PARA_COMPARAR = 90;

    private static final Logger LOG = LoggerFactory.getLogger(PublicacionService.class);

    private final PublicacionRepository publicaciones;
    private final PublicacionDisponibleRepository disponibles;
    private final CatalogoService catalogo;
    private final ReferenciaPrecioService referencias;
    private final EntityManager entityManager;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param publicaciones repositorio de publicaciones
     * @param disponibles   vista de publicaciones disponibles (agricultor, calificación, kilos disponibles)
     * @param catalogo      catálogo de variedades, unidades y distritos
     * @param referencias   referencia de precio del módulo {@code precio} (marca PRECIO_FUERA_RANGO)
     * @param entityManager contexto de persistencia (para refrescar lo que calcula la base de datos)
     * @param reloj         reloj de la aplicación
     */
    public PublicacionService(PublicacionRepository publicaciones, PublicacionDisponibleRepository disponibles,
            CatalogoService catalogo, ReferenciaPrecioService referencias, EntityManager entityManager, Clock reloj) {
        this.publicaciones = publicaciones;
        this.disponibles = disponibles;
        this.catalogo = catalogo;
        this.referencias = referencias;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    /**
     * Publica una cosecha. Nace PUBLICADA (moderación posterior).
     *
     * @param idAgricultor id del agricultor de la sesión
     * @param datos        datos de los 3 pasos
     * @return publicación guardada con los valores calculados por la base de datos
     * @throws ReglaNegocioException si la variedad, la unidad o el distrito no existen, falta el peso de una
     *                               unidad variable o las fechas no son válidas
     */
    @Transactional
    public PublicacionDto publicar(long idAgricultor, NuevaPublicacionDto datos) {
        Preconditions.checkNotNull(datos, "datos");
        VariedadDto variedad = catalogo.buscarVariedad(datos.idVariedad())
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_VARIEDAD));
        UnidadDto unidad = catalogo.buscarUnidad(datos.idUnidad())
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_UNIDAD));
        DistritoDto distrito = catalogo.buscarDistrito(datos.idDistrito())
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_DISTRITO));

        BigDecimal peso = pesoPorUnidad(unidad, datos.pesoUnidadKg());
        validarFechas(datos.fechaCosecha(), datos.fechaLimite(), variedad.vidaUtilDias());

        Publicacion nueva = new Publicacion(idAgricultor, variedad.id(), distrito.id(), unidad.id(), datos.cantidad(),
                peso, datos.precioUnitario(), datos.fechaCosecha(), datos.fechaLimite());
        // Prioridad aprobada: PRECIO_FUERA_RANGO > CANTIDAD_INUSUAL > PRIMERA_PUBLICACION (esta última, del trigger)
        BigDecimal precioKg = datos.precioUnitario().divide(peso, 4, RoundingMode.HALF_UP);
        if (referencias.evaluar(variedad.idCultivo(), distrito.id(), precioKg).fueraDeRango()) {
            nueva.marcarParaRevision(MarcaRevision.PRECIO_FUERA_RANGO);
        } else if (esCantidadInusual(variedad.idCultivo(), distrito.id(), datos.cantidad().multiply(peso))) {
            nueva.marcarParaRevision(MarcaRevision.CANTIDAD_INUSUAL);
        }
        Publicacion publicacion = publicaciones.saveAndFlush(nueva);
        entityManager.refresh(publicacion);

        LOG.info("Cosecha publicada: id={}, agricultor={}, cultivo={}, kg={}, marca={}", publicacion.getId(),
                idAgricultor, variedad.cultivo(), publicacion.getCantidadKg(), publicacion.getMarcaRevision());
        return aDto(publicacion, variedad, unidad, distrito, disponibles.findById(publicacion.getId()).orElse(null));
    }

    /**
     * Devuelve una publicación. Solo su dueño la ve si no está PUBLICADA.
     *
     * @param idPublicacion id de la publicación
     * @param idUsuario     id del usuario de la sesión, o {@code null} si no inició sesión
     * @return publicación con sus nombres de catálogo
     * @throws RecursoNoEncontradoException si no existe o quien la pide no puede verla
     */
    @Transactional(readOnly = true)
    public PublicacionDto obtener(long idPublicacion, Long idUsuario) {
        Publicacion publicacion = publicaciones.findById(idPublicacion)
                .filter(p -> p.getEstado() == EstadoPublicacion.PUBLICADA || p.esDe(idUsuario))
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        VariedadDto variedad = catalogo.buscarVariedad(publicacion.getIdVariedad()).orElseThrow();
        UnidadDto unidad = catalogo.buscarUnidad(publicacion.getIdUnidad()).orElseThrow();
        DistritoDto distrito = catalogo.buscarDistrito(publicacion.getIdDistrito()).orElseThrow();
        return aDto(publicacion, variedad, unidad, distrito, disponibles.findById(idPublicacion).orElse(null));
    }

    /**
     * Lista las publicaciones de la moderación posterior.
     *
     * @param vista  cola "Por revisar" u ocultas
     * @param pagina página pedida
     * @return página de publicaciones con su estado y marca
     */
    @Transactional(readOnly = true)
    public Page<PublicacionModeracionDto> listarParaModeracion(VistaModeracion vista, Pageable pagina) {
        Page<Publicacion> encontradas = vista == VistaModeracion.OCULTAS
                ? publicaciones.buscarOcultas(pagina)
                : publicaciones.buscarPorRevisar(pagina);
        NombresCatalogo nombres = nombresCatalogo();
        return encontradas.map(nombres::aDtoModeracion);
    }

    /**
     * Devuelve una publicación para la moderación, leída de nuevo de la base de datos (refleja lo que acaban de
     * cambiar los triggers de {@code revisiones_publicacion} en la misma transacción).
     *
     * @param idPublicacion id de la publicación
     * @return publicación con su estado y marca actuales
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public PublicacionModeracionDto obtenerParaModeracion(long idPublicacion) {
        Publicacion publicacion = publicaciones.findById(idPublicacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        entityManager.refresh(publicacion);
        return nombresCatalogo().aDtoModeracion(publicacion);
    }

    /**
     * CANTIDAD_INUSUAL: más de 3 veces el promedio de kilos del mismo cultivo y distrito en los últimos 90 días,
     * con al menos 3 publicaciones para comparar.
     */
    private boolean esCantidadInusual(int idCultivo, int idDistrito, BigDecimal cantidadKg) {
        List<Integer> idsVariedad = catalogo.listarVariedades(idCultivo).stream().map(VariedadDto::id).toList();
        if (idsVariedad.isEmpty()) {
            return false;
        }
        EstadisticaCantidad estadistica = publicaciones.estadisticaCantidad(idsVariedad, idDistrito,
                LocalDateTime.now(reloj).minusDays(DIAS_PARA_COMPARAR));
        return estadistica != null && estadistica.promedioKg() != null
                && estadistica.publicaciones() >= MINIMO_PARA_COMPARAR
                && cantidadKg.doubleValue() > FACTOR_CANTIDAD_INUSUAL * estadistica.promedioKg();
    }

    private NombresCatalogo nombresCatalogo() {
        return new NombresCatalogo(
                catalogo.listarVariedades(null).stream().collect(Collectors.toMap(VariedadDto::id, Function.identity())),
                catalogo.listarUnidades().stream().collect(Collectors.toMap(UnidadDto::id, Function.identity())),
                catalogo.listarDistritos(null).stream().collect(Collectors.toMap(DistritoDto::id, Function.identity())));
    }

    /** Catálogos cargados una sola vez para armar varias publicaciones. */
    private record NombresCatalogo(Map<Integer, VariedadDto> variedades, Map<Integer, UnidadDto> unidades,
            Map<Integer, DistritoDto> distritos) {

        PublicacionModeracionDto aDtoModeracion(Publicacion p) {
            VariedadDto v = variedades.get(p.getIdVariedad());
            UnidadDto u = unidades.get(p.getIdUnidad());
            DistritoDto d = distritos.get(p.getIdDistrito());
            return new PublicacionModeracionDto(p.getId(), p.getIdAgricultor(), v.cultivo(), v.nombre(), u.nombre(),
                    u.nombrePlural(), p.getCantidad(), p.getPesoUnidadKg(), p.getCantidadKg(), p.getPrecioUnitario(),
                    p.getPrecioKg(), d.nombre(), d.provincia(), p.getFechaCosecha(), p.getFechaLimite(),
                    p.getEstado().name(), p.getMarcaRevision() == null ? null : p.getMarcaRevision().name(),
                    p.getFechaPublicacion());
        }
    }

    /** Peso por unidad: el del catálogo en las unidades fijas; el que indica el agricultor en jaba y saco. */
    private static BigDecimal pesoPorUnidad(UnidadDto unidad, BigDecimal pesoIndicado) {
        if (!unidad.esPesoVariable()) {
            return unidad.factorKg();
        }
        if (pesoIndicado == null || pesoIndicado.signum() <= 0) {
            throw new ReglaNegocioException(MENSAJE_PESO);
        }
        return pesoIndicado;
    }

    private void validarFechas(LocalDate cosecha, LocalDate limite, int vidaUtilDias) {
        LocalDate hoy = LocalDate.now(reloj);
        if (limite != null) {
            if (limite.isBefore(hoy)) {
                throw new ReglaNegocioException(MENSAJE_LIMITE_PASADO);
            }
            if (limite.isBefore(cosecha)) {
                throw new ReglaNegocioException(MENSAJE_LIMITE_ANTES_COSECHA);
            }
        } else if (cosecha.plusDays(vidaUtilDias).isBefore(hoy)) {
            throw new ReglaNegocioException(MENSAJE_COSECHA_VENCIDA);
        }
    }

    /** Arma el DTO; {@code vista} aporta agricultor, calificación, kilos disponibles y días restantes (RF-03). */
    private static PublicacionDto aDto(Publicacion p, VariedadDto variedad, UnidadDto unidad, DistritoDto distrito,
            PublicacionDisponible vista) {
        return new PublicacionDto(p.getId(), p.getIdAgricultor(), variedad.idCultivo(), variedad.cultivo(),
                variedad.id(), variedad.nombre(), unidad.id(), unidad.nombre(), unidad.nombrePlural(), p.getCantidad(),
                p.getPesoUnidadKg(), p.getCantidadKg(), p.getPrecioUnitario(), p.getPrecioKg(), distrito.id(),
                distrito.nombre(), distrito.provincia(), p.getFechaCosecha(), p.getFechaLimite(), p.getEstado(),
                p.getFechaPublicacion(),
                vista == null ? null : vista.getAgricultor(),
                vista == null ? null : vista.getCalificacionAgricultor(),
                vista == null ? null : vista.getDisponibleKg(),
                vista == null ? null : BusquedaService.unidadesDisponibles(vista.getDisponibleKg(), vista.getPesoUnidadKg()),
                vista == null ? null : vista.getDiasRestantes(),
                vista != null && vista.isPorVencer());
    }
}
