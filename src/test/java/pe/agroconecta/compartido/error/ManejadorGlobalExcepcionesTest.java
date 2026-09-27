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
import org.springframework.security.access.AccessDeniedException;

/**
 * Pruebas unitarias de {@link ManejadorGlobalExcepciones}, sin contexto de Spring.
 */
class ManejadorGlobalExcepcionesTest {

    private static final String MENSAJE_TRIGGER = "La cantidad supera lo disponible en la publicacion";

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    @Test
    void errorDeTriggerTraducidoPorSpringDevuelve422ConElMensajeDelTrigger() {
        SQLException signal = new SQLException(MENSAJE_TRIGGER, "45000", 1644);
        var ex = new UncategorizedSQLException("insertar transaccion", "INSERT INTO transacciones ...", signal);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarErrorBaseDatos(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(422);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(MENSAJE_TRIGGER);
        assertThat(respuesta.getBody().errores()).isEmpty();
    }

    @Test
    void errorDeTriggerEnvueltoPorHibernateDevuelve422ConElMensajeDelTrigger() {
        SQLException signal = new SQLException(MENSAJE_TRIGGER, "45000", 1644);
        var ex = new GenericJDBCException("could not execute statement", signal);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarErrorBaseDatos(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(MENSAJE_TRIGGER);
    }

    @Test
    void claveDuplicadaDevuelve409SinDetallesDeSql() {
        var sql = new SQLIntegrityConstraintViolationException(
                "Duplicate entry '987654321' for key 'uk_usuario_celular'", "23000", 1062);
        var ex = new DataIntegrityViolationException("could not execute statement", sql);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarErrorBaseDatos(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().mensaje())
                .isEqualTo(ManejadorGlobalExcepciones.MENSAJE_DUPLICADO)
                .doesNotContain("uk_usuario_celular", "Duplicate");
    }

    @Test
    void violacionDeCheckEnMariaDbDevuelve409() {
        var sql = new SQLIntegrityConstraintViolationException(
                "CONSTRAINT `chk_publicacion_cantidad` failed for `agroconecta`.`publicaciones`", "23000", 4025);

        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejarErrorBaseDatos(new DataIntegrityViolationException("check", sql));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ManejadorGlobalExcepciones.MENSAJE_CHECK);
    }

    @Test
    void otroErrorDeBaseDeDatosDevuelve500Generico() {
        var sql = new SQLException("Communications link failure", "08S01", 0);

        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejarErrorBaseDatos(new UncategorizedSQLException("consulta", "SELECT 1", sql));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ManejadorGlobalExcepciones.MENSAJE_INTERNO);
    }

    @Test
    void errorNoPrevistoDevuelve500Generico() throws Exception {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejarErrorGeneral(new IllegalStateException("detalle interno"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ManejadorGlobalExcepciones.MENSAJE_INTERNO);
    }

    @Test
    void excepcionDeSeguridadSeRelanzaParaSpringSecurity() {
        var ex = new AccessDeniedException("denegado");

        assertThatThrownBy(() -> manejador.manejarErrorGeneral(ex)).isSameAs(ex);
    }
}
