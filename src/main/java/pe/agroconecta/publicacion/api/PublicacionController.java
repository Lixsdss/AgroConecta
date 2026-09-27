package pe.agroconecta.publicacion.api;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.publicacion.aplicacion.PublicacionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionService;

/**
 * Publicaciones de cosechas ({@code /api/v1/publicaciones}, RF-01).
 */
@RestController
@RequestMapping("/api/v1/publicaciones")
public class PublicacionController {

    private final PublicacionService publicaciones;

    /**
     * Crea el controlador.
     *
     * @param publicaciones servicio de publicaciones
     */
    public PublicacionController(PublicacionService publicaciones) {
        this.publicaciones = publicaciones;
    }

    /**
     * Publica una cosecha del agricultor de la sesión.
     *
     * @param jwt     token de la sesión
     * @param request datos de los 3 pasos
     * @return 201 con la publicación y {@code Location}
     */
    @PostMapping
    @PreAuthorize("hasRole('AGRICULTOR')")
    public ResponseEntity<PublicacionDto> publicar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PublicarRequest request) {
        PublicacionDto creada = publicaciones.publicar(Long.parseLong(jwt.getSubject()), request.aDto());
        return ResponseEntity.created(URI.create("/api/v1/publicaciones/" + creada.id())).body(creada);
    }

    /**
     * Devuelve una publicación PUBLICADA, o de cualquier estado si la pide su dueño.
     *
     * @param id  id de la publicación
     * @param jwt token de la sesión, o {@code null} si no inició sesión
     * @return publicación
     */
    @GetMapping("/{id}")
    public PublicacionDto obtener(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return publicaciones.obtener(id, jwt == null ? null : Long.valueOf(jwt.getSubject()));
    }
}
