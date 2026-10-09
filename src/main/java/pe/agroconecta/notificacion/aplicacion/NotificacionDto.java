package pe.agroconecta.notificacion.aplicacion;

import java.time.LocalDateTime;

/**
 * Aviso del centro de avisos.
 *
 * @param id            id del aviso
 * @param tipo          tipo ({@code POR_VENCER}, {@code OFERTA}, {@code MENSAJE}, {@code MODERACION}, ...)
 * @param titulo        título
 * @param contenido     texto
 * @param idReferencia  publicación, conversación o venta a la que lleva, o {@code null}
 * @param leida         {@code true} si ya se leyó
 * @param fechaCreacion fecha y hora
 */
public record NotificacionDto(Long id, String tipo, String titulo, String contenido, Long idReferencia,
        boolean leida, LocalDateTime fechaCreacion) {
}
