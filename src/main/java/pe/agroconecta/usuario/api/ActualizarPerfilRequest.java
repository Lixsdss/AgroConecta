package pe.agroconecta.usuario.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import pe.agroconecta.usuario.aplicacion.ActualizacionPerfilDto;
import pe.agroconecta.usuario.aplicacion.ReglasUsuario;
import pe.agroconecta.usuario.dominio.TipoComprador;

/**
 * Cuerpo de {@code PUT /api/v1/usuarios/me}. Los datos del negocio los exige el servicio solo si el usuario es
 * comprador, porque el rol sale del token y no de la petición.
 *
 * @param nombreCompleto nombre completo
 * @param idDistrito     id del distrito
 * @param nombreChacra   nombre de la chacra (agricultor, opcional)
 * @param nombreNegocio  nombre del negocio (comprador)
 * @param tipoComprador  tipo de negocio (comprador)
 * @param ruc            RUC (comprador, opcional)
 */
public record ActualizarPerfilRequest(
        @NotBlank(message = "Escriba su nombre.")
        @Size(min = 3, max = 120, message = "Su nombre debe tener entre 3 y 120 letras.")
        String nombreCompleto,

        @NotNull(message = "Elija su distrito.")
        Integer idDistrito,

        @Size(max = 80, message = "El nombre de la chacra puede tener hasta 80 letras.")
        String nombreChacra,

        @Size(max = 120, message = "El nombre del negocio puede tener hasta 120 letras.")
        String nombreNegocio,

        TipoComprador tipoComprador,

        @Pattern(regexp = ReglasUsuario.PATRON_RUC_OPCIONAL,
                message = "El RUC tiene 11 números y empieza en 1 o 2.")
        String ruc) {

    /**
     * Convierte la petición en los datos del servicio de perfil.
     *
     * @return datos editables del perfil
     */
    public ActualizacionPerfilDto aDto() {
        return new ActualizacionPerfilDto(nombreCompleto, idDistrito, nombreChacra, nombreNegocio, tipoComprador, ruc);
    }
}
