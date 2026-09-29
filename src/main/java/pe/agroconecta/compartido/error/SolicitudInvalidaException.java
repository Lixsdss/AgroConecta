package pe.agroconecta.compartido.error;

/**
 * La solicitud no se puede procesar por su forma (por ejemplo, un archivo vacío o con columnas distintas a las
 * esperadas), antes de aplicar reglas de negocio.
 *
 * <p>{@link ManejadorGlobalExcepciones} la responde con 400 y su mensaje en español.</p>
 */
public class SolicitudInvalidaException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que verá el usuario.
     *
     * @param mensaje mensaje en español
     */
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
