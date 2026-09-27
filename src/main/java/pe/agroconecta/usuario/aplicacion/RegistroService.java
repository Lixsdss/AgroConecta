package pe.agroconecta.usuario.aplicacion;

import java.time.Clock;
import java.time.LocalDateTime;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
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
 * Registro de agricultores y compradores (RF-08, UC-10). Crea el usuario y su subtipo en la misma transacción y
 * devuelve la sesión iniciada. Los triggers {@code trg_agricultores_bi} y {@code trg_compradores_bi} repiten la
 * verificación del rol.
 */
@Service
public class RegistroService {

    static final String MENSAJE_CELULAR_DUPLICADO = "Este celular ya está registrado.";
    static final String MENSAJE_RUC_DUPLICADO = "Este RUC ya está registrado.";
    static final String MENSAJE_DISTRITO_INEXISTENTE = "El distrito elegido no existe.";
    static final String MENSAJE_DATOS_COMPRADOR = "Indique el nombre y el tipo de su negocio.";
    static final String MENSAJE_ROL_NO_PERMITIDO = "Solo pueden registrarse agricultores y compradores.";

    private static final Logger LOG = LoggerFactory.getLogger(RegistroService.class);

    private final UsuarioRepository usuarios;
    private final AgricultorRepository agricultores;
    private final CompradorRepository compradores;
    private final CatalogoService catalogo;
    private final PasswordEncoder passwordEncoder;
    private final EmisorToken emisorToken;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param usuarios        repositorio de usuarios
     * @param agricultores    repositorio de agricultores
     * @param compradores     repositorio de compradores
     * @param catalogo        catálogo de distritos
     * @param passwordEncoder codificador BCrypt
     * @param emisorToken     emisor de tokens JWT
     * @param reloj           reloj de la aplicación
     */
    public RegistroService(UsuarioRepository usuarios, AgricultorRepository agricultores,
            CompradorRepository compradores, CatalogoService catalogo, PasswordEncoder passwordEncoder,
            EmisorToken emisorToken, Clock reloj) {
        this.usuarios = usuarios;
        this.agricultores = agricultores;
        this.compradores = compradores;
        this.catalogo = catalogo;
        this.passwordEncoder = passwordEncoder;
        this.emisorToken = emisorToken;
        this.reloj = reloj;
    }

    /**
     * Registra un agricultor o un comprador e inicia su sesión.
     *
     * @param datos datos del formulario de registro
     * @return sesión iniciada del nuevo usuario
     * @throws ReglaNegocioException si el rol no se puede registrar, el distrito no existe o faltan datos del
     *                               comprador
     * @throws ConflictoException    si el celular o el RUC ya están registrados
     */
    @Transactional
    public SesionDto registrar(RegistroDto datos) {
        Preconditions.checkNotNull(datos, "datos");
        if (datos.rol() != Rol.AGRICULTOR && datos.rol() != Rol.COMPRADOR) {
            throw new ReglaNegocioException(MENSAJE_ROL_NO_PERMITIDO);
        }
        String celular = ReglasUsuario.normalizarCelular(datos.celular());
        if (usuarios.existsByCelular(celular)) {
            throw new ConflictoException(MENSAJE_CELULAR_DUPLICADO);
        }
        if (!catalogo.existeDistrito(datos.idDistrito())) {
            throw new ReglaNegocioException(MENSAJE_DISTRITO_INEXISTENTE);
        }

        Usuario usuario = new Usuario(datos.rol(), ReglasUsuario.normalizarTexto(datos.nombreCompleto()), celular,
                passwordEncoder.encode(datos.contrasena()), datos.idDistrito());
        usuario.registrarAcceso(LocalDateTime.now(reloj));
        usuario = usuarios.saveAndFlush(usuario);

        if (datos.rol() == Rol.AGRICULTOR) {
            agricultores.saveAndFlush(new Agricultor(usuario, ReglasUsuario.normalizarTexto(datos.nombreChacra())));
        } else {
            registrarComprador(usuario, datos);
        }

        LOG.info("Usuario registrado: id={}, rol={}, celular={}", usuario.getId(), usuario.getRol(),
                ReglasUsuario.enmascararCelular(celular));
        return emisorToken.emitir(usuario);
    }

    private void registrarComprador(Usuario usuario, RegistroDto datos) {
        String nombreNegocio = ReglasUsuario.normalizarTexto(datos.nombreNegocio());
        if (nombreNegocio == null || datos.tipoComprador() == null) {
            throw new ReglaNegocioException(MENSAJE_DATOS_COMPRADOR);
        }
        String ruc = StringUtils.trimToNull(datos.ruc());
        if (ruc != null && compradores.existsByRuc(ruc)) {
            throw new ConflictoException(MENSAJE_RUC_DUPLICADO);
        }
        compradores.saveAndFlush(new Comprador(usuario, datos.tipoComprador(), nombreNegocio, ruc));
    }
}
