package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.compartido.error.AutenticacionFallidaException;
import pe.agroconecta.compartido.error.DemasiadosIntentosException;
import pe.agroconecta.usuario.dominio.EstadoUsuario;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class SesionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-09-27T15:00:00Z");
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Mock private UsuarioRepository usuarios;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmisorToken emisorToken;
    @Mock private LimiteIntentosLogin limiteIntentos;

    private SesionService servicio;

    @BeforeEach
    void preparar() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash-de-relleno");
        servicio = new SesionService(usuarios, passwordEncoder, emisorToken, limiteIntentos,
                Clock.fixed(AHORA, LIMA));
    }

    @Test
    void iniciaSesionConCelularConEspaciosYRegistraElAcceso() {
        Usuario usuario = usuario(EstadoUsuario.ACTIVO);
        when(usuarios.findByCelular("911111111")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave2026", "hash")).thenReturn(true);
        SesionDto sesion = new SesionDto("token", AHORA, 5L, "Juan", Rol.AGRICULTOR);
        when(emisorToken.emitir(usuario)).thenReturn(sesion);

        assertThat(servicio.iniciarSesion(" 911 111 111 ", "clave2026")).isSameAs(sesion);
        assertThat(usuario.getUltimoAcceso()).isEqualTo(LocalDateTime.ofInstant(AHORA, LIMA));
        verify(limiteIntentos).verificarPermitido("911111111");
        verify(limiteIntentos).reiniciar("911111111");
    }

    @Test
    void unCelularBloqueadoNoLlegaAVerificarLaContrasena() {
        doThrow(new DemasiadosIntentosException("bloqueado")).when(limiteIntentos).verificarPermitido("911111111");

        assertThatThrownBy(() -> servicio.iniciarSesion("911 111 111", "clave2026"))
                .isInstanceOf(DemasiadosIntentosException.class);
        verify(usuarios, never()).findByCelular(anyString());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void rechazaUnaContrasenaIncorrectaSinDecirCualDatoFallo() {
        when(usuarios.findByCelular("911111111")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(passwordEncoder.matches("otra", "hash")).thenReturn(false);

        assertThatThrownBy(() -> servicio.iniciarSesion("911111111", "otra"))
                .isInstanceOf(AutenticacionFallidaException.class)
                .hasMessage(SesionService.MENSAJE_CREDENCIALES);
        verify(limiteIntentos).registrarFallo("911111111");
        verify(limiteIntentos, never()).reiniciar(anyString());
    }

    @Test
    void conUnCelularInexistenteTambienComparaLaContrasena() {
        when(usuarios.findByCelular("911111111")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion("911111111", "clave2026"))
                .isInstanceOf(AutenticacionFallidaException.class)
                .hasMessage(SesionService.MENSAJE_CREDENCIALES);
        verify(passwordEncoder).matches(eq("clave2026"), eq("hash-de-relleno"));
        verify(emisorToken, never()).emitir(any());
        verify(limiteIntentos).registrarFallo("911111111");
    }

    @Test
    void rechazaAUnUsuarioSuspendido() {
        when(usuarios.findByCelular("911111111")).thenReturn(Optional.of(usuario(EstadoUsuario.SUSPENDIDO)));
        when(passwordEncoder.matches("clave2026", "hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciarSesion("911111111", "clave2026"))
                .isInstanceOf(AutenticacionFallidaException.class)
                .hasMessage(SesionService.MENSAJE_SUSPENDIDA);
        verify(limiteIntentos, never()).registrarFallo(anyString());
    }

    @Test
    void rechazaAUnUsuarioInactivo() {
        when(usuarios.findByCelular("911111111")).thenReturn(Optional.of(usuario(EstadoUsuario.INACTIVO)));
        when(passwordEncoder.matches("clave2026", "hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciarSesion("911111111", "clave2026"))
                .isInstanceOf(AutenticacionFallidaException.class)
                .hasMessage(SesionService.MENSAJE_DESACTIVADA);
    }

    private static Usuario usuario(EstadoUsuario estado) {
        Usuario usuario = RegistroServiceTest.conId(
                new Usuario(Rol.AGRICULTOR, "Juan", "911111111", "hash", 1), 5L);
        ReflectionTestUtils.setField(usuario, "estado", estado);
        return usuario;
    }
}
