package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.publicacion.dominio.EstadoPublicacion;
import pe.agroconecta.publicacion.dominio.Publicacion;
import pe.agroconecta.publicacion.dominio.PublicacionDisponible;
import pe.agroconecta.publicacion.infraestructura.PublicacionDisponibleRepository;
import pe.agroconecta.publicacion.infraestructura.PublicacionRepository;

/**
 * "Mis ventas" del agricultor (RF-07, UC-09): sus cosechas y las acciones rápidas bajar el precio, extender y
 * retirar, más la corrección de una cosecha oculta por la moderación (RF-09). Las reglas de estado las repite
 * {@code trg_publicaciones_bu} (última barrera); este módulo nunca cambia el estado de una BLOQUEADA.
 */
@Service
public class MisCosechasService {

    /** Cosechas por página. */
    public static final int TAMANO_PAGINA = 20;
    /** Días que se puede extender (UC-09 paso 3). */
    public static final Set<Integer> DIAS_EXTENSION = Set.of(7, 14, 30);

    static final String MENSAJE_NO_EXISTE = "La publicación no existe.";
    static final String MENSAJE_GRUPO = "El grupo debe ser ACTIVAS o CERRADAS.";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";
    static final String MENSAJE_DIAS = "Elija extender 7, 14 o 30 días.";
    static final String MENSAJE_PRECIO_MAYOR = "El nuevo precio debe ser menor que el actual (S/ %s).";
    static final String MENSAJE_PRECIO_ESTADO = "Solo se baja el precio de una cosecha que está a la venta.";
    static final String MENSAJE_EXTENDER_ESTADO = "Solo se extiende una cosecha a la venta o vencida.";
    static final String MENSAJE_RETIRAR_OCULTA =
            "Una cosecha oculta por la moderación no se retira; corríjala para que la revisen.";
    static final String MENSAJE_RETIRAR_ESTADO = "Esta cosecha ya no se puede retirar.";
    static final String MENSAJE_CORREGIR_ESTADO = "Solo se corrige una cosecha oculta por la moderación.";
    static final String MENSAJE_SIN_CAMBIOS = "No cambió ningún dato.";

    private static final Set<EstadoPublicacion> ACTIVAS =
            EnumSet.of(EstadoPublicacion.PUBLICADA, EstadoPublicacion.BLOQUEADA, EstadoPublicacion.VENCIDA);
    private static final Set<EstadoPublicacion> CERRADAS =
            EnumSet.of(EstadoPublicacion.VENDIDA, EstadoPublicacion.RETIRADA);
    private static final Logger LOG = LoggerFactory.getLogger(MisCosechasService.class);

