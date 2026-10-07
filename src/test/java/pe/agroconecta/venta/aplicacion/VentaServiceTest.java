package pe.agroconecta.venta.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.mensajeria.aplicacion.ConversacionService;
import pe.agroconecta.mensajeria.aplicacion.ParticipantesDto;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.publicacion.aplicacion.PublicacionResumenDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.venta.dominio.EstadoTransaccion;
import pe.agroconecta.venta.dominio.Transaccion;
import pe.agroconecta.venta.infraestructura.HistorialRepository;
import pe.agroconecta.venta.infraestructura.TransaccionRepository;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final long AGRICULTOR = 3L;
    private static final long COMPRADOR = 12L;
    private static final ParticipantesDto PARTES = new ParticipantesDto(7L, 15L, COMPRADOR, AGRICULTOR);
    private static final UnidadDto JABA = new UnidadDto(2, "Jaba", "jabas", null);
    private static final UnidadDto SACO = new UnidadDto(3, "Saco", "sacos", null);

    @Mock private TransaccionRepository transacciones;
    @Mock private HistorialRepository historial;
    @Mock private ConversacionService conversaciones;
    @Mock private PublicacionConsultaService publicaciones;
    @Mock private UsuarioConsultaService usuarios;
    @Mock private CatalogoService catalogo;
    @Mock private NotificacionService notificaciones;
    @Mock private EntityManager entityManager;

    private VentaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new VentaService(transacciones, historial, conversaciones, publicaciones, usuarios, catalogo,
                notificaciones, entityManager);
    }

    @Test
    void lasAccionesSiguenLasTransicionesDelTrigger() {
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.ACORDADA, true))
                .containsExactly(AccionVenta.CONFIRMAR_RECEPCION, AccionVenta.RECOGIDO, AccionVenta.CANCELAR);
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.ACORDADA, false))
                .containsExactly(AccionVenta.RECOGIDO, AccionVenta.CANCELAR);
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.RECOGIDO, false))
                .containsExactly(AccionVenta.EN_CAMINO);
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.EN_CAMINO, true))
                .containsExactly(AccionVenta.CONFIRMAR_RECEPCION);
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.EN_CAMINO, false)).isEmpty();
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.ENTREGADO, true))
                .containsExactly(AccionVenta.CONFIRMAR_RECEPCION);
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.PROPUESTA, true)).isEmpty();
        assertThat(VentaService.accionesPermitidas(EstadoTransaccion.COMPLETADA, true)).isEmpty();
    }

    @Test
    void unTerceroNoRegistraAcuerdos() {
        when(conversaciones.participantes(7L)).thenReturn(PARTES);

        assertThatThrownBy(() -> servicio.registrar(99L, nueva(2, "5", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(transacciones, never()).saveAndFlush(any());
    }

    @Test
    void unaJabaDistintaDeLaPublicadaPideElPeso() {
        when(conversaciones.participantes(7L)).thenReturn(PARTES);
        when(catalogo.buscarUnidad(3)).thenReturn(Optional.of(SACO));
        when(publicaciones.bloquearParaVenta(15L)).thenReturn(Optional.of(publicacion("PUBLICADA")));

        assertThatThrownBy(() -> servicio.registrar(COMPRADOR, nueva(3, "2", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(VentaService.MENSAJE_PESO);
    }

    @Test
    void noRegistraSiLaCosechaNoEstaDisponibleOFaltanKilos() {
        when(conversaciones.participantes(7L)).thenReturn(PARTES);
        when(catalogo.buscarUnidad(2)).thenReturn(Optional.of(JABA));
        when(publicaciones.bloquearParaVenta(15L))
                .thenReturn(Optional.of(publicacion("VENDIDA")), Optional.of(publicacion("PUBLICADA")));

        assertThatThrownBy(() -> servicio.registrar(COMPRADOR, nueva(2, "5", null)))
                .hasMessage(VentaService.MENSAJE_NO_DISPONIBLE);
        assertThatThrownBy(() -> servicio.registrar(COMPRADOR, nueva(2, "31", null)))
                .hasMessage("La cantidad supera lo disponible (≈ 600 kg).");
        verify(transacciones, never()).saveAndFlush(any());
    }

    @Test
    void validaElCambioDeEstadoConMensajesClaros() {
        Transaccion recogida = venta(EstadoTransaccion.RECOGIDO);
        Transaccion terminada = venta(EstadoTransaccion.COMPLETADA);
        Transaccion acordada = venta(EstadoTransaccion.ACORDADA);
        when(transacciones.bloquear(anyLong())).thenReturn(Optional.of(recogida), Optional.of(terminada),
                Optional.of(acordada), Optional.of(recogida));
        when(conversaciones.participantes(7L)).thenReturn(PARTES);

        assertThatThrownBy(() -> servicio.cambiarEstado(COMPRADOR, 1L, AccionVenta.CANCELAR))
                .hasMessage(VentaService.MENSAJE_CANCELAR_TARDE);
        assertThatThrownBy(() -> servicio.cambiarEstado(COMPRADOR, 1L, AccionVenta.RECOGIDO))
                .hasMessage(VentaService.MENSAJE_TERMINADA);
        assertThatThrownBy(() -> servicio.cambiarEstado(AGRICULTOR, 1L, AccionVenta.CONFIRMAR_RECEPCION))
                .hasMessage(VentaService.MENSAJE_SOLO_COMPRADOR);
        assertThatThrownBy(() -> servicio.cambiarEstado(COMPRADOR, 1L, AccionVenta.RECOGIDO))
                .hasMessage(VentaService.MENSAJE_FUERA_DE_SECUENCIA);
        verify(transacciones, never()).saveAndFlush(any());
    }

    @Test
    void laVentaInexistenteDa404() {
        when(transacciones.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(COMPRADOR, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    private static NuevaVentaDto nueva(int idUnidad, String cantidad, String peso) {
        return new NuevaVentaDto(7L, idUnidad, new BigDecimal(cantidad), peso == null ? null : new BigDecimal(peso),
                new BigDecimal("28"), LocalDate.now());
    }

    private static Transaccion venta(EstadoTransaccion estado) {
        Transaccion t = Transaccion.acordada(7L, 2, new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("28"),
                LocalDate.now(), COMPRADOR);
        t.cambiarEstado(estado, COMPRADOR);
        return t;
    }

    private static PublicacionResumenDto publicacion(String estado) {
        return new PublicacionResumenDto(15L, AGRICULTOR, "Juan Carlos Chunga", "Mango", "Kent", "Tambogrande",
                estado, "Jaba", "jabas", new BigDecimal("20.000"), new BigDecimal("28.00"), new BigDecimal("1.4000"),
                new BigDecimal("600.00"), new BigDecimal("30.00"), 5, 2);
    }
}
