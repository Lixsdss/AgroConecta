package pe.agroconecta.venta.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila de "Mis ventas" o "Mis compras" (UC-08 paso 5).
 *
 * @param id                 id de la venta
 * @param soyComprador       {@code true} si el usuario de la sesión es el comprador
 * @param contraparte        nombre de la otra parte (el negocio, si es el comprador)
 * @param cultivo            cultivo
 * @param variedad           variedad
 * @param cantidad           cantidad acordada
 * @param unidad             unidad en singular
 * @param unidadPlural       unidad en plural
 * @param cantidadKg         kilos acordados
 * @param precioUnitario     soles por unidad
 * @param precioKg           soles por kilo
 * @param montoTotal         total en soles
 * @param fechaRecojo        día de recojo
 * @param estado             estado
 * @param fechaCreacion      fecha y hora del acuerdo
 * @param fechaActualizacion fecha y hora del último cambio
 */
public record VentaResumenDto(Long id, boolean soyComprador, String contraparte, String cultivo, String variedad,
        BigDecimal cantidad, String unidad, String unidadPlural, BigDecimal cantidadKg, BigDecimal precioUnitario,
        BigDecimal precioKg, BigDecimal montoTotal, LocalDate fechaRecojo, String estado,
        LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
}
