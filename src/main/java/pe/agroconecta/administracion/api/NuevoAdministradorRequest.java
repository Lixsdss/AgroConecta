package pe.agroconecta.administracion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.agroconecta.usuario.aplicacion.NuevoAdministradorDto;
import pe.agroconecta.usuario.aplicacion.ReglasUsuario;

/**
 * Cuerpo de {@code POST /api/v1/admin/administradores}, con las reglas del registro.
 *
 * @param nombreCompleto nombre completo
 * @param celular        celular de 9 dígitos que empieza en 9 (se admiten espacios)
 * @param idDistrito     distrito
 * @param contrasena     contraseña de 8 a 72 caracteres con letras y números
 */
public record NuevoAdministradorRequest(
        @NotBlank(message = "Escriba el nombre.")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 letras.")
        String nombreCompleto,

        @NotBlank(message = "Escriba el número de celular.")
        @Pattern(regexp = ReglasUsuario.PATRON_CELULAR_CON_ESPACIOS,
                message = "Escriba los 9 números del celular, empezando por 9.")
        String celular,

        @NotNull(message = "Elija el distrito.")
        Integer idDistrito,

        @NotBlank(message = "Cree una contraseña.")
        @Pattern(regexp = ReglasUsuario.PATRON_CONTRASENA,
                message = "Use al menos 8 caracteres, con letras y números.")
        String contrasena) {

    /**
     * Convierte el cuerpo en los datos del servicio.
     *
     * @return datos del administrador nuevo
     */
    public NuevoAdministradorDto aDto() {
        return new NuevoAdministradorDto(nombreCompleto, celular, idDistrito, contrasena);
    }
}
