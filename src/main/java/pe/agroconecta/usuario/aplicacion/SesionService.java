package pe.agroconecta.usuario.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.compartido.error.AutenticacionFallidaException;
import pe.agroconecta.compartido.error.DemasiadosIntentosException;
import pe.agroconecta.usuario.dominio.EstadoUsuario;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

/**
 * Inicio de sesión con celular y contraseña (RF-08, UC-10). No indica cuál de los dos datos falló y limita los
 * intentos fallidos por celular ({@link LimiteIntentosLogin}).
 */
@Service
public class SesionService {

    static final String MENSAJE_CREDENCIALES = "Celular o contraseña incorrectos.";
    static final String MENSAJE_SUSPENDIDA = "Su cuenta está suspendida. Comuníquese con el administrador.";
    static final String MENSAJE_DESACTIVADA = "Su cuenta está desactivada. Comuníquese con el administrador.";

    private static final Logger LOG = LoggerFactory.getLogger(SesionService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final EmisorToken emisorToken;
    private final LimiteIntentosLogin limiteIntentos;
    private final Clock reloj;
    /** Hash de relleno: se compara aunque el celular no exista, para no revelarlo por el tiempo de respuesta. */
    private final String hashDeRelleno;

    /**
     * Crea el servicio.
     *
     * @param usuarios        repositorio de usuarios
     * @param passwordEncoder codificador BCrypt
     * @param emisorToken     emisor de tokens JWT
     * @param limiteIntentos  límite de intentos fallidos por celular
     * @param reloj           reloj de la aplicación
     */
    public SesionService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, EmisorToken emisorToken,
            LimiteIntentosLogin limiteIntentos, Clock reloj) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.emisorToken = emisorToken;
        this.limiteIntentos = limiteIntentos;
        this.reloj = reloj;
        this.hashDeRelleno = passwordEncoder.encode("relleno-sin-usuario-1");
    }

    /**
     * Verifica las credenciales, registra el acceso y emite el token.
     *
     * @param celular    celular (puede traer espacios)
     * @param contrasena contraseña en texto plano
     * @return sesión iniciada
     * @throws AutenticacionFallidaException si las credenciales son incorrectas o la cuenta no está ACTIVA
     * @throws DemasiadosIntentosException   si el celular agotó sus intentos fallidos en la ventana vigente
     */
    @Transactional
    public SesionDto iniciarSesion(String celular, String contrasena) {
        String normalizado = ReglasUsuario.normalizarCelular(celular);
        limiteIntentos.verificarPermitido(normalizado);
        Optional<Usuario> encontrado = usuarios.findByCelular(normalizado);
        String hash = encontrado.map(Usuario::getPasswordHash).orElse(hashDeRelleno);
        boolean coincide = passwordEncoder.matches(contrasena, hash);
        if (encontrado.isEmpty() || !coincide) {
            LOG.warn("Inicio de sesión fallido: celular={}", ReglasUsuario.enmascararCelular(normalizado));
            limiteIntentos.registrarFallo(normalizado);
            throw new AutenticacionFallidaException(MENSAJE_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();
        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
            throw new AutenticacionFallidaException(MENSAJE_SUSPENDIDA);
        }
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new AutenticacionFallidaException(MENSAJE_DESACTIVADA);
        }

        limiteIntentos.reiniciar(normalizado);
        usuario.registrarAcceso(LocalDateTime.now(reloj));
        LOG.info("Inicio de sesión: id={}, rol={}", usuario.getId(), usuario.getRol());
        return emisorToken.emitir(usuario);
    }
}
