package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.compartido.error.AutenticacionFallidaException;
import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.usuario.dominio.Agricultor;
import pe.agroconecta.usuario.dominio.Comprador;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.TipoComprador;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.AgricultorRepository;
import pe.agroconecta.usuario.infraestructura.CompradorRepository;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final DistritoDto PIURA = new DistritoDto(1, "Piura", 1, "Piura", true);
    private static final DistritoDto CASTILLA = new DistritoDto(2, "Castilla", 1, "Piura", false);

    @Mock private UsuarioRepository usuarios;
    @Mock private AgricultorRepository agricultores;
    @Mock private CompradorRepository compradores;
    @Mock private CatalogoService catalogo;
    @Mock private PasswordEncoder passwordEncoder;

    private PerfilService servicio;

    @BeforeEach
    void preparar() {
        servicio = new PerfilService(usuarios, agricultores, compradores, catalogo, passwordEncoder);
    }

    @Test
    void obtieneElPerfilDeUnAgricultorConSuDistritoYChacra() {
        Usuario usuario = usuario(Rol.AGRICULTOR);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(catalogo.buscarDistrito(1)).thenReturn(Optional.of(PIURA));
        when(agricultores.findById(5L)).thenReturn(Optional.of(new Agricultor(usuario, "La Esperanza")));

        PerfilDto perfil = servicio.obtener(5L);

        assertThat(perfil.distrito()).isEqualTo("Piura");
        assertThat(perfil.nombreChacra()).isEqualTo("La Esperanza");
        assertThat(perfil.nombreNegocio()).isNull();
    }

    @Test
    void unUsuarioQueYaNoExisteDebeIniciarSesion() {
        when(usuarios.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(5L)).isInstanceOf(AutenticacionFallidaException.class);
    }

    @Test
    void actualizaLosDatosDelComprador() {
        Usuario usuario = usuario(Rol.COMPRADOR);
        Comprador comprador = new Comprador(usuario, TipoComprador.OTRO, "Viejo", null);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(catalogo.existeDistrito(2)).thenReturn(true);
        when(catalogo.buscarDistrito(2)).thenReturn(Optional.of(CASTILLA));
        when(compradores.findById(5L)).thenReturn(Optional.of(comprador));

        PerfilDto perfil = servicio.actualizar(5L, new ActualizacionPerfilDto(" Rosa  Quispe ", 2, null,
                " Hotel Costa ", TipoComprador.HOTEL, "20111111111"));

        assertThat(perfil.nombreCompleto()).isEqualTo("Rosa Quispe");
        assertThat(perfil.distrito()).isEqualTo("Castilla");
        assertThat(perfil.nombreNegocio()).isEqualTo("Hotel Costa");
        assertThat(perfil.tipoComprador()).isEqualTo(TipoComprador.HOTEL);
        assertThat(perfil.ruc()).isEqualTo("20111111111");
    }

    @Test
    void actualizaLaChacraDelAgricultor() {
        Usuario usuario = usuario(Rol.AGRICULTOR);
        Agricultor agricultor = new Agricultor(usuario, null);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(catalogo.existeDistrito(1)).thenReturn(true);
        when(catalogo.buscarDistrito(1)).thenReturn(Optional.of(PIURA));
        when(agricultores.findById(5L)).thenReturn(Optional.of(agricultor));

        PerfilDto perfil = servicio.actualizar(5L, new ActualizacionPerfilDto("Juan", 1, "El Algarrobo", null, null,
                null));

        assertThat(perfil.nombreChacra()).isEqualTo("El Algarrobo");
    }

    @Test
    void rechazaUnRucDeOtroComprador() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario(Rol.COMPRADOR)));
        when(catalogo.existeDistrito(1)).thenReturn(true);
        when(compradores.existsByRucAndIdNot("20111111111", 5L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(5L, new ActualizacionPerfilDto("Rosa", 1, null, "Hotel",
                TipoComprador.HOTEL, "20111111111")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void rechazaUnDistritoInexistente() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario(Rol.AGRICULTOR)));
        when(catalogo.existeDistrito(99)).thenReturn(false);

        assertThatThrownBy(() -> servicio.actualizar(5L, new ActualizacionPerfilDto("Juan", 99, null, null, null,
                null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(RegistroService.MENSAJE_DISTRITO_INEXISTENTE);
    }

    @Test
    void cambiaLaContrasenaSiLaActualCoincide() {
        Usuario usuario = usuario(Rol.AGRICULTOR);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave2026", "hash")).thenReturn(true);
        when(passwordEncoder.encode("nueva2027")).thenReturn("hash-nuevo");

        servicio.cambiarContrasena(5L, "clave2026", "nueva2027");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
    }

    @Test
    void rechazaElCambioSiLaContrasenaActualNoCoincide() {
        Usuario usuario = usuario(Rol.AGRICULTOR);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra", "hash")).thenReturn(false);

        assertThatThrownBy(() -> servicio.cambiarContrasena(5L, "otra", "nueva2027"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(PerfilService.MENSAJE_CONTRASENA_ACTUAL);
        assertThat(usuario.getPasswordHash()).isEqualTo("hash");
    }

    private static Usuario usuario(Rol rol) {
        return RegistroServiceTest.conId(new Usuario(rol, "Nombre", "911111111", "hash", 1), 5L);
    }
}
