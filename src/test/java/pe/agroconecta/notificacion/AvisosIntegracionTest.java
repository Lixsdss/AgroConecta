package pe.agroconecta.notificacion;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Pruebas de integración del centro de avisos (RF-07, UC-09 paso 2) contra MariaDB ({@code agroconecta_test}). Los
 * avisos POR_VENCER se insertan como lo hace {@code ev_tareas_diarias}. Cada prueba se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AvisosIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private String token;
    private long idUsuario;

    @BeforeEach
    void preparar() throws Exception {
        token = registrar("912345731");
        idUsuario = jdbc.queryForObject("select id_usuario from usuarios where celular = '912345731'", Long.class);
    }

    @Test
    void listaCuentaYMarcaComoLeidos() throws Exception {
        long primero = aviso(idUsuario, "POR_VENCER", "Su mango vence en 3 días. ¿Qué desea hacer?", "2026-10-09 06:00:00");
        long segundo = aviso(idUsuario, "MODERACION", "Aviso sobre su Mango Kent.", "2026-10-09 08:00:00");

        mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains((int) segundo, (int) primero)))
                .andExpect(jsonPath("$.content[1].tipo").value("POR_VENCER"))
                .andExpect(jsonPath("$.content[1].leida").value(false))
                .andExpect(jsonPath("$.content[1].idReferencia").value(15));
        mvc.perform(get("/api/v1/notificaciones/no-leidas").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.total").value(2));

        mvc.perform(post("/api/v1/notificaciones/" + primero + "/leida").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notificaciones/no-leidas").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(post("/api/v1/notificaciones/leidas").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.marcados").value(1));
        mvc.perform(get("/api/v1/notificaciones/no-leidas").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void cadaUsuarioVeSoloSusAvisos() throws Exception {
        String otro = registrar("912345732");
        long ajeno = aviso(idUsuario, "POR_VENCER", "Su mango vence mañana. ¿Qué desea hacer?", "2026-10-09 06:00:00");

        mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + otro))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(post("/api/v1/notificaciones/" + ajeno + "/leida").header("Authorization", "Bearer " + otro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El aviso no existe."));
        mvc.perform(get("/api/v1/notificaciones").param("page", "-1").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notificaciones")).andExpect(status().isUnauthorized());
    }

    private long aviso(long usuario, String tipo, String titulo, String fecha) {
        em.flush();
        jdbc.update("insert into notificaciones (id_usuario, tipo, titulo, contenido, id_referencia, fecha_creacion) "
                + "values (?, ?, ?, '30 jabas · Tambogrande', 15, ?)", usuario, tipo, titulo, fecha);
        em.clear();
        return jdbc.queryForObject("select max(id_notificacion) from notificaciones where id_usuario = ?", Long.class,
                usuario);
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
