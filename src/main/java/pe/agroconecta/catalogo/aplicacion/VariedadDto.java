package pe.agroconecta.catalogo.aplicacion;

/**
 * Variedad con su cultivo.
 *
 * @param id           id de la variedad
 * @param nombre       nombre de la variedad
 * @param idCultivo    id del cultivo
 * @param cultivo      nombre del cultivo
 * @param vidaUtilDias vida útil del cultivo en días
 */
public record VariedadDto(Integer id, String nombre, Integer idCultivo, String cultivo, Integer vidaUtilDias) {
}
