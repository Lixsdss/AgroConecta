package pe.agroconecta.ventaagrupada.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import pe.agroconecta.ventaagrupada.aplicacion.NuevaPropuestaDto;

/**
 * "Enviar propuesta conjunta" (UC-12 paso 2).
 *
 * @param idVariedad  variedad del lote
 * @param idDistrito  distrito del lote
 * @param cantidadKg  kilos pedidos
 * @param precioKg    soles por kilo
 * @param fechaRecojo día de recojo (hoy o después)
 */
public record CrearPropuestaRequest(
        @NotNull(message = "Indique el lote.") Integer idVariedad,
        @NotNull(message = "Indique el lote.") Integer idDistrito,
        @NotNull(message = "Escriba la cantidad en kilos.")
        @Positive(message = "La cantidad debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "La cantidad puede tener hasta 2 decimales.")
        BigDecimal cantidadKg,
        @NotNull(message = "Escriba el precio por kilo.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El precio puede tener hasta 2 decimales.")
        BigDecimal precioKg,
        @NotNull(message = "Indique el día de recojo.")
        @FutureOrPresent(message = "El día de recojo no puede ser anterior a hoy.") LocalDate fechaRecojo) {

    /**
     * Devuelve los datos para el servicio.
     *
     * @return los datos para el servicio
     */
    public NuevaPropuestaDto aDto() {
        return new NuevaPropuestaDto(idVariedad, idDistrito, cantidadKg, precioKg, fechaRecojo);
    }
}
