package pe.agroconecta.precio.dominio;

/**
 * Cómo se cargó el precio ({@code chk_precio_metodo}).
 */
public enum MetodoCarga {
    /** Formulario "Cargar precio del día". */
    FORMULARIO,
    /** Archivo Excel o CSV. */
    ARCHIVO
}
