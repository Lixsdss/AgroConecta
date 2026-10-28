package pe.agroconecta.usuario.aplicacion;

/**
 * Datos de un administrador nuevo (UC-10 A5).
 *
 * @param nombreCompleto nombre completo
 * @param celular        celular de 9 dígitos (se admiten espacios)
 * @param idDistrito     distrito
 * @param contrasena     contraseña inicial
 */
public record NuevoAdministradorDto(String nombreCompleto, String celular, Integer idDistrito, String contrasena) {
}
