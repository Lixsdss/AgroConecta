package pe.agroconecta.ventaagrupada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

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
 * Pruebas de integración de la venta agrupada (RF-10, UC-12) contra MariaDB ({@code agroconecta_test}):
 * {@code v_lotes_agrupados}, reparto, {@code trg_transacciones_bi/bu} con PROPUESTA, aceptar con bloqueo, cerrar y
 * cancelar, y avisos. Cada prueba se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VentaAgrupadaIntegracionTest {

    private static final int ID_KILO = 1;
    private static final int ID_JABA = 2;
    private static final String COMPRADOR_EXTRA =
            ",\"nombreNegocio\":\"Restaurante El Chalán\",\"tipoComprador\":\"RESTAURANTE\"";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private String tokenA;
    private String tokenB;
    private String tokenComprador;
    private long idA;
    private long idB;
    private long pubA;
    private int idKent;
    private int idTambogrande;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        idKent = jdbc.queryForObject("select v.id_variedad from variedades v join cultivos c using (id_cultivo) "
                + "where c.nombre = 'Mango' and v.nombre = 'Kent'", Integer.class);
        idTambogrande = jdbc.queryForObject("select id_distrito from distritos where nombre = 'Tambogrande'",
                Integer.class);
        tokenA = registrar("AGRICULTOR", "Juan Carlos Chunga", "912345741", "");
        tokenB = registrar("AGRICULTOR", "María Elena Zapata", "912345742", "");
        tokenComprador = registrar("COMPRADOR", "Rosa Quispe", "912345743", COMPRADOR_EXTRA);
        idA = idDe("912345741");
        idB = idDe("912345742");
        pubA = publicar(tokenA, ID_JABA, "30", "20", "28", idTambogrande);     // 600 kg en jabas
        publicar(tokenB, ID_KILO, "400", "null", "1.30", idTambogrande);       // 400 kg en kilos
    }

    @Test
    void muestraElLoteYSusAgricultores() throws Exception {
        mvc.perform(get("/api/v1/lotes").param("idDistrito", String.valueOf(idTambogrande))
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].variedad").value("Kent"))
                .andExpect(jsonPath("$.content[0].totalAgricultores").value(2))
                .andExpect(jsonPath("$.content[0].volumenKg").value(1000.0))
                .andExpect(jsonPath("$.content[0].precioPromedioKg").value(1.36));
        mvc.perform(get("/api/v1/lotes/agricultores").param("idVariedad", String.valueOf(idKent))
                        .param("idDistrito", String.valueOf(idTambogrande)).header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$[*].agricultor").value(contains("Juan Carlos Chunga", "María Elena Zapata")))
                .andExpect(jsonPath("$[0].unidadesDisponibles").value(30.0));
        mvc.perform(get("/api/v1/lotes").header("Authorization", "Bearer " + tokenA)).andExpect(status().isForbidden());
    }

    @Test
    void reparteCreaLasPartesYAvisaACadaAgricultor() throws Exception {
        String respuesta = mvc.perform(proponer("600", "1.40"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/propuestas/")))
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.kgRepartidos").value(600.0))
                .andExpect(jsonPath("$.kgPendientes").value(600.0))
                .andExpect(jsonPath("$.partes", hasSize(2)))
                .andExpect(jsonPath("$.partes[0].agricultor").value("Juan Carlos Chunga"))
                .andExpect(jsonPath("$.partes[0].cantidad").value(18.0))
                .andExpect(jsonPath("$.partes[0].unidadPlural").value("jabas"))
                .andExpect(jsonPath("$.partes[0].precioUnitario").value(28.0))
                .andExpect(jsonPath("$.partes[1].cantidad").value(240.0))
                .andExpect(jsonPath("$.partes[1].precioUnitario").value(1.4))
                .andExpect(jsonPath("$.partes[1].estado").value("PROPUESTA"))
                .andReturn().getResponse().getContentAsString();

        em.flush();
        assertThat(jdbc.queryForObject("select contenido from notificaciones where id_usuario = ? and tipo = "
                + "'PROPUESTA_CONJUNTA'", String.class, idA)).startsWith(
                "Restaurante El Chalán le propone comprar 18 jabas de su Mango Kent a S/ 28.00 por jaba.");
        assertThat(jdbc.queryForObject("select count(*) from notificaciones where id_usuario = ? and tipo = "
                + "'PROPUESTA_CONJUNTA'", Integer.class, idB)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select canal_inicio from conversaciones where id_publicacion = ?", String.class,
                pubA)).isEqualTo("PROPUESTA_CONJUNTA");
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        mvc.perform(get("/api/v1/propuestas/" + id).header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.cultivo").value("Mango"));
    }

    @Test
    void elAgricultorAceptaORechazaYElCompradorVeElAvance() throws Exception {
        String respuesta = mvc.perform(proponer("600", "1.40")).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        List<Integer> partes = JsonPath.read(respuesta, "$.partes[*].idVenta");

        mvc.perform(get("/api/v1/ventas/" + partes.get(0)).header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.estado").value("PROPUESTA"))
                .andExpect(jsonPath("$.idPropuestaGrupal").value(id))
                .andExpect(jsonPath("$.acciones").value(contains("ACEPTAR", "RECHAZAR")));
        mvc.perform(cambiar(tokenComprador, partes.get(0), "ACEPTAR"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo el agricultor acepta o rechaza su parte."));
        mvc.perform(cambiar(tokenA, partes.get(0), "CANCELAR"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Para no aceptar su parte, use Rechazar."));

        mvc.perform(cambiar(tokenA, partes.get(0), "ACEPTAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACORDADA"))
                .andExpect(jsonPath("$.acciones").value(contains("RECOGIDO", "CANCELAR")));
        mvc.perform(cambiar(tokenB, partes.get(1), "RECHAZAR"))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        mvc.perform(get("/api/v1/propuestas/" + id).header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.kgAcordados").value(360.0))
                .andExpect(jsonPath("$.kgPendientes").value(0.0))
                .andExpect(jsonPath("$.partes[*].estado").value(contains("ACORDADA", "RECHAZADA")));
        em.flush();
        assertThat(jdbc.queryForObject("select count(*) from notificaciones n join usuarios u on u.id_usuario = "
                + "n.id_usuario where u.celular = '912345743' and n.tipo = 'VENTA'", Integer.class)).isEqualTo(2);
    }

    @Test
    void cerrarCancelaLasPartesPendientesYLasAcordadasSiguen() throws Exception {
        String respuesta = mvc.perform(proponer("600", "1.40")).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        List<Integer> partes = JsonPath.read(respuesta, "$.partes[*].idVenta");
        mvc.perform(cambiar(tokenA, partes.get(0), "ACEPTAR")).andExpect(status().isOk());

        mvc.perform(post("/api/v1/propuestas/" + id + "/cerrar").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.partes[*].estado").value(contains("ACORDADA", "CANCELADA")));
        mvc.perform(post("/api/v1/propuestas/" + id + "/cancelar").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La propuesta ya está cerrada o cancelada."));
        em.flush();
        assertThat(jdbc.queryForObject("select titulo from notificaciones where id_usuario = ? and tipo = 'VENTA'",
                String.class, idB)).isEqualTo("Propuesta conjunta cerrada");
    }

    @Test
    void alAceptarSeVerificaQueAunHayaKilos() throws Exception {
        String respuesta = mvc.perform(proponer("600", "1.40")).andReturn().getResponse().getContentAsString();
        List<Integer> partes = JsonPath.read(respuesta, "$.partes[*].idVenta");
        // Mientras tanto, otro comprador acuerda 15 jabas (300 kg) de la misma cosecha por el chat
        String tokenOtro = registrar("COMPRADOR", "Otro comprador", "912345744", COMPRADOR_EXTRA);
        String conversacion = mvc.perform(post("/api/v1/conversaciones").header("Authorization", "Bearer " + tokenOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idPublicacion\":" + pubA + "}"))
                .andReturn().getResponse().getContentAsString();
        long idConversacion = ((Number) JsonPath.read(conversacion, "$.id")).longValue();
        mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + tokenOtro)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idConversacion":%d,"idUnidad":%d,"cantidad":15,"precioUnitario":28,"fechaRecojo":"%s"}"""
                                .formatted(idConversacion, ID_JABA, hoy)))
                .andExpect(status().isCreated());

        mvc.perform(cambiar(tokenA, partes.get(0), "ACEPTAR"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La cantidad supera lo disponible (≈ 300 kg)."));
    }

    @Test
    void validaElLoteLaCantidadYElDueno() throws Exception {
        mvc.perform(proponer("1001", "1.40"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La cantidad supera lo disponible en el lote (≈ 1000 kg)."));
        mvc.perform(proponer("0.5", "1.40"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value(
                        "La cantidad es muy pequeña: a ningún agricultor le toca una unidad entera."));
        mvc.perform(post("/api/v1/propuestas").header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(idKent, idTambogrande + 1, "100", "1.4", hoy)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value(
                        "Ese lote ya no está disponible: necesita 2 o más agricultores con cosechas a la venta."));
        mvc.perform(post("/api/v1/propuestas").header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(idKent, idTambogrande, "100", "1.4",
                                hoy.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("fechaRecojo"));

        String respuesta = mvc.perform(proponer("600", "1.40")).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        String tokenOtro = registrar("COMPRADOR", "Otro comprador", "912345745", COMPRADOR_EXTRA);
        mvc.perform(get("/api/v1/propuestas/" + id).header("Authorization", "Bearer " + tokenOtro))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/propuestas/999999999").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/propuestas").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(idKent, idTambogrande, "100", "1.4", hoy)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/propuestas").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    private RequestBuilder proponer(String cantidadKg, String precioKg) {
        return post("/api/v1/propuestas").header("Authorization", "Bearer " + tokenComprador)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(idKent, idTambogrande, cantidadKg, precioKg, hoy));
    }

    private static String cuerpo(int idVariedad, int idDistrito, String cantidadKg, String precioKg, LocalDate recojo) {
        return """
                {"idVariedad":%d,"idDistrito":%d,"cantidadKg":%s,"precioKg":%s,"fechaRecojo":"%s"}"""
                .formatted(idVariedad, idDistrito, cantidadKg, precioKg, recojo);
    }

    private RequestBuilder cambiar(String token, long idVenta, String accion) {
        return post("/api/v1/ventas/" + idVenta + "/estado").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"" + accion + "\"}");
    }

    private long idDe(String celular) {
        return jdbc.queryForObject("select id_usuario from usuarios where celular = ?", Long.class, celular);
    }

    private long publicar(String token, int idUnidad, String cantidad, String peso, String precio, int idDistrito)
            throws Exception {
        String json = """
                {"idVariedad":%d,"idUnidad":%d,"cantidad":%s,"pesoUnidadKg":%s,"precioUnitario":%s,
                 "idDistrito":%d,"fechaCosecha":"%s","fechaLimite":null}"""
                .formatted(idKent, idUnidad, cantidad, peso, precio, idDistrito, hoy);
        String respuesta = mvc.perform(post("/api/v1/publicaciones").header("Authorization", "Bearer " + token)
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
