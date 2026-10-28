package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de una publicación nueva (RF-01, UC-01).
 *
 * @param idVariedad     id de la variedad (de ella sale el cultivo)
 * @param idUnidad       id de la unidad elegida
 * @param cantidad       cantidad en esa unidad
 * @param pesoUnidadKg   kilos por unidad; obligatorio en jaba y saco, se ignora en las de peso fijo
 * @param precioUnitario soles por unidad
 * @param idDistrito     id del distrito donde está la cosecha
 * @param fechaCosecha   fecha de cosecha
 * @param fechaLimite    fecha límite de venta, o {@code null} para calcularla con la vida útil del cultivo
 */
public record NuevaPublicacionDto(Integer idVariedad, Integer idUnidad, BigDecimal cantidad, BigDecimal pesoUnidadKg,
        BigDecimal precioUnitario, Integer idDistrito, LocalDate fechaCosecha, LocalDate fechaLimite) {
}
