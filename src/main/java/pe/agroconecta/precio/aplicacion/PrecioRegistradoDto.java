package pe.agroconecta.precio.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

import pe.agroconecta.precio.dominio.FuentePrecio;
import pe.agroconecta.precio.dominio.MetodoCarga;

/**
 * Precio cargado o editado por el administrador.
 *
 * @param id         id del precio
 * @param idCultivo  id del cultivo
 * @param cultivo    nombre del cultivo
 * @param idDistrito id de la zona
 * @param zona       nombre de la zona
 * @param fecha      día
 * @param precioKg   soles por kilo
 * @param fuente     fuente
 * @param metodo     formulario o archivo
 */
public record PrecioRegistradoDto(Long id, Integer idCultivo, String cultivo, Integer idDistrito, String zona,
        LocalDate fecha, BigDecimal precioKg, FuentePrecio fuente, MetodoCarga metodo) {
}
