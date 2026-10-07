package pe.agroconecta.venta.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Calificación de la otra parte (UC-07 paso 2).
 *
 * @param puntuacion estrellas, de 1 a 5
 * @param comentario comentario opcional, hasta 500 caracteres
 */
public record CalificarRequest(
        @NotNull(message = "Elija de 1 a 5 estrellas.")
        @Min(value = 1, message = "Elija de 1 a 5 estrellas.")
        @Max(value = 5, message = "Elija de 1 a 5 estrellas.") Integer puntuacion,
        @Size(max = 500, message = "El comentario puede tener hasta 500 caracteres.") String comentario) {
}
