package pe.agroconecta.usuario.dominio;

/**
 * Rol del usuario ({@code chk_usuario_rol}). No cambia después del registro.
 */
public enum Rol {
    /** Pequeño productor que publica cosechas. */
    AGRICULTOR,
    /** Empresa compradora. */
    COMPRADOR,
    /** Personal de la plataforma; no se registra por sí mismo. */
    ADMINISTRADOR
}
