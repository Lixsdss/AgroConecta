package pe.agroconecta.administracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.persistence.EntityManager;

import com.jayway.jsonpath.JsonPath;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * Pruebas de integración de reclamos, usuarios, suspensiones y panel (RF-09, UC-11 partes 1, 3 y 4) contra MariaDB:
 * API, seguridad, servicios, vistas y los triggers de {@code reclamos} y {@code suspensiones}. Cada prueba se
 * revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministracionIntegracionTest {

    private static final int ID_JABA = 2;
    private static final String COMPRADOR_EXTRA =
            ",\"nombreNegocio\":\"Restaurante El Chalán\",\"tipoComprador\":\"RESTAURANTE\"";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenAgricultor;
    private String tokenComprador;
    private long idAdmin;
    private long idAgricultor;
    private long idComprador;
    private long idPublicacion;
    private long idConversacion;
    private long idVenta;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() throws Exception {
        jdbc.update("insert into usuarios (rol, nombre_completo, celular, password_hash, id_distrito) "
                + "values ('ADMINISTRADOR', 'Lucía Farfán', '900000019', ?, 1)", passwordEncoder.encode("admin2026"));
        idAdmin = jdbc.queryForObject("select id_usuario from usuarios where celular = '900000019'", Long.class);
        tokenAdmin = iniciarSesion("900000019", "admin2026");
        tokenAgricultor = registrar("AGRICULTOR", "Juan Carlos Chunga", "912345721", "");
        tokenComprador = registrar("COMPRADOR", "Rosa Quispe", "912345722", COMPRADOR_EXTRA);
        idAgricultor = jdbc.queryForObject("select id_usuario from usuarios where celular = '912345721'", Long.class);
        idComprador = jdbc.queryForObject("select id_usuario from usuarios where celular = '912345722'", Long.class);
        idPublicacion = publicar();
        String respuesta = mvc.perform(post("/api/v1/conversaciones").header("Authorization", bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idPublicacion\":" + idPublicacion + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        idConversacion = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        idVenta = acordar();
    }

    // ---------------------------------------------------------------- Reclamos

    @Test
    void unaParteReclamaYElAdministradorLoResuelveUnaSolaVez() throws Exception {
        mvc.perform(reclamar(tokenComprador, idVenta, "CANTIDAD_INCOMPLETA", "  Llegaron 4 jabas, no 5.  "))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reclamos/mios?idVenta=" + idVenta))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.descripcion").value("Llegaron 4 jabas, no 5."))
                .andExpect(jsonPath("$.producto").value("Mango Kent"))
                .andExpect(jsonPath("$.reclamante").value("Restaurante El Chalán"))
                .andExpect(jsonPath("$.contraparte").value("Juan Carlos Chunga"))
                .andExpect(jsonPath("$.fechaRegistro").isNotEmpty());
        mvc.perform(reclamar(tokenComprador, idVenta, "MAL_TRATO", "Otra vez."))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya registró un reclamo sobre esta venta."));
        assertThat(avisos(idAgricultor, "RECLAMO")).containsExactly(
                "Se registró un reclamo sobre su venta de Mango Kent. AgroConecta lo revisará y les avisará.");

        mvc.perform(get("/api/v1/reclamos/mios").param("idVenta", String.valueOf(idVenta))
                        .header("Authorization", bearer(tokenComprador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").value("CANTIDAD_INCOMPLETA"));
        mvc.perform(get("/api/v1/reclamos/mios").header("Authorization", bearer(tokenAgricultor)))
                .andExpect(jsonPath("$.length()").value(0));

        String lista = mvc.perform(get("/api/v1/admin/reclamos").param("estado", "PENDIENTE")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(20))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(lista, "$.content[?(@.idVenta == " + idVenta + ")].id");
        long idReclamo = ids.getFirst();
        mvc.perform(get("/api/v1/admin/reclamos/" + idReclamo).header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reclamo.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.venta.cantidadKg").value(100.0))
                .andExpect(jsonPath("$.venta.comprador").value("Restaurante El Chalán"))
                .andExpect(jsonPath("$.venta.lineaDeTiempo[0].estado").value("ACORDADA"))
                .andExpect(jsonPath("$.venta.lineaDeTiempo[0].autor").value("Rosa Quispe"));

        mvc.perform(resolver(idReclamo, "FUNDADO", "  Se comprobó que   faltó una jaba. "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FUNDADO"))
                .andExpect(jsonPath("$.respuesta").value("Se comprobó que faltó una jaba."));
        mvc.perform(resolver(idReclamo, "INFUNDADO", "Cambio de opinión."))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El reclamo ya fue resuelto."));
        em.flush();
        em.clear();
        assertThat(jdbc.queryForObject("select fecha_resolucion is not null from reclamos where id_reclamo = ?",
                Boolean.class, idReclamo)).isTrue();
        assertThat(avisos(idComprador, "RECLAMO")).containsExactly(
                "Su reclamo sobre Mango Kent fue resuelto: fundado. Respuesta: Se comprobó que faltó una jaba.");
        assertThat(avisos(idAgricultor, "RECLAMO")).hasSize(2);
        mvc.perform(get("/api/v1/admin/usuarios/" + idAgricultor).header("Authorization", bearer(tokenAdmin)))
                .andExpect(jsonPath("$.reclamosFundados").value(1));
    }

    @Test
    void reclamarExigeSerParteDeUnaVentaExistenteYDatosCompletos() throws Exception {
        String tokenOtro = registrar("COMPRADOR", "Otro comprador", "912345723", COMPRADOR_EXTRA);

        mvc.perform(reclamar(tokenOtro, idVenta, "MAL_TRATO", "No soy parte.")).andExpect(status().isForbidden());
        mvc.perform(reclamar(tokenComprador, 999999999, "MAL_TRATO", "No existe."))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La venta no existe."));
        mvc.perform(reclamar(tokenComprador, idVenta, null, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Elija el motivo.")))
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Cuente qué pasó.")));
        mvc.perform(reclamar(tokenAdmin, idVenta, "MAL_TRATO", "Admin.")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/reclamos").header("Authorization", bearer(tokenComprador)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/reclamos/999999999").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El reclamo no existe."));
    }

    @Test
    void laVentaCanceladaTambienSeReclamaPorqueLlegoAAcordarse() throws Exception {
        mvc.perform(post("/api/v1/ventas/" + idVenta + "/estado").header("Authorization", bearer(tokenAgricultor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"CANCELAR\"}"))
                .andExpect(status().isOk());

        mvc.perform(reclamar(tokenAgricultor, idVenta, "NO_SE_PRESENTO", "Canceló a última hora."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reclamante").value("Juan Carlos Chunga"))
                .andExpect(jsonPath("$.contraparte").value("Restaurante El Chalán"));
    }

    // ---------------------------------------------------------------- Usuarios

    @Test
    void listaUsuariosConFiltros() throws Exception {
        mvc.perform(get("/api/v1/admin/usuarios").param("q", "rosa qui").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].negocio").value("Restaurante El Chalán"))
                .andExpect(jsonPath("$.content[0].tipoComprador").value("RESTAURANTE"))
                .andExpect(jsonPath("$.content[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$.content[0].reclamosFundados").value(0));
        mvc.perform(get("/api/v1/admin/usuarios").param("q", "912345721").param("rol", "AGRICULTOR")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(jsonPath("$.content[0].nombre").value("Juan Carlos Chunga"));
        mvc.perform(get("/api/v1/admin/usuarios").param("q", "912345721").param("rol", "COMPRADOR")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/v1/admin/usuarios").param("estado", "BORRADO").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El filtro de rol o estado no es válido."));
        mvc.perform(get("/api/v1/admin/usuarios").header("Authorization", bearer(tokenAgricultor)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/usuarios/999999999").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El usuario no existe."));
    }

    @Test
    void suspenderQuitaElAccesoAvisaALasPartesYReactivarLoDevuelve() throws Exception {
        mvc.perform(get("/api/v1/admin/usuarios/" + idComprador).header("Authorization", bearer(tokenAdmin)))
                .andExpect(jsonPath("$.ventasEnCurso.length()").value(1))
                .andExpect(jsonPath("$.ventasEnCurso[0].estado").value("ACORDADA"))
                .andExpect(jsonPath("$.ultimoAcceso").isNotEmpty())
                .andExpect(jsonPath("$.suspension").doesNotExist());

        mvc.perform(suspender(idComprador, "USO_INDEBIDO", "Ofreció precios falsos   en el chat."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDO"))
                .andExpect(jsonPath("$.motivoSuspension").value("USO_INDEBIDO"))
                .andExpect(jsonPath("$.suspension.detalle").value("Ofreció precios falsos en el chat."))
                .andExpect(jsonPath("$.suspension.administrador").value("Lucía Farfán"));
        mvc.perform(get("/api/v1/ventas").header("Authorization", bearer(tokenComprador)))
                .andExpect(status().isUnauthorized());
        assertThat(avisos(idComprador, "CUENTA"))
                .containsExactly("Motivo: uso indebido. Ofreció precios falsos en el chat.");
        assertThat(avisos(idAgricultor, "VENTA")).contains(
                "La cuenta de la otra parte de su venta de Mango Kent fue suspendida. Puede cancelarla si lo necesita.");
        mvc.perform(suspender(idComprador, "DATOS_FALSOS", "Otra vez."))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El usuario ya está suspendido."));

        mvc.perform(post("/api/v1/admin/usuarios/" + idComprador + "/reactivacion")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.suspension").doesNotExist());
        mvc.perform(get("/api/v1/ventas").header("Authorization", bearer(tokenComprador))).andExpect(status().isOk());
        assertThat(avisos(idComprador, "CUENTA")).hasSize(2);
        mvc.perform(post("/api/v1/admin/usuarios/" + idComprador + "/reactivacion")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El usuario no está suspendido."));
    }

    @Test
    void lasReglasDeSuspensionSeValidanAntesDeLaBaseDeDatos() throws Exception {
        mvc.perform(suspender(idAgricultor, "RECLAMOS_FUNDADOS", "Muchos reclamos."))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Se requieren al menos 3 reclamos fundados (tiene 0)."));
        mvc.perform(suspender(idAdmin, "USO_INDEBIDO", "No corresponde."))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No se suspende a un administrador."));
        mvc.perform(suspender(idAgricultor, null, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Elija el motivo.")))
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Escriba el detalle de la suspensión.")));
        mvc.perform(suspender(999999999, "USO_INDEBIDO", "No existe.")).andExpect(status().isNotFound());
    }

    @Test
    void laContrasenaTemporalPermiteIniciarSesion() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/admin/usuarios/" + idAgricultor + "/contrasena-temporal")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String temporal = JsonPath.read(respuesta, "$.contrasenaTemporal");

        assertThat(temporal).hasSize(10).matches("[A-Za-z0-9]+");
        iniciarSesion("912345721", temporal);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"celular\":\"912345721\",\"contrasena\":\"clave2026\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/usuarios/" + idAdmin + "/contrasena-temporal")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La contraseña de un administrador no se reinicia desde aquí."));
    }

    @Test
    void desactivarImpideEntrarYActivarLoDevuelve() throws Exception {
        mvc.perform(post("/api/v1/admin/usuarios/" + idAgricultor + "/desactivacion")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(get("/api/v1/ventas").header("Authorization", bearer(tokenAgricultor)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"celular\":\"912345721\",\"contrasena\":\"clave2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(startsWith("Su cuenta está desactivada")));
        mvc.perform(post("/api/v1/admin/usuarios/" + idAgricultor + "/desactivacion")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se desactiva una cuenta activa."));
        mvc.perform(suspender(idAgricultor, "USO_INDEBIDO", "Desactivado."))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se suspende una cuenta activa."));

        mvc.perform(post("/api/v1/admin/usuarios/" + idAgricultor + "/activacion")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        iniciarSesion("912345721", "clave2026");
        mvc.perform(post("/api/v1/admin/usuarios/" + idAgricultor + "/activacion")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Solo se activa una cuenta desactivada."));
        mvc.perform(post("/api/v1/admin/usuarios/" + idAdmin + "/desactivacion")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No se desactiva a un administrador."));
    }

    @Test
    void creaOtroAdministradorQuePuedeEntrar() throws Exception {
        String json = """
                {"nombreCompleto":"  Carlos   Ruiz ","celular":"900 000 029","idDistrito":1,"contrasena":"admin2027"}""";
        mvc.perform(post("/api/v1/admin/administradores").header("Authorization", bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/admin/usuarios/")))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.nombre").value("Carlos Ruiz"))
                .andExpect(jsonPath("$.ventasEnCurso.length()").value(0));
        iniciarSesion("900000029", "admin2027");

        mvc.perform(post("/api/v1/admin/administradores").header("Authorization", bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este celular ya está registrado."));
        mvc.perform(post("/api/v1/admin/administradores").header("Authorization", bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"celular\":\"12\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/administradores").header("Authorization", bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- Panel

    @Test
    void elPanelMuestraColaVentasDelMesEmbudoYReputacion() throws Exception {
        mvc.perform(post("/api/v1/ventas/" + idVenta + "/estado").header("Authorization", bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"CONFIRMAR_RECEPCION\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/ventas/" + idVenta + "/calificacion").header("Authorization", bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":5}"))
                .andExpect(status().isCreated());
        em.flush();
        em.clear();

        mvc.perform(get("/api/v1/admin/panel").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarjetas.publicacionesActivas").isNumber())
                .andExpect(jsonPath("$.tarjetas.porRevisar").isNumber())
                .andExpect(jsonPath("$.colaPorRevisar.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(3)))
                .andExpect(jsonPath("$.mes").value(hoy.toString().substring(0, 7)))
                .andExpect(jsonPath("$.kilosPorCultivo[*].cultivo").value(hasItem("Mango")))
                .andExpect(jsonPath("$.embudo[?(@.idPublicacion == " + idPublicacion + ")].producto")
                        .value(hasItem("Mango Kent")))
                .andExpect(jsonPath("$.embudo[?(@.idPublicacion == " + idPublicacion + ")].contactos").value(hasItem(1)))
                .andExpect(jsonPath("$.embudo[?(@.idPublicacion == " + idPublicacion + ")].completadas")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$.mejorReputacion[?(@.id == " + idAgricultor + ")].calificacion")
                        .value(hasItem(5.0)));
        mvc.perform(get("/api/v1/admin/panel").header("Authorization", bearer(tokenAgricultor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elExcelDelPanelTieneSusCuatroHojas() throws Exception {
        byte[] contenido = mvc.perform(get("/api/v1/admin/panel/exportar").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"AgroConecta_Panel_%s.xlsx\"".formatted(hoy)))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            assertThat(IntStream.range(0, libro.getNumberOfSheets()).mapToObj(libro::getSheetName))
                    .containsExactly("Resumen", "Ventas por mes", "Embudo", "Usuarios");
            assertThat(libro.getSheet("Resumen").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("Publicaciones activas");
            assertThat(libro.getSheet("Embudo").getRow(1).getCell(1).getStringCellValue()).isEqualTo("Mango Kent");
            assertThat(libro.getSheet("Usuarios").getLastRowNum()).isGreaterThanOrEqualTo(3);
        }
    }

    // ---------------------------------------------------------------- Ayudas

    private List<String> avisos(long idUsuario, String tipo) {
        em.flush();
        return jdbc.queryForList("select contenido from notificaciones where id_usuario = ? and tipo = ? "
                + "order by id_notificacion", String.class, idUsuario, tipo);
    }

    private RequestBuilder reclamar(String token, long venta, String motivo, String descripcion) {
        String json = """
                {"idVenta":%d,"motivo":%s,"descripcion":"%s"}""".formatted(venta,
                motivo == null ? "null" : "\"" + motivo + "\"", descripcion);
        return post("/api/v1/reclamos").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private RequestBuilder resolver(long idReclamo, String estado, String respuesta) {
        return post("/api/v1/admin/reclamos/" + idReclamo + "/resolucion").header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"%s\",\"respuesta\":\"%s\"}".formatted(estado, respuesta));
    }

    private RequestBuilder suspender(long idUsuario, String motivo, String detalle) {
        String json = """
                {"motivo":%s,"detalle":"%s"}""".formatted(motivo == null ? "null" : "\"" + motivo + "\"", detalle);
        return post("/api/v1/admin/usuarios/" + idUsuario + "/suspension").header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private long acordar() throws Exception {
        String json = """
                {"idConversacion":%d,"idUnidad":%d,"cantidad":5,"pesoUnidadKg":null,"precioUnitario":28,
                 "fechaRecojo":"%s"}""".formatted(idConversacion, ID_JABA, hoy);
        String respuesta = mvc.perform(post("/api/v1/ventas").header("Authorization", bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
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
        String respuesta = mvc.perform(post("/api/v1/publicaciones").header("Authorization", bearer(tokenAgricultor))
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
}
