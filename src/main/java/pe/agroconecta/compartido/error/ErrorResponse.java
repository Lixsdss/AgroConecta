package pe.agroconecta.compartido.error;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatusCode;

/**
 * Formato único de las respuestas de error de la API.
 *
 * @param timestamp momento en que ocurrió el error
 * @param status    código de estado HTTP
 * @param mensaje   mensaje general en español, apto para mostrarse al usuario
 * @param errores   errores por campo (solo en validaciones, código 400); en los demás casos, lista vacía
 */
public record ErrorResponse(Instant timestamp, int status, String mensaje, List<ErrorCampo> errores) {

    /**
     * Error de validación de un campo de la petición.
     *
     * @param campo   nombre del campo o parámetro inválido
     * @param mensaje mensaje de validación en español
     */
    public record ErrorCampo(String campo, String mensaje) {
    }

    /**
     * Crea una respuesta de error sin errores por campo.
     *
     * @param status  código de estado HTTP
     * @param mensaje mensaje general en español
     * @return respuesta de error con la fecha y hora actuales
     */
    public static ErrorResponse de(HttpStatusCode status, String mensaje) {
        return de(status, mensaje, List.of());
    }

    /**
     * Crea una respuesta de error con errores por campo.
     *
     * @param status  código de estado HTTP
     * @param mensaje mensaje general en español
     * @param errores errores por campo
     * @return respuesta de error con la fecha y hora actuales
     */
    public static ErrorResponse de(HttpStatusCode status, String mensaje, List<ErrorCampo> errores) {
        return new ErrorResponse(Instant.now(), status.value(), mensaje, List.copyOf(errores));
    }
}
