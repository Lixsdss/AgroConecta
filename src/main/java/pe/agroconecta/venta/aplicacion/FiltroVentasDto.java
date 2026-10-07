package pe.agroconecta.venta.aplicacion;

import java.time.LocalDate;

/**
 * Filtros de "Mis ventas" o "Mis compras" (UC-08 paso 3). Un valor {@code null} no filtra.
 *
 * @param grupo          {@code EN_CURSO} o {@code TERMINADAS}
 * @param desde          primer día del acuerdo
 * @param hasta          último día del acuerdo
 * @param contraparte    texto que se busca en el nombre de la otra parte
 * @param idConversacion solo las ventas de esa conversación
 */
public record FiltroVentasDto(String grupo, LocalDate desde, LocalDate hasta, String contraparte,
        Long idConversacion) {
}
