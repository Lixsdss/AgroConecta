package pe.agroconecta.venta.aplicacion;

/**
 * Venta en curso de un usuario (aviso al suspenderlo, UC-11 A1).
 *
 * @param idVenta       venta
 * @param estado        estado ({@code PROPUESTA}, {@code ACORDADA}, {@code RECOGIDO}, ...)
 * @param idContraparte la otra parte de la venta
 * @param producto      "Mango Kent"
 */
public record VentaEnCursoDto(Long idVenta, String estado, Long idContraparte, String producto) {
}
