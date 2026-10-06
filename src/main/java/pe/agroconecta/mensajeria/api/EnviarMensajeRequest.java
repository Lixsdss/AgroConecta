package pe.agroconecta.mensajeria.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import pe.agroconecta.mensajeria.aplicacion.NuevoMensajeDto;

/**
 * Mensaje de texto u oferta que envía un participante. {@link MensajeCoherente} exige el texto en TEXTO y la
 * cantidad y el precio en OFERTA.
 *
 * @param tipo                 {@code TEXTO} u {@code OFERTA}
 * @param contenido            texto, de 1 a 1000 caracteres (solo TEXTO)
 * @param ofertaCantidad       cantidad en la unidad de la publicación (solo OFERTA)
 * @param ofertaPrecioUnitario soles por unidad (solo OFERTA)
 */
@MensajeCoherente
public record EnviarMensajeRequest(
        @NotNull(message = "Indique si es un mensaje o una oferta.")
        @Pattern(regexp = "TEXTO|OFERTA", message = "El tipo debe ser TEXTO u OFERTA.") String tipo,
        @Size(max = 1000, message = "El mensaje puede tener hasta 1000 caracteres.") String contenido,
        @Positive(message = "La cantidad debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "La cantidad puede tener hasta 2 decimales.")
        BigDecimal ofertaCantidad,
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El precio puede tener hasta 2 decimales.")
        BigDecimal ofertaPrecioUnitario) {

    /** @return los datos para el servicio */
    public NuevoMensajeDto aDto() {
        return new NuevoMensajeDto(tipo, contenido, ofertaCantidad, ofertaPrecioUnitario);
    }
}
