package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.agroconecta.publicacion.dominio.EstadoPublicacion;

/**
 * Publicación con los nombres de su cultivo, variedad, unidad y distrito.
 *
 * @param id               id de la publicación
 * @param idAgricultor     id del agricultor
 * @param idCultivo        id del cultivo
 * @param cultivo          nombre del cultivo
 * @param idVariedad       id de la variedad
 * @param variedad         nombre de la variedad
 * @param idUnidad         id de la unidad
 * @param unidad           nombre de la unidad en singular
 * @param unidadPlural     nombre de la unidad en plural
 * @param cantidad         cantidad en la unidad elegida
 * @param pesoUnidadKg     kilos por unidad
 * @param cantidadKg       kilos totales (calculado por la base de datos)
 * @param precioUnitario   soles por unidad
 * @param precioKg         soles por kilo (calculado por la base de datos)
 * @param idDistrito       id del distrito
 * @param distrito         nombre del distrito
 * @param provincia        nombre de la provincia
 * @param fechaCosecha     fecha de cosecha
 * @param fechaLimite      fecha límite de venta
 * @param estado           estado
 * @param fechaPublicacion fecha y hora de publicación
 * @param agricultor             nombre completo del agricultor (RF-03)
 * @param calificacionAgricultor promedio de calificaciones (1 a 5), o {@code null} si no tiene (RF-03)
 * @param disponibleKg           kilos aún no comprometidos en ventas (RF-03)
 * @param unidadesDisponibles    unidades disponibles, redondeadas hacia abajo a 2 decimales (RF-03)
 * @param diasRestantes          días que faltan para la fecha límite (RF-03)
 * @param porVencer              {@code true} si está PUBLICADA y faltan 3 días o menos (RF-03)
 */
public record PublicacionDto(Long id, Long idAgricultor, Integer idCultivo, String cultivo, Integer idVariedad,
        String variedad, Integer idUnidad, String unidad, String unidadPlural, BigDecimal cantidad,
        BigDecimal pesoUnidadKg, BigDecimal cantidadKg, BigDecimal precioUnitario, BigDecimal precioKg,
        Integer idDistrito, String distrito, String provincia, LocalDate fechaCosecha, LocalDate fechaLimite,
        EstadoPublicacion estado, LocalDateTime fechaPublicacion, String agricultor,
        BigDecimal calificacionAgricultor, BigDecimal disponibleKg, BigDecimal unidadesDisponibles,
        Integer diasRestantes, boolean porVencer) {
}
