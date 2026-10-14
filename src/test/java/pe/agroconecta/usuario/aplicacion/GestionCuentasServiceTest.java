package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class GestionCuentasServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private CatalogoService catalogo;
    @Mock private PasswordEncoder passwordEncoder;

    private GestionCuentasService servicio;

    @BeforeEach
    void preparar() {
        servicio = new GestionCuentasService(usuarios, catalogo, passwordEncoder);
    }

    @Test
    void laTemporalTieneDiezLetrasYNumerosYCumpleLaReglaDeContrasenas() {
        for (int i = 0; i < 200; i++) {
            assertThat(servicio.generarTemporal()).hasSize(10).matches(ReglasUsuario.PATRON_CONTRASENA)
                    .doesNotContain("0", "O", "1", "l", "I");
        }
    }

    @Test
    void reiniciaLaContrasenaConBcryptSoloDeAgricultoresYCompradores() {
        Usuario juan = usuario(3L, Rol.AGRICULTOR);
        when(usuarios.findById(3L)).thenReturn(Optional.of(juan));
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        when(passwordEncoder.encode(texto.capture())).thenReturn("hash-nuevo");

        String temporal = servicio.reiniciarContrasena(3L);

        assertThat(texto.getValue()).isEqualTo(temporal);
        assertThat(juan.getPasswordHash()).isEqualTo("hash-nuevo");
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1L, Rol.ADMINISTRADOR)));
        assertThatThrownBy(() -> servicio.reiniciarContrasena(1L))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(GestionCuentasService.MENSAJE_TEMPORAL_ADMIN);
        assertThatThrownBy(() -> servicio.reiniciarContrasena(9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivaYActivaSoloDesdeElEstadoCorrecto() {
        Usuario juan = usuario(3L, Rol.AGRICULTOR);
        when(usuarios.findById(3L)).thenReturn(Optional.of(juan));

        assertThat(servicio.desactivar(3L).estado()).isEqualTo("INACTIVO");
        assertThatThrownBy(() -> servicio.desactivar(3L)).hasMessage(GestionCuentasService.MENSAJE_SOLO_ACTIVA);
        assertThat(servicio.activar(3L).estado()).isEqualTo("ACTIVO");
        assertThatThrownBy(() -> servicio.activar(3L)).hasMessage(GestionCuentasService.MENSAJE_SOLO_DESACTIVADA);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1L, Rol.ADMINISTRADOR)));
        assertThatThrownBy(() -> servicio.desactivar(1L)).hasMessage(GestionCuentasService.MENSAJE_DESACTIVAR_ADMIN);
        assertThat(servicio.cuenta(3L).rol()).isEqualTo("AGRICULTOR");
    }

    @Test
    void creaUnAdministradorConLasReglasDelRegistro() {
        when(usuarios.existsByCelular("900000029")).thenReturn(false);
        when(catalogo.existeDistrito(1)).thenReturn(true);
        when(passwordEncoder.encode("admin2027")).thenReturn("hash");
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 12L);
            return u;
        });

        long id = servicio.crearAdministrador(new NuevoAdministradorDto(" Carlos  Ruiz ", "900 000 029", 1, "admin2027"));

        assertThat(id).isEqualTo(12L);
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(guardado.getValue().getNombreCompleto()).isEqualTo("Carlos Ruiz");

        when(usuarios.existsByCelular("900000030")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crearAdministrador(new NuevoAdministradorDto("Ana", "900000030", 1, "x")))
                .isInstanceOf(ConflictoException.class);
        when(usuarios.existsByCelular("900000031")).thenReturn(false);
        when(catalogo.existeDistrito(999)).thenReturn(false);
        assertThatThrownBy(() -> servicio.crearAdministrador(new NuevoAdministradorDto("Ana", "900000031", 999, "x")))
                .isInstanceOf(ReglaNegocioException.class);
        verify(passwordEncoder, never()).encode("x");
    }

    private static Usuario usuario(long id, Rol rol) {
        Usuario u = new Usuario(rol, "Nombre", "912345678", "hash", 1);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}
