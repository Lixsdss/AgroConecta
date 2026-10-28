package pe.agroconecta.mensajeria.api;

import jakarta.validation.constraints.NotNull;

/**
 * Publicación con la que el comprador quiere conversar ("Escribir" o "WhatsApp").
 *
 * @param idPublicacion id de la publicación
 */
public record AbrirConversacionRequest(@NotNull(message = "Indique la publicación.") Long idPublicacion) {
}
