package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cosecha del agricultor en "Mis ventas" (RF-07), con las acciones que puede hacer en su estado.
 *
 * @param id                  id de la publicación
 * @param cultivo             cultivo
 * @param variedad            variedad
 * @param distrito            distrito
 * @param unidad              unidad en singular
 * @param unidadPlural        unidad en plural
 * @param cantidad            cantidad publicada
 * @param pesoUnidadKg        kilos por unidad
 * @param cantidadKg          kilos publicados
 * @param disponibleKg        kilos aún no comprometidos en ventas
 * @param unidadesDisponibles unidades disponibles, redondeadas hacia abajo
 * @param precioUnitario      soles por unidad
 * @param precioKg            soles por kilo
 * @param fechaCosecha        fecha de cosecha
 * @param fechaLimite         fecha límite de venta
 * @param diasRestantes       días que faltan para la fecha límite
 * @param porVencer           {@code true} si está PUBLICADA y faltan 3 días o menos
 * @param estado              estado de la publicación
 * @param fechaPublicacion    fecha y hora de publicación
 * @param acciones            acciones permitidas en su estado
 */
public record MiCosechaDto(Long id, String cultivo, String variedad, String distrito, String unidad,
        String unidadPlural, BigDecimal cantidad, BigDecimal pesoUnidadKg, BigDecimal cantidadKg,
        BigDecimal disponibleKg, BigDecimal unidadesDisponibles, BigDecimal precioUnitario, BigDecimal precioKg,
        LocalDate fechaCosecha, LocalDate fechaLimite, Integer diasRestantes, boolean porVencer, String estado,
        LocalDateTime fechaPublicacion, List<AccionCosecha> acciones) {
}
