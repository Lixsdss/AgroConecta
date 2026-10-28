package pe.agroconecta.usuario.aplicacion;

import java.time.LocalDateTime;

import pe.agroconecta.usuario.dominio.EstadoUsuario;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.TipoComprador;

/**
 * Perfil del usuario con su distrito y los datos de su subtipo. Los campos que no corresponden al rol van en
 * {@code null}.
 *
 * @param id             id del usuario
 * @param rol            rol
 * @param nombreCompleto nombre completo
 * @param celular        celular
 * @param estado         estado
 * @param idDistrito     id del distrito
 * @param distrito       nombre del distrito
 * @param provincia      nombre de la provincia
 * @param fechaRegistro  fecha de registro
 * @param nombreChacra   nombre de la chacra (agricultor)
 * @param nombreNegocio  nombre del negocio (comprador)
 * @param tipoComprador  tipo de negocio (comprador)
 * @param ruc            RUC (comprador)
 */
public record PerfilDto(Long id, Rol rol, String nombreCompleto, String celular, EstadoUsuario estado,
        Integer idDistrito, String distrito, String provincia, LocalDateTime fechaRegistro, String nombreChacra,
        String nombreNegocio, TipoComprador tipoComprador, String ruc) {
}
