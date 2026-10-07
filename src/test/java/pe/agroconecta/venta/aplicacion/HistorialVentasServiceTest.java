package pe.agroconecta.venta.aplicacion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.venta.infraestructura.CalificacionRepository;
import pe.agroconecta.venta.infraestructura.TransaccionRepository;
import pe.agroconecta.venta.infraestructura.VentaHistorialRepository;

@ExtendWith(MockitoExtension.class)
class HistorialVentasServiceTest {

    private static final FiltroVentasDto SIN_FILTROS = new FiltroVentasDto(null, null, null, null, null);

    @Mock private VentaHistorialRepository historial;
    @Mock private TransaccionRepository transacciones;
    @Mock private CalificacionRepository calificaciones;
    @Mock private CatalogoService catalogo;
    @Mock private ExportadorExcelVentas excel;

    private HistorialVentasService servicio;

    @BeforeEach
    void preparar() {
        servicio = new HistorialVentasService(historial, transacciones, calificaciones, catalogo, excel, Clock.systemDefaultZone());
    }

    @Test
    void rechazaGrupoFechasYPaginaInvalidos() {
        assertThatThrownBy(() -> servicio.listar(1L, new FiltroVentasDto("TODAS", null, null, null, null), 0))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(HistorialVentasService.MENSAJE_GRUPO);
        assertThatThrownBy(() -> servicio.listar(1L, new FiltroVentasDto(null, LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 1), null, null), 0))
                .hasMessage(HistorialVentasService.MENSAJE_FECHAS);
        assertThatThrownBy(() -> servicio.listar(1L, SIN_FILTROS, -1))
                .hasMessage(HistorialVentasService.MENSAJE_PAGINA);
        verifyNoInteractions(historial);
    }

    @Test
    void noExportaMasDeCincoMilVentas() {
        when(historial.count(any(Specification.class))).thenReturn(5001L);

        assertThatThrownBy(() -> servicio.exportar(1L, SIN_FILTROS))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(HistorialVentasService.MENSAJE_EXCEL_GRANDE);
        verifyNoInteractions(excel);
    }
}
