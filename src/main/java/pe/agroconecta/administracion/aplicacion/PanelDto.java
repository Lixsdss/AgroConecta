package pe.agroconecta.administracion.aplicacion;

import java.util.List;

/**
 * Panel del administrador (pantalla 11a, UC-11 parte 4).
 *
 * @param tarjetas        tarjetas de resumen
 * @param colaPorRevisar  las 3 publicaciones más antiguas de la cola
 * @param mes             mes actual {@code yyyy-MM}
 * @param kilosPorCultivo ventas completadas del mes por cultivo
 * @param embudo          las 10 publicaciones con más contactos
 * @param mejorReputacion las 5 mejores reputaciones
 * @param peorReputacion  las 5 peores (sin repetir las mejores)
 */
public record PanelDto(TarjetasPanelDto tarjetas, List<ColaPanelDto> colaPorRevisar, String mes,
        List<VentaMensualDto> kilosPorCultivo, List<EmbudoDto> embudo, List<ReputacionPanelDto> mejorReputacion,
        List<ReputacionPanelDto> peorReputacion) {
}
