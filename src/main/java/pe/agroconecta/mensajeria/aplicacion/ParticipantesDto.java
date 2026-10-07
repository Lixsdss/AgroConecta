package pe.agroconecta.mensajeria.aplicacion;

/**
 * Partes de una conversación.
 *
 * @param idConversacion conversación
 * @param idPublicacion  publicación
 * @param idComprador    comprador
 * @param idAgricultor   agricultor de la publicación
 */
public record ParticipantesDto(Long idConversacion, Long idPublicacion, Long idComprador, Long idAgricultor) {

    /**
     * Indica si el usuario es una de las partes.
     *
     * @param idUsuario usuario
     * @return {@code true} si es el comprador o el agricultor
     */
    public boolean participa(long idUsuario) {
        return idUsuario == idComprador || idUsuario == idAgricultor;
    }
}
