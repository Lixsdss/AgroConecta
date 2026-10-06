package pe.agroconecta.mensajeria.aplicacion;

import java.math.BigDecimal;

/**
 * Mensaje que envía un participante: texto u oferta.
 *
 * @param tipo                 {@code TEXTO} u {@code OFERTA}
 * @param contenido            texto (solo TEXTO)
 * @param ofertaCantidad       cantidad en la unidad de la publicación (solo OFERTA)
 * @param ofertaPrecioUnitario soles por unidad (solo OFERTA)
 */
public record NuevoMensajeDto(String tipo, String contenido, BigDecimal ofertaCantidad,
        BigDecimal ofertaPrecioUnitario) {
}
