package pe.agroconecta.publicacion;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de la búsqueda multicriterio (RF-03, UC-03) contra MariaDB ({@code agroconecta_test}):
 * vista {@code v_publicaciones_disponibles}, filtros, orden, paginación y errores. Cada prueba se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BusquedaIntegracionTest {

    private static final int ID_KILO = 1;
    private static final int ID_JABA = 2;
    private static final int ID_SACO = 3;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private int idKent;
    private int idEdward;
    private int idSutil;
    private int idTambogrande;
    private int idChulucanas;
    private int idProvinciaMorropon;
    private String token;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        idKent = variedad("Mango", "Kent");
        idEdward = variedad("Mango", "Edward");
        idSutil = variedad("Limón", "Sutil");
        idTambogrande = jdbc.queryForObject("select id_distrito from distritos where nombre = 'Tambogrande'",
                Integer.class);
        idChulucanas = jdbc.queryForObject("select id_distrito from distritos where nombre = 'Chulucanas'",
                Integer.class);
        idProvinciaMorropon = jdbc.queryForObject("select id_provincia from distritos where nombre = 'Chulucanas'",
                Integer.class);
        token = registrar("912345681");
    }

    @Test
    void esPublicaYDevuelveLosDatosDeCadaResultado() throws Exception {
        long id = publicar(idKent, ID_JABA, "30", "20", "28", idTambogrande, hoy);

        mvc.perform(get("/api/v1/publicaciones").param("idDistrito", String.valueOf(idTambogrande)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].agricultor").value("Prueba de búsqueda"))
                .andExpect(jsonPath("$.content[0].calificacionAgricultor").doesNotExist())
                .andExpect(jsonPath("$.content[0].cultivo").value("Mango"))
                .andExpect(jsonPath("$.content[0].variedad").value("Kent"))
                .andExpect(jsonPath("$.content[0].unidad").value("Jaba"))
                .andExpect(jsonPath("$.content[0].unidadPlural").value("jabas"))
                .andExpect(jsonPath("$.content[0].disponibleKg").value(600.0))
                .andExpect(jsonPath("$.content[0].unidadesDisponibles").value(30.0))
                .andExpect(jsonPath("$.content[0].precioKg").value(1.4))
                .andExpect(jsonPath("$.content[0].provincia").value("Piura"))
                .andExpect(jsonPath("$.content[0].diasRestantes").value(14))
                .andExpect(jsonPath("$.content[0].porVencer").value(false));
    }

    @Test
    void combinaLosFiltrosYOrdenaPorPrecioPorKilo() throws Exception {
        long mangoCaro = publicar(idKent, ID_JABA, "10", "20", "40", idTambogrande, hoy);       // 2.00 por kilo
        long mangoBarato = publicar(idKent, ID_KILO, "100", "null", "1.2", idTambogrande, hoy); // 1.20 por kilo
        long edward = publicar(idEdward, ID_JABA, "10", "20", "30", idTambogrande, hoy);        // 1.50 por kilo
        publicar(idSutil, ID_SACO, "5", "45", "90", idTambogrande, hoy);                         // otro cultivo
        publicar(idKent, ID_JABA, "10", "20", "30", idChulucanas, hoy);                          // otro distrito

        String idMango = jdbc.queryForObject("select id_cultivo from cultivos where nombre = 'Mango'", String.class);
        mvc.perform(get("/api/v1/publicaciones").param("idCultivo", idMango)
                        .param("idDistrito", String.valueOf(idTambogrande)))
                .andExpect(jsonPath("$.content[*].id").value(contains(id(mangoBarato), id(edward), id(mangoCaro))));

        mvc.perform(get("/api/v1/publicaciones").param("idVariedad", String.valueOf(idKent))
                        .param("idDistrito", String.valueOf(idTambogrande)).param("idUnidad", String.valueOf(ID_JABA)))
                .andExpect(jsonPath("$.content[*].id").value(contains(id(mangoCaro))));
    }

    @Test
    void filtraPorProvinciaYPorFechaDeCosecha() throws Exception {
        long reciente = publicar(idKent, ID_JABA, "10", "20", "30", idChulucanas, hoy);
        long antigua = publicar(idKent, ID_JABA, "10", "20", "30", idChulucanas, hoy.minusDays(10));
        publicar(idKent, ID_JABA, "10", "20", "30", idTambogrande, hoy);

        mvc.perform(get("/api/v1/publicaciones").param("idProvincia", String.valueOf(idProvinciaMorropon))
                        .param("orden", "FECHA_COSECHA"))
                .andExpect(jsonPath("$.content[*].id").value(contains(id(reciente), id(antigua))));

        mvc.perform(get("/api/v1/publicaciones").param("idProvincia", String.valueOf(idProvinciaMorropon))
                        .param("cosechaDesde", hoy.minusDays(7).toString()).param("cosechaHasta", hoy.toString()))
                .andExpect(jsonPath("$.content[*].id").value(contains(id(reciente))));
    }

    @Test
    void noMuestraPublicacionesOcultasNiVencidas() throws Exception {
        long visible = publicar(idKent, ID_JABA, "10", "20", "30", idTambogrande, hoy);
        long oculta = publicar(idKent, ID_JABA, "10", "20", "30", idTambogrande, hoy);
        long vencida = publicar(idKent, ID_JABA, "10", "20", "30", idTambogrande, hoy);
        em.flush();
        jdbc.update("update publicaciones set estado = 'BLOQUEADA' where id_publicacion = ?", oculta);
        jdbc.update("update publicaciones set estado = 'VENCIDA' where id_publicacion = ?", vencida);
        em.clear();

        mvc.perform(get("/api/v1/publicaciones").param("idDistrito", String.valueOf(idTambogrande)))
                .andExpect(jsonPath("$.content[*].id").value(contains(id(visible))));
    }

    @Test
    void paginaDeVeinteEnVeinte() throws Exception {
        for (int i = 0; i < 21; i++) {
            publicar(idKent, ID_JABA, "10", "20", String.valueOf(20 + i), idChulucanas, hoy);
        }

        mvc.perform(get("/api/v1/publicaciones").param("idDistrito", String.valueOf(idChulucanas)))
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.page.totalElements").value(21))
                .andExpect(jsonPath("$.page.totalPages").value(2));
        mvc.perform(get("/api/v1/publicaciones").param("idDistrito", String.valueOf(idChulucanas))
                        .param("page", "1").param("size", "100"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].precioUnitario").value(40.0));
    }

    @Test
    void sinResultadosDevuelvePaginaVacia() throws Exception {
        mvc.perform(get("/api/v1/publicaciones").param("idDistrito", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void rechazaParametrosInvalidosCon400() throws Exception {
        mvc.perform(get("/api/v1/publicaciones").param("cosechaDesde", hoy.toString())
                        .param("cosechaHasta", hoy.minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha «desde» no puede ser posterior a la fecha «hasta»."));
        mvc.perform(get("/api/v1/publicaciones").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La página no puede ser negativa."));
        mvc.perform(get("/api/v1/publicaciones").param("orden", "BARATO"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/publicaciones").param("cosechaDesde", "28/09/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elDetalleIncluyeAgricultorDisponibilidadYDiasRestantes() throws Exception {
        long id = publicar(idKent, ID_JABA, "30", "20", "28", idTambogrande, hoy);

        mvc.perform(get("/api/v1/publicaciones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agricultor").value("Prueba de búsqueda"))
                .andExpect(jsonPath("$.calificacionAgricultor").doesNotExist())
                .andExpect(jsonPath("$.disponibleKg").value(600.0))
                .andExpect(jsonPath("$.unidadesDisponibles").value(30.0))
                .andExpect(jsonPath("$.diasRestantes").value(14))
                .andExpect(jsonPath("$.porVencer").value(false));
    }

    private static int id(long id) {
        return (int) id;
    }

    private int variedad(String cultivo, String variedad) {
        return jdbc.queryForObject("select v.id_variedad from variedades v join cultivos c using (id_cultivo) "
                + "where c.nombre = ? and v.nombre = ?", Integer.class, cultivo, variedad);
    }

    private long publicar(int idVariedad, int idUnidad, String cantidad, String peso, String precio, int idDistrito,
            LocalDate cosecha) throws Exception {
        String json = """
                {"idVariedad":%d,"idUnidad":%d,"cantidad":%s,"pesoUnidadKg":%s,"precioUnitario":%s,
                 "idDistrito":%d,"fechaCosecha":"%s","fechaLimite":null}"""
                .formatted(idVariedad, idUnidad, cantidad, peso, precio, idDistrito, cosecha);
        String respuesta = mvc.perform(post("/api/v1/publicaciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private String registrar(String celular) throws Exception {
        String json = """
                {"rol":"AGRICULTOR","nombreCompleto":"Prueba de búsqueda","celular":"%s","idDistrito":1,
                 "contrasena":"clave2026"}""".formatted(celular);
        String respuesta = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }
}
