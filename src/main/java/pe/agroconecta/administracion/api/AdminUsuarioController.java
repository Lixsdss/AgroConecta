package pe.agroconecta.administracion.api;

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

import pe.agroconecta.administracion.aplicacion.UsuarioAdminDto;
import pe.agroconecta.administracion.aplicacion.UsuarioAdminService;
import pe.agroconecta.administracion.aplicacion.UsuarioDetalleDto;

/**
 * Gestión de usuarios ({@code /api/v1/admin}, RF-08 y RF-09, UC-11 parte 1). Solo para administradores.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

    private final UsuarioAdminService usuarios;

    /**
     * Crea el controlador.
     *
     * @param usuarios servicio de usuarios para el administrador
     */
    public AdminUsuarioController(UsuarioAdminService usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * Lista usuarios con filtros opcionales.
     *
     * @param rol    rol
     * @param estado estado
     * @param q      texto en el nombre, el negocio o el celular
     * @param page   página desde 0
     * @return página de usuarios
     */
    @GetMapping("/usuarios")
    public PagedModel<UsuarioAdminDto> listar(@RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(usuarios.listar(rol, estado, q, page));
    }

    /**
     * Detalle de un usuario.
     *
     * @param id usuario
     * @return el detalle
     */
    @GetMapping("/usuarios/{id}")
    public UsuarioDetalleDto obtener(@PathVariable long id) {
        return usuarios.obtener(id);
    }

    /**
     * Suspende a un usuario.
     *
     * @param jwt     token del administrador
     * @param id      usuario
     * @param request motivo y detalle
     * @return el usuario actualizado
     */
    @PostMapping("/usuarios/{id}/suspension")
    public UsuarioDetalleDto suspender(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody SuspenderRequest request) {
        return usuarios.suspender(Long.parseLong(jwt.getSubject()), id, request.motivo(), request.detalle());
    }

    /**
     * Levanta la suspensión vigente.
     *
     * @param id usuario
     * @return el usuario actualizado
     */
    @PostMapping("/usuarios/{id}/reactivacion")
    public UsuarioDetalleDto reactivar(@PathVariable long id) {
        return usuarios.reactivar(id);
    }

    /**
     * Genera una contraseña temporal.
     *
     * @param id usuario
     * @return la contraseña temporal
     */
    @PostMapping("/usuarios/{id}/contrasena-temporal")
    public ContrasenaTemporalResponse contrasenaTemporal(@PathVariable long id) {
        return new ContrasenaTemporalResponse(usuarios.contrasenaTemporal(id));
    }

    /**
     * Desactiva la cuenta.
     *
     * @param id usuario
     * @return el usuario actualizado
     */
    @PostMapping("/usuarios/{id}/desactivacion")
    public UsuarioDetalleDto desactivar(@PathVariable long id) {
        return usuarios.desactivar(id);
    }

    /**
     * Vuelve a activar la cuenta.
     *
     * @param id usuario
     * @return el usuario actualizado
     */
    @PostMapping("/usuarios/{id}/activacion")
    public UsuarioDetalleDto activar(@PathVariable long id) {
        return usuarios.activar(id);
    }

    /**
     * Crea otro administrador.
     *
     * @param request nombre, celular, distrito y contraseña
     * @return 201 con el administrador creado
     */
    @PostMapping("/administradores")
    public ResponseEntity<UsuarioDetalleDto> crearAdministrador(@Valid @RequestBody NuevoAdministradorRequest request) {
        UsuarioDetalleDto creado = usuarios.crearAdministrador(request.aDto());
        return ResponseEntity.created(URI.create("/api/v1/admin/usuarios/" + creado.id())).body(creado);
    }
}
