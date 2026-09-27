package pe.agroconecta.compartido.error;

/**
 * Violación de una regla de negocio detectada por un servicio de {@code aplicacion}.
 *
 * <p>{@link ManejadorGlobalExcepciones} la responde con 422 y su mensaje, que debe estar en español y
 * ser apto para mostrarse al usuario.</p>
 */
public class ReglaNegocioException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que verá el usuario.
     *
     * @param mensaje mensaje en español
     */
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
