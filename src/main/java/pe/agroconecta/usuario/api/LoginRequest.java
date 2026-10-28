package pe.agroconecta.usuario.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de {@code POST /api/v1/auth/login}.
 *
 * @param celular    celular (se admiten espacios)
 * @param contrasena contraseña
 */
public record LoginRequest(
        @NotBlank(message = "Escriba su número de celular.") String celular,
        @NotBlank(message = "Escriba su contraseña.") String contrasena) {

    /** Oculta la contraseña si la petición se imprime por error en un log. */
    @Override
    public String toString() {
        return "LoginRequest[]";
    }
}
