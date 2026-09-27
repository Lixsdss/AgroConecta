package pe.agroconecta.catalogo.aplicacion;

/**
 * Distrito de la región Piura con su provincia.
 *
 * @param id           id del distrito
 * @param nombre       nombre del distrito
 * @param idProvincia  id de la provincia
 * @param provincia    nombre de la provincia
 * @param esZonaPrecio {@code true} si tiene precio de referencia diario (RF-02)
 */
public record DistritoDto(Integer id, String nombre, Integer idProvincia, String provincia, boolean esZonaPrecio) {
}
