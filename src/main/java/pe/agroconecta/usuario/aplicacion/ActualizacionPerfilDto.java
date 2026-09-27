package pe.agroconecta.usuario.aplicacion;

import pe.agroconecta.usuario.dominio.TipoComprador;

/**
 * Datos editables del propio perfil. El rol y el celular no se cambian aquí.
 *
 * @param nombreCompleto nombre completo
 * @param idDistrito     id del distrito
 * @param nombreChacra   nombre de la chacra (solo agricultor, opcional)
 * @param nombreNegocio  nombre del negocio (solo comprador, obligatorio)
 * @param tipoComprador  tipo de negocio (solo comprador, obligatorio)
 * @param ruc            RUC (solo comprador, opcional)
 */
public record ActualizacionPerfilDto(String nombreCompleto, Integer idDistrito, String nombreChacra,
        String nombreNegocio, TipoComprador tipoComprador, String ruc) {
}
