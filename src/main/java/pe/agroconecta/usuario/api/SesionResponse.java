package pe.agroconecta.usuario.api;

import java.time.Instant;

import pe.agroconecta.usuario.aplicacion.SesionDto;
import pe.agroconecta.usuario.dominio.Rol;

/**
 * Respuesta de registro e inicio de sesión.
 *
 * @param token    JWT para la cabecera {@code Authorization}
 * @param tipo     siempre {@code Bearer}
 * @param expiraEn momento en que vence el token
 * @param usuario  datos básicos del usuario
 */
public record SesionResponse(String token, String tipo, Instant expiraEn, UsuarioSesion usuario) {

    /**
     * Datos básicos del usuario de la sesión.
     *
     * @param id             id del usuario
     * @param nombreCompleto nombre completo
     * @param rol            rol
     */
    public record UsuarioSesion(Long id, String nombreCompleto, Rol rol) {
    }

    /**
     * Crea la respuesta a partir de la sesión emitida.
     *
     * @param sesion sesión emitida por el servicio
     * @return respuesta de la API
     */
    public static SesionResponse de(SesionDto sesion) {
        return new SesionResponse(sesion.token(), "Bearer", sesion.expiraEn(),
                new UsuarioSesion(sesion.idUsuario(), sesion.nombreCompleto(), sesion.rol()));
    }
}
