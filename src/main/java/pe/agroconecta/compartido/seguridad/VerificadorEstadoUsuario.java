package pe.agroconecta.compartido.seguridad;

/**
 * Consulta si un usuario sigue ACTIVO. La implementa el módulo {@code usuario}; la seguridad la usa en cada
 * petición autenticada para que una suspensión o desactivación surta efecto de inmediato (SRS §9.1).
 */
public interface VerificadorEstadoUsuario {

    /**
     * Indica si el usuario existe y está ACTIVO.
     *
     * @param idUsuario id del usuario (claim {@code sub} del token)
     * @return {@code true} solo si el usuario existe y su estado es ACTIVO
     */
    boolean estaActivo(long idUsuario);
}
