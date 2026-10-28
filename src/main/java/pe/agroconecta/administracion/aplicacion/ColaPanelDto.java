package pe.agroconecta.administracion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Publicación de la cola "Por revisar" en el panel.
 *
 * @param id               publicación
 * @param cultivo          cultivo
 * @param variedad         variedad
 * @param cantidad         cantidad
 * @param unidadPlural     unidad en plural
 * @param agricultor       nombre del agricultor
 * @param distrito         distrito
 * @param marcaRevision    motivo por el que está en la cola
 * @param fechaPublicacion fecha de publicación
 */
public record ColaPanelDto(Long id, String cultivo, String variedad, BigDecimal cantidad, String unidadPlural,
        String agricultor, String distrito, String marcaRevision, LocalDateTime fechaPublicacion) {
}
