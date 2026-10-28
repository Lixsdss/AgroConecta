package pe.agroconecta.catalogo;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de integración de los endpoints públicos del catálogo contra los datos de V2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listaLasOchoProvinciasSinIniciarSesion() throws Exception {
        mvc.perform(get("/api/v1/catalogo/provincias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].nombre").value("Ayabaca"));
    }

    @Test
    void listaLosSesentaYCincoDistritosConCuatroZonasDePrecio() throws Exception {
        mvc.perform(get("/api/v1/catalogo/distritos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(65)))
                .andExpect(jsonPath("$[?(@.esZonaPrecio == true)]", hasSize(4)));
    }

    @Test
    void filtraLosDistritosPorProvincia() throws Exception {
        mvc.perform(get("/api/v1/catalogo/distritos").param("idProvincia", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].provincia", everyItem(is("Piura"))));
    }

    @Test
    void provinciaInexistenteDevuelveListaVacia() throws Exception {
        mvc.perform(get("/api/v1/catalogo/distritos").param("idProvincia", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
