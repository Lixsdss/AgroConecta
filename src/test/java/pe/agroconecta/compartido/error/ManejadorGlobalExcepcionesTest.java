package pe.agroconecta.compartido.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

/**
 * Pruebas unitarias de {@link ManejadorGlobalExcepciones}, sin contexto de Spring.
 */
class ManejadorGlobalExcepcionesTest {

    private static final String RUTA = "/api/v1/transacciones";
    private static final String MENSAJE_TRIGGER = "La cantidad supera lo disponible en la publicacion";

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", RUTA);

    @Test
    void errorDeTriggerTraducidoPorSpringDevuelve422ConElFormatoDelSrs() {
        SQLException signal = new SQLException(MENSAJE_TRIGGER, "45000", 1644);
        var ex = new UncategorizedSQLException("insertar transaccion", "INSERT INTO transacciones ...", signal);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarErrorBaseDatos(ex, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        ErrorResponse cuerpo = respuesta.getBody();
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.timestamp()).isNotNull();
        assertThat(cuerpo.estado()).isEqualTo(422);
        assertThat(cuerpo.error()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase());
        assertThat(cuerpo.mensaje()).isEqualTo(MENSAJE_TRIGGER);
        assertThat(cuerpo.ruta()).isEqualTo(RUTA);
        assertThat(cuerpo.errores()).isEmpty();
    }

    @Test
    void errorDeTriggerEnvueltoPorHibernateDevuelve422ConElMensajeDelTrigger() {
        SQLException signal = new SQLException(MENSAJE_TRIGGER, "45000", 1644);
        var ex = new GenericJDBCException("could not execute statement", signal);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarErrorBaseDatos(ex, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(MENSAJE_TRIGGER);
    }

    @Test
    void violacionDeCheckEnMariaDbDevuelve422SinDetallesDeSql() {
        var sql = new SQLIntegrityConstraintViolationException(
                "CONSTRAINT `chk_publicacion_cantidad` failed for `agroconecta`.`publicaciones`", "23000", 4025);

        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejarErrorBaseDatos(new DataIntegrityViolationException("check", sql), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getBody().mensaje())
                .isEqualTo(ManejadorGlobalExcepciones.MENSAJE_CHECK)
                .doesNotContain("chk_publicacion_cantidad");
    }

    @Test
    void violacionDeCheckEnMySqlDevuelve422() {
        var sql = new SQLException("Check constraint 'chk_publicacion_precio' is violated.", "HY000", 3819);

        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejarErrorBaseDatos(new DataIntegrityViolationException("check", sql), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ManejadorGlobalExcepciones.MENSAJE_CHECK);
    }

    @Test
    void claveDuplicadaDevuelve409SinDetallesDeSql() {
        var sql = new SQLIntegrityConstraintViolationException(
                "Duplicate entry '987654321' for key 'uk_usuario_celular'", "23000", 1062);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarErrorBaseDatos(
                new DataIntegrityViolationException("could not execute statement", sql), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().error()).isEqualTo("Conflict");
        assertThat(respuesta.getBody().mensaje())
                .isEqualTo(ManejadorGlobalExcepciones.MENSAJE_DUPLICADO)
                .doesNotContain("uk_usuario_celular", "Duplicate");
    }

    @Test
    void claveForaneaDevuelve409() {
        var sql = new SQLIntegrityConstraintViolationException(
                "Cannot add or update a child row: a foreign key constraint fails", "23000", 1452);

        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejarErrorBaseDatos(new DataIntegrityViolationException("fk", sql), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ManejadorGlobalExcepciones.MENSAJE_CLAVE_FORANEA);
    }

    @Test
    void reglaDeNegocioDelServicioDevuelve422ConSuMensaje() {
        var ex = new ReglaNegocioException("Solo se envían ofertas mientras la publicación está PUBLICADA");

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarReglaNegocio(ex, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ex.getMessage());
        assertThat(respuesta.getBody().ruta()).isEqualTo(RUTA);
    }

    @Test
    void otroErrorDeBaseDeDatosDevuelve500Generico() {
        var sql = new SQLException("Communications link failure", "08S01", 0);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarErrorBaseDatos(
                new UncategorizedSQLException("consulta", "SELECT 1", sql), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ManejadorGlobalExcepciones.MENSAJE_INTERNO);
    }

    @Test
    void errorNoPrevistoDevuelve500Generico() throws Exception {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejarErrorGeneral(new IllegalStateException("detalle interno"), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ManejadorGlobalExcepciones.MENSAJE_INTERNO);
        assertThat(respuesta.getBody().estado()).isEqualTo(500);
    }

    @Test
    void excepcionDeSeguridadSeRelanzaParaSpringSecurity() {
        var ex = new AccessDeniedException("denegado");

        assertThatThrownBy(() -> manejador.manejarErrorGeneral(ex, request)).isSameAs(ex);
    }
}
