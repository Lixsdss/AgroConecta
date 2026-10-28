package pe.agroconecta.usuario.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.usuario.aplicacion.PerfilDto;
import pe.agroconecta.usuario.aplicacion.PerfilService;

/**
 * Perfil del usuario de la sesión ({@code /api/v1/usuarios/me}), para cualquier rol (RF-08, UC-10).
 */
@RestController
@RequestMapping("/api/v1/usuarios/me")
@PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR', 'ADMINISTRADOR')")
public class PerfilController {

    private final PerfilService perfil;

    /**
     * Crea el controlador.
     *
     * @param perfil servicio de perfil
     */
    public PerfilController(PerfilService perfil) {
        this.perfil = perfil;
    }

    /**
     * Devuelve el perfil propio.
     *
     * @param jwt token de la sesión
     * @return perfil con distrito y subtipo
     */
    @GetMapping
    public PerfilDto obtener(@AuthenticationPrincipal Jwt jwt) {
        return perfil.obtener(idUsuario(jwt));
    }

    /**
     * Actualiza los datos editables del perfil propio.
     *
     * @param jwt     token de la sesión
     * @param request nuevos datos
     * @return perfil actualizado
     */
    @PutMapping
    public PerfilDto actualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ActualizarPerfilRequest request) {
        return perfil.actualizar(idUsuario(jwt), request.aDto());
    }

    /**
     * Cambia la contraseña propia.
     *
     * @param jwt     token de la sesión
     * @param request contraseña actual y nueva
     * @return 204 sin cuerpo
     */
    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CambiarContrasenaRequest request) {
        perfil.cambiarContrasena(idUsuario(jwt), request.contrasenaActual(), request.contrasenaNueva());
        return ResponseEntity.noContent().build();
    }

    private static long idUsuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
