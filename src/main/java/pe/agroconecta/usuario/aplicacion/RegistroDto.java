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

    /** Oculta la contraseña si el registro se imprime por error en un log. */
    @Override
    public String toString() {
        return "RegistroDto[rol=" + rol + ", idDistrito=" + idDistrito + "]";
    }
}
