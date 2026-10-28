package pe.agroconecta.ventaagrupada.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import pe.agroconecta.venta.aplicacion.ParteDto;

/**
 * Propuesta conjunta con el avance de sus partes (UC-12 paso 8).
 *
 * @param id            id de la propuesta
 * @param idVariedad    variedad del lote
 * @param cultivo       cultivo
 * @param variedad      variedad
 * @param idDistrito    distrito del lote
 * @param distrito      distrito
 * @param cantidadKg    kilos pedidos
 * @param precioKg      soles por kilo
 * @param fechaRecojo   día de recojo
 * @param estado        {@code ABIERTA}, {@code CERRADA} o {@code CANCELADA}
 * @param fechaCreacion fecha y hora de la propuesta
 * @param kgRepartidos  kilos de todas las partes (puede ser menor que lo pedido por el redondeo)
 * @param kgAcordados   kilos de las partes aceptadas (y las que siguieron su ciclo)
 * @param kgPendientes  kilos de las partes que esperan respuesta
 * @param partes        partes, una por cosecha del lote
 */
public record PropuestaDto(Long id, Integer idVariedad, String cultivo, String variedad, Integer idDistrito,
        String distrito, BigDecimal cantidadKg, BigDecimal precioKg, LocalDate fechaRecojo, String estado,
        LocalDateTime fechaCreacion, BigDecimal kgRepartidos, BigDecimal kgAcordados, BigDecimal kgPendientes,
        List<ParteDto> partes) {
}
