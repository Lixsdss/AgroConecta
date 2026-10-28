package pe.agroconecta.publicacion.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import pe.agroconecta.publicacion.aplicacion.NuevaPublicacionDto;

/**
 * Cuerpo de {@code POST /api/v1/publicaciones}. Las reglas que dependen del catálogo (peso de jaba y saco, vida
 * útil del cultivo) las valida el servicio.
 *
 * @param idVariedad     id de la variedad
 * @param idUnidad       id de la unidad
 * @param cantidad       cantidad en la unidad elegida
 * @param pesoUnidadKg   kilos por unidad (jaba y saco)
 * @param precioUnitario soles por unidad
 * @param idDistrito     id del distrito donde está la cosecha
 * @param fechaCosecha   fecha de cosecha (no futura)
 * @param fechaLimite    fecha límite de venta (opcional)
 */
public record PublicarRequest(
        @NotNull(message = "Elija la variedad.")
        Integer idVariedad,

        @NotNull(message = "Elija cómo lo vende.")
        Integer idUnidad,

        @NotNull(message = "Escriba cuánto tiene.")
        @Positive(message = "La cantidad debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "Use como máximo 2 decimales en la cantidad.")
        BigDecimal cantidad,

        @Positive(message = "Los kilos por unidad deben ser mayores que cero.")
        @Digits(integer = 7, fraction = 3, message = "Use como máximo 3 decimales en los kilos por unidad.")
        BigDecimal pesoUnidadKg,

        @NotNull(message = "Escriba el precio.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "Use como máximo 2 decimales en el precio.")
        BigDecimal precioUnitario,

        @NotNull(message = "Elija dónde está su cosecha.")
        Integer idDistrito,

        @NotNull(message = "Indique cuándo cosechó.")
        @PastOrPresent(message = "La fecha de cosecha no puede ser futura.")
        LocalDate fechaCosecha,

        LocalDate fechaLimite) {

    /**
     * Convierte la petición en los datos del servicio.
     *
     * @return datos de la publicación nueva
     */
    public NuevaPublicacionDto aDto() {
        return new NuevaPublicacionDto(idVariedad, idUnidad, cantidad, pesoUnidadKg, precioUnitario, idDistrito,
                fechaCosecha, fechaLimite);
    }
}
