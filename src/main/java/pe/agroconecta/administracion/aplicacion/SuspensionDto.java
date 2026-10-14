package pe.agroconecta.administracion.aplicacion;

import java.time.LocalDateTime;

/**
 * Suspensión vigente de un usuario.
 *
 * @param motivo        motivo de catálogo
 * @param detalle       justificación
 * @param fechaInicio   inicio
 * @param administrador nombre del administrador que suspendió
 */
public record SuspensionDto(String motivo, String detalle, LocalDateTime fechaInicio, String administrador) {
}
