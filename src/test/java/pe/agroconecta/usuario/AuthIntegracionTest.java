package pe.agroconecta.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

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
 * Pruebas de integración de RF-08 contra MariaDB ({@code agroconecta_test}): API, seguridad, servicios, triggers
 * y formato de errores. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegracionTest {

    private static final int ID_DISTRITO_PIURA = 1;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    // ---------- Registro ----------

    @Test
    void registroDeAgricultorDevuelve201ConSesionYPerfil() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(agricultor("911 111 111", "Juan Chunga")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/usuarios/me"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("AGRICULTOR"))
                .andExpect(jsonPath("$.usuario.nombreCompleto").value("Juan Chunga"))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(respuesta, "$.token");
        assertThat(clavesDelToken(token)).contains("\"roles\":[\"AGRICULTOR\"]", "\"iss\":\"agroconecta\"");

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celular").value("911111111"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.distrito").value("Piura"))
                .andExpect(jsonPath("$.nombreChacra").value("La Esperanza"))
                .andExpect(jsonPath("$.nombreNegocio").doesNotExist());
    }

    @Test
    void registroDeCompradorGuardaLosDatosDelNegocio() throws Exception {
        String token = registrar(comprador("922222222", "20123456789"));

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("COMPRADOR"))
                .andExpect(jsonPath("$.nombreNegocio").value("Restaurante El Chalán"))
                .andExpect(jsonPath("$.tipoComprador").value("RESTAURANTE"))
                .andExpect(jsonPath("$.ruc").value("20123456789"));
    }

    @Test
    void compradorSinDatosDelNegocioDevuelve400ConElDetalle() throws Exception {
        String sinNegocio = """
                {"rol":"COMPRADOR","nombreCompleto":"Rosa Quispe","celular":"933333333",
                 "idDistrito":1,"contrasena":"clave2026"}""";

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(sinNegocio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.ruta").value("/api/v1/auth/registro"))
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Indique el nombre y el tipo de su negocio.")));
    }

    @Test
    void contrasenaDebilYCelularInvalidoDevuelven400PorCampo() throws Exception {
        String invalido = """
                {"rol":"AGRICULTOR","nombreCompleto":"Juan Chunga","celular":"81234567",
                 "idDistrito":1,"contrasena":"solotexto"}""";

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("contrasena")))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("celular")));
    }

    @Test
    void nadiePuedeRegistrarseComoAdministrador() throws Exception {
        String admin = """
                {"rol":"ADMINISTRADOR","nombreCompleto":"Intruso","celular":"944444444",
                 "idDistrito":1,"contrasena":"clave2026"}""";

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].mensaje").value(hasItem("Solo pueden registrarse agricultores y compradores.")));
    }

    @Test
    void celularDuplicadoDevuelve409() throws Exception {
        registrar(agricultor("955555555", "Juan Chunga"));

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(comprador("955555555", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este celular ya está registrado."));
    }

    @Test
    void rucDuplicadoDevuelve409() throws Exception {
        registrar(comprador("966666661", "20999999991"));

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(comprador("966666662", "20999999991")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este RUC ya está registrado."));
    }

    @Test
    void distritoInexistenteDevuelve422() throws Exception {
        String json = agricultor("977777777", "Juan Chunga").replace("\"idDistrito\":1", "\"idDistrito\":9999");

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El distrito elegido no existe."));
    }

    // ---------- Inicio de sesión ----------

    @Test
    void loginCorrectoDevuelveTokenValido() throws Exception {
        registrar(agricultor("988888888", "Juan Chunga"));

        String token = iniciarSesion("988 888 888", "clave2026");

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Juan Chunga"));
        assertThat(jdbc.queryForObject("select ultimo_acceso is not null from usuarios where celular = '988888888'",
                Boolean.class)).isTrue();
    }

    @Test
    void loginFallidoNoDiceQueDatoFallo() throws Exception {
        registrar(agricultor("999999991", "Juan Chunga"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"celular\":\"999999991\",\"contrasena\":\"otraClave1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Celular o contraseña incorrectos."));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"celular\":\"999999990\",\"contrasena\":\"clave2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Celular o contraseña incorrectos."));
    }

    @Test
    void usuarioSuspendidoNoIniciaSesion() throws Exception {
        registrar(agricultor("999999992", "Juan Chunga"));
        cambiarEstado("999999992", "SUSPENDIDO");

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"celular\":\"999999992\",\"contrasena\":\"clave2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Su cuenta está suspendida. Comuníquese con el administrador."));
    }

    // ---------- Seguridad ----------

    @Test
    void sinTokenDevuelve401ConElFormatoDeLaApi() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value("Inicie sesión para continuar."))
                .andExpect(jsonPath("$.ruta").value("/api/v1/usuarios/me"));
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer no.es.un-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Inicie sesión para continuar."));
    }

    @Test
    void tokenDeUnUsuarioQueDejoDeEstarActivoDevuelve401() throws Exception {
        String token = registrar(agricultor("999999993", "Juan Chunga"));
        cambiarEstado("999999993", "INACTIVO");

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Perfil ----------

    @Test
    void actualizarPerfilCambiaNombreDistritoYChacra() throws Exception {
        String token = registrar(agricultor("999999994", "Juan Chunga"));
        int idTambogrande = jdbc.queryForObject("select id_distrito from distritos where ubigeo = '200114'",
                Integer.class);
        String cambios = """
                {"nombreCompleto":"  Juan   Chunga Ruiz ","idDistrito":%d,"nombreChacra":"El Algarrobo"}"""
                .formatted(idTambogrande);

        mvc.perform(put("/api/v1/usuarios/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cambios))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Juan Chunga Ruiz"))
                .andExpect(jsonPath("$.distrito").value("Tambogrande"))
                .andExpect(jsonPath("$.nombreChacra").value("El Algarrobo"));
    }

    @Test
    void compradorNoPuedeQuitarLosDatosDeSuNegocio() throws Exception {
        String token = registrar(comprador("999999995", null));

        mvc.perform(put("/api/v1/usuarios/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Rosa Quispe\",\"idDistrito\":1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Indique el nombre y el tipo de su negocio."));
    }

    @Test
    void cambiarContrasenaExigeLaActualYLuegoPermiteIngresarConLaNueva() throws Exception {
        String token = registrar(agricultor("999999996", "Juan Chunga"));

        mvc.perform(put("/api/v1/usuarios/me/contrasena").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"equivocada1\",\"contrasenaNueva\":\"nueva2027\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta."));

        mvc.perform(put("/api/v1/usuarios/me/contrasena").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"clave2026\",\"contrasenaNueva\":\"nueva2027\"}"))
                .andExpect(status().isNoContent());

        iniciarSesion("999999996", "nueva2027");
    }

    // ---------- Utilidades ----------

    /**
     * Simula lo que hace el trigger de suspensiones (o el administrador) desde otra transacción: cambia el estado
     * con SQL y vacía el contexto de persistencia, que la prueba comparte con los servicios.
     */
    private void cambiarEstado(String celular, String estado) {
        em.flush();
        jdbc.update("update usuarios set estado = ? where celular = ?", estado, celular);
        em.clear();
    }

    private String registrar(String json) throws Exception {
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

    private static String agricultor(String celular, String nombre) {
        return """
                {"rol":"AGRICULTOR","nombreCompleto":"%s","celular":"%s","idDistrito":%d,
                 "contrasena":"clave2026","nombreChacra":"La Esperanza"}""".formatted(nombre, celular, ID_DISTRITO_PIURA);
    }

    private static String comprador(String celular, String ruc) {
        String campoRuc = ruc == null ? "" : ",\"ruc\":\"" + ruc + "\"";
        return """
                {"rol":"COMPRADOR","nombreCompleto":"Rosa Quispe","celular":"%s","idDistrito":%d,
                 "contrasena":"clave2026","nombreNegocio":"Restaurante El Chalán","tipoComprador":"RESTAURANTE"%s}"""
                .formatted(celular, ID_DISTRITO_PIURA, campoRuc);
    }

    private static String clavesDelToken(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
    }
}
