package pe.agroconecta.catalogo.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.CultivoDto;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.catalogo.aplicacion.ProvinciaDto;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.catalogo.aplicacion.VariedadDto;

/**
 * Endpoints públicos de los catálogos ({@code /api/v1/catalogo}): ubicación, cultivos, variedades y unidades.
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {

    private final CatalogoService catalogo;

    /**
     * Crea el controlador.
     *
     * @param catalogo servicio de catálogos
     */
    public CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    /**
     * Lista las provincias de Piura.
     *
     * @return provincias ordenadas por nombre
     */
    @GetMapping("/provincias")
    public List<ProvinciaDto> listarProvincias() {
        return catalogo.listarProvincias();
    }

    /**
     * Lista los distritos de Piura, todos o los de una provincia.
     *
     * @param idProvincia id de la provincia (opcional)
     * @return distritos ordenados por provincia y nombre
     */
    @GetMapping("/distritos")
    public List<DistritoDto> listarDistritos(@RequestParam(required = false) Integer idProvincia) {
        return catalogo.listarDistritos(idProvincia);
    }

    /**
     * Lista los cultivos activos.
     *
     * @return cultivos con su unidad local y peso sugerido
     */
    @GetMapping("/cultivos")
    public List<CultivoDto> listarCultivos() {
        return catalogo.listarCultivos();
    }

    /**
     * Lista las variedades, todas o las de un cultivo.
     *
     * @param idCultivo id del cultivo (opcional)
     * @return variedades ordenadas por nombre
     */
    @GetMapping("/variedades")
    public List<VariedadDto> listarVariedades(@RequestParam(required = false) Integer idCultivo) {
        return catalogo.listarVariedades(idCultivo);
    }

    /**
     * Lista las unidades de venta.
     *
     * @return unidades del catálogo
     */
    @GetMapping("/unidades")
    public List<UnidadDto> listarUnidades() {
        return catalogo.listarUnidades();
    }
}
