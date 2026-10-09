package pe.agroconecta.publicacion.api;

import jakarta.validation.Valid;

import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.publicacion.aplicacion.MiCosechaDto;
import pe.agroconecta.publicacion.aplicacion.MisCosechasService;

/**
 * "Mis ventas" del agricultor y acciones rápidas ({@code /api/v1/publicaciones}, RF-07; corrección de RF-09).
 * Solo el dueño de la publicación.
 */
@RestController
@RequestMapping("/api/v1/publicaciones")
@PreAuthorize("hasRole('AGRICULTOR')")
public class MisCosechasController {

    private final MisCosechasService cosechas;

    /**
     * Crea el controlador.
     *
     * @param cosechas servicio de mis cosechas
     */
    public MisCosechasController(MisCosechasService cosechas) {
        this.cosechas = cosechas;
    }

    /**
     * Mis cosechas (20 por página).
     *
     * @param jwt   token de la sesión
     * @param grupo {@code ACTIVAS} (por defecto) o {@code CERRADAS}
     * @param page  página desde 0
     * @return página de cosechas
     */
    @GetMapping("/mias")
    public PagedModel<MiCosechaDto> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String grupo, @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(cosechas.listar(usuario(jwt), grupo, page));
    }

    /**
     * "Bajar el precio".
     *
     * @param jwt     token de la sesión
     * @param id      id de la publicación
     * @param request nuevo precio
     * @return la cosecha actualizada
     */
    @PostMapping("/{id}/precio")
    public MiCosechaDto bajarPrecio(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody BajarPrecioRequest request) {
        return cosechas.bajarPrecio(usuario(jwt), id, request.precioUnitario());
    }

    /**
     * "Extender" 7, 14 o 30 días.
     *
     * @param jwt     token de la sesión
     * @param id      id de la publicación
     * @param request días
     * @return la cosecha actualizada
     */
    @PostMapping("/{id}/extender")
    public MiCosechaDto extender(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody ExtenderRequest request) {
        return cosechas.extender(usuario(jwt), id, request.dias());
    }

    /**
     * "Retirar".
     *
     * @param jwt token de la sesión
     * @param id  id de la publicación
     * @return la cosecha actualizada
     */
    @PostMapping("/{id}/retirar")
    public MiCosechaDto retirar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return cosechas.retirar(usuario(jwt), id);
    }

    /**
     * "Corregir" una cosecha oculta por la moderación.
     *
     * @param jwt     token de la sesión
     * @param id      id de la publicación
     * @param request cantidad, peso por unidad y precio
     * @return la cosecha actualizada
     */
    @PutMapping("/{id}/correccion")
    public MiCosechaDto corregir(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody CorregirRequest request) {
        return cosechas.corregir(usuario(jwt), id, request.cantidad(), request.pesoUnidadKg(),
                request.precioUnitario());
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
