package pe.agroconecta.usuario.aplicacion;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

/**
 * Crea el primer ADMINISTRADOR al arrancar, porque {@code modelo-bd.sql} no guarda contraseñas. Solo actúa si no
 * existe ningún administrador y las variables {@code ADMIN_CELULAR}, {@code ADMIN_NOMBRE} y {@code ADMIN_PASSWORD}
 * tienen valor; su distrito es Piura (ubigeo 200101).
 */
@Component
public class InicializadorAdministrador implements ApplicationRunner {

    /** Ubigeo del distrito de Piura, asignado al primer administrador. */
    static final String UBIGEO_PIURA = "200101";

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorAdministrador.class);

    private final UsuarioRepository usuarios;
    private final CatalogoService catalogo;
    private final PasswordEncoder passwordEncoder;
    private final String celular;
    private final String nombre;
    private final String contrasena;

    /**
     * Crea el inicializador.
     *
     * @param usuarios        repositorio de usuarios
     * @param catalogo        catálogo de distritos
     * @param passwordEncoder codificador BCrypt
     * @param celular         celular del administrador ({@code ADMIN_CELULAR})
     * @param nombre          nombre del administrador ({@code ADMIN_NOMBRE})
     * @param contrasena      contraseña del administrador ({@code ADMIN_PASSWORD})
     */
    public InicializadorAdministrador(UsuarioRepository usuarios, CatalogoService catalogo,
            PasswordEncoder passwordEncoder,
            @Value("${agroconecta.admin-inicial.celular:}") String celular,
            @Value("${agroconecta.admin-inicial.nombre:}") String nombre,
            @Value("${agroconecta.admin-inicial.contrasena:}") String contrasena) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.passwordEncoder = passwordEncoder;
        this.celular = ReglasUsuario.normalizarCelular(celular);
        this.nombre = ReglasUsuario.normalizarTexto(nombre);
        this.contrasena = contrasena;
    }

    /**
     * Crea el administrador si corresponde; nunca detiene el arranque.
     *
     * @param args argumentos de la aplicación (no se usan)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (StringUtils.isAnyBlank(celular, nombre, contrasena) || usuarios.existsByRol(Rol.ADMINISTRADOR)) {
            return;
        }
        if (!celular.matches(ReglasUsuario.PATRON_CELULAR)
                || !contrasena.matches(ReglasUsuario.PATRON_CONTRASENA)) {
            LOG.error("No se creó el administrador inicial: ADMIN_CELULAR o ADMIN_PASSWORD no tienen el formato válido");
            return;
        }
        if (usuarios.existsByCelular(celular)) {
            LOG.error("No se creó el administrador inicial: el celular {} ya pertenece a otro usuario",
                    ReglasUsuario.enmascararCelular(celular));
            return;
        }
        DistritoDto piura = catalogo.buscarDistritoPorUbigeo(UBIGEO_PIURA)
                .orElseThrow(() -> new IllegalStateException("No existe el distrito de Piura (V2)"));
        Usuario admin = usuarios.save(new Usuario(Rol.ADMINISTRADOR, nombre, celular,
                passwordEncoder.encode(contrasena), piura.id()));
        LOG.info("Administrador inicial creado: id={}, celular={}", admin.getId(),
                ReglasUsuario.enmascararCelular(celular));
    }
}
