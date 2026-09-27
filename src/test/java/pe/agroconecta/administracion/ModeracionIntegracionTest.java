package pe.agroconecta.administracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de la moderación posterior (RF-09) contra MariaDB: API, seguridad, servicio,
 * {@code trg_revisiones_bi}/{@code trg_revisiones_ai} y la marca CANTIDAD_INUSUAL. Cada prueba se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModeracionIntegracionTest {

    private static final int ID_JABA = 2;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenAgricultor;
    private long idAgricultor;
    private int idKent;

    @BeforeEach
    void preparar() throws Exception {
        jdbc.update("insert into usuarios (rol, nombre_completo, celular, password_hash, id_distrito) "
                + "values ('ADMINISTRADOR', 'Lucía Farfán', '900000009', ?, 1)", passwordEncoder.encode("admin2026"));
        tokenAdmin = iniciarSesion("900000009", "admin2026");
        tokenAgricultor = registrarAgricultor("913000001");
        idAgricultor = jdbc.queryForObject("select id_usuario from usuarios where celular = '913000001'", Long.class);
        idKent = jdbc.queryForObject("select v.id_variedad from variedades v join cultivos c using (id_cultivo) "
                + "where c.nombre = 'Mango' and v.nombre = 'Kent'", Integer.class);
    }

    @Test
    void laPrimeraPublicacionApareceEnLaColaConSuAgricultor() throws Exception {
        long id = publicar(1, "30");

        mvc.perform(get("/api/v1/moderacion/publicaciones").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].marcaRevision").value(hasItem("PRIMERA_PUBLICACION")))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].agricultor.nombreCompleto").value(hasItem("Juan Prueba")))
                .andExpect(jsonPath("$.page.totalElements").isNumber());
    }

    @Test
    void conformeLaSacaDeLaColaSinAvisarAlAgricultor() throws Exception {
        long id = publicar(1, "30");

        mvc.perform(decidir(id, "{\"decision\":\"CONFORME\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPublicacion").value("PUBLICADA"));

        assertThat(marcaDe(id)).isNull();
        assertThat(avisosDeModeracion()).isZero();
        mvc.perform(get("/api/v1/moderacion/publicaciones").header("Authorization", bearer(tokenAdmin)))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) id))));
    }

    @Test
    void advertirMantieneLaPublicacionYAvisaAlAgricultor() throws Exception {
        long id = publicar(1, "30");

        mvc.perform(decidir(id, """
                        {"decision":"ADVERTENCIA","motivo":"PESO_UNIDAD_INCORRECTO",
                         "mensaje":"El peso por jaba no parece correcto. Revise cuántos kilos tiene cada jaba."}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPublicacion").value("PUBLICADA"));

        assertThat(avisosDeModeracion()).isEqualTo(1);
    }

    @Test
    void ocultarLaBloqueaYVolverAMostrarLaDevuelve() throws Exception {
        long id = publicar(1, "30");

        mvc.perform(decidir(id, """
                        {"decision":"BLOQUEO","motivo":"PRECIO_NO_COINCIDE",
                         "mensaje":"El precio no coincide con el mercado de hoy. Revise el precio por jaba."}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPublicacion").value("BLOQUEADA"));
        mvc.perform(get("/api/v1/publicaciones/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/moderacion/publicaciones").param("vista", "OCULTAS")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(jsonPath("$.content[*].id").value(hasItem((int) id)));

        mvc.perform(decidir(id, "{\"decision\":\"DESBLOQUEO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPublicacion").value("PUBLICADA"));
        mvc.perform(get("/api/v1/publicaciones/" + id)).andExpect(status().isOk());
        assertThat(avisosDeModeracion()).isEqualTo(2);

        mvc.perform(get("/api/v1/moderacion/decisiones").param("fecha", LocalDate.now().toString())
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decision").value("DESBLOQUEO"))
                .andExpect(jsonPath("$[1].decision").value("BLOQUEO"))
                .andExpect(jsonPath("$[1].producto").value("Mango Kent"))
                .andExpect(jsonPath("$[1].administrador").value("Lucía Farfán"));
    }

    @Test
    void ocultarSinMotivoNiMensajeDevuelve400() throws Exception {
        long id = publicar(1, "30");

        mvc.perform(decidir(id, "{\"decision\":\"BLOQUEO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Elija el motivo.")))
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Escriba el mensaje para el agricultor.")));
    }

    @Test
    void decisionesQueNoCorrespondenAlEstadoDevuelven422() throws Exception {
        long primera = publicar(1, "30");
        long segunda = publicar(1, "20");

        mvc.perform(decidir(primera, "{\"decision\":\"DESBLOQUEO\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se vuelve a mostrar una publicación oculta."));
        mvc.perform(decidir(segunda, "{\"decision\":\"CONFORME\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La publicación no está en la cola de revisión."));
        mvc.perform(decidir(999_999, "{\"decision\":\"CONFORME\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElAdministradorModera() throws Exception {
        long id = publicar(1, "30");

        mvc.perform(get("/api/v1/moderacion/publicaciones").header("Authorization", bearer(tokenAgricultor)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/moderacion/publicaciones/" + id + "/decisiones")
                        .header("Authorization", bearer(tokenAgricultor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"CONFORME\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaCantidadMuyMayorQueElPromedioDeLaZonaEntraALaCola() throws Exception {
        int idPaita = jdbc.queryForObject("select id_distrito from distritos where ubigeo = '200501'", Integer.class);
        publicar(idPaita, "5");
        publicar(idPaita, "5");
        long normal = publicar(idPaita, "5");
        long inusual = publicar(idPaita, "50");

        assertThat(marcaDe(normal)).isNull();
        assertThat(marcaDe(inusual)).isEqualTo("CANTIDAD_INUSUAL");
    }

    // ---------- Utilidades ----------

    private long publicar(int idDistrito, String jabas) throws Exception {
        String json = """
                {"idVariedad":%d,"idUnidad":%d,"cantidad":%s,"pesoUnidadKg":20,"precioUnitario":28,
                 "idDistrito":%d,"fechaCosecha":"%s"}""".formatted(idKent, ID_JABA, jabas, idDistrito, LocalDate.now());
        String respuesta = mvc.perform(post("/api/v1/publicaciones").header("Authorization", bearer(tokenAgricultor))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private RequestBuilder decidir(long idPublicacion, String json) {
        return post("/api/v1/moderacion/publicaciones/" + idPublicacion + "/decisiones")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String registrarAgricultor(String celular) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rol":"AGRICULTOR","nombreCompleto":"Juan Prueba","celular":"%s","idDistrito":1,
                                 "contrasena":"clave2026"}""".formatted(celular)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String iniciarSesion(String celular, String contrasena) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"celular\":\"%s\",\"contrasena\":\"%s\"}".formatted(celular, contrasena)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String marcaDe(long idPublicacion) {
        return jdbc.queryForObject("select marca_revision from publicaciones where id_publicacion = ?", String.class,
                idPublicacion);
    }

    private int avisosDeModeracion() {
        return jdbc.queryForObject("select count(*) from notificaciones where tipo = 'MODERACION' and id_usuario = ?",
                Integer.class, idAgricultor);
    }
}
