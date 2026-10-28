package pe.agroconecta.administracion.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
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

import pe.agroconecta.administracion.aplicacion.DecisionDelDiaDto;
import pe.agroconecta.administracion.aplicacion.DecisionRegistradaDto;
import pe.agroconecta.administracion.aplicacion.ModeracionService;
import pe.agroconecta.administracion.aplicacion.PublicacionEnRevisionDto;
import pe.agroconecta.publicacion.aplicacion.VistaModeracion;

/**
 * Moderación posterior de publicaciones ({@code /api/v1/moderacion}, RF-09). Solo para administradores.
 */
@RestController
@RequestMapping("/api/v1/moderacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ModeracionController {

    private static final int TAMANO_MAXIMO = 50;

    private final ModeracionService moderacion;

    /**
     * Crea el controlador.
     *
     * @param moderacion servicio de moderación
     */
    public ModeracionController(ModeracionService moderacion) {
        this.moderacion = moderacion;
    }

    /**
     * Lista la cola "Por revisar" o las publicaciones ocultas.
     *
     * @param vista listado pedido ({@code POR_REVISAR} por defecto)
     * @param page  número de página (desde 0)
     * @param size  tamaño de página (máx. 50)
     * @return página de publicaciones con su agricultor
     */
    @GetMapping("/publicaciones")
    public PagedModel<PublicacionEnRevisionDto> listar(
            @RequestParam(defaultValue = "POR_REVISAR") VistaModeracion vista,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, TAMANO_MAXIMO));
        return new PagedModel<>(moderacion.listar(vista, pagina));
    }

    /**
     * Lista las decisiones de un día.
     *
     * @param fecha día consultado (hoy por defecto)
     * @return decisiones del día, la más reciente primero
     */
    @GetMapping("/decisiones")
    public List<DecisionDelDiaDto> decisiones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return moderacion.decisionesDelDia(fecha == null ? LocalDate.now() : fecha);
    }

    /**
     * Registra una decisión de moderación sobre una publicación.
     *
     * @param id      id de la publicación
     * @param jwt     token del administrador
     * @param request decisión, motivo y mensaje
     * @return 201 con la revisión registrada y el estado resultante
     */
    @PostMapping("/publicaciones/{id}/decisiones")
    public ResponseEntity<DecisionRegistradaDto> decidir(@PathVariable long id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DecisionRequest request) {
        DecisionRegistradaDto registrada = moderacion.decidir(Long.parseLong(jwt.getSubject()), id,
                request.decision(), request.motivo(), request.mensaje());
        URI ubicacion = URI.create("/api/v1/moderacion/decisiones?fecha=" + LocalDate.now());
        return ResponseEntity.created(ubicacion).body(registrada);
    }
}
