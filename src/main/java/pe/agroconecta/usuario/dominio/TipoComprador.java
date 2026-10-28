package pe.agroconecta.usuario.dominio;

/**
 * Tipo de negocio del comprador ({@code chk_comprador_tipo}).
 */
public enum TipoComprador {
    /** Puesto o empresa de un mercado mayorista. */
    MERCADO_MAYORISTA,
    /** Restaurante. */
    RESTAURANTE,
    /** Hotel. */
    HOTEL,
    /** Procesadora de alimentos. */
    PROCESADORA,
    /** Molino (arroz). */
    MOLINO,
    /** Otro tipo de negocio. */
    OTRO
}
