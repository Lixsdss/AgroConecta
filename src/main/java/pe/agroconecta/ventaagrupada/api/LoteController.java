package pe.agroconecta.ventaagrupada.api;

import java.util.List;

import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.ventaagrupada.aplicacion.CosechaLoteDto;
import pe.agroconecta.ventaagrupada.aplicacion.LoteDto;
import pe.agroconecta.ventaagrupada.aplicacion.LoteService;

/**
 * "Lotes agrupados" ({@code /api/v1/lotes}, RF-10). Solo el comprador.
 */
@RestController
@RequestMapping("/api/v1/lotes")
@PreAuthorize("hasRole('COMPRADOR')")
public class LoteController {

    private final LoteService lotes;

    /**
     * Crea el controlador.
     *
     * @param lotes servicio de lotes
     */
    public LoteController(LoteService lotes) {
        this.lotes = lotes;
    }

    /**
     * Lotes con filtros opcionales (20 por página).
     *
     * @param idCultivo  cultivo
     * @param idDistrito distrito
     * @param page       página desde 0
     * @return página de lotes
     */
    @GetMapping
    public PagedModel<LoteDto> listar(@RequestParam(required = false) Integer idCultivo,
            @RequestParam(required = false) Integer idDistrito, @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(lotes.listar(idCultivo, idDistrito, page));
    }

    /**
     * Cosechas de un lote ("Ver agricultores").
     *
     * @param idVariedad variedad
     * @param idDistrito distrito
     * @return cosechas del lote
     */
    @GetMapping("/agricultores")
    public List<CosechaLoteDto> agricultores(@RequestParam int idVariedad, @RequestParam int idDistrito) {
        return lotes.cosechas(idVariedad, idDistrito);
    }
}
