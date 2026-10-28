package pe.agroconecta.publicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
 * Pruebas de integración de RF-01 contra MariaDB ({@code agroconecta_test}): API, seguridad, servicio y
 * {@code trg_publicaciones_bi}. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicacionIntegracionTest {

    private static final int ID_JABA = 2;
    private static final int ID_QUINTAL = 4;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private int idKent;
    private int idTambogrande;
    private String tokenAgricultor;

    @BeforeEach
    void preparar() throws Exception {
        idKent = jdbc.queryForObject("select v.id_variedad from variedades v join cultivos c using (id_cultivo) "
                + "where c.nombre = 'Mango' and v.nombre = 'Kent'", Integer.class);
        idTambogrande = jdbc.queryForObject("select id_distrito from distritos where ubigeo = '200114'",
                Integer.class);
        tokenAgricultor = registrar("AGRICULTOR", "912345671", "");
    }

    @Test
    void publicaEnJabasYCalculaKilosPrecioPorKiloYFechaLimite() throws Exception {
        LocalDate hoy = LocalDate.now();

        String respuesta = mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "30", "20", "28", hoy, null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/publicaciones/")))
                .andExpect(jsonPath("$.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.cultivo").value("Mango"))
                .andExpect(jsonPath("$.variedad").value("Kent"))
                .andExpect(jsonPath("$.unidadPlural").value("jabas"))
                .andExpect(jsonPath("$.distrito").value("Tambogrande"))
                .andExpect(jsonPath("$.cantidadKg").value(600.0))
                .andExpect(jsonPath("$.precioKg").value(1.4))
                .andExpect(jsonPath("$.fechaLimite").value(hoy.plusDays(14).toString()))
                .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        assertThat(marcaDe(id)).isEqualTo("PRIMERA_PUBLICACION");
    }

    @Test
    void soloLaPrimeraPublicacionDelAgricultorEntraALaCola() throws Exception {
        mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "30", "20", "28", LocalDate.now(), null)))
                .andExpect(status().isCreated());

        String segunda = mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "10", "20", "30", LocalDate.now(), null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(marcaDe(((Number) JsonPath.read(segunda, "$.id")).longValue())).isNull();
    }

    @Test
    void enQuintalesUsaElPesoDelCatalogoAunqueSeEnvieOtro() throws Exception {
        mvc.perform(publicar(tokenAgricultor, cuerpo(ID_QUINTAL, "10", "50", "92", LocalDate.now(), null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pesoUnidadKg").value(46.0))
                .andExpect(jsonPath("$.cantidadKg").value(460.0))
                .andExpect(jsonPath("$.precioKg").value(2.0));
    }

    @Test
    void jabaSinPesoDevuelve422() throws Exception {
        mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "30", null, "28", LocalDate.now(), null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Indique cuántos kilos pesa cada jaba o saco."));
    }

    @Test
    void fechaDeCosechaFuturaDevuelve400() throws Exception {
        mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "30", "20", "28", LocalDate.now().plusDays(1), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("fechaCosecha")));
    }

    @Test
    void fechaLimitePasadaDevuelve422() throws Exception {
        LocalDate hoy = LocalDate.now();
        mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "30", "20", "28", hoy.minusDays(5), hoy.minusDays(1))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La fecha límite no puede ser anterior a hoy."));
    }

    @Test
    void cosechaQueYaPasoSuVidaUtilDevuelve422() throws Exception {
        mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "30", "20", "28", LocalDate.now().minusDays(30), null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value(
                        "Con esa fecha de cosecha, el producto ya pasó su tiempo de venta. Revise la fecha."));
    }

    @Test
    void variedadInexistenteDevuelve422() throws Exception {
        String json = cuerpo(ID_JABA, "30", "20", "28", LocalDate.now(), null)
                .replace("\"idVariedad\":" + idKent, "\"idVariedad\":9999");
        mvc.perform(publicar(tokenAgricultor, json))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La variedad elegida no existe."));
    }

    @Test
    void unCompradorNoPuedePublicar() throws Exception {
        String tokenComprador = registrar("COMPRADOR", "912345672",
                ",\"nombreNegocio\":\"Hotel Costa\",\"tipoComprador\":\"HOTEL\"");

        mvc.perform(publicar(tokenComprador, cuerpo(ID_JABA, "30", "20", "28", LocalDate.now(), null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permiso para realizar esta acción."));
    }

    @Test
    void sinSesionNoSePuedePublicar() throws Exception {
        mvc.perform(post("/api/v1/publicaciones").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(ID_JABA, "30", "20", "28", LocalDate.now(), null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unaPublicacionBloqueadaSoloLaVeSuDueno() throws Exception {
        String respuesta = mvc.perform(publicar(tokenAgricultor, cuerpo(ID_JABA, "30", "20", "28", LocalDate.now(), null)))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        mvc.perform(get("/api/v1/publicaciones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcaRevision").doesNotExist());

        em.flush();
        jdbc.update("update publicaciones set estado = 'BLOQUEADA' where id_publicacion = ?", id);
        em.clear();

        mvc.perform(get("/api/v1/publicaciones/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La publicación no existe."));
        mvc.perform(get("/api/v1/publicaciones/" + id).header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));
    }

    @Test
    void publicacionInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/v1/publicaciones/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void elCatalogoDelFormularioEsPublico() throws Exception {
        mvc.perform(get("/api/v1/catalogo/cultivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.nombre == 'Mango')].unidadLocal").value(hasItem("Jaba")))
                .andExpect(jsonPath("$[?(@.nombre == 'Mango')].pesoSugeridoKg").value(hasItem(20.0)));
        mvc.perform(get("/api/v1/catalogo/variedades").param("idCultivo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre").value(hasItem("Kent")));
        mvc.perform(get("/api/v1/catalogo/unidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[1].nombre").value("Jaba"))
                .andExpect(jsonPath("$[1].factorKg").value(nullValue()))
                .andExpect(jsonPath("$[1].esPesoVariable").doesNotExist());
    }

    // ---------- Utilidades ----------

    private org.springframework.test.web.servlet.RequestBuilder publicar(String token, String json) {
        return post("/api/v1/publicaciones").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String cuerpo(int idUnidad, String cantidad, String peso, String precio, LocalDate cosecha,
            LocalDate limite) {
        return """
                {"idVariedad":%d,"idUnidad":%d,"cantidad":%s,"pesoUnidadKg":%s,"precioUnitario":%s,
                 "idDistrito":%d,"fechaCosecha":"%s","fechaLimite":%s}"""
                .formatted(idKent, idUnidad, cantidad, peso, precio, idTambogrande, cosecha,
                        limite == null ? "null" : "\"" + limite + "\"");
    }

    private String registrar(String rol, String celular, String extra) throws Exception {
        String json = """
                {"rol":"%s","nombreCompleto":"Prueba","celular":"%s","idDistrito":1,"contrasena":"clave2026"%s}"""
                .formatted(rol, celular, extra);
        String respuesta = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String marcaDe(long idPublicacion) {
        return jdbc.queryForObject("select marca_revision from publicaciones where id_publicacion = ?", String.class,
                idPublicacion);
    }
}
