package pe.agroconecta.catalogo.aplicacion;

import java.math.BigDecimal;

/**
 * Unidad de venta del catálogo.
 *
 * @param id           id de la unidad
 * @param nombre       nombre en singular
 * @param nombrePlural nombre en plural
 * @param factorKg     kilos por unidad, o {@code null} si el peso es variable (jaba, saco)
 */
public record UnidadDto(Integer id, String nombre, String nombrePlural, BigDecimal factorKg) {

    /**
     * Indica si el agricultor debe decir cuántos kilos pesa cada unidad.
     *
     * @return {@code true} en jaba y saco
     */
    public boolean esPesoVariable() {
        return factorKg == null;
    }
}
