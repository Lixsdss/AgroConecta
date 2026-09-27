package pe.agroconecta.catalogo.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.catalogo.aplicacion.ProvinciaDto;

/**
 * Endpoints públicos de los catálogos de ubicación ({@code /api/v1/catalogo}).
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
}
