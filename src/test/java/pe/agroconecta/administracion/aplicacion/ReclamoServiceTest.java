package pe.agroconecta.administracion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.administracion.dominio.EstadoReclamo;
import pe.agroconecta.administracion.dominio.MotivoReclamo;
import pe.agroconecta.administracion.dominio.Reclamo;
import pe.agroconecta.administracion.infraestructura.ReclamoRepository;
import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.venta.aplicacion.VentaParaAdministracionDto;
import pe.agroconecta.venta.aplicacion.VentaService;

@ExtendWith(MockitoExtension.class)
class ReclamoServiceTest {

    private static final long COMPRADOR = 7L;
    private static final long AGRICULTOR = 3L;

    @Mock private ReclamoRepository reclamos;
    @Mock private VentaService ventas;
    @Mock private NotificacionService notificaciones;
    @Mock private EntityManager entityManager;

    private ReclamoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReclamoService(reclamos, ventas, notificaciones, entityManager);
    }

    @Test
    void registraElReclamoYAvisaALaOtraParte() {
        when(ventas.paraAdministracion(21L)).thenReturn(venta(true));
        when(reclamos.saveAndFlush(any(Reclamo.class))).thenAnswer(i -> conId(i.getArgument(0), 5L));

        ReclamoDto dto = servicio.registrar(COMPRADOR, 21L, MotivoReclamo.CANTIDAD_INCOMPLETA, "  Faltó una jaba. ");

        assertThat(dto.descripcion()).isEqualTo("Faltó una jaba.");
        assertThat(dto.reclamante()).isEqualTo("Restaurante El Chalán");
        assertThat(dto.contraparte()).isEqualTo("Juan Chunga");
        ArgumentCaptor<NuevaNotificacionDto> aviso = ArgumentCaptor.forClass(NuevaNotificacionDto.class);
        verify(notificaciones).crear(aviso.capture());
        assertThat(aviso.getValue().idUsuario()).isEqualTo(AGRICULTOR);
        assertThat(aviso.getValue().tipo()).isEqualTo("RECLAMO");
        assertThat(aviso.getValue().idReferencia()).isEqualTo(21L);
    }

    @Test
    void rechazaAjenosVentasSinAcuerdoYDuplicados() {
        when(ventas.paraAdministracion(21L)).thenReturn(venta(true));
        assertThatThrownBy(() -> servicio.registrar(99L, 21L, MotivoReclamo.MAL_TRATO, "x"))
                .isInstanceOf(AccessDeniedException.class);

        when(reclamos.existsByIdTransaccionAndIdReclamante(21L, COMPRADOR)).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrar(COMPRADOR, 21L, MotivoReclamo.MAL_TRATO, "x"))
                .isInstanceOf(ConflictoException.class).hasMessage(ReclamoService.MENSAJE_DUPLICADO);

        when(ventas.paraAdministracion(22L)).thenReturn(venta(false));
        assertThatThrownBy(() -> servicio.registrar(COMPRADOR, 22L, MotivoReclamo.MAL_TRATO, "x"))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(ReclamoService.MENSAJE_NO_ACORDADA);
        verify(reclamos, never()).saveAndFlush(any());
    }

    @Test
    void resuelveUnaVezYAvisaALasDosPartes() {
        Reclamo reclamo = conId(new Reclamo(21L, AGRICULTOR, MotivoReclamo.NO_SE_PRESENTO, "No vino."), 5L);
        when(reclamos.findById(5L)).thenReturn(Optional.of(reclamo));
        when(ventas.paraAdministracion(21L)).thenReturn(venta(true));

        ReclamoDto dto = servicio.resolver(1L, 5L, EstadoReclamo.INFUNDADO, "  Sí   se presentó. ");

        assertThat(dto.estado()).isEqualTo("INFUNDADO");
        assertThat(dto.respuesta()).isEqualTo("Sí se presentó.");
        ArgumentCaptor<NuevaNotificacionDto> avisos = ArgumentCaptor.forClass(NuevaNotificacionDto.class);
        verify(notificaciones, times(2)).crear(avisos.capture());
        assertThat(avisos.getAllValues()).extracting(NuevaNotificacionDto::idUsuario)
                .containsExactly(AGRICULTOR, COMPRADOR);
        assertThat(avisos.getAllValues().getFirst().contenido()).contains("infundado");

        assertThatThrownBy(() -> servicio.resolver(1L, 5L, EstadoReclamo.FUNDADO, "Otra."))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(ReclamoService.MENSAJE_RESUELTO);
        assertThatThrownBy(() -> servicio.resolver(1L, 5L, EstadoReclamo.PENDIENTE, "x"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.resolver(1L, 6L, EstadoReclamo.FUNDADO, "x"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listaPendientesDelMasAntiguoYConsultaLosPropios() {
        Reclamo reclamo = conId(new Reclamo(21L, COMPRADOR, MotivoReclamo.MAL_TRATO, "x"), 5L);
        when(ventas.paraAdministracion(21L)).thenReturn(venta(true));
        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        when(reclamos.findByEstado(any(), pedido.capture())).thenReturn(new PageImpl<>(List.of(reclamo)));
        when(reclamos.findAll(any(Pageable.class))).thenReturn(Page.empty());
        when(reclamos.findByIdReclamanteAndIdTransaccion(COMPRADOR, 21L)).thenReturn(List.of(reclamo));
        when(reclamos.findByIdReclamanteOrderByFechaRegistroDescIdDesc(COMPRADOR)).thenReturn(List.of());
        when(reclamos.findById(5L)).thenReturn(Optional.of(reclamo));

        assertThat(servicio.listar(EstadoReclamo.PENDIENTE, 0).getContent()).hasSize(1);
        assertThat(pedido.getValue().getSort().getOrderFor("fechaRegistro").isAscending()).isTrue();
        assertThat(servicio.listar(null, 0)).isEmpty();
        assertThatThrownBy(() -> servicio.listar(null, -1)).isInstanceOf(SolicitudInvalidaException.class);
        assertThat(servicio.mios(COMPRADOR, 21L)).hasSize(1);
        assertThat(servicio.mios(COMPRADOR, null)).isEmpty();
        assertThat(servicio.obtener(5L).venta().id()).isEqualTo(21L);
    }

    private static VentaParaAdministracionDto venta(boolean llegoAAcordada) {
        return new VentaParaAdministracionDto(21L, COMPRADOR, "Restaurante El Chalán", AGRICULTOR, "Juan Chunga",
                "Mango", "Kent", BigDecimal.TEN, "jaba", "jabas", new BigDecimal("200"), new BigDecimal("280"),
                "COMPLETADA", llegoAAcordada, List.of());
    }

    private static Reclamo conId(Reclamo reclamo, long id) {
        ReflectionTestUtils.setField(reclamo, "id", id);
        return reclamo;
    }
}
