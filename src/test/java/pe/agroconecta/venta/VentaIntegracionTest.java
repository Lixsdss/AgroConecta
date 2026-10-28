package pe.agroconecta.venta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * Pruebas de integración de RF-06 y RF-11 (UC-05, UC-06, UC-08) contra MariaDB ({@code agroconecta_test}): API,
 * seguridad, servicio, {@code trg_transacciones_bi/bu/ai/au}, {@code sp_sincronizar_publicacion}, avisos y Excel.
 * Cada prueba se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VentaIntegracionTest {

    private static final int ID_KILO = 1;
    private static final int ID_JABA = 2;
    private static final int ID_SACO = 3;
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
    private long idPublicacion;
    private long idConversacion;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        tokenAgricultor = registrar("AGRICULTOR", "Juan Carlos Chunga", "912345701", "");
        tokenComprador = registrar("COMPRADOR", "Rosa Quispe", "912345702", COMPRADOR_EXTRA);
        idAgricultor = jdbc.queryForObject("select id_usuario from usuarios where celular = '912345701'", Long.class);
        idPublicacion = publicar();
        String respuesta = mvc.perform(post("/api/v1/conversaciones").header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idPublicacion\":" + idPublicacion + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        idConversacion = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @Test
    void elAcuerdoQueAgotaLosKilosVendeLaPublicacionYCancelarLaDevuelve() throws Exception {
        long id = acordar(tokenComprador, ID_JABA, "30", null, "28");

        mvc.perform(get("/api/v1/ventas/" + id).header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.estado").value("ACORDADA"))
                .andExpect(jsonPath("$.soyComprador").value(true))
                .andExpect(jsonPath("$.contraparte.nombre").value("Juan Carlos Chunga"))
                .andExpect(jsonPath("$.contraparte.celular").value("912345701"))
                .andExpect(jsonPath("$.cantidadKg").value(600.0))
                .andExpect(jsonPath("$.montoTotal").value(840.0))
                .andExpect(jsonPath("$.precioKg").value(1.4))
                .andExpect(jsonPath("$.lineaDeTiempo", hasSize(1)))
                .andExpect(jsonPath("$.lineaDeTiempo[0].autor").value("Rosa Quispe"))
                .andExpect(jsonPath("$.lineaDeTiempo[0].esUsted").value(true))
                .andExpect(jsonPath("$.acciones").value(contains("CONFIRMAR_RECEPCION", "RECOGIDO", "CANCELAR")));
        assertThat(estadoPublicacion()).isEqualTo("VENDIDA");
        em.flush();
        assertThat(jdbc.queryForObject("select contenido from notificaciones where id_usuario = ? and tipo = 'VENTA'",
                String.class, idAgricultor)).contains("Rosa Quispe (Restaurante El Chalán)").contains("912345702");

        mvc.perform(cambiar(tokenAgricultor, id, "CANCELAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.acciones", hasSize(0)));
        assertThat(estadoPublicacion()).isEqualTo("PUBLICADA");
        mvc.perform(cambiar(tokenComprador, id, "RECOGIDO"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La venta ya terminó y no se modifica."));
    }

    @Test
    void conTransporteSoloElCompradorConfirmaYLaLineaDeTiempoQuedaCompleta() throws Exception {
        long id = acordar(tokenAgricultor, ID_JABA, "10", null, "28");

        mvc.perform(cambiar(tokenAgricultor, id, "RECOGIDO")).andExpect(jsonPath("$.estado").value("RECOGIDO"));
        mvc.perform(cambiar(tokenComprador, id, "CANCELAR"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value(startsWith("Una venta recogida ya no se cancela.")));
        mvc.perform(cambiar(tokenComprador, id, "EN_CAMINO")).andExpect(jsonPath("$.estado").value("EN_CAMINO"));
        mvc.perform(cambiar(tokenAgricultor, id, "CONFIRMAR_RECEPCION"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo el comprador confirma que recibió."));

        mvc.perform(cambiar(tokenComprador, id, "CONFIRMAR_RECEPCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.lineaDeTiempo[*].estado")
                        .value(contains("ACORDADA", "RECOGIDO", "EN_CAMINO", "ENTREGADO", "COMPLETADA")))
                .andExpect(jsonPath("$.lineaDeTiempo[4].autor").value("Rosa Quispe"));
    }

    @Test
    void recojoEnChacra_ElCompradorConfirmaDesdeAcordada() throws Exception {
        long id = acordar(tokenComprador, ID_JABA, "5", null, "28");

        mvc.perform(cambiar(tokenComprador, id, "EN_CAMINO"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Ese cambio no corresponde al estado actual de la venta."));
        mvc.perform(cambiar(tokenComprador, id, "CONFIRMAR_RECEPCION"))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.lineaDeTiempo[*].estado").value(contains("ACORDADA", "ENTREGADO", "COMPLETADA")));
        em.flush();
        assertThat(jdbc.queryForObject("select count(*) from notificaciones where id_usuario = ? and tipo = 'VENTA'",
                Integer.class, idAgricultor)).isEqualTo(2);
    }

    @Test
    void validaUnidadPesoKilosFechaYDisponibilidad() throws Exception {
        // Unidad de peso fijo distinta de la publicada: toma el peso del catálogo
        String respuesta = mvc.perform(registrarVenta(tokenComprador, ID_KILO, "100", null, "1.5", hoy))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/ventas/")))
                .andExpect(jsonPath("$.unidad").value("Kilo"))
                .andExpect(jsonPath("$.cantidadKg").value(100.0))
                .andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(respuesta, "$.id")).longValue()).isPositive();

        mvc.perform(registrarVenta(tokenComprador, ID_KILO, "501", null, "1.5", hoy))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La cantidad supera lo disponible (≈ 500 kg)."));
        mvc.perform(registrarVenta(tokenComprador, ID_SACO, "2", null, "60", hoy))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Indique cuántos kilos pesa cada jaba o saco."));
        mvc.perform(registrarVenta(tokenComprador, ID_SACO, "2", "45", "60", hoy))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadKg").value(90.0));
        mvc.perform(registrarVenta(tokenComprador, ID_JABA, "1", null, "28", hoy.minusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("fechaRecojo"));
        mvc.perform(registrarVenta(tokenComprador, 999, "1", null, "28", hoy))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La unidad elegida no existe."));

        em.flush();
        jdbc.update("update publicaciones set estado = 'BLOQUEADA' where id_publicacion = ?", idPublicacion);
        em.clear();
        mvc.perform(registrarVenta(tokenComprador, ID_JABA, "1", null, "28", hoy))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La cosecha ya no está disponible."));
    }

    @Test
    void unTerceroNoVeNiCambiaLaVenta() throws Exception {
        long id = acordar(tokenComprador, ID_JABA, "5", null, "28");
        String tokenOtro = registrar("COMPRADOR", "Otro comprador", "912345703", COMPRADOR_EXTRA);

        mvc.perform(get("/api/v1/ventas/" + id).header("Authorization", "Bearer " + tokenOtro))
                .andExpect(status().isForbidden());
        mvc.perform(cambiar(tokenOtro, id, "CANCELAR")).andExpect(status().isForbidden());
        mvc.perform(registrarVenta(tokenOtro, ID_JABA, "1", null, "28", hoy)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/ventas/999999999").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/ventas/" + id + "/estado").header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"VOLAR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elHistorialFiltraPorGrupoContraparteYConversacion() throws Exception {
        long enCurso = acordar(tokenComprador, ID_JABA, "5", null, "28");
        long terminada = acordar(tokenComprador, ID_JABA, "5", null, "27");
        mvc.perform(cambiar(tokenComprador, terminada, "CONFIRMAR_RECEPCION")).andExpect(status().isOk());

        mvc.perform(get("/api/v1/ventas").param("grupo", "EN_CURSO").header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(jsonPath("$.content[*].id").value(contains((int) enCurso)))
                .andExpect(jsonPath("$.content[0].contraparte").value("Restaurante El Chalán"))
                .andExpect(jsonPath("$.content[0].unidad").value("Jaba"))
                .andExpect(jsonPath("$.content[0].soyComprador").value(false));
        mvc.perform(get("/api/v1/ventas").param("grupo", "TERMINADAS").param("contraparte", "chalán")
                        .header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(jsonPath("$.content[*].id").value(contains((int) terminada)));
        mvc.perform(get("/api/v1/ventas").param("contraparte", "chunga")
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/api/v1/ventas").param("idConversacion", String.valueOf(idConversacion))
                        .param("desde", hoy.toString()).param("hasta", hoy.toString())
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(get("/api/v1/ventas").param("grupo", "TODAS").header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/ventas").param("desde", hoy.toString()).param("hasta", hoy.minusDays(1).toString())
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportaElHistorialAExcel() throws Exception {
        acordar(tokenComprador, ID_JABA, "5", null, "28");

        byte[] archivo = mvc.perform(get("/api/v1/ventas/exportar").header("Authorization", "Bearer " + tokenAgricultor))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        containsString("AgroConecta_Historial_" + hoy + ".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Fecha del acuerdo");
            assertThat(hoja.getLastRowNum()).isEqualTo(1);
            assertThat(hoja.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Mango");
            assertThat(hoja.getRow(1).getCell(8).getNumericCellValue()).isEqualTo(140.0);
            assertThat(hoja.getRow(1).getCell(9).getStringCellValue()).isEqualTo("Restaurante El Chalán");
            assertThat(hoja.getRow(1).getCell(10).getStringCellValue()).isEqualTo("Venta acordada");
        }
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
