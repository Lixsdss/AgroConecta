package pe.agroconecta.usuario.aplicacion;

import java.security.SecureRandom;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.usuario.dominio.EstadoUsuario;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

/**
 * Gestión de cuentas que hace el administrador (RF-08, UC-11 parte 1, UC-10 A5): contraseña temporal, desactivar y
 * volver a activar una cuenta, y crear otro administrador. La suspensión vive en {@code administracion}
 * (tabla {@code suspensiones}).
 */
@Service
public class GestionCuentasService {

    /** Largo de la contraseña temporal. */
    static final int LARGO_TEMPORAL = 10;

    /** Letras y números sin los que se confunden al dictarlos por teléfono (0/O, 1/l/I). */
    private static final String ALFABETO_TEMPORAL = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    static final String MENSAJE_NO_EXISTE = "El usuario no existe.";
    static final String MENSAJE_TEMPORAL_ADMIN = "La contraseña de un administrador no se reinicia desde aquí.";
    static final String MENSAJE_DESACTIVAR_ADMIN = "No se desactiva a un administrador.";
    static final String MENSAJE_SOLO_ACTIVA = "Solo se desactiva una cuenta activa.";
    static final String MENSAJE_SOLO_DESACTIVADA = "Solo se activa una cuenta desactivada.";

    private static final Logger LOG = LoggerFactory.getLogger(GestionCuentasService.class);

    private final UsuarioRepository usuarios;
    private final CatalogoService catalogo;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom azar = new SecureRandom();

    /**
     * Crea el servicio.
     *
     * @param usuarios        repositorio de usuarios
     * @param catalogo        catálogo de distritos
     * @param passwordEncoder codificador BCrypt
     */
    public GestionCuentasService(UsuarioRepository usuarios, CatalogoService catalogo,
            PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Datos de la cuenta.
     *
     * @param idUsuario usuario
     * @return rol, estado y último acceso
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public CuentaDto cuenta(long idUsuario) {
        return aDto(buscar(idUsuario));
    }

    /**
     * Genera una contraseña temporal de 10 letras y números, la guarda con BCrypt y la devuelve una sola vez.
     *
     * @param idUsuario agricultor o comprador
     * @return la contraseña temporal en texto plano (nunca se registra en el log)
     * @throws ReglaNegocioException si es administrador
     */
    @Transactional
    public String reiniciarContrasena(long idUsuario) {
        Usuario usuario = buscar(idUsuario);
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            throw new ReglaNegocioException(MENSAJE_TEMPORAL_ADMIN);
        }
        String temporal = generarTemporal();
        usuario.cambiarPasswordHash(passwordEncoder.encode(temporal));
        usuarios.saveAndFlush(usuario);
        LOG.info("Contraseña temporal generada para el usuario {}", idUsuario);
        return temporal;
    }

    /**
     * Desactiva una cuenta ACTIVA de agricultor o comprador.
     *
     * @param idUsuario usuario
     * @return la cuenta actualizada
     */
    @Transactional
    public CuentaDto desactivar(long idUsuario) {
        Usuario usuario = buscar(idUsuario);
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            throw new ReglaNegocioException(MENSAJE_DESACTIVAR_ADMIN);
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ReglaNegocioException(MENSAJE_SOLO_ACTIVA);
        }
        usuario.desactivar();
        usuarios.saveAndFlush(usuario);
        LOG.info("Cuenta {} desactivada", idUsuario);
        return aDto(usuario);
    }

    /**
     * Vuelve a activar una cuenta INACTIVA.
     *
     * @param idUsuario usuario
     * @return la cuenta actualizada
     */
    @Transactional
    public CuentaDto activar(long idUsuario) {
        Usuario usuario = buscar(idUsuario);
        if (usuario.getEstado() != EstadoUsuario.INACTIVO) {
            throw new ReglaNegocioException(MENSAJE_SOLO_DESACTIVADA);
        }
        usuario.activar();
        usuarios.saveAndFlush(usuario);
        LOG.info("Cuenta {} activada de nuevo", idUsuario);
        return aDto(usuario);
    }

    /**
     * Crea otro administrador (UC-10 A5), con las reglas del registro.
     *
     * @param datos nombre, celular, distrito y contraseña
     * @return id del administrador creado
     * @throws ConflictoException    si el celular ya está registrado
     * @throws ReglaNegocioException si el distrito no existe
     */
    @Transactional
    public long crearAdministrador(NuevoAdministradorDto datos) {
        Preconditions.checkNotNull(datos, "datos");
        String celular = ReglasUsuario.normalizarCelular(datos.celular());
        if (usuarios.existsByCelular(celular)) {
            throw new ConflictoException(RegistroService.MENSAJE_CELULAR_DUPLICADO);
        }
        if (!catalogo.existeDistrito(datos.idDistrito())) {
            throw new ReglaNegocioException(RegistroService.MENSAJE_DISTRITO_INEXISTENTE);
        }
        Usuario admin = usuarios.saveAndFlush(new Usuario(Rol.ADMINISTRADOR,
                ReglasUsuario.normalizarTexto(datos.nombreCompleto()), celular,
                passwordEncoder.encode(datos.contrasena()), datos.idDistrito()));
        LOG.info("Administrador creado: id={}, celular={}", admin.getId(), ReglasUsuario.enmascararCelular(celular));
        return admin.getId();
    }

    /** Diez caracteres con al menos una letra y un número, para que cumpla la regla de contraseñas. */
    String generarTemporal() {
        String temporal;
        do {
            StringBuilder texto = new StringBuilder(LARGO_TEMPORAL);
            for (int i = 0; i < LARGO_TEMPORAL; i++) {
                texto.append(ALFABETO_TEMPORAL.charAt(azar.nextInt(ALFABETO_TEMPORAL.length())));
            }
            temporal = texto.toString();
        } while (!temporal.matches(ReglasUsuario.PATRON_CONTRASENA));
        return temporal;
    }

    private Usuario buscar(long idUsuario) {
        return usuarios.findById(idUsuario).orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
    }

    private static CuentaDto aDto(Usuario u) {
        return new CuentaDto(u.getId(), u.getRol().name(), u.getEstado().name(), u.getUltimoAcceso());
    }
}
