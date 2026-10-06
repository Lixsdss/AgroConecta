package pe.agroconecta.notificacion.dominio;

/**
 * Tipos de aviso ({@code chk_notificacion_tipo}). POR_VENCER la crea {@code ev_tareas_diarias} y MODERACION
 * {@code trg_revisiones_ai}; los demás, el módulo que ejecuta la acción.
 */
public enum TipoNotificacion {
    /** Publicación por vencer (RF-07). */
    POR_VENCER,
    /** Oferta en el chat (RF-04). */
    OFERTA,
    /** Mensaje en el chat (RF-04). */
    MENSAJE,
    /** Decisión de moderación (RF-09). */
    MODERACION,
    /** Propuesta conjunta (RF-10). */
    PROPUESTA_CONJUNTA,
    /** Cambio en una venta (RF-06, RF-11). */
    VENTA,
    /** Calificación recibida (RF-05). */
    CALIFICACION,
    /** Reclamo (RF-09). */
    RECLAMO,
    /** Aviso sobre la cuenta (RF-09). */
    CUENTA
}
