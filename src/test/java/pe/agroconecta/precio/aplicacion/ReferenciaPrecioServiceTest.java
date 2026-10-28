package pe.agroconecta.precio.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.CultivoDto;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.precio.dominio.PrecioVigente;
import pe.agroconecta.precio.infraestructura.PrecioVigenteRepository;
import pe.agroconecta.precio.infraestructura.RangoPrecioPlataformaRepository;

@ExtendWith(MockitoExtension.class)
class ReferenciaPrecioServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 28);
    private static final int MANGO = 1;
    private static final int TAMBOGRANDE = 9;
    private static final int CHULUCANAS = 29;
    private static final int CASTILLA = 2;

    @Mock private PrecioVigenteRepository vigentes;
    @Mock private RangoPrecioPlataformaRepository rangos;
    @Mock private CatalogoService catalogo;

    private ReferenciaPrecioService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-09-28T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new ReferenciaPrecioService(vigentes, rangos, catalogo, reloj);
        lenient().when(rangos.findById(any())).thenReturn(Optional.empty());
        lenient().when(catalogo.listarCultivos())
                .thenReturn(List.of(new CultivoDto(MANGO, "Mango", 14, 2, "Jaba", new BigDecimal("20.000"))));
        lenient().when(catalogo.buscarUnidad(2)).thenReturn(Optional.of(new UnidadDto(2, "Jaba", "jabas", null)));
    }

    @Test
    void usaElPrecioDeLaZonaConToleranciaDeQuincePorCiento() {
        when(vigentes.findByIdCultivoOrderByZonaAsc(MANGO)).thenReturn(List.of(
                vigente(TAMBOGRANDE, "Tambogrande", HOY, "1.40"), vigente(CHULUCANAS, "Chulucanas", HOY, "1.60")));

        ReferenciaPrecioDto r = servicio.referencia(MANGO, TAMBOGRANDE).orElseThrow();

        assertThat(r.zona()).isEqualTo("Tambogrande");
        assertThat(r.esPromedioDeZonas()).isFalse();
        assertThat(r.minimoKg()).isEqualByComparingTo("1.19");
        assertThat(r.maximoKg()).isEqualByComparingTo("1.61");
        assertThat(r.precioUnidadLocal()).isEqualByComparingTo("28.00");
    }

    @Test
    void siElDistritoNoEsZonaPromediaLasZonas() {
        when(vigentes.findByIdCultivoOrderByZonaAsc(MANGO)).thenReturn(List.of(
                vigente(TAMBOGRANDE, "Tambogrande", HOY.minusDays(2), "1.40"),
                vigente(CHULUCANAS, "Chulucanas", HOY, "1.60")));

        ReferenciaPrecioDto r = servicio.referencia(MANGO, CASTILLA).orElseThrow();

        assertThat(r.esPromedioDeZonas()).isTrue();
        assertThat(r.zona()).isEqualTo(ReferenciaPrecioService.PROMEDIO_DE_ZONAS);
        assertThat(r.precioKg()).isEqualByComparingTo("1.50");
        assertThat(r.fecha()).isEqualTo(HOY);
    }

    @Test
    void losPreciosDeMasDeSieteDiasNoSirvenDeReferencia() {
        when(vigentes.findByIdCultivoOrderByZonaAsc(MANGO))
                .thenReturn(List.of(vigente(TAMBOGRANDE, "Tambogrande", HOY.minusDays(8), "1.40")));

        assertThat(servicio.referencia(MANGO, TAMBOGRANDE)).isEmpty();
        assertThat(servicio.evaluar(MANGO, TAMBOGRANDE, new BigDecimal("5"))).isEqualTo(EvaluacionPrecio.SIN_REFERENCIA);
    }

    @Test
    void evaluaBajoDentroYAlto() {
        when(vigentes.findByIdCultivoOrderByZonaAsc(MANGO))
                .thenReturn(List.of(vigente(TAMBOGRANDE, "Tambogrande", HOY.minusDays(7), "1.40")));

        assertThat(servicio.evaluar(MANGO, TAMBOGRANDE, new BigDecimal("1.18"))).isEqualTo(EvaluacionPrecio.BAJO);
        assertThat(servicio.evaluar(MANGO, TAMBOGRANDE, new BigDecimal("1.19"))).isEqualTo(EvaluacionPrecio.DENTRO);
        assertThat(servicio.evaluar(MANGO, TAMBOGRANDE, new BigDecimal("1.61"))).isEqualTo(EvaluacionPrecio.DENTRO);
        assertThat(servicio.evaluar(MANGO, TAMBOGRANDE, new BigDecimal("1.62"))).isEqualTo(EvaluacionPrecio.ALTO);
        assertThat(EvaluacionPrecio.ALTO.fueraDeRango()).isTrue();
        assertThat(EvaluacionPrecio.DENTRO.fueraDeRango()).isFalse();
    }

    private static PrecioVigente vigente(int idDistrito, String zona, LocalDate fecha, String precioKg) {
        try {
            var constructor = PrecioVigente.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PrecioVigente v = constructor.newInstance();
            ReflectionTestUtils.setField(v, "idCultivo", MANGO);
            ReflectionTestUtils.setField(v, "idDistrito", idDistrito);
            ReflectionTestUtils.setField(v, "cultivo", "Mango");
            ReflectionTestUtils.setField(v, "zona", zona);
            ReflectionTestUtils.setField(v, "fecha", fecha);
            ReflectionTestUtils.setField(v, "precioKg", new BigDecimal(precioKg));
            ReflectionTestUtils.setField(v, "unidadLocal", "Jaba");
            return v;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
