package pe.agroconecta.precio.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Punto de la serie de precios de un cultivo en una zona (gráfico de 30 días).
 *
 * @param fecha    día
 * @param precioKg soles por kilo
 */
public record PuntoHistorialDto(LocalDate fecha, BigDecimal precioKg) {
}
