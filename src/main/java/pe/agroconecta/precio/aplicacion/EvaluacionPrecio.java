package pe.agroconecta.precio.aplicacion;

/**
 * Resultado de comparar un precio por kilo con la referencia (±15 %). Lo usa {@code publicacion} para la marca
 * PRECIO_FUERA_RANGO y la pantalla para el veredicto de color.
 */
public enum EvaluacionPrecio {
    /** Dentro de ±15 %: "Buen precio". */
    DENTRO,
    /** Más de 15 % por debajo: "Precio bajo, podría ganar más". */
    BAJO,
    /** Más de 15 % por encima: "Precio muy alto, puede tardar en vender". */
    ALTO,
    /** No hay precio de referencia de 7 días o menos. */
    SIN_REFERENCIA;

    /**
     * Indica si el precio queda fuera del rango de la referencia.
     *
     * @return {@code true} en BAJO y ALTO
     */
    public boolean fueraDeRango() {
        return this == BAJO || this == ALTO;
    }
}
