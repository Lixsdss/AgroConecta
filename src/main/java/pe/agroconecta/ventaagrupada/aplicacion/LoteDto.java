package pe.agroconecta.ventaagrupada.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lote de la venta agrupada (RF-10, UC-12 paso 1).
 *
 * @param idCultivo          cultivo
 * @param cultivo            nombre del cultivo
 * @param idVariedad         variedad
 * @param variedad           nombre de la variedad
 * @param idDistrito         distrito
 * @param distrito           nombre del distrito
 * @param totalAgricultores  agricultores distintos
 * @param totalPublicaciones publicaciones del lote
 * @param volumenKg          kilos disponibles
 * @param precioPromedioKg   precio por kilo promedio
 * @param fechaLimiteProxima fecha límite más próxima
 */
public record LoteDto(Integer idCultivo, String cultivo, Integer idVariedad, String variedad, Integer idDistrito,
        String distrito, long totalAgricultores, long totalPublicaciones, BigDecimal volumenKg,
        BigDecimal precioPromedioKg, LocalDate fechaLimiteProxima) {
}
