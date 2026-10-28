package pe.agroconecta.precio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de RF-02 contra MariaDB: carga por formulario y archivo, edición, vistas
 * {@code v_precios_vigentes} y {@code v_rango_precio_plataforma}, referencia y marca PRECIO_FUERA_RANGO.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PrecioIntegracionTest {

    private static final int ID_MANGO = 1;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager em;

    private String tokenAdmin;
    private String tokenAgricultor;
    private int idTambogrande;
    private int idChulucanas;
    private int idCastilla;
    private int idPaita;

    @BeforeEach
    void preparar() throws Exception {
        jdbc.update("insert into usuarios (rol, nombre_completo, celular, password_hash, id_distrito) "
                + "values ('ADMINISTRADOR', 'Lucía Farfán', '900000008', ?, 1)", passwordEncoder.encode("admin2026"));
        tokenAdmin = token("/api/v1/auth/login", "{\"celular\":\"900000008\",\"contrasena\":\"admin2026\"}", 200);
        tokenAgricultor = token("/api/v1/auth/registro", """
                {"rol":"AGRICULTOR","nombreCompleto":"Juan Prueba","celular":"914000001","idDistrito":1,
                 "contrasena":"clave2026"}""", 201);
        idTambogrande = distrito("200114");
        idChulucanas = distrito("200401");
        idCastilla = distrito("200104");
        idPaita = distrito("200501");
    }

    @Test
    void cargaEditaYMuestraLaTendenciaEnKiloYEnJaba() throws Exception {
        LocalDate hoy = LocalDate.now();
        cargar(ID_MANGO, idTambogrande, hoy.minusDays(1), "1.20", 201);
        String hoyJson = cargar(ID_MANGO, idTambogrande, hoy, "1.40", 201);
        long idPrecio = ((Number) JsonPath.read(hoyJson, "$.id")).longValue();

        mvc.perform(get("/api/v1/precios/vigentes").param("idCultivo", "1").param("idDistrito", "" + idTambogrande))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPrecio").value(idPrecio))
                .andExpect(jsonPath("$[0].precioKg").value(1.4))
                .andExpect(jsonPath("$[0].unidadLocal").value("Jaba"))
                .andExpect(jsonPath("$[0].precioUnidadLocal").value(28.0))
                .andExpect(jsonPath("$[0].tendencia").value("SUBE"));

        mvc.perform(put("/api/v1/precios/" + idPrecio).header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioKg\":1.10,\"fuente\":\"ENCUESTA_ACOPIADORES\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuente").value("ENCUESTA_ACOPIADORES"));
        // La prueba comparte el contexto de persistencia entre peticiones: se vacía para releer la vista
        em.clear();
        mvc.perform(get("/api/v1/precios/vigentes").param("idCultivo", "1").param("idDistrito", "" + idTambogrande))
                .andExpect(jsonPath("$[0].tendencia").value("BAJA"));

        mvc.perform(get("/api/v1/precios/historial").param("idCultivo", "1").param("idDistrito", "" + idTambogrande))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].precioKg").value(1.2));
    }

    @Test
    void rechazaDuplicadoZonaInvalidaFechaFuturaYAQuienNoEsAdministrador() throws Exception {
        LocalDate hoy = LocalDate.now();
        cargar(ID_MANGO, idTambogrande, hoy, "1.40", 201);

        mvc.perform(post("/api/v1/precios").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(precio(ID_MANGO, idTambogrande, hoy, "1.50")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "Ya hay un precio de Mango en Tambogrande para esa fecha. Use Editar para cambiarlo."));
        mvc.perform(post("/api/v1/precios").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(precio(ID_MANGO, idPaita, hoy, "1.50")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El distrito elegido no es una zona de precio."));
        mvc.perform(post("/api/v1/precios").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(precio(ID_MANGO, idChulucanas, hoy.plusDays(1), "1.50")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/precios").header("Authorization", "Bearer " + tokenAgricultor)
                        .contentType(MediaType.APPLICATION_JSON).content(precio(ID_MANGO, idChulucanas, hoy, "1.50")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/precios/999999").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioKg\":1.10,\"fuente\":\"MERCADO_SULLANA\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void laReferenciaUsaLaZonaOElPromedioDeZonasYSinPreciosNoHayReferencia() throws Exception {
        LocalDate hoy = LocalDate.now();
        cargar(ID_MANGO, idTambogrande, hoy, "1.40", 201);
        cargar(ID_MANGO, idChulucanas, hoy, "1.60", 201);

        mvc.perform(get("/api/v1/precios/referencia").param("idCultivo", "1").param("idDistrito", "" + idTambogrande))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zona").value("Tambogrande"))
                .andExpect(jsonPath("$.esPromedioDeZonas").value(false))
                .andExpect(jsonPath("$.minimoKg").value(1.19))
                .andExpect(jsonPath("$.maximoKg").value(1.61))
                .andExpect(jsonPath("$.precioUnidadLocal").value(28.0))
                .andExpect(jsonPath("$.rangoPlataforma").doesNotExist());
        mvc.perform(get("/api/v1/precios/referencia").param("idCultivo", "1").param("idDistrito", "" + idCastilla))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esPromedioDeZonas").value(true))
                .andExpect(jsonPath("$.precioKg").value(1.5));
        mvc.perform(get("/api/v1/precios/referencia").param("idCultivo", "2").param("idDistrito", "" + idCastilla))
                .andExpect(status().isNoContent());
    }

    @Test
    void importaUnCsvConFilasValidasEInformaLasInvalidas() throws Exception {
        LocalDate hoy = LocalDate.now();
        String csv = """
                cultivo;zona;fecha;precio_kg
                Mango;Tambogrande;%1$s;1,40
                LIMÓN;chulucanas;%2$s;2.10
                Mango;Paita;%1$s;1.40
                Uva;Piura;%1$s;3.00
                Mango;Tambogrande;%1$s;1.45
                Arroz;Sullana;mañana;1.70
                Arroz;Sullana;%1$s;-1
                """.formatted(hoy, hoy.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")));

        mvc.perform(multipart("/api/v1/precios/archivo")
                        .file(new MockMultipartFile("archivo", "precios.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                        .param("fuente", "MERCADO_MAYORISTA_PIURA")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importadas").value(2))
                .andExpect(jsonPath("$.rechazadas", hasSize(5)))
                .andExpect(jsonPath("$.rechazadas[*].motivo").value(hasItem("La zona \"Paita\" no es una zona de precio.")))
                .andExpect(jsonPath("$.rechazadas[*].motivo").value(hasItem("El cultivo \"Uva\" no existe.")))
                .andExpect(jsonPath("$.rechazadas[*].motivo").value(hasItem("Ese cultivo, zona y fecha ya están en la fila 2.")));

        assertThat(jdbc.queryForObject("select count(*) from precios_mercado where metodo = 'ARCHIVO'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void importaUnExcelConCeldasDeFechaYNumero() throws Exception {
        byte[] excel;
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Precios");
            Row cabecera = hoja.createRow(0);
            String[] columnas = {"Cultivo", "Zona", "Fecha", "Precio_kg"};
            for (int i = 0; i < columnas.length; i++) {
                cabecera.createCell(i).setCellValue(columnas[i]);
            }
            Row fila = hoja.createRow(1);
            fila.createCell(0).setCellValue("Plátano");
            fila.createCell(1).setCellValue("Sullana");
            fila.createCell(2).setCellValue(LocalDate.now().toString());
            fila.createCell(3).setCellValue(0.95);
            libro.write(salida);
            excel = salida.toByteArray();
        }

        mvc.perform(multipart("/api/v1/precios/archivo")
                        .file(new MockMultipartFile("archivo", "precios.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel))
                        .param("fuente", "MERCADO_SULLANA")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importadas").value(1))
                .andExpect(jsonPath("$.rechazadas", hasSize(0)));
    }

    @Test
    void unArchivoSinLasColumnasEsperadasOConOtroFormatoDevuelve400() throws Exception {
        mvc.perform(multipart("/api/v1/precios/archivo")
                        .file(new MockMultipartFile("archivo", "precios.csv", "text/csv", "a,b\n1,2".getBytes()))
                        .param("fuente", "MERCADO_SULLANA").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La primera fila debe tener las columnas: cultivo, zona, fecha, precio_kg."));
        mvc.perform(multipart("/api/v1/precios/archivo")
                        .file(new MockMultipartFile("archivo", "precios.txt", "text/plain", "x".getBytes()))
                        .param("fuente", "MERCADO_SULLANA").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Suba un archivo Excel (.xlsx) o CSV (.csv)."));
    }

    @Test
    void unPrecioMuyAltoFrenteALaReferenciaMarcaLaPublicacion() throws Exception {
        cargar(ID_MANGO, idTambogrande, LocalDate.now(), "1.40", 201);
        int idKent = jdbc.queryForObject("select id_variedad from variedades where id_cultivo = 1 and nombre = 'Kent'",
                Integer.class);

        String respuesta = mvc.perform(post("/api/v1/publicaciones").header("Authorization", "Bearer " + tokenAgricultor)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idVariedad":%d,"idUnidad":2,"cantidad":30,"pesoUnidadKg":20,"precioUnitario":40,
                                 "idDistrito":%d,"fechaCosecha":"%s"}""".formatted(idKent, idTambogrande, LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        assertThat(jdbc.queryForObject("select marca_revision from publicaciones where id_publicacion = ?", String.class, id))
                .isEqualTo("PRECIO_FUERA_RANGO");
    }

    // ---------- Utilidades ----------

    private String cargar(int idCultivo, int idDistrito, LocalDate fecha, String precioKg, int esperado) throws Exception {
        return mvc.perform(post("/api/v1/precios").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(precio(idCultivo, idDistrito, fecha, precioKg)))
                .andExpect(status().is(esperado))
                .andReturn().getResponse().getContentAsString();
    }

    private static String precio(int idCultivo, int idDistrito, LocalDate fecha, String precioKg) {
        return """
                {"idCultivo":%d,"idDistrito":%d,"fecha":"%s","precioKg":%s,"fuente":"MERCADO_MAYORISTA_PIURA"}"""
                .formatted(idCultivo, idDistrito, fecha, precioKg);
    }

    private String token(String ruta, String json, int esperado) throws Exception {
        String respuesta = mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(esperado))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private int distrito(String ubigeo) {
        return jdbc.queryForObject("select id_distrito from distritos where ubigeo = ?", Integer.class, ubigeo);
    }
}
