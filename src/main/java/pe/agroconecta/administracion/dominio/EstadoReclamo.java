package pe.agroconecta.administracion.dominio;

/**
 * Estados de un reclamo ({@code chk_reclamo_estado}).
 */
public enum EstadoReclamo {
    /** Esperando la resolución del administrador. */
    PENDIENTE,
    /** El administrador le dio la razón al reclamante. */
    FUNDADO,
    /** El administrador no le dio la razón. */
    INFUNDADO
}
