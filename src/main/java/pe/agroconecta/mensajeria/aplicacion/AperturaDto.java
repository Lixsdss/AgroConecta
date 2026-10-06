package pe.agroconecta.mensajeria.aplicacion;

/**
 * Resultado de abrir una conversación.
 *
 * @param conversacion la conversación
 * @param creada       {@code true} si se creó ahora; {@code false} si ya existía
 */
public record AperturaDto(ConversacionDto conversacion, boolean creada) {
}
