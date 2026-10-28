package pe.agroconecta.usuario.aplicacion;

import java.time.LocalDateTime;

/**
 * Datos de la cuenta de un usuario para la administración (RF-08, RF-09).
 *
 * @param id           id del usuario
 * @param rol          {@code AGRICULTOR}, {@code COMPRADOR} o {@code ADMINISTRADOR}
 * @param estado       {@code ACTIVO}, {@code SUSPENDIDO} o {@code INACTIVO}
 * @param ultimoAcceso último inicio de sesión, o {@code null}
 */
public record CuentaDto(Long id, String rol, String estado, LocalDateTime ultimoAcceso) {
}
