package pe.agroconecta.mensajeria.aplicacion;

/**
 * La otra persona de la conversación.
 *
 * @param id      id del usuario
 * @param nombre  nombre completo
 * @param negocio nombre del negocio si es el comprador; {@code null} si es el agricultor
 */
public record OtraParteDto(Long id, String nombre, String negocio) {
}
