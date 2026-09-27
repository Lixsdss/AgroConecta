package pe.agroconecta.usuario.aplicacion;

import java.time.LocalDateTime;

/**
 * Datos básicos de un usuario para otros módulos (por ejemplo, el agricultor de una publicación en la
 * moderación).
 *
 * @param id             id del usuario
 * @param nombreCompleto nombre completo
 * @param celular        celular
 * @param distrito       nombre de su distrito
 * @param fechaRegistro  fecha de registro
 */
public record UsuarioResumenDto(Long id, String nombreCompleto, String celular, String distrito,
        LocalDateTime fechaRegistro) {
}
