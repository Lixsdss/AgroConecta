package pe.agroconecta.venta.aplicacion;

import java.math.BigDecimal;

/**
 * Parte de una propuesta conjunta (RF-10): la venta de un agricultor del lote.
 *
 * @param idVenta        id de la venta
 * @param agricultor     nombre del agricultor
 * @param cantidad       unidades de la parte
 * @param unidad         unidad en singular
 * @param unidadPlural   unidad en plural
 * @param cantidadKg     kilos de la parte
 * @param precioUnitario soles por unidad
 * @param montoTotal     total en soles
 * @param estado         estado de la venta ({@code PROPUESTA}, {@code ACORDADA}, {@code RECHAZADA}, ...)
 */
public record ParteDto(Long idVenta, String agricultor, BigDecimal cantidad, String unidad, String unidadPlural,
        BigDecimal cantidadKg, BigDecimal precioUnitario, BigDecimal montoTotal, String estado) {
}
