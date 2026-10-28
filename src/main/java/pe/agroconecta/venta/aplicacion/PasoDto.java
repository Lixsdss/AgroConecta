package pe.agroconecta.venta.aplicacion;

import java.time.LocalDateTime;

/**
 * Paso de la línea de tiempo de una venta ({@code transacciones_historial}).
 *
 * @param estado estado al que pasó
 * @param fecha  fecha y hora
 * @param autor  nombre de quien hizo el cambio, o {@code null} si fue un proceso automático
 * @param esUsted {@code true} si lo hizo el usuario de la sesión
 */
public record PasoDto(String estado, LocalDateTime fecha, String autor, boolean esUsted) {
}
