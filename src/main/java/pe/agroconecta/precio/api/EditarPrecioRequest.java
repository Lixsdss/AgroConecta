package pe.agroconecta.precio.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import pe.agroconecta.precio.dominio.FuentePrecio;

/**
 * Cuerpo de {@code PUT /api/v1/precios/{id}} ("Editar" de la tabla de precios).
 *
 * @param precioKg soles por kilo
 * @param fuente   fuente del precio
 */
public record EditarPrecioRequest(
        @NotNull(message = "Escriba el precio por kilo.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "Use como máximo 2 decimales en el precio.")
        BigDecimal precioKg,

        @NotNull(message = "Elija la fuente.")
        FuentePrecio fuente) {
}
