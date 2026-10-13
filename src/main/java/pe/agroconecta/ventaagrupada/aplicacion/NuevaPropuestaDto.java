package pe.agroconecta.ventaagrupada.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Propuesta conjunta que envía el comprador (UC-12 paso 2).
 *
 * @param idVariedad  variedad del lote
 * @param idDistrito  distrito del lote
 * @param cantidadKg  kilos pedidos
 * @param precioKg    soles por kilo
 * @param fechaRecojo día de recojo (también límite para responder)
 */
public record NuevaPropuestaDto(Integer idVariedad, Integer idDistrito, BigDecimal cantidadKg, BigDecimal precioKg,
        LocalDate fechaRecojo) {
}
