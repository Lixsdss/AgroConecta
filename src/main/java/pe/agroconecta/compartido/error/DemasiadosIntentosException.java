package pe.agroconecta.compartido.error;

/**
 * Operación rechazada temporalmente porque se superó un límite de intentos (por ejemplo, demasiados inicios de
 * sesión fallidos con el mismo celular).
 *
 * <p>{@link ManejadorGlobalExcepciones} la responde con 429 y su mensaje.</p>
 */
public class DemasiadosIntentosException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que verá el usuario.
     *
     * @param mensaje mensaje en español
     */
    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
