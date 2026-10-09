package pe.agroconecta.notificacion.api;

import java.util.Map;

import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.notificacion.aplicacion.NotificacionDto;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;

/**
 * Centro de avisos del usuario ({@code /api/v1/notificaciones}, RF-07). Cada usuario ve solo sus avisos.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificaciones;

    /**
     * Crea el controlador.
     *
     * @param notificaciones servicio de avisos
     */
    public NotificacionController(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    /**
     * Mis avisos, el más reciente primero (20 por página).
     *
     * @param jwt  token de la sesión
     * @param page página desde 0
     * @return página de avisos
     */
    @GetMapping
    public PagedModel<NotificacionDto> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(notificaciones.listar(usuario(jwt), page));
    }

    /**
     * Cantidad de avisos sin leer.
     *
     * @param jwt token de la sesión
     * @return {@code {"total": n}}
     */
    @GetMapping("/no-leidas")
    public Map<String, Long> noLeidas(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("total", notificaciones.noLeidas(usuario(jwt)));
    }

    /**
     * Marca un aviso como leído.
     *
     * @param jwt token de la sesión
     * @param id  id del aviso
     * @return 204
     */
    @PostMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        notificaciones.marcarLeida(usuario(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marca como leídos todos mis avisos.
     *
     * @param jwt token de la sesión
     * @return {@code {"marcados": n}}
     */
    @PostMapping("/leidas")
    public Map<String, Integer> marcarTodas(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("marcados", notificaciones.marcarTodasLeidas(usuario(jwt)));
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
