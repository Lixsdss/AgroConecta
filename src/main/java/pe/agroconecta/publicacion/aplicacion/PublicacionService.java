package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.catalogo.aplicacion.VariedadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.publicacion.dominio.EstadoPublicacion;
import pe.agroconecta.publicacion.dominio.Publicacion;
import pe.agroconecta.publicacion.infraestructura.PublicacionRepository;

/**
 * Publicación de cosechas (RF-01, UC-01). Valida con mensajes amigables lo mismo que {@code trg_publicaciones_bi}
 * (la base de datos es la última barrera) y refresca la entidad para leer lo que calcula la base de datos:
 * peso de las unidades fijas, fecha límite, marca de moderación y columnas generadas.
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

    private static final Logger LOG = LoggerFactory.getLogger(PublicacionService.class);

    private final PublicacionRepository publicaciones;
    private final CatalogoService catalogo;
    private final EntityManager entityManager;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param publicaciones repositorio de publicaciones
     * @param catalogo      catálogo de variedades, unidades y distritos
     * @param entityManager contexto de persistencia (para refrescar lo que calcula la base de datos)
     * @param reloj         reloj de la aplicación
     */
    public PublicacionService(PublicacionRepository publicaciones, CatalogoService catalogo,
            EntityManager entityManager, Clock reloj) {
        this.publicaciones = publicaciones;
        this.catalogo = catalogo;
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

        Publicacion publicacion = publicaciones.saveAndFlush(new Publicacion(idAgricultor, variedad.id(),
                distrito.id(), unidad.id(), datos.cantidad(), peso, datos.precioUnitario(), datos.fechaCosecha(),
                datos.fechaLimite()));
        entityManager.refresh(publicacion);

        LOG.info("Cosecha publicada: id={}, agricultor={}, cultivo={}, kg={}, marca={}", publicacion.getId(),
                idAgricultor, variedad.cultivo(), publicacion.getCantidadKg(), publicacion.getMarcaRevision());
        return aDto(publicacion, variedad, unidad, distrito);
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
        return aDto(publicacion, variedad, unidad, distrito);
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

    private static PublicacionDto aDto(Publicacion p, VariedadDto variedad, UnidadDto unidad, DistritoDto distrito) {
        return new PublicacionDto(p.getId(), p.getIdAgricultor(), variedad.idCultivo(), variedad.cultivo(),
                variedad.id(), variedad.nombre(), unidad.id(), unidad.nombre(), unidad.nombrePlural(), p.getCantidad(),
                p.getPesoUnidadKg(), p.getCantidadKg(), p.getPrecioUnitario(), p.getPrecioKg(), distrito.id(),
                distrito.nombre(), distrito.provincia(), p.getFechaCosecha(), p.getFechaLimite(), p.getEstado(),
                p.getFechaPublicacion());
    }
}
