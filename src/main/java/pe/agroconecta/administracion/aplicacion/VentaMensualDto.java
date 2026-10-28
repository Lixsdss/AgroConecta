package pe.agroconecta.administracion.aplicacion;

import java.math.BigDecimal;

/**
 * Ventas completadas de un cultivo en un mes ({@code v_ventas_mensuales_cultivo}).
 *
 * @param mes               mes {@code yyyy-MM}
 * @param cultivo           cultivo
 * @param ventasCompletadas ventas completadas
 * @param kilosVendidos     kilos vendidos
 * @param montoTotal        monto total en soles
 */
public record VentaMensualDto(String mes, String cultivo, long ventasCompletadas, BigDecimal kilosVendidos,
        BigDecimal montoTotal) {
}
