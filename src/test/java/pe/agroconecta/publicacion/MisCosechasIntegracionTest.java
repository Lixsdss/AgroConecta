package pe.agroconecta.publicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de "Mis ventas" y las acciones rápidas (RF-07, UC-09) y de la corrección de una cosecha
 * oculta (RF-09) contra MariaDB ({@code agroconecta_test}): {@code trg_publicaciones_bu} y la vista. Cada prueba se
 * revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MisCosechasIntegracionTest {

    private static final int ID_JABA = 2;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private String token;
    private long idPublicacion;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        token = registrar("912345721");
        idPublicacion = publicar(token, "30", "28");
    }

    @Test
    void listaMisCosechasConSusAcciones() throws Exception {
        long otra = publicar(token, "10", "30");
        cambiarEnBase("update publicaciones set estado = 'RETIRADA' where id_publicacion = ?", otra);

        mvc.perform(get("/api/v1/publicaciones/mias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains((int) idPublicacion)))
                .andExpect(jsonPath("$.content[0].estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.content[0].unidad").value("Jaba"))
                .andExpect(jsonPath("$.content[0].acciones").value(contains("BAJAR_PRECIO", "EXTENDER", "RETIRAR")));
        mvc.perform(get("/api/v1/publicaciones/mias").param("grupo", "CERRADAS").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content[*].id").value(contains((int) otra)))
                .andExpect(jsonPath("$.content[0].acciones", hasSize(0)));
        mvc.perform(get("/api/v1/publicaciones/mias").param("grupo", "TODAS").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/publicaciones/mias")).andExpect(status().isUnauthorized());
    }

    @Test
    void bajaElPrecioSoloAUnoMenor() throws Exception {
        mvc.perform(post("/api/v1/publicaciones/" + idPublicacion + "/precio").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"precioUnitario\":28}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El nuevo precio debe ser menor que el actual (S/ 28.00)."));
        mvc.perform(post("/api/v1/publicaciones/" + idPublicacion + "/precio").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"precioUnitario\":24}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUnitario").value(24.0))
                .andExpect(jsonPath("$.precioKg").value(1.2));
    }

    @Test
    void extenderDevuelveUnaVencidaALaVenta() throws Exception {
        // El trigger no acepta fechas límite pasadas: se simula el vencimiento solo con el estado (fecha límite hoy + 14)
        cambiarEnBase("update publicaciones set estado = 'VENCIDA' where id_publicacion = ?", idPublicacion);

        mvc.perform(post("/api/v1/publicaciones/" + idPublicacion + "/precio").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"precioUnitario\":20}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(extender(idPublicacion, 10)).andExpect(status().isBadRequest());
        mvc.perform(extender(idPublicacion, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.fechaLimite").value(hoy.plusDays(21).toString()))
                .andExpect(jsonPath("$.diasRestantes").value(21));
        mvc.perform(extender(idPublicacion, 14))
                .andExpect(jsonPath("$.fechaLimite").value(hoy.plusDays(35).toString()));
    }

    @Test
    void retiraYNoRetiraUnaOculta() throws Exception {
        mvc.perform(post("/api/v1/publicaciones/" + idPublicacion + "/retirar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RETIRADA"))
                .andExpect(jsonPath("$.acciones", hasSize(0)));
        mvc.perform(post("/api/v1/publicaciones/" + idPublicacion + "/retirar").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Esta cosecha ya no se puede retirar."));

        long oculta = publicar(token, "10", "30");
        cambiarEnBase("update publicaciones set estado = 'BLOQUEADA', marca_revision = null where id_publicacion = ?", oculta);
        mvc.perform(post("/api/v1/publicaciones/" + oculta + "/retirar").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value(
                        "Una cosecha oculta por la moderación no se retira; corríjala para que la revisen."));
    }

    @Test
    void corregirUnaOcultaLaDevuelveALaColaComoCorregida() throws Exception {
        mvc.perform(corregir(idPublicacion, "25", "20", "27"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se corrige una cosecha oculta por la moderación."));
        cambiarEnBase("update publicaciones set estado = 'BLOQUEADA', marca_revision = null where id_publicacion = ?",
                idPublicacion);

        mvc.perform(corregir(idPublicacion, "30", "20", "28"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No cambió ningún dato."));
        mvc.perform(corregir(idPublicacion, "25", "22", "27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.cantidadKg").value(550.0))
                .andExpect(jsonPath("$.acciones").value(contains("CORREGIR")));
        em.flush();
        assertThat(jdbc.queryForObject("select marca_revision from publicaciones where id_publicacion = ?", String.class,
                idPublicacion)).isEqualTo("CORREGIDA");
    }

    @Test
    void otroAgricultorNoTocaMiCosecha() throws Exception {
        String otro = registrar("912345722");

        mvc.perform(post("/api/v1/publicaciones/" + idPublicacion + "/retirar").header("Authorization", "Bearer " + otro))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/publicaciones/999999999/retirar").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private void cambiarEnBase(String sql, Object... parametros) {
        em.flush();
        jdbc.update(sql, parametros);
        em.clear();
    }

    private RequestBuilder extender(long id, int dias) {
        return post("/api/v1/publicaciones/" + id + "/extender").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":" + dias + "}");
    }

    private RequestBuilder corregir(long id, String cantidad, String peso, String precio) {
        return put("/api/v1/publicaciones/" + id + "/correccion").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":%s,\"pesoUnidadKg\":%s,\"precioUnitario\":%s}".formatted(cantidad, peso, precio));
    }

    private long publicar(String tokenAgricultor, String cantidad, String precio) throws Exception {
        int idKent = jdbc.queryForObject("select v.id_variedad from variedades v join cultivos c using (id_cultivo) "
                + "where c.nombre = 'Mango' and v.nombre = 'Kent'", Integer.class);
        String json = """
                {"idVariedad":%d,"idUnidad":%d,"cantidad":%s,"pesoUnidadKg":20,"precioUnitario":%s,
                 "idDistrito":1,"fechaCosecha":"%s","fechaLimite":null}""".formatted(idKent, ID_JABA, cantidad, precio, hoy);
        String respuesta = mvc.perform(post("/api/v1/publicaciones").header("Authorization", "Bearer " + tokenAgricultor)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private String registrar(String celular) throws Exception {
        String json = """
                {"rol":"AGRICULTOR","nombreCompleto":"Prueba","celular":"%s","idDistrito":1,"contrasena":"clave2026"}"""
                .formatted(celular);
        String respuesta = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }
}
