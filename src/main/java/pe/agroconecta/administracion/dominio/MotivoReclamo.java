package pe.agroconecta.administracion.dominio;

/**
 * Motivos de un reclamo ({@code chk_reclamo_motivo}).
 */
public enum MotivoReclamo {
    /** La otra parte no se presentó al recojo. */
    NO_SE_PRESENTO,
    /** El producto no coincide con lo acordado. */
    PRODUCTO_NO_COINCIDE,
    /** Llegó menos cantidad de la acordada. */
    CANTIDAD_INCOMPLETA,
    /** No se pagó lo acordado. */
    INCUMPLIO_PAGO,
    /** Trato indebido. */
    MAL_TRATO
}
