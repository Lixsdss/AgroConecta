package pe.agroconecta.notificacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.agroconecta.notificacion.dominio.Notificacion;
import pe.agroconecta.notificacion.dominio.TipoNotificacion;
import pe.agroconecta.notificacion.infraestructura.NotificacionRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificaciones;
    @InjectMocks private NotificacionService servicio;

    @Test
    void creaElAvisoSinLeerYRecortaElTexto() {
        servicio.crear(new NuevaNotificacionDto(5L, "OFERTA", "Nueva oferta", "x".repeat(400), 7L));

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).save(guardada.capture());
        Notificacion n = guardada.getValue();
        assertThat(n.getIdUsuario()).isEqualTo(5L);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacion.OFERTA);
        assertThat(n.getTitulo()).isEqualTo("Nueva oferta");
        assertThat(n.getContenido()).hasSize(NotificacionService.MAXIMO_CONTENIDO).endsWith("...");
        assertThat(n.getIdReferencia()).isEqualTo(7L);
        assertThat(n.isLeida()).isFalse();
    }

    @Test
    void noRepiteUnAvisoQueSigueSinLeer() {
        NuevaNotificacionDto aviso = new NuevaNotificacionDto(5L, "MENSAJE", "Nuevo mensaje", "Hola", 7L);
        when(notificaciones.existsByIdUsuarioAndTipoAndIdReferenciaAndLeidaFalse(5L, TipoNotificacion.MENSAJE, 7L))
                .thenReturn(true, false);

        assertThat(servicio.crearSiNoHayPendiente(aviso)).isFalse();
        verify(notificaciones, never()).save(any());
        assertThat(servicio.crearSiNoHayPendiente(aviso)).isTrue();
        verify(notificaciones).save(any());
    }
}
