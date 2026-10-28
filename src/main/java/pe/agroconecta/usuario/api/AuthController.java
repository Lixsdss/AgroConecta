package pe.agroconecta.usuario.api;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.usuario.aplicacion.RegistroService;
import pe.agroconecta.usuario.aplicacion.SesionService;

/**
 * Registro e inicio de sesión ({@code /api/v1/auth}), ambos públicos (RF-08, UC-10).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final URI PERFIL_PROPIO = URI.create("/api/v1/usuarios/me");

    private final RegistroService registro;
    private final SesionService sesion;

    /**
     * Crea el controlador.
     *
     * @param registro servicio de registro
     * @param sesion   servicio de inicio de sesión
     */
    public AuthController(RegistroService registro, SesionService sesion) {
        this.registro = registro;
        this.sesion = sesion;
    }

    /**
     * Registra un agricultor o un comprador y devuelve su sesión iniciada.
     *
     * @param request datos del formulario de registro
     * @return 201 con la sesión y {@code Location} al perfil propio
     */
    @PostMapping("/registro")
    public ResponseEntity<SesionResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.created(PERFIL_PROPIO).body(SesionResponse.de(registro.registrar(request.aDto())));
    }

    /**
     * Inicia sesión con celular y contraseña.
     *
     * @param request credenciales
     * @return 200 con la sesión
     */
    @PostMapping("/login")
    public SesionResponse iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return SesionResponse.de(sesion.iniciarSesion(request.celular(), request.contrasena()));
    }
}
