package pe.agroconecta.precio.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Precio vigente de un cultivo en una zona (tarjeta "Precios de hoy" y tabla del administrador).
 *
 * @param idPrecio          id del precio (para "Editar")
 * @param idCultivo         id del cultivo
 * @param cultivo           nombre del cultivo
 * @param idDistrito        id de la zona
 * @param zona              nombre de la zona
 * @param fecha             día del precio
 * @param precioKg          soles por kilo
 * @param unidadLocal       unidad en que se comercializa el cultivo
 * @param precioUnidadLocal soles por unidad local, o {@code null}
 * @param tendencia         SUBE, BAJA, IGUAL o SIN_DATO
 * @param fuente            fuente del precio
 * @param actualizado       última carga o edición
 */
public record PrecioVigenteDto(Long idPrecio, Integer idCultivo, String cultivo, Integer idDistrito, String zona,
        LocalDate fecha, BigDecimal precioKg, String unidadLocal, BigDecimal precioUnidadLocal, String tendencia,
        String fuente, LocalDateTime actualizado) {
}
