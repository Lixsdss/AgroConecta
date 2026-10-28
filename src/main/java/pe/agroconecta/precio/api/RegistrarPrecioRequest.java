package pe.agroconecta.precio.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import pe.agroconecta.precio.dominio.FuentePrecio;

/**
 * Cuerpo de {@code POST /api/v1/precios} ("Cargar precio del día").
 *
 * @param idCultivo  id del cultivo
 * @param idDistrito id de la zona de precio
 * @param fecha      día (hoy si falta)
 * @param precioKg   soles por kilo
 * @param fuente     fuente del precio
 */
public record RegistrarPrecioRequest(
        @NotNull(message = "Elija el cultivo.")
        Integer idCultivo,

        @NotNull(message = "Elija la zona.")
        Integer idDistrito,

        @PastOrPresent(message = "La fecha no puede ser futura.")
        LocalDate fecha,

        @NotNull(message = "Escriba el precio por kilo.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "Use como máximo 2 decimales en el precio.")
        BigDecimal precioKg,

        @NotNull(message = "Elija la fuente.")
        FuentePrecio fuente) {
}
