package pe.agroconecta.mensajeria.aplicacion;

/**
 * Enlace para abrir WhatsApp con el agricultor (UC-04 A1).
 *
 * @param idConversacion conversación creada o existente
 * @param enlace         enlace {@code https://wa.me/51…?text=…}
 */
public record EnlaceWhatsappDto(Long idConversacion, String enlace) {
}
