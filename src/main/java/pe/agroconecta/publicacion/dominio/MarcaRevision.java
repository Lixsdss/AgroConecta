package pe.agroconecta.publicacion.dominio;

/**
 * Marca que pone la publicación en la cola de moderación posterior ({@code chk_publicacion_marca}, RF-09).
 */
public enum MarcaRevision {
    /** Primera publicación del agricultor (la pone {@code trg_publicaciones_bi}). */
    PRIMERA_PUBLICACION,
    /** Precio alejado de la referencia (la pone el servicio; umbral a definir en RF-09). */
    PRECIO_FUERA_RANGO,
    /** Cantidad inusual (la pone el servicio; umbral a definir en RF-09). */
    CANTIDAD_INUSUAL,
    /** El agricultor corrigió una publicación bloqueada (la pone {@code trg_publicaciones_bu}). */
    CORREGIDA
}
