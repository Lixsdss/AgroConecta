package pe.agroconecta.precio.infraestructura;

/**
 * Fila de datos de un archivo de precios, tal como viene (sin validar).
 *
 * @param numero   número de fila en el archivo (la cabecera es la 1)
 * @param cultivo  texto de la columna cultivo
 * @param zona     texto de la columna zona
 * @param fecha    fecha en formato {@code AAAA-MM-DD} o {@code dd/mm/aaaa}
 * @param precioKg texto de la columna precio_kg
 */
public record FilaArchivoPrecio(int numero, String cultivo, String zona, String fecha, String precioKg) {
}
