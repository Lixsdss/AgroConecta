package pe.agroconecta.publicacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.publicacion.dominio.PublicacionDisponible;
import pe.agroconecta.publicacion.infraestructura.PublicacionDisponibleRepository;

@ExtendWith(MockitoExtension.class)
class BusquedaServiceTest {

    private static final FiltroBusquedaDto SIN_FILTROS = new FiltroBusquedaDto(null, null, null, null, null, null,
            null);

    @Mock
    private PublicacionDisponibleRepository disponibles;
    @Mock
    private CatalogoService catalogo;

    private BusquedaService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-06T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new BusquedaService(disponibles, catalogo, reloj);
    }

    @Test
    void ordenaPorPrecioPorKiloPorDefectoYPideVeintePorPagina() {
        when(catalogo.listarUnidades()).thenReturn(List.of());
        when(disponibles.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        servicio.buscar(SIN_FILTROS, null, 2);

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(disponibles).findAll(any(Specification.class), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(BusquedaService.TAMANO_PAGINA);
        assertThat(pagina.getValue().getSort()).isEqualTo(OrdenBusqueda.PRECIO_KG.comoSort());
    }

    @Test
    void elOrdenPorCalificacionEsDescendenteYDesempataPorPrecio() {
        Sort orden = OrdenBusqueda.CALIFICACION.comoSort();

        assertThat(orden.getOrderFor("calificacionAgricultor").isDescending()).isTrue();
        assertThat(orden.getOrderFor("precioKg").isAscending()).isTrue();
        assertThat(OrdenBusqueda.FECHA_COSECHA.comoSort().getOrderFor("fechaCosecha").isDescending()).isTrue();
    }

    @Test
    void convierteCadaResultadoConLaUnidadEnSingular() {
        when(catalogo.listarUnidades()).thenReturn(List.of(new UnidadDto(2, "Jaba", "jabas", null)));
        when(disponibles.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(disponible("510.00", "20.000", 1))));

        PublicacionDisponibleDto r = servicio.buscar(SIN_FILTROS, OrdenBusqueda.FECHA_COSECHA, 0).getContent().get(0);

        assertThat(r.unidad()).isEqualTo("Jaba");
        assertThat(r.unidadPlural()).isEqualTo("jabas");
        assertThat(r.unidadesDisponibles()).isEqualByComparingTo("25.50");
        assertThat(r.porVencer()).isTrue();
    }

    @Test
    void lasUnidadesDisponiblesSeRedondeanHaciaAbajo() {
        assertThat(BusquedaService.unidadesDisponibles(new BigDecimal("599.99"), new BigDecimal("20")))
                .isEqualByComparingTo("29.99");
        assertThat(BusquedaService.unidadesDisponibles(new BigDecimal("100"), new BigDecimal("46")))
                .isEqualByComparingTo("2.17");
    }

    @Test
    void rechazaRangoDeFechasInvertidoYPaginaNegativa() {
        FiltroBusquedaDto invertido = new FiltroBusquedaDto(null, null, null, null, null,
                LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> servicio.buscar(invertido, null, 0))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(BusquedaService.MENSAJE_RANGO_FECHAS);
        assertThatThrownBy(() -> servicio.buscar(SIN_FILTROS, null, -1))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(BusquedaService.MENSAJE_PAGINA);
        verifyNoInteractions(disponibles);
    }

    private static PublicacionDisponible disponible(String disponibleKg, String pesoUnidadKg, int porVencer) {
        try {
            var constructor = PublicacionDisponible.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PublicacionDisponible p = constructor.newInstance();
            ReflectionTestUtils.setField(p, "id", 7L);
            ReflectionTestUtils.setField(p, "idUnidad", 2);
            ReflectionTestUtils.setField(p, "unidadPlural", "jabas");
            ReflectionTestUtils.setField(p, "disponibleKg", new BigDecimal(disponibleKg));
            ReflectionTestUtils.setField(p, "pesoUnidadKg", new BigDecimal(pesoUnidadKg));
            ReflectionTestUtils.setField(p, "porVencer", porVencer);
            return p;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
