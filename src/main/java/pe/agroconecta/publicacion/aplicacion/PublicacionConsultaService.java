package pe.agroconecta.publicacion.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.publicacion.dominio.PublicacionDisponible;
import pe.agroconecta.publicacion.infraestructura.EspecificacionesBusqueda;
import pe.agroconecta.publicacion.infraestructura.PublicacionDisponibleRepository;
import pe.agroconecta.publicacion.infraestructura.PublicacionRepository;

/**
 * Consultas de solo lectura sobre publicaciones para los demás módulos (RF-04 en adelante). Devuelve DTO con el
 * estado como texto y lee la vista {@code v_publicaciones_disponibles}, que incluye todos los estados.
 */
@Service
@Transactional(readOnly = true)
public class PublicacionConsultaService {

    private final PublicacionDisponibleRepository disponibles;
    private final PublicacionRepository publicaciones;
    private final CatalogoService catalogo;
    private final EntityManager entityManager;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param disponibles   vista de publicaciones
     * @param publicaciones tabla de publicaciones
     * @param catalogo      catálogo (nombre en singular de las unidades)
     * @param entityManager contexto de persistencia (para releer los kilos disponibles tras el bloqueo)
     * @param reloj         reloj de la aplicación (cosechas vigentes de un lote)
     */
    public PublicacionConsultaService(PublicacionDisponibleRepository disponibles,
            PublicacionRepository publicaciones, CatalogoService catalogo, EntityManager entityManager, Clock reloj) {
        this.disponibles = disponibles;
        this.publicaciones = publicaciones;
        this.catalogo = catalogo;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    /**
     * Busca una publicación en cualquier estado.
     *
     * @param idPublicacion id de la publicación
     * @return la publicación, o vacío si no existe
     */
    public Optional<PublicacionResumenDto> buscar(long idPublicacion) {
        return disponibles.findById(idPublicacion).map(p -> aDto(p, unidades()));
    }

    /**
     * Bloquea la publicación ({@code PESSIMISTIC_WRITE}) hasta el fin de la transacción de quien llama y la
     * devuelve con sus kilos disponibles actuales. La usa {@code venta} antes de registrar un acuerdo (RNF-02).
     *
     * @param idPublicacion id de la publicación
     * @return la publicación, o vacío si no existe
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PublicacionResumenDto> bloquearParaVenta(long idPublicacion) {
        if (publicaciones.bloquear(idPublicacion).isEmpty()) {
            return Optional.empty();
        }
        // Relee la vista: los kilos disponibles pueden haber cambiado desde que se cargó en esta transacción
        return disponibles.findById(idPublicacion).map(p -> {
            entityManager.refresh(p);
            return aDto(p, unidades());
        });
    }

    /**
     * Cosechas de un lote de la venta agrupada (RF-10): PUBLICADA, con kilos disponibles y fecha límite vigente, de una
     * variedad en un distrito, la que tiene más kilos primero.
     *
     * @param idVariedad variedad
     * @param idDistrito distrito
     * @return cosechas del lote
     */
    public List<PublicacionResumenDto> cosechasDeLote(int idVariedad, int idDistrito) {
        Map<Integer, UnidadDto> unidades = unidades();
        return disponibles.findAll(EspecificacionesBusqueda.disponibles(LocalDate.now(reloj))
                        .and(EspecificacionesBusqueda.igual("idVariedad", idVariedad))
                        .and(EspecificacionesBusqueda.igual("idDistrito", idDistrito)),
                        Sort.by(Sort.Order.desc("disponibleKg"), Sort.Order.asc("id")))
                .stream().map(p -> aDto(p, unidades)).toList();
    }

    /**
     * Busca varias publicaciones en cualquier estado, indexadas por id. Los ids inexistentes no aparecen.
     *
     * @param ids ids de las publicaciones
     * @return publicaciones encontradas
     */
    public Map<Long, PublicacionResumenDto> buscarVarias(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, UnidadDto> unidades = unidades();
        return disponibles.findAllById(ids).stream()
                .collect(Collectors.toMap(PublicacionDisponible::getId, p -> aDto(p, unidades)));
    }

    /**
     * Devuelve los ids de todas las publicaciones de un agricultor, en cualquier estado.
     *
     * @param idAgricultor id del agricultor
     * @return ids de sus publicaciones
     */
    public List<Long> idsDeAgricultor(long idAgricultor) {
        return publicaciones.idsDeAgricultor(idAgricultor);
    }

    private Map<Integer, UnidadDto> unidades() {
        return catalogo.listarUnidades().stream().collect(Collectors.toMap(UnidadDto::id, Function.identity()));
    }

    private static PublicacionResumenDto aDto(PublicacionDisponible p, Map<Integer, UnidadDto> unidades) {
        UnidadDto unidad = unidades.get(p.getIdUnidad());
        return new PublicacionResumenDto(p.getId(), p.getIdAgricultor(), p.getAgricultor(), p.getCultivo(),
                p.getVariedad(), p.getDistrito(), p.getEstado().name(),
                unidad == null ? p.getUnidadPlural() : unidad.nombre(), p.getUnidadPlural(), p.getPesoUnidadKg(),
                p.getPrecioUnitario(), p.getPrecioKg(), p.getDisponibleKg(),
                BusquedaService.unidadesDisponibles(p.getDisponibleKg(), p.getPesoUnidadKg()), p.getDiasRestantes(),
                p.getIdUnidad());
    }
}
