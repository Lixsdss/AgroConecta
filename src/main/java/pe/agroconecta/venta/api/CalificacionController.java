package pe.agroconecta.venta.api;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.venta.aplicacion.CalificacionService;
import pe.agroconecta.venta.aplicacion.VentaDetalleDto;

/**
 * Calificaciones y reputación (RF-05): calificar una venta terminada y consultar la reputación pública.
 */
@RestController
public class CalificacionController {

    private final CalificacionService calificaciones;

    /**
     * Crea el controlador.
     *
     * @param calificaciones servicio de calificaciones
     */
    public CalificacionController(CalificacionService calificaciones) {
        this.calificaciones = calificaciones;
    }

    /**
     * Califica a la otra parte de una venta COMPLETADA (una sola vez).
     *
     * @param jwt     token de la sesión
     * @param id      id de la venta
     * @param request estrellas y comentario
     * @return 201 con la venta y sus calificaciones
     */
    @PostMapping("/api/v1/ventas/{id}/calificacion")
    @PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
    public ResponseEntity<VentaDetalleDto> calificar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody CalificarRequest request) {
        VentaDetalleDto venta = calificaciones.calificar(Long.parseLong(jwt.getSubject()), id, request.puntuacion(),
                request.comentario());
        return ResponseEntity.created(URI.create("/api/v1/ventas/" + id)).body(venta);
    }

    /**
     * Reputación pública de un agricultor o comprador (10 calificaciones por página).
     *
     * @param idUsuario usuario
     * @param page      página desde 0
     * @return promedio, total y calificaciones recibidas
     */
    @GetMapping("/api/v1/reputaciones/{idUsuario}")
    public ReputacionResponse reputacion(@PathVariable long idUsuario, @RequestParam(defaultValue = "0") int page) {
        return ReputacionResponse.de(calificaciones.reputacion(idUsuario, page));
    }
}
