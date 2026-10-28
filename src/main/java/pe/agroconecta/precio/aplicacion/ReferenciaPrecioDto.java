package pe.agroconecta.precio.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Referencia de precio para un cultivo en un distrito (paso 3 de "Vender" y marca PRECIO_FUERA_RANGO).
 *
 * @param idCultivo         id del cultivo
 * @param cultivo           nombre del cultivo
 * @param zona              zona de la referencia, o "las zonas de Piura" si es un promedio
 * @param esPromedioDeZonas {@code true} si el distrito no es zona y se promedian las zonas
 * @param fecha             día del precio más reciente usado
 * @param precioKg          precio de referencia por kilo
 * @param minimoKg          límite inferior (−15 %)
 * @param maximoKg          límite superior (+15 %)
 * @param unidadLocal       unidad local del cultivo
 * @param precioUnidadLocal referencia en la unidad local, o {@code null}
 * @param rangoPlataforma   ventas completadas en la plataforma (30 días), o {@code null}
 */
public record ReferenciaPrecioDto(Integer idCultivo, String cultivo, String zona, boolean esPromedioDeZonas,
        LocalDate fecha, BigDecimal precioKg, BigDecimal minimoKg, BigDecimal maximoKg, String unidadLocal,
        BigDecimal precioUnidadLocal, RangoPlataformaDto rangoPlataforma) {

    /**
     * Rango de precios por kilo de las ventas completadas en la plataforma.
     *
     * @param minimoKg   mínimo
     * @param maximoKg   máximo
     * @param promedioKg promedio ponderado por kilos
     * @param ventas     número de ventas
     */
    public record RangoPlataformaDto(BigDecimal minimoKg, BigDecimal maximoKg, BigDecimal promedioKg, Long ventas) {
    }
}
