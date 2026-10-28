package pe.agroconecta.venta.aplicacion;

/**
 * La otra parte de una venta.
 *
 * @param id      id del usuario
 * @param nombre  nombre completo
 * @param negocio nombre del negocio si es el comprador; {@code null} si es el agricultor
 * @param celular celular (se muestra porque ya hay acuerdo, UC-05 paso 6)
 */
public record ContraparteDto(Long id, String nombre, String negocio, String celular) {
}
