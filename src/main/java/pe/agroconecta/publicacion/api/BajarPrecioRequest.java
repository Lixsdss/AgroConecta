package pe.agroconecta.publicacion.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * "Bajar el precio" (RF-07).
 *
 * @param precioUnitario nuevo precio por unidad
 */
public record BajarPrecioRequest(
        @NotNull(message = "Escriba el nuevo precio.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El precio puede tener hasta 2 decimales.")
        BigDecimal precioUnitario) {
}
