package pe.agroconecta.mensajeria.infraestructura;

/**
 * Cantidad de mensajes de una conversación (por ejemplo, los no leídos).
 *
 * @param idConversacion conversación
 * @param total          cantidad
 */
public record ConteoPorConversacion(Long idConversacion, Long total) {
}