    private final PublicacionRepository publicaciones;
    private final PublicacionDisponibleRepository disponibles;
    private final CatalogoService catalogo;
    private final EntityManager entityManager;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param publicaciones tabla de publicaciones
     * @param disponibles   vista de publicaciones (datos para mostrar)
     * @param catalogo      unidades
     * @param entityManager contexto de persistencia (para releer lo que cambió la base de datos)
     * @param reloj         reloj de la aplicación
     */
    public MisCosechasService(PublicacionRepository publicaciones, PublicacionDisponibleRepository disponibles,
            CatalogoService catalogo, EntityManager entityManager, Clock reloj) {
        this.publicaciones = publicaciones;
        this.disponibles = disponibles;
        this.catalogo = catalogo;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    /**
     * Cosechas del agricultor. ACTIVAS: la que vence antes primero; CERRADAS: la más reciente primero.
     *
     * @param idAgricultor agricultor de la sesión
     * @param grupo        {@code ACTIVAS} (por defecto) o {@code CERRADAS}
     * @param pagina       página desde 0 (20 por página)
     * @return página de cosechas
     * @throws SolicitudInvalidaException si el grupo o la página no son válidos
     */
    @Transactional(readOnly = true)
    public Page<MiCosechaDto> listar(long idAgricultor, String grupo, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        boolean cerradas = "CERRADAS".equals(grupo);
        if (StringUtils.isNotBlank(grupo) && !cerradas && !"ACTIVAS".equals(grupo)) {
            throw new SolicitudInvalidaException(MENSAJE_GRUPO);
        }
        Sort orden = cerradas
                ? Sort.by(Sort.Order.desc("fechaPublicacion"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.asc("fechaLimite"), Sort.Order.desc("id"));
        Map<Integer, UnidadDto> unidades = unidades();
        return disponibles.findByIdAgricultorAndEstadoIn(idAgricultor, cerradas ? CERRADAS : ACTIVAS,
                PageRequest.of(pagina, TAMANO_PAGINA, orden)).map(p -> aDto(p, unidades));
    }

    /**
     * "Bajar el precio": solo en una cosecha PUBLICADA y a un precio menor al actual.
     *
     * @param idAgricultor   agricultor de la sesión
     * @param idPublicacion  publicación
     * @param precioUnitario nuevo precio por unidad
     * @return la cosecha actualizada
     */
    @Transactional
    public MiCosechaDto bajarPrecio(long idAgricultor, long idPublicacion, BigDecimal precioUnitario) {
        Preconditions.checkNotNull(precioUnitario, "precioUnitario");
        Publicacion p = propia(idAgricultor, idPublicacion);
        if (p.getEstado() != EstadoPublicacion.PUBLICADA) {
            throw new ReglaNegocioException(MENSAJE_PRECIO_ESTADO);
        }
        if (precioUnitario.compareTo(p.getPrecioUnitario()) >= 0) {
            throw new ReglaNegocioException(MENSAJE_PRECIO_MAYOR.formatted(
                    p.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }
        p.cambiarPrecio(precioUnitario);
        return guardar(p, "precio bajado");
    }

    /**
     * "Extender": la nueva fecha límite es la mayor entre la actual y hoy, más 7, 14 o 30 días. Una VENCIDA vuelve a
     * PUBLICADA por {@code trg_publicaciones_bu}.
     *
     * @param idAgricultor  agricultor de la sesión
     * @param idPublicacion publicación
     * @param dias          7, 14 o 30
     * @return la cosecha actualizada
     */
    @Transactional
    public MiCosechaDto extender(long idAgricultor, long idPublicacion, int dias) {
        if (!DIAS_EXTENSION.contains(dias)) {
            throw new SolicitudInvalidaException(MENSAJE_DIAS);
        }
        Publicacion p = propia(idAgricultor, idPublicacion);
        if (p.getEstado() != EstadoPublicacion.PUBLICADA && p.getEstado() != EstadoPublicacion.VENCIDA) {
            throw new ReglaNegocioException(MENSAJE_EXTENDER_ESTADO);
        }
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate base = p.getFechaLimite().isAfter(hoy) ? p.getFechaLimite() : hoy;
        p.extenderHasta(base.plusDays(dias));
        return guardar(p, "extendida " + dias + " días");
    }

    /**
     * "Retirar": PUBLICADA o VENCIDA pasa a RETIRADA. Las ventas ya acordadas siguen su curso.
     *
     * @param idAgricultor  agricultor de la sesión
     * @param idPublicacion publicación
     * @return la cosecha actualizada
     */
    @Transactional
    public MiCosechaDto retirar(long idAgricultor, long idPublicacion) {
        Publicacion p = propia(idAgricultor, idPublicacion);
        if (p.getEstado() == EstadoPublicacion.BLOQUEADA) {
            throw new ReglaNegocioException(MENSAJE_RETIRAR_OCULTA);
        }
        if (p.getEstado() != EstadoPublicacion.PUBLICADA && p.getEstado() != EstadoPublicacion.VENCIDA) {
            throw new ReglaNegocioException(MENSAJE_RETIRAR_ESTADO);
        }
        p.retirar();
        return guardar(p, "retirada");
    }

    /**
     * "Corregir" una cosecha oculta por la moderación: cantidad, peso por unidad (jaba o saco) y precio. La base de
     * datos la marca CORREGIDA y vuelve a la cola "Por revisar"; sigue oculta.
     *
     * @param idAgricultor   agricultor de la sesión
     * @param idPublicacion  publicación
     * @param cantidad       cantidad en la unidad publicada
     * @param pesoUnidadKg   kilos por unidad (solo jaba o saco), o {@code null}
     * @param precioUnitario precio por unidad
     * @return la cosecha actualizada
     */
    @Transactional
    public MiCosechaDto corregir(long idAgricultor, long idPublicacion, BigDecimal cantidad, BigDecimal pesoUnidadKg,
            BigDecimal precioUnitario) {
        Preconditions.checkNotNull(cantidad, "cantidad");
        Preconditions.checkNotNull(precioUnitario, "precioUnitario");
        Publicacion p = propia(idAgricultor, idPublicacion);
        if (p.getEstado() != EstadoPublicacion.BLOQUEADA) {
            throw new ReglaNegocioException(MENSAJE_CORREGIR_ESTADO);
        }
        boolean pesoVariable = catalogo.buscarUnidad(p.getIdUnidad()).map(UnidadDto::esPesoVariable).orElse(false);
        BigDecimal peso = pesoVariable ? pesoUnidadKg : null;
        boolean cambia = cantidad.compareTo(p.getCantidad()) != 0
                || precioUnitario.compareTo(p.getPrecioUnitario()) != 0
                || (peso != null && peso.compareTo(p.getPesoUnidadKg()) != 0);
        if (!cambia) {
            throw new ReglaNegocioException(MENSAJE_SIN_CAMBIOS);
        }
        p.corregir(cantidad, peso, precioUnitario);
        return guardar(p, "corregida (vuelve a la cola de moderación)");
    }

    /** Publicación del agricultor; 404 si no existe, 403 si es de otro. */
    private Publicacion propia(long idAgricultor, long idPublicacion) {
        Publicacion p = publicaciones.findById(idPublicacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        if (!p.esDe(idAgricultor)) {
            LOG.warn("Acción negada sobre la publicación {} para el usuario {}", idPublicacion, idAgricultor);
            throw new AccessDeniedException("No es dueño de la publicación");
        }
        return p;
    }

    /** Guarda (el trigger valida y puede cambiar el estado o la marca) y lee el resultado desde la vista. */
    private MiCosechaDto guardar(Publicacion p, String detalle) {
        publicaciones.saveAndFlush(p);
        LOG.info("Publicación {}: {}", p.getId(), detalle);
        return disponibles.findById(p.getId()).map(v -> {
            entityManager.refresh(v); // El trigger pudo cambiar estado, marca o peso
            return aDto(v, unidades());
        }).orElseThrow();
    }

    /**
     * Acciones del agricultor según el estado.
     *
     * @param estado estado de la publicación
     * @return acciones permitidas
     */
    static List<AccionCosecha> acciones(EstadoPublicacion estado) {
        return switch (estado) {
            case PUBLICADA -> List.of(AccionCosecha.BAJAR_PRECIO, AccionCosecha.EXTENDER, AccionCosecha.RETIRAR);
            case VENCIDA -> List.of(AccionCosecha.EXTENDER, AccionCosecha.RETIRAR);
            case BLOQUEADA -> List.of(AccionCosecha.CORREGIR);
            default -> List.of();
        };
    }

    private Map<Integer, UnidadDto> unidades() {
        return catalogo.listarUnidades().stream().collect(Collectors.toMap(UnidadDto::id, Function.identity()));
    }

    private static MiCosechaDto aDto(PublicacionDisponible p, Map<Integer, UnidadDto> unidades) {
        UnidadDto unidad = unidades.get(p.getIdUnidad());
        return new MiCosechaDto(p.getId(), p.getCultivo(), p.getVariedad(), p.getDistrito(),
                unidad == null ? p.getUnidadPlural() : unidad.nombre(), p.getUnidadPlural(), p.getCantidad(),
                p.getPesoUnidadKg(), p.getCantidadKg(), p.getDisponibleKg(),
                BusquedaService.unidadesDisponibles(p.getDisponibleKg(), p.getPesoUnidadKg()), p.getPrecioUnitario(),
                p.getPrecioKg(), p.getFechaCosecha(), p.getFechaLimite(), p.getDiasRestantes(), p.isPorVencer(),
                p.getEstado().name(), p.getFechaPublicacion(), acciones(p.getEstado()));
    }
}
