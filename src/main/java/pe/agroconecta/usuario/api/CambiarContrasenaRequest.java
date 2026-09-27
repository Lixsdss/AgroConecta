package pe.agroconecta.usuario.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import pe.agroconecta.usuario.aplicacion.ReglasUsuario;

/**
 * Cuerpo de {@code PUT /api/v1/usuarios/me/contrasena}.
 *
 * @param contrasenaActual contraseña actual
 * @param contrasenaNueva  nueva contraseña de 8 a 72 caracteres con letras y números
 */
public record CambiarContrasenaRequest(
        @NotBlank(message = "Escriba su contraseña actual.")
        String contrasenaActual,

        @NotBlank(message = "Escriba la nueva contraseña.")
        @Pattern(regexp = ReglasUsuario.PATRON_CONTRASENA,
                message = "Use al menos 8 caracteres, con letras y números.")
        String contrasenaNueva) {

    /** Oculta las contraseñas si la petición se imprime por error en un log. */
    @Override
    public String toString() {
        return "CambiarContrasenaRequest[]";
    }
}
