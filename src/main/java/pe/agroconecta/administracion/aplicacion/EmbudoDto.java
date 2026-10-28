package pe.agroconecta.administracion.aplicacion;

/**
 * Embudo de una publicación ({@code v_conversion_publicacion}).
 *
 * @param idPublicacion publicación
 * @param producto      "Mango Kent"
 * @param agricultor    nombre del agricultor
 * @param clicsWhatsapp clics en WhatsApp
 * @param contactos     conversaciones
 * @param ofertas       ofertas
 * @param acuerdos      ventas acordadas
 * @param completadas   ventas completadas
 */
public record EmbudoDto(Long idPublicacion, String producto, String agricultor, long clicsWhatsapp, long contactos,
        long ofertas, long acuerdos, long completadas) {
}
