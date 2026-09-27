package pe.agroconecta.usuario.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import pe.agroconecta.usuario.aplicacion.RegistroDto;
import pe.agroconecta.usuario.aplicacion.ReglasUsuario;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.TipoComprador;

/**
 * Cuerpo de {@code POST /api/v1/auth/registro}.
 *
 * @param rol            AGRICULTOR o COMPRADOR
 * @param nombreCompleto nombre completo
 * @param celular        celular de 9 dígitos que empieza en 9 (se admiten espacios)
 * @param idDistrito     id del distrito
 * @param contrasena     contraseña de 8 a 72 caracteres con letras y números
 * @param nombreChacra   nombre de la chacra (agricultor, opcional)
 * @param nombreNegocio  nombre del negocio (comprador, obligatorio)
 * @param tipoComprador  tipo de negocio (comprador, obligatorio)
 * @param ruc            RUC (comprador, opcional)
 */
public record RegistroRequest(
        @NotNull(message = "Elija si es agricultor o comprador.")
        Rol rol,

        @NotBlank(message = "Escriba su nombre.")
        @Size(min = 3, max = 120, message = "Su nombre debe tener entre 3 y 120 letras.")
        String nombreCompleto,

        @NotBlank(message = "Escriba su número de celular.")
        @Pattern(regexp = ReglasUsuario.PATRON_CELULAR_CON_ESPACIOS,
                message = "Escriba los 9 números de su celular, empezando por 9.")
        String celular,

        @NotNull(message = "Elija su distrito.")
        Integer idDistrito,

        @NotBlank(message = "Cree una contraseña.")
        @Pattern(regexp = ReglasUsuario.PATRON_CONTRASENA,
                message = "Use al menos 8 caracteres, con letras y números.")
        String contrasena,

        @Size(max = 80, message = "El nombre de la chacra puede tener hasta 80 letras.")
        String nombreChacra,

        @Size(max = 120, message = "El nombre del negocio puede tener hasta 120 letras.")
        String nombreNegocio,

        TipoComprador tipoComprador,

        @Pattern(regexp = ReglasUsuario.PATRON_RUC_OPCIONAL,
                message = "El RUC tiene 11 números y empieza en 1 o 2.")
        String ruc) {

    /**
     * Solo agricultores y compradores se registran por sí mismos.
     *
     * @return {@code true} si el rol está vacío (lo reporta {@code @NotNull}) o se puede registrar
     */
    @AssertTrue(message = "Solo pueden registrarse agricultores y compradores.")
    public boolean isRolPermitido() {
        return rol != Rol.ADMINISTRADOR;
    }

    /**
     * Un comprador debe indicar el nombre y el tipo de su negocio.
     *
     * @return {@code true} si no es comprador o si tiene ambos datos
     */
    @AssertTrue(message = "Indique el nombre y el tipo de su negocio.")
    public boolean isDatosDeCompradorCompletos() {
        return rol != Rol.COMPRADOR || (StringUtils.isNotBlank(nombreNegocio) && tipoComprador != null);
    }

    /**
     * Convierte la petición en los datos del servicio de registro.
     *
     * @return datos de registro
     */
    public RegistroDto aDto() {
        return new RegistroDto(rol, nombreCompleto, celular, idDistrito, contrasena, nombreChacra, nombreNegocio,
                tipoComprador, ruc);
    }

    /** Oculta la contraseña si la petición se imprime por error en un log. */
    @Override
    public String toString() {
        return "RegistroRequest[rol=" + rol + ", idDistrito=" + idDistrito + "]";
    }
}
