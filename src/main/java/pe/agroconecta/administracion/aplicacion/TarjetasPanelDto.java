package pe.agroconecta.administracion.aplicacion;

import java.time.LocalDateTime;

/**
 * Tarjetas del panel ({@code v_panel_admin}).
 *
 * @param publicacionesActivas   publicaciones PUBLICADA
 * @param publicadasUltimos7Dias publicadas en los últimos 7 días
 * @param porRevisar             publicaciones en la cola
 * @param porRevisarMasAntigua   la más antigua de la cola, o {@code null}
 * @param conformesHoy           decisiones CONFORME de hoy
 * @param bloqueadasHoy          decisiones BLOQUEO de hoy
 * @param preciosCargadosHoy     precios cargados hoy
 * @param preciosEsperadosHoy    precios esperados hoy
 * @param reclamosPendientes     reclamos PENDIENTE
 */
public record TarjetasPanelDto(long publicacionesActivas, long publicadasUltimos7Dias, long porRevisar,
        LocalDateTime porRevisarMasAntigua, long conformesHoy, long bloqueadasHoy, long preciosCargadosHoy,
        long preciosEsperadosHoy, long reclamosPendientes) {
}
