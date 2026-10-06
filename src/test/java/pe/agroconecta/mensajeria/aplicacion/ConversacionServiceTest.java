package pe.agroconecta.mensajeria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.mensajeria.dominio.CanalInicio;
import pe.agroconecta.mensajeria.dominio.Conversacion;
import pe.agroconecta.mensajeria.infraestructura.ConversacionRepository;
import pe.agroconecta.mensajeria.infraestructura.MensajeRepository;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.publicacion.aplicacion.PublicacionResumenDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;

@ExtendWith(MockitoExtension.class)
class ConversacionServiceTest {

    private static final long AGRICULTOR = 3L;
    private static final long COMPRADOR = 12L;
    private static final long TERCERO = 99L;

    @Mock private ConversacionRepository conversaciones;
    @Mock private MensajeRepository mensajes;
    @Mock private PublicacionConsultaService publicaciones;
    @Mock private UsuarioConsultaService usuarios;
    @Mock private NotificacionService notificaciones;
    @Mock private EntityManager entityManager;

    private ConversacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ConversacionService(conversaciones, mensajes, publicaciones, usuarios, notificaciones,
                entityManager);
    }

    @Test
    void elEnlaceDeWhatsappLlevaElCelularYElTextoCodificado() {
        String enlace = ConversacionService.enlaceWhatsapp(agricultor(), publicacion("PUBLICADA", "Kent"));

        assertThat(enlace).isEqualTo("https://wa.me/51987654321?text=Hola%2C%20Juan%20Carlos%20Chunga.%20Vi%20su%20"
                + "Mango%20Kent%20en%20AgroConecta%20%2830%20jabas%20a%20S%2F%2028.00%20por%20jaba%29.%20"
                + "%C2%BFSigue%20disponible%3F");
        assertThat(ConversacionService.enlaceWhatsapp(agricultor(), publicacion("PUBLICADA", "Común")))
                .contains("Mango%20com%C3%BAn");
    }

    @Test
    void noAbreUnaConversacionNuevaSobreUnaPublicacionNoDisponible() {
        when(publicaciones.buscar(15L)).thenReturn(Optional.of(publicacion("VENDIDA", "Kent")));
        when(conversaciones.findByIdPublicacionAndIdComprador(15L, COMPRADOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.abrir(COMPRADOR, 15L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(ConversacionService.MENSAJE_NO_DISPONIBLE);
        verify(conversaciones, never()).saveAndFlush(any());
    }

    @Test
    void laPublicacionInexistenteDa404() {
        when(publicaciones.buscar(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.whatsapp(COMPRADOR, 15L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unTerceroRecibe403() {
        when(conversaciones.findById(7L)).thenReturn(Optional.of(conversacion()));
        when(publicaciones.buscar(15L)).thenReturn(Optional.of(publicacion("PUBLICADA", "Kent")));

        assertThatThrownBy(() -> servicio.mensajes(TERCERO, 7L, null)).isInstanceOf(AccessDeniedException.class);
        verify(mensajes, never()).marcarLeidos(any(), any());
    }

    @Test
    void laOfertaNoSuperaLoDisponible() {
        when(conversaciones.findById(7L)).thenReturn(Optional.of(conversacion()));
        when(publicaciones.buscar(15L)).thenReturn(Optional.of(publicacion("PUBLICADA", "Kent")));

        assertThatThrownBy(() -> servicio.enviar(COMPRADOR, 7L,
                        new NuevoMensajeDto("OFERTA", null, new BigDecimal("30.5"), new BigDecimal("27"))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La oferta supera lo disponible (≈ 30 jabas).");
        verifyNoInteractions(notificaciones);
    }

    @Test
    void unAgricultorSinPublicacionesNoTieneConversaciones() {
        when(publicaciones.idsDeAgricultor(AGRICULTOR)).thenReturn(List.of());

        assertThat(servicio.listar(AGRICULTOR, "AGRICULTOR", 0)).isEmpty();
        assertThat(servicio.noLeidos(AGRICULTOR, "AGRICULTOR")).isZero();
        verifyNoInteractions(conversaciones, mensajes);
        assertThatThrownBy(() -> servicio.listar(COMPRADOR, "COMPRADOR", -1))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private static Conversacion conversacion() {
        Conversacion c = new Conversacion(15L, COMPRADOR, CanalInicio.CHAT);
        ReflectionTestUtils.setField(c, "id", 7L);
        return c;
    }

    private static UsuarioResumenDto agricultor() {
        return new UsuarioResumenDto(AGRICULTOR, "Juan Carlos Chunga", "987654321", "Tambogrande",
                LocalDateTime.now(), "AGRICULTOR", null);
    }

    private static PublicacionResumenDto publicacion(String estado, String variedad) {
        return new PublicacionResumenDto(15L, AGRICULTOR, "Juan Carlos Chunga", "Mango", variedad, "Tambogrande",
                estado, "Jaba", "jabas", new BigDecimal("20.000"), new BigDecimal("28.00"), new BigDecimal("1.4000"),
                new BigDecimal("600.00"), new BigDecimal("30.00"), 5);
    }
}
