package pe.agroconecta.venta.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import pe.agroconecta.venta.aplicacion.NuevaVentaDto;

/**
 * "Registrar acuerdo" (UC-05 paso 2).
 *
 * @param idConversacion conversación de la que nace el acuerdo
 * @param idUnidad       unidad acordada
 * @param cantidad       cantidad en esa unidad
 * @param pesoUnidadKg   kilos por unidad (solo jaba o saco distinta de la publicada)
 * @param precioUnitario soles por unidad
 * @param fechaRecojo    día de recojo (hoy o después)
 */
public record RegistrarVentaRequest(
        @NotNull(message = "Indique la conversación.") Long idConversacion,
        @NotNull(message = "Elija la unidad.") Integer idUnidad,
        @NotNull(message = "Indique la cantidad.")
        @Positive(message = "La cantidad debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "La cantidad puede tener hasta 2 decimales.")
        BigDecimal cantidad,
        @Positive(message = "El peso debe ser mayor que cero.")
        @Digits(integer = 7, fraction = 3, message = "El peso puede tener hasta 3 decimales.")
        BigDecimal pesoUnidadKg,
        @NotNull(message = "Indique el precio por unidad.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El precio puede tener hasta 2 decimales.")
        BigDecimal precioUnitario,
        @NotNull(message = "Indique el día de recojo.")
        @FutureOrPresent(message = "El día de recojo no puede ser anterior a hoy.") LocalDate fechaRecojo) {

    /** @return los datos para el servicio */
    public NuevaVentaDto aDto() {
        return new NuevaVentaDto(idConversacion, idUnidad, cantidad, pesoUnidadKg, precioUnitario, fechaRecojo);
    }
}
