package pe.agroconecta.publicacion.api;

import jakarta.validation.constraints.NotNull;

/**
 * "Extender" la fecha límite (RF-07): 7, 14 o 30 días.
 *
 * @param dias días que se agregan
 */
public record ExtenderRequest(@NotNull(message = "Elija cuántos días extender.") Integer dias) {
}
