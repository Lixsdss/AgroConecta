package pe.agroconecta.mensajeria.dominio;

/**
 * Cómo empezó la conversación ({@code chk_conversacion_canal}).
 */
public enum CanalInicio {
    /** El comprador pulsó "Escribir". */
    CHAT,
    /** El comprador pulsó "WhatsApp" (UC-04 A1). */
    WHATSAPP,
    /** Nació de una propuesta conjunta (RF-10). */
    PROPUESTA_CONJUNTA
}
