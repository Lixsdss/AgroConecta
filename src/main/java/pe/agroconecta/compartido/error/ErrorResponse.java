package pe.agroconecta.compartido.error;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Formato único de las respuestas de error de la API (SRS §9.2).
 *
 * @param timestamp momento en que ocurrió el error, con zona horaria
 * @param estado    código de estado HTTP
 * @param error     frase estándar del código HTTP (por ejemplo, "Conflict")
 * @param mensaje   mensaje en español, apto para mostrarse al usuario
 * @param ruta      ruta de la petición que falló
 * @param errores   errores por campo (solo en validaciones, código 400); en los demás casos, lista vacía
 */
public record ErrorResponse(OffsetDateTime timestamp, int estado, String error, String mensaje, String ruta,
        List<ErrorCampo> errores) {

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
     * @param mensaje mensaje en español
     * @param ruta    ruta de la petición
     * @return respuesta de error con la fecha y hora actuales
     */
    public static ErrorResponse de(HttpStatusCode status, String mensaje, String ruta) {
        return de(status, mensaje, ruta, List.of());
    }

    /**
     * Crea una respuesta de error con errores por campo.
     *
     * @param status  código de estado HTTP
     * @param mensaje mensaje en español
     * @param ruta    ruta de la petición
     * @param errores errores por campo
     * @return respuesta de error con la fecha y hora actuales
     */
    public static ErrorResponse de(HttpStatusCode status, String mensaje, String ruta, List<ErrorCampo> errores) {
        HttpStatus conocido = HttpStatus.resolve(status.value());
        String frase = conocido != null ? conocido.getReasonPhrase() : String.valueOf(status.value());
        return new ErrorResponse(OffsetDateTime.now(), status.value(), frase, mensaje, ruta, List.copyOf(errores));
    }
}
