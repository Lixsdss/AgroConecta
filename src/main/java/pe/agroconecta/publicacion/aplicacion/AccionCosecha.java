package pe.agroconecta.publicacion.aplicacion;

/**
 * Acciones del agricultor sobre su cosecha (RF-07 acciones rápidas; corrección de RF-09).
 */
public enum AccionCosecha {
    /** Bajar el precio por unidad (PUBLICADA). */
    BAJAR_PRECIO,
    /** Extender la fecha límite (PUBLICADA o VENCIDA; una VENCIDA vuelve a PUBLICADA). */
    EXTENDER,
    /** Retirar la cosecha (PUBLICADA o VENCIDA). */
    RETIRAR,
    /** Corregir los datos de una cosecha oculta por la moderación (BLOQUEADA). */
    CORREGIR
}
