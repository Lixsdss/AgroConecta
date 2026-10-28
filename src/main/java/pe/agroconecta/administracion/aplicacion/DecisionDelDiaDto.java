package pe.agroconecta.administracion.aplicacion;

import java.time.LocalDateTime;

import pe.agroconecta.administracion.dominio.DecisionModeracion;
import pe.agroconecta.administracion.dominio.MotivoModeracion;

/**
 * Decisión de moderación de un día (pestaña "Decisiones de hoy").
 *
 * @param id            id de la revisión
 * @param idPublicacion id de la publicación
 * @param producto      cultivo y variedad
 * @param decision      decisión
 * @param motivo        motivo, o {@code null}
 * @param mensaje       mensaje al agricultor, o {@code null}
 * @param fecha         fecha y hora
 * @param administrador nombre del administrador
 */
public record DecisionDelDiaDto(Long id, Long idPublicacion, String producto, DecisionModeracion decision,
        MotivoModeracion motivo, String mensaje, LocalDateTime fecha, String administrador) {
}
