package pe.agroconecta.publicacion.aplicacion;

import static pe.agroconecta.publicacion.infraestructura.EspecificacionesBusqueda.cosechaEntre;
import static pe.agroconecta.publicacion.infraestructura.EspecificacionesBusqueda.disponibles;
import static pe.agroconecta.publicacion.infraestructura.EspecificacionesBusqueda.igual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.publicacion.dominio.PublicacionDisponible;
import pe.agroconecta.publicacion.infraestructura.PublicacionDisponibleRepository;

/**
 * Búsqueda multicriterio de publicaciones (RF-03, UC-03). Siempre muestra solo publicaciones PUBLICADA, con kilos
 * disponibles y con fecha límite hoy o después; los filtros del usuario se suman a esa condición.
 */
@Service
public class BusquedaService {

    /** Resultados por página (SRS RF-03). */
    public static final int TAMANO_PAGINA = 20;

    static final String MENSAJE_RANGO_FECHAS = "La fecha «desde» no puede ser posterior a la fecha «hasta».";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

    private final PublicacionDisponibleRepository disponibles;
    private final CatalogoService catalogo;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param disponibles vista de publicaciones disponibles
     * @param catalogo    catálogo (nombre en singular de las unidades)
     * @param reloj       reloj de la aplicación
     */
    public BusquedaService(PublicacionDisponibleRepository disponibles, CatalogoService catalogo, Clock reloj) {
        this.disponibles = disponibles;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    /**
     * Busca publicaciones disponibles.
     *
     * @param filtro filtros opcionales
     * @param orden  orden de los resultados ({@code null}: precio por kilo)
     * @param pagina número de página desde 0 (20 resultados por página)
     * @return página de resultados
     * @throws SolicitudInvalidaException si el rango de fechas está invertido o la página es negativa
     */
    @Transactional(readOnly = true)
    public Page<PublicacionDisponibleDto> buscar(FiltroBusquedaDto filtro, OrdenBusqueda orden, int pagina) {
        Preconditions.checkNotNull(filtro, "filtro");
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        if (filtro.cosechaDesde() != null && filtro.cosechaHasta() != null
                && filtro.cosechaDesde().isAfter(filtro.cosechaHasta())) {
            throw new SolicitudInvalidaException(MENSAJE_RANGO_FECHAS);
        }
        Specification<PublicacionDisponible> condicion = disponibles(LocalDate.now(reloj))
                .and(igual("idCultivo", filtro.idCultivo()))
                .and(igual("idVariedad", filtro.idVariedad()))
                .and(igual("idProvincia", filtro.idProvincia()))
                .and(igual("idDistrito", filtro.idDistrito()))
                .and(igual("idUnidad", filtro.idUnidad()))
                .and(cosechaEntre(filtro.cosechaDesde(), filtro.cosechaHasta()));
        OrdenBusqueda elegido = orden == null ? OrdenBusqueda.PRECIO_KG : orden;

        Map<Integer, UnidadDto> unidades = catalogo.listarUnidades().stream()
                .collect(Collectors.toMap(UnidadDto::id, Function.identity()));
        return disponibles.findAll(condicion, PageRequest.of(pagina, TAMANO_PAGINA, elegido.comoSort()))
                .map(p -> aDto(p, unidades.get(p.getIdUnidad())));
    }

    /**
     * Unidades disponibles: kilos disponibles entre el peso por unidad, redondeado hacia abajo a 2 decimales.
     *
     * @param disponibleKg kilos disponibles
     * @param pesoUnidadKg kilos por unidad
     * @return unidades disponibles
     */
    static BigDecimal unidadesDisponibles(BigDecimal disponibleKg, BigDecimal pesoUnidadKg) {
        return disponibleKg.divide(pesoUnidadKg, 2, RoundingMode.DOWN);
    }

    private static PublicacionDisponibleDto aDto(PublicacionDisponible p, UnidadDto unidad) {
        return new PublicacionDisponibleDto(p.getId(), p.getIdAgricultor(), p.getAgricultor(),
                p.getCalificacionAgricultor(), p.getIdCultivo(), p.getCultivo(), p.getIdVariedad(), p.getVariedad(),
                p.getIdUnidad(), unidad == null ? p.getUnidadPlural() : unidad.nombre(), p.getUnidadPlural(),
                p.getCantidad(), p.getPesoUnidadKg(), p.getCantidadKg(), p.getDisponibleKg(),
                unidadesDisponibles(p.getDisponibleKg(), p.getPesoUnidadKg()), p.getPrecioUnitario(),
                p.getPrecioKg(), p.getIdDistrito(), p.getDistrito(), p.getIdProvincia(), p.getProvincia(),
                p.getFechaCosecha(), p.getFechaLimite(), p.getDiasRestantes(), p.isPorVencer(),
                p.getFechaPublicacion());
    }
}
