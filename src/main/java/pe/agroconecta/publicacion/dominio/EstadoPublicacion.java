package pe.agroconecta.publicacion.dominio;

/**
 * Estado de la publicación ({@code chk_publicacion_estado}). Las transiciones las controla
 * {@code trg_publicaciones_bu}; VENDIDA y VENCIDA las fijan la base de datos y el evento diario.
 */
public enum EstadoPublicacion {
    /** Visible en la búsqueda (moderación posterior: así nace). */
    PUBLICADA,
    /** Ocultada por un administrador (RF-09). */
    BLOQUEADA,
    /** Lo acordado cubre todos sus kilos. */
    VENDIDA,
    /** Pasó su fecha límite (RF-07). */
    VENCIDA,
    /** Retirada por el agricultor. */
    RETIRADA
}
