package pe.agroconecta.publicacion.aplicacion;

import java.math.BigDecimal;

/**
 * Datos de una publicación, en cualquier estado, para otros módulos (por ejemplo, la cabecera del chat de
 * {@code mensajeria}). El estado va como texto, nunca como enum del dominio.
 *
 * @param id                  id de la publicación
 * @param idAgricultor        id del agricultor
 * @param agricultor          nombre completo del agricultor
 * @param cultivo             nombre del cultivo
 * @param variedad            nombre de la variedad
 * @param distrito            nombre del distrito
 * @param estado              estado ({@code PUBLICADA}, {@code VENDIDA}, {@code VENCIDA}, ...)
 * @param unidad              nombre de la unidad en singular
 * @param unidadPlural        nombre de la unidad en plural
 * @param pesoUnidadKg        kilos por unidad
 * @param precioUnitario      soles por unidad
 * @param precioKg            soles por kilo
 * @param disponibleKg        kilos aún no comprometidos en ventas
 * @param unidadesDisponibles unidades disponibles, redondeadas hacia abajo a 2 decimales
 * @param diasRestantes       días que faltan para la fecha límite
 * @param idUnidad            id de la unidad de venta
 */
public record PublicacionResumenDto(Long id, Long idAgricultor, String agricultor, String cultivo, String variedad,
        String distrito, String estado, String unidad, String unidadPlural, BigDecimal pesoUnidadKg,
        BigDecimal precioUnitario, BigDecimal precioKg, BigDecimal disponibleKg, BigDecimal unidadesDisponibles,
        Integer diasRestantes, Integer idUnidad) {

    /**
     * Devuelve {@code true} si la publicación está PUBLICADA.
     *
     * @return {@code true} si la publicación está PUBLICADA
     */
    public boolean estaPublicada() {
        return "PUBLICADA".equals(estado);
    }
}
