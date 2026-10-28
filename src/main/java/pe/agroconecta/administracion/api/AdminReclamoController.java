package pe.agroconecta.administracion.api;

import jakarta.validation.Valid;

import org.springframework.data.web.PagedModel;
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

import pe.agroconecta.administracion.aplicacion.ReclamoDetalleDto;
import pe.agroconecta.administracion.aplicacion.ReclamoDto;
import pe.agroconecta.administracion.aplicacion.ReclamoService;
import pe.agroconecta.administracion.dominio.EstadoReclamo;

/**
 * Reclamos para el administrador ({@code /api/v1/admin/reclamos}, RF-09, UC-11 parte 3).
 */
@RestController
@RequestMapping("/api/v1/admin/reclamos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReclamoController {

    private final ReclamoService reclamos;

    /**
     * Crea el controlador.
     *
     * @param reclamos servicio de reclamos
     */
    public AdminReclamoController(ReclamoService reclamos) {
        this.reclamos = reclamos;
    }

    /**
     * Lista los reclamos por estado.
     *
     * @param estado estado (todos si se omite)
     * @param page   página desde 0
     * @return página de reclamos
     */
    @GetMapping
    public PagedModel<ReclamoDto> listar(@RequestParam(required = false) EstadoReclamo estado,
            @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(reclamos.listar(estado, page));
    }

    /**
     * Reclamo con su venta y la línea de tiempo.
     *
     * @param id id del reclamo
     * @return el detalle
     */
    @GetMapping("/{id}")
    public ReclamoDetalleDto obtener(@PathVariable long id) {
        return reclamos.obtener(id);
    }

    /**
     * Resuelve un reclamo pendiente.
     *
     * @param jwt     token del administrador
     * @param id      id del reclamo
     * @param request resultado y respuesta
     * @return el reclamo resuelto
     */
    @PostMapping("/{id}/resolucion")
    public ReclamoDto resolver(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody ResolverReclamoRequest request) {
        return reclamos.resolver(Long.parseLong(jwt.getSubject()), id, request.estado(), request.respuesta());
    }
}
