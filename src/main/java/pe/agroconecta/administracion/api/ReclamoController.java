package pe.agroconecta.administracion.api;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.administracion.aplicacion.ReclamoDto;
import pe.agroconecta.administracion.aplicacion.ReclamoService;

/**
 * Reclamos de las partes de una venta ({@code /api/v1/reclamos}, RF-09, UC-11 parte 3).
 */
@RestController
@RequestMapping("/api/v1/reclamos")
@PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
public class ReclamoController {

    private final ReclamoService reclamos;

    /**
     * Crea el controlador.
     *
     * @param reclamos servicio de reclamos
     */
    public ReclamoController(ReclamoService reclamos) {
        this.reclamos = reclamos;
    }

    /**
     * Registra un reclamo sobre una venta que llegó a ACORDADA.
     *
     * @param jwt     token de la sesión
     * @param request venta, motivo y descripción
     * @return 201 con el reclamo
     */
    @PostMapping
    public ResponseEntity<ReclamoDto> registrar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegistrarReclamoRequest request) {
        ReclamoDto reclamo = reclamos.registrar(Long.parseLong(jwt.getSubject()), request.idVenta(),
                request.motivo(), request.descripcion());
        return ResponseEntity.created(URI.create("/api/v1/reclamos/mios?idVenta=" + reclamo.idVenta()))
                .body(reclamo);
    }

    /**
     * Reclamos que registró el usuario.
     *
     * @param jwt     token de la sesión
     * @param idVenta solo los de esa venta (opcional)
     * @return reclamos, el más reciente primero
     */
    @GetMapping("/mios")
    public List<ReclamoDto> mios(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Long idVenta) {
        return reclamos.mios(Long.parseLong(jwt.getSubject()), idVenta);
    }
}
