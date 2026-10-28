package pe.agroconecta.administracion.dominio;

/**
 * Motivo de catálogo para advertir u ocultar una publicación ({@code chk_revision_motivo}).
 */
public enum MotivoModeracion {
    /** El precio no coincide con el mercado. */
    PRECIO_NO_COINCIDE,
    /** La cantidad no parece correcta. */
    CANTIDAD_INCORRECTA,
    /** El peso por unidad no parece correcto. */
    PESO_UNIDAD_INCORRECTO,
    /** Faltan datos o hay datos incorrectos. */
    DATOS_INCORRECTOS,
    /** Otro motivo (explicado en el mensaje). */
    OTRO
}
