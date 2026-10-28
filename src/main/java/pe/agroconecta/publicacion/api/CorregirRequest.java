package pe.agroconecta.publicacion.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corrección de una cosecha oculta por la moderación (RF-09).
 *
 * @param cantidad       cantidad en la unidad publicada
 * @param pesoUnidadKg   kilos por unidad (solo jaba o saco)
 * @param precioUnitario precio por unidad
 */
public record CorregirRequest(
        @NotNull(message = "Escriba la cantidad.")
        @Positive(message = "La cantidad debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "La cantidad puede tener hasta 2 decimales.")
        BigDecimal cantidad,
        @Positive(message = "El peso debe ser mayor que cero.")
        @Digits(integer = 7, fraction = 3, message = "El peso puede tener hasta 3 decimales.")
        BigDecimal pesoUnidadKg,
        @NotNull(message = "Escriba el precio.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El precio puede tener hasta 2 decimales.")
        BigDecimal precioUnitario) {
}
