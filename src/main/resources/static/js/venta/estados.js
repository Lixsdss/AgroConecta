/**
 * Textos de los estados de una venta (RF-06, RF-11), iguales a los del diseño ("entrega").
 */

/** Estado → [texto, ícono]. El color nunca va solo: siempre con texto. */
export const ESTADOS = {
    PROPUESTA: ['Propuesta conjunta', 'layers'],
    ACORDADA: ['Venta acordada', 'handshake'],
    RECOGIDO: ['Ya recogieron', 'package-check'],
    EN_CAMINO: ['En camino', 'truck'],
    ENTREGADO: ['Ya entregado', 'circle-check'],
    COMPLETADA: ['Venta terminada', 'circle-check'],
    RECHAZADA: ['Rechazada', 'circle-x'],
    CANCELADA: ['Venta cancelada', 'circle-x']
};

/** Acción → [texto del botón, ícono, clase del botón]. */
export const ACCIONES = {
    CONFIRMAR_RECEPCION: ['Confirmar que recibí', 'circle-check', 'btn-primary'],
    RECOGIDO: ['Ya recogieron la carga', 'package-check', 'btn-outline-primary'],
    EN_CAMINO: ['La carga va en camino', 'truck', 'btn-outline-primary'],
    CANCELAR: ['Cancelar venta', 'x', 'btn-outline-danger'],
    ACEPTAR: ['Aceptar mi parte', 'check', 'btn-primary'],
    RECHAZAR: ['Rechazar', 'x', 'btn-outline-danger']
};

/** @returns {string} texto del estado */
export function textoEstado(estado) {
    return ESTADOS[estado]?.[0] ?? estado;
}
