package pe.agroconecta.usuario.aplicacion;

import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.TipoComprador;

/**
 * Datos para registrar un agricultor o un comprador (UC-10).
 *
 * @param rol            AGRICULTOR o COMPRADOR
 * @param nombreCompleto nombre completo
 * @param celular        celular (puede traer espacios)
 * @param idDistrito     id del distrito
 * @param contrasena     contraseña en texto plano (solo en memoria; se guarda con BCrypt)
 * @param nombreChacra   nombre de la chacra (solo agricultor, opcional)
 * @param nombreNegocio  nombre del negocio (solo comprador, obligatorio)
 * @param tipoComprador  tipo de negocio (solo comprador, obligatorio)
 * @param ruc            RUC (solo comprador, opcional)
 */
public record RegistroDto(Rol rol, String nombreCompleto, String celular, Integer idDistrito, String contrasena,
        String nombreChacra, String nombreNegocio, TipoComprador tipoComprador, String ruc) {

    /**
     * Crea los datos de registro con el rol y el tipo de comprador como texto, para los módulos que no conocen los
     * enums del dominio de usuario.
     *
     * @param rol            AGRICULTOR o COMPRADOR
     * @param nombreCompleto nombre completo
     * @param celular        celular
     * @param idDistrito     id del distrito
     * @param contrasena     contraseña en texto plano
     * @param nombreChacra   nombre de la chacra (solo agricultor), o {@code null}
     * @param nombreNegocio  nombre del negocio (solo comprador), o {@code null}
     * @param tipoComprador  tipo de negocio (solo comprador), o {@code null}
     * @param ruc            RUC (solo comprador), o {@code null}
     * @return datos de registro
     * @throws IllegalArgumentException si el rol o el tipo de comprador no existen
     */
    public static RegistroDto deTexto(String rol, String nombreCompleto, String celular, Integer idDistrito,
            String contrasena, String nombreChacra, String nombreNegocio, String tipoComprador, String ruc) {
        return new RegistroDto(Rol.valueOf(rol), nombreCompleto, celular, idDistrito, contrasena, nombreChacra,
                nombreNegocio, tipoComprador == null ? null : TipoComprador.valueOf(tipoComprador), ruc);
    }

    /** Oculta la contraseña si el registro se imprime por error en un log. */
    @Override
    public String toString() {
        return "RegistroDto[rol=" + rol + ", idDistrito=" + idDistrito + "]";
    }
}
