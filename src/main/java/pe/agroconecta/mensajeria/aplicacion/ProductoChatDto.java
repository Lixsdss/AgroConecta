package pe.agroconecta.mensajeria.aplicacion;

import java.math.BigDecimal;

/**
 * Publicación sobre la que se conversa (cabecera del chat).
 *
 * @param cultivo             cultivo
 * @param variedad            variedad
 * @param distrito            distrito
 * @param estado              estado de la publicación
 * @param unidad              unidad en singular
 * @param unidadPlural        unidad en plural
 * @param pesoUnidadKg        kilos por unidad
 * @param precioUnitario      soles por unidad
 * @param precioKg            soles por kilo
 * @param disponibleKg        kilos disponibles
 * @param unidadesDisponibles unidades disponibles
 * @param diasRestantes       días que faltan para la fecha límite
 */
public record ProductoChatDto(String cultivo, String variedad, String distrito, String estado, String unidad,
        String unidadPlural, BigDecimal pesoUnidadKg, BigDecimal precioUnitario, BigDecimal precioKg,
        BigDecimal disponibleKg, BigDecimal unidadesDisponibles, Integer diasRestantes) {
}
