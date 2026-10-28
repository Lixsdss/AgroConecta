package pe.agroconecta.mensajeria.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje del chat.
 *
 * @param id                   id del mensaje
 * @param idRemitente          id de quien lo envió
 * @param mio                  {@code true} si lo envió el usuario de la sesión
 * @param tipo                 {@code TEXTO} u {@code OFERTA}
 * @param contenido            texto (solo TEXTO)
 * @param ofertaCantidad       cantidad ofertada (solo OFERTA)
 * @param ofertaPrecioUnitario precio por unidad ofertado (solo OFERTA)
 * @param ofertaKg             kilos de la oferta: cantidad × peso por unidad (solo OFERTA)
 * @param ofertaTotal          total de la oferta: cantidad × precio por unidad (solo OFERTA)
 * @param fechaEnvio           fecha y hora de envío
 */
public record MensajeDto(Long id, Long idRemitente, boolean mio, String tipo, String contenido,
        BigDecimal ofertaCantidad, BigDecimal ofertaPrecioUnitario, BigDecimal ofertaKg, BigDecimal ofertaTotal,
        LocalDateTime fechaEnvio) {
}
