package pe.agroconecta.ventaagrupada.dominio;

/**
 * Estados de una propuesta conjunta ({@code chk_propuesta_estado}).
 */
public enum EstadoPropuesta {
    /** Esperando respuestas de los agricultores. */
    ABIERTA,
    /** El comprador la cerró o pasó la fecha de recojo ({@code ev_tareas_diarias}). */
    CERRADA,
    /** El comprador la canceló. */
    CANCELADA
}
