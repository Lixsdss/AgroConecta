package pe.agroconecta.venta;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de RF-05 (UC-07) contra MariaDB ({@code agroconecta_test}): calificar una venta terminada,
 * {@code trg_calificaciones_bi}, {@code uk_calificacion_trans_evaluador}, {@code v_reputacion_usuario}, reputación
 * pública y avisos. Cada prueba se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CalificacionIntegracionTest {

    private static final int ID_JABA = 2;
    private static final String COMPRADOR_EXTRA =
            ",\"nombreNegocio\":\"Restaurante El Chalán\",\"tipoComprador\":\"RESTAURANTE\"";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private String tokenAgricultor;
    private String tokenComprador;
    private long idAgricultor;
    private long idComprador;
    private long idPublicacion;
    private long idConversacion;
    private long idVenta;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        tokenAgricultor = registrar("AGRICULTOR", "Juan Carlos Chunga", "912345711", "");
        tokenComprador = registrar("COMPRADOR", "Rosa Quispe", "912345712", COMPRADOR_EXTRA);
        idAgricultor = jdbc.queryForObject("select id_usuario from usuarios where celular = '912345711'", Long.class);
        idComprador = jdbc.queryForObject("select id_usuario from usuarios where celular = '912345712'", Long.class);
        idPublicacion = publicar();
        String respuesta = mvc.perform(post("/api/v1/conversaciones").header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idPublicacion\":" + idPublicacion + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        idConversacion = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        idVenta = acordar(tokenComprador, ID_JABA, "5", null, "28");
    }

    @Test
    void ambasPartesCalificanUnaVezLaVentaTerminada() throws Exception {
        mvc.perform(calificar(tokenComprador, 5, "Mango de primera."))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se califican ventas terminadas."));
        mvc.perform(get("/api/v1/ventas/" + idVenta).header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.calificaciones.puedoCalificar").value(false));

        completar();
        mvc.perform(calificar(tokenComprador, 5, "  Mango de primera.  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calificaciones.puedoCalificar").value(false))
                .andExpect(jsonPath("$.calificaciones.mia.puntuacion").value(5))
                .andExpect(jsonPath("$.calificaciones.mia.comentario").value("Mango de primera."))
                .andExpect(jsonPath("$.calificaciones.recibida").doesNotExist());
        mvc.perform(get("/api/v1/ventas/" + idVenta).header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(jsonPath("$.calificaciones.puedoCalificar").value(true))
                .andExpect(jsonPath("$.calificaciones.recibida.autor").value("Restaurante El Chalán"));
        mvc.perform(calificar(tokenAgricultor, 4, ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calificaciones.mia.comentario").doesNotExist())
                .andExpect(jsonPath("$.calificaciones.recibida.puntuacion").value(5));

        mvc.perform(calificar(tokenComprador, 1, "Cambio de opinión"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya calificó esta venta. Las calificaciones no se cambian."));
        em.flush();
        assertThat(jdbc.queryForObject("select contenido from notificaciones where id_usuario = ? and tipo = 'CALIFICACION'",
                String.class, idAgricultor)).isEqualTo("Restaurante El Chalán le dio 5 estrellas por la venta de Mango Kent.");
        mvc.perform(get("/api/v1/ventas").param("grupo", "TERMINADAS").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.content[0].miPuntuacion").value(5));
    }

    @Test
    void laReputacionEsPublicaYSeVeEnLaBusqueda() throws Exception {
        mvc.perform(get("/api/v1/reputaciones/" + idAgricultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").doesNotExist())
                .andExpect(jsonPath("$.total").value(0));

        completar();
        mvc.perform(calificar(tokenComprador, 5, "Mango de primera.")).andExpect(status().isCreated());
        mvc.perform(calificar(tokenAgricultor, 4, "Puntual.")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/reputaciones/" + idAgricultor))
                .andExpect(jsonPath("$.promedio").value(5.0))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.calificaciones.content[0].autor").value("Restaurante El Chalán"))
                .andExpect(jsonPath("$.calificaciones.content[0].comentario").value("Mango de primera."))
                .andExpect(jsonPath("$.calificaciones.page.size").value(10));
        mvc.perform(get("/api/v1/reputaciones/" + idComprador))
                .andExpect(jsonPath("$.promedio").value(4.0))
                .andExpect(jsonPath("$.calificaciones.content[0].autor").value("Juan Carlos Chunga"));
        em.flush();
        em.clear();
        mvc.perform(get("/api/v1/publicaciones/" + idPublicacion))
                .andExpect(jsonPath("$.calificacionAgricultor").value(5.0));
        mvc.perform(get("/api/v1/reputaciones/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void validaPuntuacionComentarioYParticipante() throws Exception {
        completar();
        String tokenOtro = registrar("COMPRADOR", "Otro comprador", "912345713", COMPRADOR_EXTRA);

        mvc.perform(calificar(tokenOtro, 5, null)).andExpect(status().isForbidden());
        mvc.perform(calificar(tokenComprador, 0, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("puntuacion"));
        mvc.perform(calificar(tokenComprador, 6, null)).andExpect(status().isBadRequest());
        mvc.perform(calificar(tokenComprador, 5, "x".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("comentario"));
        mvc.perform(post("/api/v1/ventas/" + idVenta + "/calificacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntuacion\":5}"))
                .andExpect(status().isUnauthorized());
    }

    /** Completa la venta y limpia el contexto: las vistas leídas antes en esta transacción quedan desactualizadas. */
    private void completar() throws Exception {
        mvc.perform(cambiar(tokenComprador, idVenta, "CONFIRMAR_RECEPCION")).andExpect(status().isOk());
        em.flush();
        em.clear();
    }

    private RequestBuilder calificar(String token, int puntuacion, String comentario) {
        String json = comentario == null ? "{\"puntuacion\":%d}".formatted(puntuacion)
                : "{\"puntuacion\":%d,\"comentario\":\"%s\"}".formatted(puntuacion, comentario);
        return post("/api/v1/ventas/" + idVenta + "/calificacion").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private long acordar(String token, int idUnidad, String cantidad, String peso, String precio) throws Exception {
        String respuesta = mvc.perform(registrarVenta(token, idUnidad, cantidad, peso, precio, hoy))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private String estadoPublicacion() {
        em.flush();
        return jdbc.queryForObject("select estado from publicaciones where id_publicacion = ?", String.class,
                idPublicacion);
    }

    private RequestBuilder registrarVenta(String token, int idUnidad, String cantidad, String peso, String precio,
            LocalDate recojo) {
        String json = """
                {"idConversacion":%d,"idUnidad":%d,"cantidad":%s,"pesoUnidadKg":%s,"precioUnitario":%s,
                 "fechaRecojo":"%s"}""".formatted(idConversacion, idUnidad, cantidad, peso == null ? "null" : peso,
                precio, recojo);
        return post("/api/v1/ventas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private RequestBuilder cambiar(String token, long idVenta, String accion) {
        return post("/api/v1/ventas/" + idVenta + "/estado").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"" + accion + "\"}");
    }

    private long publicar() throws Exception {
        int idKent = jdbc.queryForObject("select v.id_variedad from variedades v join cultivos c using (id_cultivo) "
                + "where c.nombre = 'Mango' and v.nombre = 'Kent'", Integer.class);
        int idTambogrande = jdbc.queryForObject("select id_distrito from distritos where nombre = 'Tambogrande'",
                Integer.class);
        String json = """
                {"idVariedad":%d,"idUnidad":%d,"cantidad":30,"pesoUnidadKg":20,"precioUnitario":28,
                 "idDistrito":%d,"fechaCosecha":"%s","fechaLimite":null}"""
                .formatted(idKent, ID_JABA, idTambogrande, hoy);
        String respuesta = mvc.perform(post("/api/v1/publicaciones").header("Authorization", "Bearer " + tokenAgricultor)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private String registrar(String rol, String nombre, String celular, String extra) throws Exception {
        String json = """
                {"rol":"%s","nombreCompleto":"%s","celular":"%s","idDistrito":1,"contrasena":"clave2026"%s}"""
                .formatted(rol, nombre, celular, extra);
        String respuesta = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }
}
