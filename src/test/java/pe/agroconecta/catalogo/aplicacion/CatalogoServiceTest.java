package pe.agroconecta.catalogo.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.catalogo.dominio.Cultivo;
import pe.agroconecta.catalogo.dominio.Distrito;
import pe.agroconecta.catalogo.dominio.Provincia;
import pe.agroconecta.catalogo.dominio.UnidadMedida;
import pe.agroconecta.catalogo.dominio.Variedad;
import pe.agroconecta.catalogo.infraestructura.CultivoRepository;
import pe.agroconecta.catalogo.infraestructura.DistritoRepository;
import pe.agroconecta.catalogo.infraestructura.ProvinciaRepository;
import pe.agroconecta.catalogo.infraestructura.UnidadMedidaRepository;
import pe.agroconecta.catalogo.infraestructura.VariedadRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock private ProvinciaRepository provincias;
    @Mock private DistritoRepository distritos;
    @Mock private CultivoRepository cultivos;
    @Mock private VariedadRepository variedades;
    @Mock private UnidadMedidaRepository unidades;

    private CatalogoService servicio;
    private Distrito tambogrande;

    @BeforeEach
    void preparar() {
        servicio = new CatalogoService(provincias, distritos, cultivos, variedades, unidades);
        tambogrande = distrito(9, "Tambogrande", provincia(1, "Piura"), true);
    }

    @Test
    void listaProvinciasComoDto() {
        when(provincias.findAllByOrderByNombreAsc()).thenReturn(List.of(provincia(2, "Ayabaca")));

        assertThat(servicio.listarProvincias()).containsExactly(new ProvinciaDto(2, "Ayabaca"));
    }

    @Test
    void listaTodosLosDistritosSinFiltro() {
        when(distritos.listarTodos()).thenReturn(List.of(tambogrande));

        assertThat(servicio.listarDistritos(null))
                .containsExactly(new DistritoDto(9, "Tambogrande", 1, "Piura", true));
        verify(distritos, never()).listarPorProvincia(1);
    }

    @Test
    void filtraLosDistritosPorProvincia() {
        when(distritos.listarPorProvincia(1)).thenReturn(List.of(tambogrande));

        assertThat(servicio.listarDistritos(1)).hasSize(1);
    }

    @Test
    void buscaDistritosPorIdYPorUbigeo() {
        when(distritos.buscarConProvincia(9)).thenReturn(Optional.of(tambogrande));
        when(distritos.buscarPorUbigeo("200114")).thenReturn(Optional.of(tambogrande));

        assertThat(servicio.buscarDistrito(9)).map(DistritoDto::nombre).contains("Tambogrande");
        assertThat(servicio.buscarDistrito(null)).isEmpty();
        assertThat(servicio.buscarDistritoPorUbigeo("200114")).isPresent();
    }

    @Test
    void indicaSiExisteUnDistrito() {
        when(distritos.existsById(9)).thenReturn(true);

        assertThat(servicio.existeDistrito(9)).isTrue();
        assertThat(servicio.existeDistrito(null)).isFalse();
    }

    @Test
    void listaCultivosConSuUnidadLocalYPesoSugerido() {
        when(cultivos.listarActivos()).thenReturn(List.of(mango()));

        assertThat(servicio.listarCultivos())
                .containsExactly(new CultivoDto(1, "Mango", 14, 2, "Jaba", new BigDecimal("20.000")));
    }

    @Test
    void listaYBuscaVariedadesConSuCultivo() {
        Variedad kent = variedad(1, "Kent", mango());
        when(variedades.listarTodas()).thenReturn(List.of(kent));
        when(variedades.listarPorCultivo(1)).thenReturn(List.of(kent));
        when(variedades.buscarConCultivo(1)).thenReturn(Optional.of(kent));

        VariedadDto esperado = new VariedadDto(1, "Kent", 1, "Mango", 14);
        assertThat(servicio.listarVariedades(null)).containsExactly(esperado);
        assertThat(servicio.listarVariedades(1)).containsExactly(esperado);
        assertThat(servicio.buscarVariedad(1)).contains(esperado);
        assertThat(servicio.buscarVariedad(null)).isEmpty();
    }

    @Test
    void listaYBuscaUnidadesIndicandoLasDePesoVariable() {
        UnidadMedida jaba = unidad(2, "Jaba", "jabas", null);
        UnidadMedida kilo = unidad(1, "Kilo", "kilos", new BigDecimal("1.000"));
        when(unidades.findAllByOrderByIdAsc()).thenReturn(List.of(kilo, jaba));
        when(unidades.findById(2)).thenReturn(Optional.of(jaba));

        assertThat(servicio.listarUnidades()).extracting(UnidadDto::esPesoVariable).containsExactly(false, true);
        assertThat(servicio.buscarUnidad(2)).map(UnidadDto::nombrePlural).contains("jabas");
        assertThat(servicio.buscarUnidad(null)).isEmpty();
    }

    private static Cultivo mango() {
        Cultivo cultivo = instanciar(Cultivo.class);
        ReflectionTestUtils.setField(cultivo, "id", 1);
        ReflectionTestUtils.setField(cultivo, "nombre", "Mango");
        ReflectionTestUtils.setField(cultivo, "vidaUtilDias", (short) 14);
        ReflectionTestUtils.setField(cultivo, "unidadLocal", unidad(2, "Jaba", "jabas", null));
        ReflectionTestUtils.setField(cultivo, "pesoSugeridoKg", new BigDecimal("20.000"));
        ReflectionTestUtils.setField(cultivo, "activo", true);
        return cultivo;
    }

    private static Variedad variedad(int id, String nombre, Cultivo cultivo) {
        Variedad variedad = instanciar(Variedad.class);
        ReflectionTestUtils.setField(variedad, "id", id);
        ReflectionTestUtils.setField(variedad, "nombre", nombre);
        ReflectionTestUtils.setField(variedad, "cultivo", cultivo);
        return variedad;
    }

    private static UnidadMedida unidad(int id, String nombre, String plural, BigDecimal factor) {
        UnidadMedida unidad = instanciar(UnidadMedida.class);
        ReflectionTestUtils.setField(unidad, "id", id);
        ReflectionTestUtils.setField(unidad, "nombre", nombre);
        ReflectionTestUtils.setField(unidad, "nombrePlural", plural);
        ReflectionTestUtils.setField(unidad, "factorKg", factor);
        return unidad;
    }

    private static Provincia provincia(int id, String nombre) {
        Provincia provincia = instanciar(Provincia.class);
        ReflectionTestUtils.setField(provincia, "id", id);
        ReflectionTestUtils.setField(provincia, "nombre", nombre);
        return provincia;
    }

    private static Distrito distrito(int id, String nombre, Provincia provincia, boolean zona) {
        Distrito distrito = instanciar(Distrito.class);
        ReflectionTestUtils.setField(distrito, "id", id);
        ReflectionTestUtils.setField(distrito, "nombre", nombre);
        ReflectionTestUtils.setField(distrito, "provincia", provincia);
        ReflectionTestUtils.setField(distrito, "esZonaPrecio", zona);
        return distrito;
    }

    private static <T> T instanciar(Class<T> tipo) {
        try {
            var constructor = tipo.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
