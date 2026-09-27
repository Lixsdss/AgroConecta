package pe.agroconecta.catalogo.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.catalogo.dominio.Distrito;
import pe.agroconecta.catalogo.dominio.Provincia;
import pe.agroconecta.catalogo.infraestructura.DistritoRepository;
import pe.agroconecta.catalogo.infraestructura.ProvinciaRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock private ProvinciaRepository provincias;
    @Mock private DistritoRepository distritos;

    private CatalogoService servicio;
    private Distrito tambogrande;

    @BeforeEach
    void preparar() {
        servicio = new CatalogoService(provincias, distritos);
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
