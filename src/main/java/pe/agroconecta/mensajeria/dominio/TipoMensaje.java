package pe.agroconecta.mensajeria.dominio;

/**
 * Tipo de mensaje ({@code chk_mensaje_tipo}).
 */
public enum TipoMensaje {
    /** Mensaje de texto: siempre se acepta (sirve para coordinar la entrega). */
    TEXTO,
    /** Oferta con cantidad y precio por unidad: solo mientras la publicación está PUBLICADA. */
    OFERTA
}
