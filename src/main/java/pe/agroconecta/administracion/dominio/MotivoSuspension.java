package pe.agroconecta.administracion.dominio;

/**
 * Motivos de suspensión ({@code chk_suspension_motivo}).
 */
public enum MotivoSuspension {
    /** Tres o más reclamos fundados en su contra ({@code trg_suspensiones_bi} lo verifica). */
    RECLAMOS_FUNDADOS,
    /** Registró datos falsos. */
    DATOS_FALSOS,
    /** Publicaciones engañosas. */
    PUBLICACIONES_ENGANOSAS,
    /** Uso indebido de la plataforma. */
    USO_INDEBIDO
}
