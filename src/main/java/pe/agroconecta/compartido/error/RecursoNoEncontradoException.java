package pe.agroconecta.compartido.error;

/**
 * El recurso pedido no existe o quien lo pide no puede verlo (se responde igual para no revelar su existencia).
 *
 * <p>{@link ManejadorGlobalExcepciones} la responde con 404 y su mensaje en español.</p>
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que verá el usuario.
     *
     * @param mensaje mensaje en español
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
