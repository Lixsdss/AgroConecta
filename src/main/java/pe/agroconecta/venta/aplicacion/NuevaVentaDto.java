package pe.agroconecta.venta.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Acuerdo que registra una de las partes (UC-05 paso 2).
 *
 * @param idConversacion conversación de la que nace
 * @param idUnidad       unidad acordada
 * @param cantidad       cantidad en esa unidad
 * @param pesoUnidadKg   kilos por unidad (solo jaba o saco distinta de la publicada), o {@code null}
 * @param precioUnitario soles por unidad
 * @param fechaRecojo    día de recojo
 */
public record NuevaVentaDto(Long idConversacion, Integer idUnidad, BigDecimal cantidad, BigDecimal pesoUnidadKg,
        BigDecimal precioUnitario, LocalDate fechaRecojo) {
}
