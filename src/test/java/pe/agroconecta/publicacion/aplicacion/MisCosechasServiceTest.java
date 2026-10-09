package pe.agroconecta.publicacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.publicacion.dominio.EstadoPublicacion;
import pe.agroconecta.publicacion.dominio.Publicacion;
import pe.agroconecta.publicacion.infraestructura.PublicacionDisponibleRepository;
import pe.agroconecta.publicacion.infraestructura.PublicacionRepository;

@ExtendWith(MockitoExtension.class)
class MisCosechasServiceTest {

    private static final long AGRICULTOR = 3L;
    private static final LocalDate HOY = LocalDate.of(2026, 10, 9);

    @Mock private PublicacionRepository publicaciones;
    @Mock private PublicacionDisponibleRepository disponibles;
    @Mock private CatalogoService catalogo;
    @Mock private EntityManager entityManager;

    private MisCosechasService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new MisCosechasService(publicaciones, disponibles, catalogo, entityManager, reloj);
    }

    @Test
    void lasAccionesDependenDelEstado() {
        assertThat(MisCosechasService.acciones(EstadoPublicacion.PUBLICADA))
                .containsExactly(AccionCosecha.BAJAR_PRECIO, AccionCosecha.EXTENDER, AccionCosecha.RETIRAR);
        assertThat(MisCosechasService.acciones(EstadoPublicacion.VENCIDA))
                .containsExactly(AccionCosecha.EXTENDER, AccionCosecha.RETIRAR);
        assertThat(MisCosechasService.acciones(EstadoPublicacion.BLOQUEADA)).containsExactly(AccionCosecha.CORREGIR);
        assertThat(MisCosechasService.acciones(EstadoPublicacion.VENDIDA)).isEmpty();
        assertThat(MisCosechasService.acciones(EstadoPublicacion.RETIRADA)).isEmpty();
    }

    @Test
    void unaVencidaConFechaPasadaSeExtiendeDesdeHoy() {
        Publicacion p = publicacion(EstadoPublicacion.VENCIDA, HOY.minusDays(3));
        when(publicaciones.findById(15L)).thenReturn(Optional.of(p));
        // La vista vacía corta el método después de guardar: aquí solo interesa la fecha que se guardó
        when(disponibles.findById(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.extender(AGRICULTOR, 15L, 7)).isInstanceOf(java.util.NoSuchElementException.class);

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicaciones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getFechaLimite()).isEqualTo(HOY.plusDays(7));
    }

    @Test
    void validaDiasEstadosYDueno() {
        when(publicaciones.findById(15L)).thenReturn(Optional.of(publicacion(EstadoPublicacion.VENDIDA, HOY)));
        when(publicaciones.findById(16L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.extender(AGRICULTOR, 15L, 10)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.extender(AGRICULTOR, 15L, 7))
                .hasMessage(MisCosechasService.MENSAJE_EXTENDER_ESTADO);
        assertThatThrownBy(() -> servicio.retirar(AGRICULTOR, 15L)).hasMessage(MisCosechasService.MENSAJE_RETIRAR_ESTADO);
        assertThatThrownBy(() -> servicio.bajarPrecio(AGRICULTOR, 15L, BigDecimal.ONE))
                .hasMessage(MisCosechasService.MENSAJE_PRECIO_ESTADO);
        assertThatThrownBy(() -> servicio.corregir(AGRICULTOR, 15L, BigDecimal.ONE, null, BigDecimal.ONE))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(MisCosechasService.MENSAJE_CORREGIR_ESTADO);
        assertThatThrownBy(() -> servicio.retirar(99L, 15L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.retirar(AGRICULTOR, 16L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(publicaciones, never()).saveAndFlush(any());
    }

    @Test
    void enUnidadDePesoFijoLaCorreccionIgnoraElPeso() {
        Publicacion p = publicacion(EstadoPublicacion.BLOQUEADA, HOY.plusDays(5));
        when(publicaciones.findById(15L)).thenReturn(Optional.of(p));
        when(catalogo.buscarUnidad(4)).thenReturn(Optional.of(new UnidadDto(4, "Quintal", "quintales",
                new BigDecimal("46.000"))));

        assertThatThrownBy(() -> servicio.corregir(AGRICULTOR, 15L, new BigDecimal("30"), new BigDecimal("50"),
                new BigDecimal("28"))).hasMessage(MisCosechasService.MENSAJE_SIN_CAMBIOS);
        assertThatThrownBy(() -> servicio.listar(AGRICULTOR, "TODAS", 0)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.listar(AGRICULTOR, null, -1)).isInstanceOf(SolicitudInvalidaException.class);
        assertThat(List.of(7, 14, 30)).allMatch(MisCosechasService.DIAS_EXTENSION::contains);
    }

    private static Publicacion publicacion(EstadoPublicacion estado, LocalDate fechaLimite) {
        Publicacion p = new Publicacion(AGRICULTOR, 1, 9, 4, new BigDecimal("30"), new BigDecimal("46"),
                new BigDecimal("28"), HOY.minusDays(10), fechaLimite);
        ReflectionTestUtils.setField(p, "id", 15L);
        ReflectionTestUtils.setField(p, "estado", estado);
        return p;
    }
}
