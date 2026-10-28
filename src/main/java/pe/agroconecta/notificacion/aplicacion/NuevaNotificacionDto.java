package pe.agroconecta.notificacion.aplicacion;

/**
 * Aviso que pide crear otro módulo. El tipo va como texto (uno de {@code chk_notificacion_tipo}: MENSAJE, OFERTA,
 * VENTA, PROPUESTA_CONJUNTA, CALIFICACION, RECLAMO o CUENTA).
 *
 * @param idUsuario    usuario que lo recibe
 * @param tipo         tipo de aviso
 * @param titulo       título
 * @param contenido    texto
 * @param idReferencia id del objeto del aviso, o {@code null}
 */
public record NuevaNotificacionDto(Long idUsuario, String tipo, String titulo, String contenido,
        Long idReferencia) {
}
