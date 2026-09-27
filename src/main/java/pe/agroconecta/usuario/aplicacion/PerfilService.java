package pe.agroconecta.usuario.aplicacion;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.compartido.error.AutenticacionFallidaException;
import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.usuario.dominio.Agricultor;
import pe.agroconecta.usuario.dominio.Comprador;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.AgricultorRepository;
import pe.agroconecta.usuario.infraestructura.CompradorRepository;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

/**
 * Consulta y edición del propio perfil, y cambio de la propia contraseña (RF-08, UC-10).
 */
@Service
public class PerfilService {

    static final String MENSAJE_CONTRASENA_ACTUAL = "La contraseña actual no es correcta.";
    static final String MENSAJE_SIN_SESION = "Inicie sesión para continuar.";

    private static final Logger LOG = LoggerFactory.getLogger(PerfilService.class);

    private final UsuarioRepository usuarios;
    private final AgricultorRepository agricultores;
    private final CompradorRepository compradores;
    private final CatalogoService catalogo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea el servicio.
     *
     * @param usuarios        repositorio de usuarios
     * @param agricultores    repositorio de agricultores
     * @param compradores     repositorio de compradores
     * @param catalogo        catálogo de distritos
     * @param passwordEncoder codificador BCrypt
     */
    public PerfilService(UsuarioRepository usuarios, AgricultorRepository agricultores,
            CompradorRepository compradores, CatalogoService catalogo, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.agricultores = agricultores;
        this.compradores = compradores;
        this.catalogo = catalogo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Devuelve el perfil del usuario.
     *
     * @param idUsuario id del usuario de la sesión
     * @return perfil con distrito y subtipo
     * @throws AutenticacionFallidaException si el usuario ya no existe
     */
    @Transactional(readOnly = true)
    public PerfilDto obtener(long idUsuario) {
        return armarPerfil(buscarUsuario(idUsuario));
    }

    /**
     * Actualiza los datos editables del perfil. Los campos que no corresponden al rol se ignoran.
     *
     * @param idUsuario id del usuario de la sesión
     * @param datos     nuevos datos
     * @return perfil actualizado
     * @throws ReglaNegocioException si el distrito no existe o faltan datos del comprador
     * @throws ConflictoException    si el RUC pertenece a otro comprador
     */
    @Transactional
    public PerfilDto actualizar(long idUsuario, ActualizacionPerfilDto datos) {
        Usuario usuario = buscarUsuario(idUsuario);
        if (!catalogo.existeDistrito(datos.idDistrito())) {
            throw new ReglaNegocioException(RegistroService.MENSAJE_DISTRITO_INEXISTENTE);
        }
        usuario.actualizarDatos(ReglasUsuario.normalizarTexto(datos.nombreCompleto()), datos.idDistrito());

        if (usuario.getRol() == Rol.AGRICULTOR) {
            agricultores.findById(idUsuario)
                    .ifPresent(a -> a.actualizar(ReglasUsuario.normalizarTexto(datos.nombreChacra())));
        } else if (usuario.getRol() == Rol.COMPRADOR) {
            actualizarComprador(idUsuario, datos);
        }
        usuarios.flush();
        LOG.info("Perfil actualizado: id={}", idUsuario);
        return armarPerfil(usuario);
    }

    /**
     * Cambia la contraseña del propio usuario. El token actual sigue vigente.
     *
     * @param idUsuario        id del usuario de la sesión
     * @param contrasenaActual contraseña actual
     * @param contrasenaNueva  nueva contraseña (ya validada)
     * @throws ReglaNegocioException si la contraseña actual no coincide
     */
    @Transactional
    public void cambiarContrasena(long idUsuario, String contrasenaActual, String contrasenaNueva) {
        Usuario usuario = buscarUsuario(idUsuario);
        if (!passwordEncoder.matches(contrasenaActual, usuario.getPasswordHash())) {
            throw new ReglaNegocioException(MENSAJE_CONTRASENA_ACTUAL);
        }
        usuario.cambiarPasswordHash(passwordEncoder.encode(contrasenaNueva));
        LOG.info("Contraseña cambiada por el propio usuario: id={}", idUsuario);
    }

    private void actualizarComprador(long idUsuario, ActualizacionPerfilDto datos) {
        String nombreNegocio = ReglasUsuario.normalizarTexto(datos.nombreNegocio());
        if (nombreNegocio == null || datos.tipoComprador() == null) {
            throw new ReglaNegocioException(RegistroService.MENSAJE_DATOS_COMPRADOR);
        }
        String ruc = StringUtils.trimToNull(datos.ruc());
        if (ruc != null && compradores.existsByRucAndIdNot(ruc, idUsuario)) {
            throw new ConflictoException(RegistroService.MENSAJE_RUC_DUPLICADO);
        }
        compradores.findById(idUsuario).ifPresent(c -> c.actualizar(datos.tipoComprador(), nombreNegocio, ruc));
    }

    private Usuario buscarUsuario(long idUsuario) {
        return usuarios.findById(idUsuario)
                .orElseThrow(() -> new AutenticacionFallidaException(MENSAJE_SIN_SESION));
    }

    private PerfilDto armarPerfil(Usuario u) {
        DistritoDto distrito = catalogo.buscarDistrito(u.getIdDistrito())
                .orElseThrow(() -> new IllegalStateException("Distrito inexistente para el usuario " + u.getId()));
        String nombreChacra = null;
        Comprador comprador = null;
        if (u.getRol() == Rol.AGRICULTOR) {
            nombreChacra = agricultores.findById(u.getId()).map(Agricultor::getNombreChacra).orElse(null);
        } else if (u.getRol() == Rol.COMPRADOR) {
            comprador = compradores.findById(u.getId()).orElse(null);
        }
        return new PerfilDto(u.getId(), u.getRol(), u.getNombreCompleto(), u.getCelular(), u.getEstado(),
                distrito.id(), distrito.nombre(), distrito.provincia(), u.getFechaRegistro(), nombreChacra,
                comprador == null ? null : comprador.getNombreNegocio(),
                comprador == null ? null : comprador.getTipoComprador(),
                comprador == null ? null : comprador.getRuc());
    }
}
