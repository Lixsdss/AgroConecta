package pe.agroconecta.venta.infraestructura;

import java.time.LocalDateTime;

/**
 * Calificación recibida por un usuario, con el nombre de quien la dio.
 *
 * @param puntuacion estrellas (1 a 5)
 * @param comentario comentario, o {@code null}
 * @param fecha      fecha y hora
 * @param autor      negocio del comprador o nombre del agricultor que calificó
 */
public record CalificacionRecibida(Integer puntuacion, String comentario, LocalDateTime fecha, String autor) {
}
