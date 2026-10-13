package pe.agroconecta.ventaagrupada.aplicacion;

import java.math.BigDecimal;

/**
 * Cosecha de un lote ("Ver agricultores", RF-10).
 *
 * @param idPublicacion       publicación
 * @param agricultor          nombre del agricultor
 * @param unidad              unidad en singular
 * @param unidadPlural        unidad en plural
 * @param unidadesDisponibles unidades disponibles
 * @param disponibleKg        kilos disponibles
 * @param precioUnitario      soles por unidad
 * @param precioKg            soles por kilo
 */
public record CosechaLoteDto(Long idPublicacion, String agricultor, String unidad, String unidadPlural,
        BigDecimal unidadesDisponibles, BigDecimal disponibleKg, BigDecimal precioUnitario, BigDecimal precioKg) {
}
