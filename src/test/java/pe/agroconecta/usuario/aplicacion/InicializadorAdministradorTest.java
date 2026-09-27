package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InicializadorAdministradorTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private CatalogoService catalogo;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void creaElAdministradorEnPiuraSiNoExisteNinguno() {
        when(usuarios.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(catalogo.buscarDistritoPorUbigeo(InicializadorAdministrador.UBIGEO_PIURA))
                .thenReturn(Optional.of(new DistritoDto(1, "Piura", 1, "Piura", true)));
        when(passwordEncoder.encode("admin2026")).thenReturn("hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        inicializador("900 000 000", "Ana Admin", "admin2026").run(null);

        ArgumentCaptor<Usuario> admin = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(admin.capture());
        assertThat(admin.getValue().getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(admin.getValue().getCelular()).isEqualTo("900000000");
        assertThat(admin.getValue().getIdDistrito()).isEqualTo(1);
    }

    @Test
    void noHaceNadaSinLasVariables() {
        inicializador("", "", "").run(null);

        verify(usuarios, never()).save(any());
    }

    @Test
    void noCreaOtroSiYaExisteUnAdministrador() {
        when(usuarios.existsByRol(Rol.ADMINISTRADOR)).thenReturn(true);

        inicializador("900000000", "Ana Admin", "admin2026").run(null);

        verify(usuarios, never()).save(any());
    }

    @Test
    void noCreaElAdministradorConUnaContrasenaDebil() {
        when(usuarios.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);

        inicializador("900000000", "Ana Admin", "corta").run(null);

        verify(usuarios, never()).save(any());
    }

    @Test
    void noUsaUnCelularDeOtroUsuario() {
        when(usuarios.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(usuarios.existsByCelular("900000000")).thenReturn(true);

        inicializador("900000000", "Ana Admin", "admin2026").run(null);

        verify(usuarios, never()).save(any());
    }

    private InicializadorAdministrador inicializador(String celular, String nombre, String contrasena) {
        return new InicializadorAdministrador(usuarios, catalogo, passwordEncoder, celular, nombre, contrasena);
    }
}
