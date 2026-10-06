package pe.agroconecta.mensajeria.aplicacion;

import java.time.LocalDateTime;

/**
 * Conversación vista por uno de sus participantes.
 *
 * @param id            id de la conversación
 * @param idPublicacion id de la publicación
 * @param canalInicio   canal por el que empezó
 * @param fechaInicio   fecha y hora de inicio
 * @param soyAgricultor {@code true} si el usuario de la sesión es el agricultor de la publicación
 * @param otraParte     la otra persona
 * @param publicacion   publicación sobre la que se conversa
 * @param ultimoMensaje último mensaje, o {@code null} si no hay
 * @param noLeidos      mensajes de la otra parte sin leer
 */
public record ConversacionDto(Long id, Long idPublicacion, String canalInicio, LocalDateTime fechaInicio,
        boolean soyAgricultor, OtraParteDto otraParte, ProductoChatDto publicacion, UltimoMensajeDto ultimoMensaje,
        long noLeidos) {
}
