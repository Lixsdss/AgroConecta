package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

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
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.usuario.dominio.Agricultor;
import pe.agroconecta.usuario.dominio.Comprador;
import pe.agroconecta.usuario.dominio.EstadoUsuario;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.TipoComprador;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.AgricultorRepository;
import pe.agroconecta.usuario.infraestructura.CompradorRepository;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private AgricultorRepository agricultores;
    @Mock private CompradorRepository compradores;
    @Mock private CatalogoService catalogo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmisorToken emisorToken;

    private RegistroService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-09-27T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new RegistroService(usuarios, agricultores, compradores, catalogo, passwordEncoder, emisorToken,
                reloj);
    }

    @Test
    void registraUnAgricultorConDatosNormalizadosYContrasenaCifrada() {
        when(catalogo.existeDistrito(1)).thenReturn(true);
        when(passwordEncoder.encode("clave2026")).thenReturn("hash");
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> conId(i.getArgument(0), 7L));
        SesionDto sesion = new SesionDto("token", Instant.now(), 7L, "Juan Chunga", Rol.AGRICULTOR);
        when(emisorToken.emitir(any(Usuario.class))).thenReturn(sesion);

        SesionDto resultado = servicio.registrar(new RegistroDto(Rol.AGRICULTOR, "  Juan   Chunga ", "911 111 111", 1,
                "clave2026", " La Esperanza ", "ignorado", TipoComprador.HOTEL, "20123456789"));

        assertThat(resultado).isSameAs(sesion);
        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(usuario.capture());
        assertThat(usuario.getValue().getNombreCompleto()).isEqualTo("Juan Chunga");
        assertThat(usuario.getValue().getCelular()).isEqualTo("911111111");
        assertThat(usuario.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(usuario.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getValue().getUltimoAcceso()).isNotNull();
        ArgumentCaptor<Agricultor> agricultor = ArgumentCaptor.forClass(Agricultor.class);
        verify(agricultores).saveAndFlush(agricultor.capture());
        assertThat(agricultor.getValue().getNombreChacra()).isEqualTo("La Esperanza");
        verify(compradores, never()).saveAndFlush(any());
    }

    @Test
    void registraUnCompradorConSuNegocio() {
        when(catalogo.existeDistrito(1)).thenReturn(true);
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> conId(i.getArgument(0), 8L));

        servicio.registrar(comprador("Restaurante El Chalán", TipoComprador.RESTAURANTE, " 20123456789 "));

        ArgumentCaptor<Comprador> comprador = ArgumentCaptor.forClass(Comprador.class);
        verify(compradores).saveAndFlush(comprador.capture());
        assertThat(comprador.getValue().getNombreNegocio()).isEqualTo("Restaurante El Chalán");
        assertThat(comprador.getValue().getTipoComprador()).isEqualTo(TipoComprador.RESTAURANTE);
        assertThat(comprador.getValue().getRuc()).isEqualTo("20123456789");
        verify(agricultores, never()).saveAndFlush(any());
    }

    @Test
    void rechazaUnCelularYaRegistrado() {
        when(usuarios.existsByCelular("911111111")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(comprador("Negocio", TipoComprador.OTRO, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(RegistroService.MENSAJE_CELULAR_DUPLICADO);
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void rechazaUnRucYaRegistrado() {
        when(catalogo.existeDistrito(1)).thenReturn(true);
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> conId(i.getArgument(0), 8L));
        when(compradores.existsByRuc("20123456789")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(comprador("Negocio", TipoComprador.OTRO, "20123456789")))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(RegistroService.MENSAJE_RUC_DUPLICADO);
    }

    @Test
    void rechazaUnDistritoInexistente() {
        when(catalogo.existeDistrito(1)).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(comprador("Negocio", TipoComprador.OTRO, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(RegistroService.MENSAJE_DISTRITO_INEXISTENTE);
    }

    @Test
    void rechazaUnCompradorSinDatosDelNegocio() {
        when(catalogo.existeDistrito(1)).thenReturn(true);
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> conId(i.getArgument(0), 8L));

        assertThatThrownBy(() -> servicio.registrar(comprador("   ", TipoComprador.OTRO, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(RegistroService.MENSAJE_DATOS_COMPRADOR);
    }

    @Test
    void rechazaElRegistroDeUnAdministrador() {
        RegistroDto admin = new RegistroDto(Rol.ADMINISTRADOR, "Intruso", "911111111", 1, "clave2026", null, null,
                null, null);

        assertThatThrownBy(() -> servicio.registrar(admin))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(RegistroService.MENSAJE_ROL_NO_PERMITIDO);
    }

    @Test
    void noMuestraLaContrasenaAlImprimirLosDatos() {
        assertThat(comprador("Negocio", TipoComprador.OTRO, null).toString()).doesNotContain("clave2026");
    }

    private static RegistroDto comprador(String negocio, TipoComprador tipo, String ruc) {
        return new RegistroDto(Rol.COMPRADOR, "Rosa Quispe", "911111111", 1, "clave2026", null, negocio, tipo, ruc);
    }

    static Usuario conId(Usuario usuario, long id) {
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
