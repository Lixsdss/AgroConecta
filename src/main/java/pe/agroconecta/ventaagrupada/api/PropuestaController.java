package pe.agroconecta.ventaagrupada.api;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.ventaagrupada.aplicacion.PropuestaDto;
import pe.agroconecta.ventaagrupada.aplicacion.PropuestaService;

/**
 * Propuestas conjuntas ({@code /api/v1/propuestas}, RF-10). Solo el comprador; cada uno ve las suyas.
 */
@RestController
@RequestMapping("/api/v1/propuestas")
@PreAuthorize("hasRole('COMPRADOR')")
public class PropuestaController {

    private final PropuestaService propuestas;

    /**
     * Crea el controlador.
     *
     * @param propuestas servicio de propuestas
     */
    public PropuestaController(PropuestaService propuestas) {
        this.propuestas = propuestas;
    }

    /**
     * "Enviar propuesta conjunta": reparte los kilos y crea una parte por cosecha.
     *
     * @param jwt     token del comprador
     * @param request lote, kilos, precio y día de recojo
     * @return 201 con {@code Location} y la propuesta
     */
    @PostMapping
    public ResponseEntity<PropuestaDto> crear(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CrearPropuestaRequest request) {
        PropuestaDto propuesta = propuestas.crear(usuario(jwt), request.aDto());
        return ResponseEntity.created(URI.create("/api/v1/propuestas/" + propuesta.id())).body(propuesta);
    }

    /**
     * "Mis propuestas" (20 por página).
     *
     * @param jwt  token del comprador
     * @param page página desde 0
     * @return página de propuestas
     */
    @GetMapping
    public PagedModel<PropuestaDto> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(propuestas.listar(usuario(jwt), page));
    }

    /**
     * Avance de una propuesta.
     *
     * @param jwt token del comprador
     * @param id  id de la propuesta
     * @return la propuesta
     */
    @GetMapping("/{id}")
    public PropuestaDto obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return propuestas.obtener(usuario(jwt), id);
    }

    /**
     * Cierra la propuesta y cancela las partes pendientes.
     *
     * @param jwt token del comprador
     * @param id  id de la propuesta
     * @return la propuesta
     */
    @PostMapping("/{id}/cerrar")
    public PropuestaDto cerrar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return propuestas.terminar(usuario(jwt), id, true);
    }

    /**
     * Cancela la propuesta y las partes pendientes.
     *
     * @param jwt token del comprador
     * @param id  id de la propuesta
     * @return la propuesta
     */
    @PostMapping("/{id}/cancelar")
    public PropuestaDto cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return propuestas.terminar(usuario(jwt), id, false);
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
