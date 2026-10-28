package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Publicación vista por la moderación: incluye su estado real y su marca de revisión. Lo usa el módulo
 * {@code administracion}, por eso el estado y la marca van como texto y no como enums del dominio.
 *
 * @param id               id de la publicación
 * @param idAgricultor     id del agricultor
 * @param cultivo          nombre del cultivo
 * @param variedad         nombre de la variedad
 * @param unidad           unidad en singular
 * @param unidadPlural     unidad en plural
 * @param cantidad         cantidad en la unidad elegida
 * @param pesoUnidadKg     kilos por unidad
 * @param cantidadKg       kilos totales
 * @param precioUnitario   soles por unidad
 * @param precioKg         soles por kilo
 * @param distrito         distrito de la cosecha
 * @param provincia        provincia de la cosecha
 * @param fechaCosecha     fecha de cosecha
 * @param fechaLimite      fecha límite
 * @param estado           estado actual (valor de {@code chk_publicacion_estado})
 * @param marcaRevision    marca de la cola de moderación (valor de {@code chk_publicacion_marca}), o {@code null}
 * @param fechaPublicacion fecha y hora de publicación
 */
public record PublicacionModeracionDto(Long id, Long idAgricultor, String cultivo, String variedad, String unidad,
        String unidadPlural, BigDecimal cantidad, BigDecimal pesoUnidadKg, BigDecimal cantidadKg,
        BigDecimal precioUnitario, BigDecimal precioKg, String distrito, String provincia, LocalDate fechaCosecha,
        LocalDate fechaLimite, String estado, String marcaRevision, LocalDateTime fechaPublicacion) {
}
