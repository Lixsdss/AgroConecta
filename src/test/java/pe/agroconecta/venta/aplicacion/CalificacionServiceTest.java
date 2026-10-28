package pe.agroconecta.venta.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;
import pe.agroconecta.venta.dominio.EstadoTransaccion;
import pe.agroconecta.venta.dominio.VentaHistorial;
import pe.agroconecta.venta.infraestructura.CalificacionRepository;
import pe.agroconecta.venta.infraestructura.ReputacionRepository;
import pe.agroconecta.venta.infraestructura.VentaHistorialRepository;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceTest {

    private static final long AGRICULTOR = 3L;
    private static final long COMPRADOR = 12L;

    @Mock private CalificacionRepository calificaciones;
    @Mock private ReputacionRepository reputaciones;
    @Mock private VentaHistorialRepository ventas;
    @Mock private VentaService ventaService;
    @Mock private UsuarioConsultaService usuarios;
    @Mock private NotificacionService notificaciones;

    private CalificacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CalificacionService(calificaciones, reputaciones, ventas, ventaService, usuarios,
                notificaciones);
    }

    @Test
    void soloSeCalificanVentasTerminadasUnaVezYPorSusPartes() {
        when(ventas.findById(1L)).thenReturn(Optional.of(venta(EstadoTransaccion.ENTREGADO)));
        when(ventas.findById(2L)).thenReturn(Optional.of(venta(EstadoTransaccion.COMPLETADA)));
        when(calificaciones.existsByIdTransaccionAndIdEvaluador(2L, COMPRADOR)).thenReturn(true);

        assertThatThrownBy(() -> servicio.calificar(COMPRADOR, 1L, 5, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(CalificacionService.MENSAJE_NO_TERMINADA);
        assertThatThrownBy(() -> servicio.calificar(99L, 2L, 5, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.calificar(COMPRADOR, 2L, 5, null))
                .isInstanceOf(ConflictoException.class).hasMessage(CalificacionService.MENSAJE_YA_CALIFICADA);
        verify(calificaciones, never()).saveAndFlush(any());
    }

    @Test
    void laVentaInexistenteDa404() {
        when(ventas.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.calificar(COMPRADOR, 5L, 4, "Bien"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void laReputacionSinCalificacionesTienePromedioNulo() {
        when(usuarios.buscarResumenes(List.of(AGRICULTOR))).thenReturn(Map.of(AGRICULTOR,
                new UsuarioResumenDto(AGRICULTOR, "Juan", "987654321", "Piura", LocalDateTime.now(), "AGRICULTOR", null)));
        when(reputaciones.findById(AGRICULTOR)).thenReturn(Optional.empty());
        when(calificaciones.recibidas(any(Long.class), any(Pageable.class))).thenReturn(Page.empty());

        ReputacionDto r = servicio.reputacion(AGRICULTOR, 0);

        assertThat(r.promedio()).isNull();
        assertThat(r.total()).isZero();
        assertThat(r.calificaciones()).isEmpty();
    }

    @Test
    void elAdministradorYElUsuarioInexistenteNoTienenReputacion() {
        when(usuarios.buscarResumenes(List.of(1L))).thenReturn(Map.of(1L,
                new UsuarioResumenDto(1L, "Admin", "900000000", "Piura", LocalDateTime.now(), "ADMINISTRADOR", null)));
        when(usuarios.buscarResumenes(List.of(7L))).thenReturn(Map.of());

        assertThatThrownBy(() -> servicio.reputacion(1L, 0)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.reputacion(7L, 0)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.reputacion(7L, -1)).isInstanceOf(SolicitudInvalidaException.class);
    }

    private static VentaHistorial venta(EstadoTransaccion estado) {
        try {
            var constructor = VentaHistorial.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            VentaHistorial v = constructor.newInstance();
            ReflectionTestUtils.setField(v, "idComprador", COMPRADOR);
            ReflectionTestUtils.setField(v, "idAgricultor", AGRICULTOR);
            ReflectionTestUtils.setField(v, "estado", estado);
            ReflectionTestUtils.setField(v, "cultivo", "Mango");
            ReflectionTestUtils.setField(v, "variedad", "Kent");
            ReflectionTestUtils.setField(v, "montoTotal", BigDecimal.TEN);
            return v;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
