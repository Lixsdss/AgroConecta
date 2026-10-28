package pe.agroconecta.catalogo.aplicacion;

import java.math.BigDecimal;

/**
 * Cultivo con los datos que usa el formulario de publicación.
 *
 * @param id             id del cultivo
 * @param nombre         nombre
 * @param vidaUtilDias   días que dura a la venta (sugiere la fecha límite)
 * @param idUnidadLocal  id de la unidad en que se comercializa en Piura
 * @param unidadLocal    nombre de esa unidad
 * @param pesoSugeridoKg peso típico de la unidad local si es variable, o {@code null}
 */
public record CultivoDto(Integer id, String nombre, Integer vidaUtilDias, Integer idUnidadLocal, String unidadLocal,
        BigDecimal pesoSugeridoKg) {
}
