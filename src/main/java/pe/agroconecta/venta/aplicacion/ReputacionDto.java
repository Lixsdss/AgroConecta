package pe.agroconecta.venta.aplicacion;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;

/**
 * Reputación de un usuario (SRS §7.2): promedio, total y calificaciones recibidas.
 *
 * @param idUsuario      usuario
 * @param promedio       promedio de estrellas con un decimal, o {@code null} si no tiene calificaciones
 * @param total          cantidad de calificaciones recibidas
 * @param calificaciones página de calificaciones recibidas, la más reciente primero
 */
public record ReputacionDto(Long idUsuario, BigDecimal promedio, long total, Page<CalificacionDto> calificaciones) {
}
