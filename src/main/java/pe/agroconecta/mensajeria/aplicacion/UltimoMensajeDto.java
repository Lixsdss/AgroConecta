package pe.agroconecta.mensajeria.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Último mensaje de una conversación (lista "Mensajes").
 *
 * @param tipo                 {@code TEXTO} u {@code OFERTA}
 * @param contenido            texto (solo TEXTO)
 * @param ofertaCantidad       cantidad ofertada (solo OFERTA)
 * @param ofertaPrecioUnitario precio por unidad ofertado (solo OFERTA)
 * @param mio                  {@code true} si lo envió el usuario de la sesión
 * @param fechaEnvio           fecha y hora de envío
 */
public record UltimoMensajeDto(String tipo, String contenido, BigDecimal ofertaCantidad,
        BigDecimal ofertaPrecioUnitario, boolean mio, LocalDateTime fechaEnvio) {
}
