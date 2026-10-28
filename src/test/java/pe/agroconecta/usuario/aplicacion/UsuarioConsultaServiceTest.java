package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.CompradorRepository;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioConsultaServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private CompradorRepository compradores;
    @Mock private CatalogoService catalogo;
    @InjectMocks private UsuarioConsultaService servicio;

    @Test
    void devuelveElResumenDeCadaUsuarioConSuDistrito() {
        Usuario juan = RegistroServiceTest.conId(new Usuario(Rol.AGRICULTOR, "Juan", "911111111", "h", 9), 3L);
        when(usuarios.findAllById(Set.of(3L, 99L))).thenReturn(List.of(juan));
        when(catalogo.listarDistritos(null)).thenReturn(List.of(new DistritoDto(9, "Tambogrande", 1, "Piura", true)));

        var resultado = servicio.buscarResumenes(Set.of(3L, 99L));

        assertThat(resultado).containsOnlyKeys(3L);
        assertThat(resultado.get(3L).distrito()).isEqualTo("Tambogrande");
        assertThat(resultado.get(3L).nombreCompleto()).isEqualTo("Juan");
    }

    @Test
    void sinIdsNoConsulta() {
        assertThat(servicio.buscarResumenes(Set.of())).isEmpty();
        verifyNoInteractions(usuarios, catalogo);
    }

    @Test
    void existeCelularQuitaLosEspacios() {
        when(usuarios.existsByCelular("911111111")).thenReturn(true);

        assertThat(servicio.existeCelular("911 111 111")).isTrue();
    }
}
