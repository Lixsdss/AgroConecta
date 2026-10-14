package pe.agroconecta.administracion.api;

/**
 * Respuesta de {@code POST /api/v1/admin/usuarios/{id}/contrasena-temporal}: se muestra una sola vez.
 *
 * @param contrasenaTemporal contraseña temporal
 */
public record ContrasenaTemporalResponse(String contrasenaTemporal) {
}
