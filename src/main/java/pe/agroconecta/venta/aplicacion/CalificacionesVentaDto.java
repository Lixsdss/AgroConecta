package pe.agroconecta.venta.aplicacion;

/**
 * Calificaciones de una venta vistas por una de sus partes (UC-08 paso 6).
 *
 * @param puedoCalificar {@code true} si la venta está COMPLETADA y el usuario aún no calificó
 * @param mia            la que dio el usuario, o {@code null}
 * @param recibida       la que le dio la otra parte, o {@code null}
 */
public record CalificacionesVentaDto(boolean puedoCalificar, CalificacionDto mia, CalificacionDto recibida) {
}
