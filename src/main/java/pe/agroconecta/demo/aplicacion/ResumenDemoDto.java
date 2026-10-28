package pe.agroconecta.demo.aplicacion;

/**
 * Cantidad de datos de demostración creados.
 *
 * @param usuarios      agricultores, compradores y el administrador
 * @param precios       precios de referencia
 * @param publicaciones cosechas publicadas
 * @param ventas        ventas, incluidas las partes de la propuesta conjunta
 */
public record ResumenDemoDto(int usuarios, int precios, int publicaciones, int ventas) {
}
