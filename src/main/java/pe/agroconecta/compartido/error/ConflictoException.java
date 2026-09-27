package pe.agroconecta.compartido.error;

/**
 * Conflicto con un dato que ya existe (por ejemplo, un celular ya registrado), detectado por un servicio
 * antes de llegar a la base de datos.
 *
 * <p>{@link ManejadorGlobalExcepciones} la responde con 409 y su mensaje, que debe estar en español y
 * ser apto para mostrarse al usuario.</p>
 */
public class ConflictoException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que verá el usuario.
     *
     * @param mensaje mensaje en español
     */
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
