package pe.agroconecta.usuario.aplicacion;

import java.time.Instant;

import pe.agroconecta.usuario.dominio.Rol;

/**
 * Sesión iniciada: token JWT y datos básicos del usuario.
 *
 * @param token          JWT firmado (HS256)
 * @param expiraEn       momento en que vence el token
 * @param idUsuario      id del usuario
 * @param nombreCompleto nombre completo
 * @param rol            rol del usuario
 */
public record SesionDto(String token, Instant expiraEn, Long idUsuario, String nombreCompleto, Rol rol) {
}
