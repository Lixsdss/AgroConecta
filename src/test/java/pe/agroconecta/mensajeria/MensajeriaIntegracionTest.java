package pe.agroconecta.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de RF-04 (UC-04) contra MariaDB ({@code agroconecta_test}): API, seguridad, servicio,
 * {@code trg_conversaciones_bi}, {@code trg_mensajes_bi} y avisos en {@code notificaciones}. Cada prueba se
 * revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MensajeriaIntegracionTest {

    private static final int ID_JABA = 2;
    private static final String COMPRADOR_EXTRA = ",\"nombreNegocio\":\"Restaurante El Chalán\",\"tipoComprador\":\"RESTAURANTE\"";

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

    @BeforeEach
    void preparar() throws Exception {
        tokenAgricultor = registrar("AGRICULTOR", "Juan Carlos Chunga", "912345691", "");
        tokenComprador = registrar("COMPRADOR", "Rosa Quispe", "912345692", COMPRADOR_EXTRA);
        idAgricultor = idDe("912345691");
        idComprador = idDe("912345692");
        idPublicacion = publicar("30", "28");
    }

    @Test
    void elCompradorAbreUnaSolaConversacionPorPublicacion() throws Exception {
        String respuesta = mvc.perform(abrir(tokenComprador, idPublicacion))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/conversaciones/")))
                .andExpect(jsonPath("$.canalInicio").value("CHAT"))
                .andExpect(jsonPath("$.soyAgricultor").value(false))
                .andExpect(jsonPath("$.otraParte.nombre").value("Juan Carlos Chunga"))
                .andExpect(jsonPath("$.otraParte.negocio").doesNotExist())
                .andExpect(jsonPath("$.publicacion.cultivo").value("Mango"))
                .andExpect(jsonPath("$.publicacion.unidadesDisponibles").value(30.0))
                .andExpect(jsonPath("$.ultimoMensaje").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        mvc.perform(abrir(tokenComprador, idPublicacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        assertThat(jdbc.queryForObject("select count(*) from conversaciones where id_publicacion = ?",
                Integer.class, idPublicacion)).isEqualTo(1);
    }

    @Test
    void elAgricultorNoAbreConversacionesYUnaPublicacionNoDisponibleNoSeContacta() throws Exception {
        mvc.perform(abrir(tokenAgricultor, idPublicacion)).andExpect(status().isForbidden());

        em.flush();
        jdbc.update("update publicaciones set estado = 'VENCIDA' where id_publicacion = ?", idPublicacion);
        em.clear();
        mvc.perform(abrir(tokenComprador, idPublicacion))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se puede contactar una publicación disponible."));
        mvc.perform(abrir(tokenComprador, 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void textoYOfertaLlegan_YLaOtraParteRecibeAvisos() throws Exception {
        long id = abrirConversacion();

        mvc.perform(enviar(tokenComprador, id, "{\"tipo\":\"TEXTO\",\"contenido\":\"Buenos días, me interesa.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mio").value(true))
                .andExpect(jsonPath("$.tipo").value("TEXTO"));
        mvc.perform(enviar(tokenComprador, id, "{\"tipo\":\"TEXTO\",\"contenido\":\"¿Sigue disponible?\"}"))
                .andExpect(status().isCreated());
        mvc.perform(enviar(tokenComprador, id,
                        "{\"tipo\":\"OFERTA\",\"ofertaCantidad\":30,\"ofertaPrecioUnitario\":27}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ofertaKg").value(600.0))
                .andExpect(jsonPath("$.ofertaTotal").value(810.0));

        // Un solo aviso MENSAJE sin leer por conversación; la oferta siempre avisa
        assertThat(avisos(idAgricultor, "MENSAJE")).isEqualTo(1);
        assertThat(avisos(idAgricultor, "OFERTA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select contenido from notificaciones where id_usuario = ? and tipo = 'OFERTA'",
                String.class, idAgricultor))
                .isEqualTo("Restaurante El Chalán le ofrece 30 jabas a S/ 27.00 por jaba (Mango Kent).");

        // El agricultor ve 3 no leídos, los lee al abrir el chat y responde
        mvc.perform(get("/api/v1/conversaciones/no-leidos").header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/api/v1/conversaciones").header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].soyAgricultor").value(true))
                .andExpect(jsonPath("$.content[0].otraParte.negocio").value("Restaurante El Chalán"))
                .andExpect(jsonPath("$.content[0].ultimoMensaje.tipo").value("OFERTA"))
                .andExpect(jsonPath("$.content[0].noLeidos").value(3));
        mvc.perform(get("/api/v1/conversaciones/" + id + "/mensajes")
                        .header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].mio").value(false));
        mvc.perform(get("/api/v1/conversaciones/no-leidos").header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(jsonPath("$.total").value(0));

        String respuesta = mvc.perform(enviar(tokenAgricultor, id,
                        "{\"tipo\":\"OFERTA\",\"ofertaCantidad\":30,\"ofertaPrecioUnitario\":28}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ultimo = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        assertThat(avisos(idComprador, "OFERTA")).isEqualTo(1);

        // Consulta periódica: solo lo posterior
        mvc.perform(get("/api/v1/conversaciones/" + id + "/mensajes").param("despuesDe", String.valueOf(ultimo - 1))
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ofertaPrecioUnitario").value(28.0));
    }

    @Test
    void laOfertaNoSuperaLoDisponibleNiSeHaceSobreUnaPublicacionNoDisponible() throws Exception {
        long id = abrirConversacion();

        mvc.perform(enviar(tokenComprador, id,
                        "{\"tipo\":\"OFERTA\",\"ofertaCantidad\":31,\"ofertaPrecioUnitario\":27}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La oferta supera lo disponible (≈ 30 jabas)."));

        em.flush();
        jdbc.update("update publicaciones set estado = 'VENCIDA' where id_publicacion = ?", idPublicacion);
        em.clear();
        mvc.perform(enviar(tokenComprador, id,
                        "{\"tipo\":\"OFERTA\",\"ofertaCantidad\":10,\"ofertaPrecioUnitario\":27}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se hacen ofertas mientras la publicación está disponible."));
        // El texto se sigue aceptando (coordinación, UC-04 A3)
        mvc.perform(enviar(tokenComprador, id, "{\"tipo\":\"TEXTO\",\"contenido\":\"¿La renueva?\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unTerceroNoLeeNiEscribe() throws Exception {
        long id = abrirConversacion();
        String tokenOtro = registrar("COMPRADOR", "Otro comprador", "912345693", COMPRADOR_EXTRA);

        mvc.perform(get("/api/v1/conversaciones/" + id).header("Authorization", "Bearer " + tokenOtro))
                .andExpect(status().isForbidden());
        mvc.perform(enviar(tokenOtro, id, "{\"tipo\":\"TEXTO\",\"contenido\":\"Hola\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/conversaciones/999999999").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isNotFound());
    }

    @Test
    void validaElTextoYLaOfertaPorCampo() throws Exception {
        long id = abrirConversacion();

        mvc.perform(enviar(tokenComprador, id, "{\"tipo\":\"TEXTO\",\"contenido\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("contenido"));
        mvc.perform(enviar(tokenComprador, id, "{\"tipo\":\"OFERTA\",\"ofertaCantidad\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("ofertaPrecioUnitario"));
        mvc.perform(enviar(tokenComprador, id, "{\"tipo\":\"AUDIO\",\"contenido\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whatsappCreaLaConversacionSumaClicsYDevuelveElEnlace() throws Exception {
        String celular = jdbc.queryForObject("select celular from usuarios where id_usuario = ?", String.class,
                idAgricultor);

        mvc.perform(whatsapp(tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enlace").value(startsWith("https://wa.me/51" + celular + "?text=Hola%2C%20Juan")));
        mvc.perform(whatsapp(tokenComprador)).andExpect(status().isOk());

        em.flush();
        assertThat(jdbc.queryForObject("select canal_inicio from conversaciones where id_publicacion = ?",
                String.class, idPublicacion)).isEqualTo("WHATSAPP");
        assertThat(jdbc.queryForObject("select clics_whatsapp from conversaciones where id_publicacion = ?",
                Integer.class, idPublicacion)).isEqualTo(2);
        mvc.perform(post("/api/v1/conversaciones/whatsapp").header("Authorization", "Bearer " + tokenAgricultor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idPublicacion\":" + idPublicacion + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesionNoHayChat() throws Exception {
        mvc.perform(get("/api/v1/conversaciones")).andExpect(status().isUnauthorized());
    }

    private long abrirConversacion() throws Exception {
        String respuesta = mvc.perform(abrir(tokenComprador, idPublicacion))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private int avisos(long idUsuario, String tipo) {
        em.flush();
        return jdbc.queryForObject("select count(*) from notificaciones where id_usuario = ? and tipo = ?",
                Integer.class, idUsuario, tipo);
    }

    private RequestBuilder abrir(String token, long publicacion) {
        return post("/api/v1/conversaciones").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idPublicacion\":" + publicacion + "}");
    }

    private RequestBuilder whatsapp(String token) {
        return post("/api/v1/conversaciones/whatsapp").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idPublicacion\":" + idPublicacion + "}");
    }

    private RequestBuilder enviar(String token, long idConversacion, String json) {
        return post("/api/v1/conversaciones/" + idConversacion + "/mensajes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private long idDe(String celular) {
        return jdbc.queryForObject("select id_usuario from usuarios where celular = ?", Long.class, celular);
    }

    private long publicar(String cantidad, String precio) throws Exception {
        int idKent = jdbc.queryForObject("select v.id_variedad from variedades v join cultivos c using (id_cultivo) "
                + "where c.nombre = 'Mango' and v.nombre = 'Kent'", Integer.class);
        int idTambogrande = jdbc.queryForObject("select id_distrito from distritos where nombre = 'Tambogrande'",
                Integer.class);
        String json = """
                {"idVariedad":%d,"idUnidad":%d,"cantidad":%s,"pesoUnidadKg":20,"precioUnitario":%s,
                 "idDistrito":%d,"fechaCosecha":"%s","fechaLimite":null}"""
                .formatted(idKent, ID_JABA, cantidad, precio, idTambogrande, LocalDate.now());
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
