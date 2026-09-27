package pe.agroconecta.usuario.dominio;

/**
 * Estado del usuario ({@code chk_usuario_estado}). SUSPENDIDO lo fijan los triggers de {@code suspensiones}.
 */
public enum EstadoUsuario {
    /** Puede usar la plataforma. */
    ACTIVO,
    /** Suspendido por un administrador (RF-09). */
    SUSPENDIDO,
    /** Desactivado (borrado lógico). */
    INACTIVO
}
