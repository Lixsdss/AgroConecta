package pe.agroconecta.venta.aplicacion;

import java.time.LocalDateTime;

/**
 * Una calificación, para mostrar.
 *
 * @param puntuacion estrellas (1 a 5)
 * @param comentario comentario, o {@code null}
 * @param fecha      fecha y hora
 * @param autor      negocio del comprador o nombre del agricultor que calificó
 */
public record CalificacionDto(Integer puntuacion, String comentario, LocalDateTime fecha, String autor) {
}
