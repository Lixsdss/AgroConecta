package pe.agroconecta.publicacion.aplicacion;

import java.time.LocalDate;

/**
 * Filtros opcionales de la búsqueda (RF-03, UC-03 paso 2). Un valor {@code null} no filtra.
 *
 * @param idCultivo    cultivo (el filtro "Categoría")
 * @param idVariedad   variedad
 * @param idProvincia  provincia (todos sus distritos)
 * @param idDistrito   distrito
 * @param idUnidad     unidad en que vende el agricultor
 * @param cosechaDesde fecha de cosecha mínima
 * @param cosechaHasta fecha de cosecha máxima
 */
public record FiltroBusquedaDto(Integer idCultivo, Integer idVariedad, Integer idProvincia, Integer idDistrito,
        Integer idUnidad, LocalDate cosechaDesde, LocalDate cosechaHasta) {
}
