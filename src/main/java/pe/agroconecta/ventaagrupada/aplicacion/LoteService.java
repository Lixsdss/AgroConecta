package pe.agroconecta.ventaagrupada.aplicacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.ventaagrupada.dominio.LoteAgrupado;
import pe.agroconecta.ventaagrupada.infraestructura.LoteAgrupadoRepository;

/**
 * "Lotes agrupados" (RF-10, UC-12 paso 1): lotes de {@code v_lotes_agrupados} y las cosechas de cada uno.
 */
@Service
@Transactional(readOnly = true)
public class LoteService {

    /** Lotes por página. */
    public static final int TAMANO_PAGINA = 20;

    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

    private final LoteAgrupadoRepository lotes;
    private final PublicacionConsultaService publicaciones;

    /**
     * Crea el servicio.
     *
     * @param lotes         vista de lotes
     * @param publicaciones cosechas de un lote
     */
    public LoteService(LoteAgrupadoRepository lotes, PublicacionConsultaService publicaciones) {
        this.lotes = lotes;
        this.publicaciones = publicaciones;
    }

    /**
     * Lotes con filtros opcionales, el de más kilos primero (20 por página).
     *
     * @param idCultivo  cultivo, o {@code null}
     * @param idDistrito distrito, o {@code null}
     * @param pagina     página desde 0
     * @return página de lotes
     * @throws SolicitudInvalidaException si la página es negativa
     */
    public Page<LoteDto> listar(Integer idCultivo, Integer idDistrito, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        return lotes.buscar(idCultivo, idDistrito, PageRequest.of(pagina, TAMANO_PAGINA)).map(LoteService::aDto);
    }

    /**
     * Cosechas de un lote ("Ver agricultores"), la que tiene más kilos primero.
     *
     * @param idVariedad variedad
     * @param idDistrito distrito
     * @return cosechas
     */
    public List<CosechaLoteDto> cosechas(int idVariedad, int idDistrito) {
        return publicaciones.cosechasDeLote(idVariedad, idDistrito).stream()
                .map(p -> new CosechaLoteDto(p.id(), p.agricultor(), p.unidad(), p.unidadPlural(),
                        p.unidadesDisponibles(), p.disponibleKg(), p.precioUnitario(), p.precioKg()))
                .toList();
    }

    static LoteDto aDto(LoteAgrupado l) {
        return new LoteDto(l.getIdCultivo(), l.getCultivo(), l.getIdVariedad(), l.getVariedad(), l.getIdDistrito(),
                l.getDistrito(), l.getTotalAgricultores(), l.getTotalPublicaciones(), l.getVolumenKg(),
                l.getPrecioPromedioKg(), l.getFechaLimiteProxima());
    }
}
