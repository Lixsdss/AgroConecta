package pe.agroconecta.compartido.error;

/**
 * Intento de inicio de sesión rechazado: credenciales incorrectas o cuenta no activa.
 *
 * <p>{@link ManejadorGlobalExcepciones} la responde con 401 y su mensaje. El mensaje nunca debe indicar
 * cuál de los datos (celular o contraseña) fue el incorrecto.</p>
 */
public class AutenticacionFallidaException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que verá el usuario.
     *
     * @param mensaje mensaje en español
     */
    public AutenticacionFallidaException(String mensaje) {
        super(mensaje);
    }
}
