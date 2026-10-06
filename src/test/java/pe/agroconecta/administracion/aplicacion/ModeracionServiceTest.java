package pe.agroconecta.administracion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.administracion.dominio.DecisionModeracion;
import pe.agroconecta.administracion.dominio.MotivoModeracion;
import pe.agroconecta.administracion.dominio.RevisionPublicacion;
import pe.agroconecta.administracion.infraestructura.RevisionPublicacionRepository;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.publicacion.aplicacion.PublicacionModeracionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionService;
import pe.agroconecta.publicacion.aplicacion.VistaModeracion;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;

@ExtendWith(MockitoExtension.class)
class ModeracionServiceTest {

    private static final UsuarioResumenDto JUAN =
            new UsuarioResumenDto(3L, "Juan Chunga", "987654321", "Tambogrande", LocalDateTime.now(), "AGRICULTOR", null);
    private static final UsuarioResumenDto LUCIA =
            new UsuarioResumenDto(1L, "Lucía Farfán", "900000009", "Piura", LocalDateTime.now(), "AGRICULTOR", null);

    @Mock private RevisionPublicacionRepository revisiones;
    @Mock private PublicacionService publicaciones;
    @Mock private UsuarioConsultaService usuarios;

    private ModeracionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ModeracionService(revisiones, publicaciones, usuarios);
    }

    @Test
    void listaLaColaConElAgricultorDeCadaPublicacion() {
        PageRequest pagina = PageRequest.of(0, 20);
        when(publicaciones.listarParaModeracion(VistaModeracion.POR_REVISAR, pagina))
                .thenReturn(new PageImpl<>(List.of(publicacion("PUBLICADA", "PRIMERA_PUBLICACION")), pagina, 1));
        when(usuarios.buscarResumenes(anyCollection())).thenReturn(Map.of(3L, JUAN));

        var resultado = servicio.listar(VistaModeracion.POR_REVISAR, pagina);

        assertThat(resultado.getContent()).singleElement()
                .satisfies(p -> {
                    assertThat(p.agricultor()).isEqualTo(JUAN);
                    assertThat(p.marcaRevision()).isEqualTo("PRIMERA_PUBLICACION");
                });
    }

    @Test
    void ocultarGuardaMotivoYMensajeYDevuelveElEstadoQueDejoElTrigger() {
        when(publicaciones.obtenerParaModeracion(15L))
                .thenReturn(publicacion("PUBLICADA", null), publicacion("BLOQUEADA", null));
        when(revisiones.saveAndFlush(any(RevisionPublicacion.class))).thenAnswer(i -> conId(i.getArgument(0), 4L));

        DecisionRegistradaDto resultado = servicio.decidir(1L, 15L, DecisionModeracion.BLOQUEO,
                MotivoModeracion.PRECIO_NO_COINCIDE, "  Revise   el precio.  ");

        ArgumentCaptor<RevisionPublicacion> guardada = ArgumentCaptor.forClass(RevisionPublicacion.class);
        verify(revisiones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getMotivo()).isEqualTo(MotivoModeracion.PRECIO_NO_COINCIDE);
        assertThat(guardada.getValue().getMensaje()).isEqualTo("Revise el precio.");
        assertThat(resultado.estadoPublicacion()).isEqualTo("BLOQUEADA");
        assertThat(resultado.idRevision()).isEqualTo(4L);
    }

    @Test
    void conformeIgnoraMotivoYMensaje() {
        when(publicaciones.obtenerParaModeracion(15L)).thenReturn(publicacion("PUBLICADA", "CANTIDAD_INUSUAL"));
        when(revisiones.saveAndFlush(any(RevisionPublicacion.class))).thenAnswer(i -> conId(i.getArgument(0), 5L));

        servicio.decidir(1L, 15L, DecisionModeracion.CONFORME, MotivoModeracion.OTRO, "texto");

        ArgumentCaptor<RevisionPublicacion> guardada = ArgumentCaptor.forClass(RevisionPublicacion.class);
        verify(revisiones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getMotivo()).isNull();
        assertThat(guardada.getValue().getMensaje()).isNull();
    }

    @Test
    void rechazaDecisionesQueNoCorrespondenAlEstado() {
        when(publicaciones.obtenerParaModeracion(15L)).thenReturn(publicacion("PUBLICADA", null));
        assertThatThrownBy(() -> servicio.decidir(1L, 15L, DecisionModeracion.DESBLOQUEO, null, null))
                .hasMessage(ModeracionService.MENSAJE_NO_OCULTA);
        assertThatThrownBy(() -> servicio.decidir(1L, 15L, DecisionModeracion.CONFORME, null, null))
                .hasMessage(ModeracionService.MENSAJE_FUERA_DE_COLA);

        when(publicaciones.obtenerParaModeracion(16L)).thenReturn(publicacion("VENDIDA", "PRIMERA_PUBLICACION"));
        assertThatThrownBy(() -> servicio.decidir(1L, 16L, DecisionModeracion.ADVERTENCIA,
                MotivoModeracion.OTRO, "x"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(ModeracionService.MENSAJE_NO_VISIBLE);
        verify(revisiones, never()).saveAndFlush(any());
    }

    @Test
    void advertirSinMensajeUtilSeRechaza() {
        when(publicaciones.obtenerParaModeracion(15L)).thenReturn(publicacion("PUBLICADA", null));

        assertThatThrownBy(() -> servicio.decidir(1L, 15L, DecisionModeracion.ADVERTENCIA,
                MotivoModeracion.OTRO, "   "))
                .hasMessage(ModeracionService.MENSAJE_FALTA_MOTIVO);
    }

    @Test
    void listaLasDecisionesDelDiaConProductoYAdministrador() {
        RevisionPublicacion revision = conId(new RevisionPublicacion(15L, 1L, DecisionModeracion.CONFORME, null,
                null), 4L);
        LocalDate hoy = LocalDate.of(2026, 9, 27);
        when(revisiones.findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(hoy.atStartOfDay(),
                hoy.plusDays(1).atStartOfDay())).thenReturn(List.of(revision));
        when(usuarios.buscarResumenes(anyCollection())).thenReturn(Map.of(1L, LUCIA));
        when(publicaciones.obtenerParaModeracion(15L)).thenReturn(publicacion("PUBLICADA", null));

        assertThat(servicio.decisionesDelDia(hoy)).singleElement().satisfies(d -> {
            assertThat(d.producto()).isEqualTo("Mango Kent");
            assertThat(d.administrador()).isEqualTo("Lucía Farfán");
        });
    }

    private static PublicacionModeracionDto publicacion(String estado, String marca) {
        return new PublicacionModeracionDto(15L, 3L, "Mango", "Kent", "Jaba", "jabas", BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "Tambogrande", "Piura",
                LocalDate.now(), LocalDate.now().plusDays(14), estado, marca, LocalDateTime.now());
    }

    private static RevisionPublicacion conId(RevisionPublicacion revision, long id) {
        ReflectionTestUtils.setField(revision, "id", id);
        return revision;
    }
}
