package pe.agroconecta.venta.aplicacion;

/**
 * Cambios de estado que una parte puede pedir sobre una venta (UC-05 A1, UC-06).
 */
public enum AccionVenta {
    /** "Ya recogieron": ACORDADA → RECOGIDO (cualquiera de las partes). */
    RECOGIDO,
    /** "Va en camino": RECOGIDO → EN_CAMINO (cualquiera de las partes). */
    EN_CAMINO,
    /** "Confirmar que recibí": → ENTREGADO → COMPLETADA en una sola transacción (solo el comprador). */
    CONFIRMAR_RECEPCION,
    /** "Cancelar venta": ACORDADA → CANCELADA (cualquiera de las partes); PROPUESTA → CANCELADA (solo el comprador). */
    CANCELAR,
    /** "Aceptar mi parte" de una propuesta conjunta: PROPUESTA → ACORDADA (solo el agricultor, RF-10). */
    ACEPTAR,
    /** "Rechazar" una parte de una propuesta conjunta: PROPUESTA → RECHAZADA (solo el agricultor, RF-10). */
    RECHAZAR
}
