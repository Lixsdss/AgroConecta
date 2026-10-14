package pe.agroconecta.administracion.aplicacion;

import java.math.BigDecimal;

/**
 * Reputación de un usuario en el panel.
 *
 * @param id                  usuario
 * @param nombre              nombre (o negocio, si es comprador)
 * @param rol                 rol
 * @param calificacion        promedio de estrellas
 * @param totalCalificaciones calificaciones recibidas
 */
public record ReputacionPanelDto(Long id, String nombre, String rol, BigDecimal calificacion,
        long totalCalificaciones) {
}
