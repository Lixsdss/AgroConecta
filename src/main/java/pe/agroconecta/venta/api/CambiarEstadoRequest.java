package pe.agroconecta.venta.api;

import jakarta.validation.constraints.NotNull;

import pe.agroconecta.venta.aplicacion.AccionVenta;

/**
 * Cambio de estado que pide una parte.
 *
 * @param accion {@code RECOGIDO}, {@code EN_CAMINO}, {@code CONFIRMAR_RECEPCION} o {@code CANCELAR}
 */
public record CambiarEstadoRequest(@NotNull(message = "Indique la acción.") AccionVenta accion) {
}
