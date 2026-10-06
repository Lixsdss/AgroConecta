package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resultado de la búsqueda (RF-03, UC-03 paso 6).
 *
 * @param id                     id de la publicación
 * @param idAgricultor           id del agricultor
 * @param agricultor             nombre completo del agricultor
 * @param calificacionAgricultor promedio de calificaciones (1 a 5), o {@code null} si no tiene
 * @param idCultivo              id del cultivo
 * @param cultivo                nombre del cultivo
 * @param idVariedad             id de la variedad
 * @param variedad               nombre de la variedad
 * @param idUnidad               id de la unidad
 * @param unidad                 nombre de la unidad en singular
 * @param unidadPlural           nombre de la unidad en plural
 * @param cantidad               cantidad publicada
 * @param pesoUnidadKg           kilos por unidad
 * @param cantidadKg             kilos publicados
 * @param disponibleKg           kilos aún no comprometidos en ventas
 * @param unidadesDisponibles    unidades disponibles, redondeadas hacia abajo a 2 decimales
 * @param precioUnitario         soles por unidad
 * @param precioKg               soles por kilo
 * @param idDistrito             id del distrito
 * @param distrito               nombre del distrito
 * @param idProvincia            id de la provincia
 * @param provincia              nombre de la provincia
 * @param fechaCosecha           fecha de cosecha
 * @param fechaLimite            fecha límite de venta
 * @param diasRestantes          días que faltan para la fecha límite
 * @param porVencer              {@code true} si faltan 3 días o menos
 * @param fechaPublicacion       fecha y hora de publicación
 */
public record PublicacionDisponibleDto(Long id, Long idAgricultor, String agricultor, BigDecimal calificacionAgricultor,
        Integer idCultivo, String cultivo, Integer idVariedad, String variedad, Integer idUnidad, String unidad,
        String unidadPlural, BigDecimal cantidad, BigDecimal pesoUnidadKg, BigDecimal cantidadKg,
        BigDecimal disponibleKg, BigDecimal unidadesDisponibles, BigDecimal precioUnitario, BigDecimal precioKg,
        Integer idDistrito, String distrito, Integer idProvincia, String provincia, LocalDate fechaCosecha,
        LocalDate fechaLimite, Integer diasRestantes, boolean porVencer, LocalDateTime fechaPublicacion) {
}
