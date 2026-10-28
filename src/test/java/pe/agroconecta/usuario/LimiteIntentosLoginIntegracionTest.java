package pe.agroconecta.usuario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Límite de intentos de inicio de sesión (RF-08, seguridad): tras 5 intentos fallidos con el mismo celular en
 * 15 minutos, los siguientes intentos responden 429 con el formato {@code ErrorResponse} hasta que vence la
 * ventana; un inicio de sesión correcto reinicia el contador. El reloj de la aplicación se reemplaza por uno
 * ajustable para simular el paso del tiempo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(LimiteIntentosLoginIntegracionTest.ConfigReloj.class)
class LimiteIntentosLoginIntegracionTest {

    private static final String MENSAJE_BLOQUEO = "Demasiados intentos fallidos. Espere 15 minutos e intente de nuevo.";
    private static final String CONTRASENA = "clave2026";
    private static final String ERRONEA = "otraClave1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RelojAjustable reloj;

    @Test
    void cincoFallosBloqueanConCodigo429HastaQueVenceLaVentana() throws Exception {
        registrar("977000001");
        for (int i = 0; i < 5; i++) {
            login("977000001", ERRONEA).andExpect(status().isUnauthorized());
        }

        login("977000001", ERRONEA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.estado").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_BLOQUEO))
                .andExpect(jsonPath("$.ruta").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.errores").isEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
        // Mientras dura el bloqueo ni siquiera la contraseña correcta permite entrar
        login("977000001", CONTRASENA).andExpect(status().isTooManyRequests());

        reloj.avanzar(Duration.ofMinutes(14));
        login("977000001", CONTRASENA).andExpect(status().isTooManyRequests());

        reloj.avanzar(Duration.ofMinutes(1).plusSeconds(1));
        login("977000001", CONTRASENA).andExpect(status().isOk());
    }

    @Test
    void unInicioCorrectoReiniciaElContador() throws Exception {
        registrar("977000002");
        for (int i = 0; i < 4; i++) {
            login("977000002", ERRONEA).andExpect(status().isUnauthorized());
        }
        login("977000002", CONTRASENA).andExpect(status().isOk());

        // Sin el reinicio, el segundo de estos intentos ya respondería 429
        for (int i = 0; i < 5; i++) {
            login("977000002", ERRONEA).andExpect(status().isUnauthorized());
        }
        login("977000002", ERRONEA).andExpect(status().isTooManyRequests());
    }

    @Test
    void elBloqueoEsPorCelular() throws Exception {
        registrar("977000003");
        registrar("977000004");
        for (int i = 0; i < 5; i++) {
            login("977000003", ERRONEA).andExpect(status().isUnauthorized());
        }

        login("977000003", CONTRASENA).andExpect(status().isTooManyRequests());
        login("977000004", CONTRASENA).andExpect(status().isOk());
    }

    @Test
    void elCelularConEspaciosCuentaComoElMismo() throws Exception {
        registrar("977000005");
        for (int i = 0; i < 5; i++) {
            login(i % 2 == 0 ? "977 000 005" : "977000005", ERRONEA).andExpect(status().isUnauthorized());
        }

        login(" 977000005 ", CONTRASENA).andExpect(status().isTooManyRequests());
    }

    private ResultActions login(String celular, String contrasena) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"celular\":\"%s\",\"contrasena\":\"%s\"}".formatted(celular, contrasena)));
    }

    private void registrar(String celular) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rol":"AGRICULTOR","nombreCompleto":"Juan Chunga","celular":"%s","idDistrito":1,
                                 "contrasena":"%s","nombreChacra":"La Esperanza"}""".formatted(celular, CONTRASENA)))
                .andExpect(status().isCreated());
    }

    /** Reemplaza el reloj de la aplicación por uno que la prueba puede adelantar. */
    @TestConfiguration
    static class ConfigReloj {

        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable(Instant.now(), ZoneId.systemDefault());
        }
    }

    /** Reloj que empieza en un instante dado y solo avanza cuando la prueba lo pide. */
    static class RelojAjustable extends Clock {

        private volatile Instant ahora;
        private final ZoneId zona;

        RelojAjustable(Instant inicio, ZoneId zona) {
            this.ahora = inicio;
            this.zona = zona;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId otraZona) {
            return new RelojAjustable(ahora, otraZona);
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
