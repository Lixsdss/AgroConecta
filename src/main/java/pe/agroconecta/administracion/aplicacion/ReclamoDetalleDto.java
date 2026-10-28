package pe.agroconecta.administracion.aplicacion;

import pe.agroconecta.venta.aplicacion.VentaParaAdministracionDto;

/**
 * Reclamo con su venta y la línea de tiempo (UC-11 parte 3, paso 2).
 *
 * @param reclamo el reclamo
 * @param venta   la venta reclamada
 */
public record ReclamoDetalleDto(ReclamoDto reclamo, VentaParaAdministracionDto venta) {
}
