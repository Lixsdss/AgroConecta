package pe.agroconecta.administracion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.agroconecta.publicacion.aplicacion.PublicacionModeracionDto;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;

/**
 * Publicación de la cola de moderación con los datos de su agricultor (pantalla 11c).
 *
 * @param id               id de la publicación
 * @param cultivo          cultivo
 * @param variedad         variedad
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
 * @param estado           estado actual
 * @param marcaRevision    marca de la cola, o {@code null}
 * @param fechaPublicacion fecha y hora de publicación
 * @param agricultor       datos del agricultor, o {@code null} si no se encontró
 */
public record PublicacionEnRevisionDto(Long id, String cultivo, String variedad, String unidad, String unidadPlural,
        BigDecimal cantidad, BigDecimal pesoUnidadKg, BigDecimal cantidadKg, BigDecimal precioUnitario,
        BigDecimal precioKg, String distrito, String provincia, LocalDate fechaCosecha, LocalDate fechaLimite,
        String estado, String marcaRevision, LocalDateTime fechaPublicacion, UsuarioResumenDto agricultor) {

    /**
     * Combina la publicación con su agricultor.
     *
     * @param p          publicación
     * @param agricultor agricultor, o {@code null}
     * @return publicación en revisión
     */
    public static PublicacionEnRevisionDto de(PublicacionModeracionDto p, UsuarioResumenDto agricultor) {
        return new PublicacionEnRevisionDto(p.id(), p.cultivo(), p.variedad(), p.unidad(), p.unidadPlural(),
                p.cantidad(), p.pesoUnidadKg(), p.cantidadKg(), p.precioUnitario(), p.precioKg(), p.distrito(),
                p.provincia(), p.fechaCosecha(), p.fechaLimite(), p.estado(), p.marcaRevision(), p.fechaPublicacion(),
                agricultor);
    }
}
