package pe.agroconecta.publicacion.infraestructura;

/**
 * Cantidad de publicaciones y promedio de kilos de un cultivo en un distrito, para detectar cantidades inusuales.
 *
 * @param publicaciones número de publicaciones comparables
 * @param promedioKg    promedio de kilos, o {@code null} si no hay publicaciones
 */
public record EstadisticaCantidad(long publicaciones, Double promedioKg) {
}
