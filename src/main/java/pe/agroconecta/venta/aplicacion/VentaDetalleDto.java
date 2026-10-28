package pe.agroconecta.venta.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Venta vista por una de sus partes: términos, contraparte, línea de tiempo y acciones posibles.
 *
 * @param id                 id de la venta
 * @param idConversacion     conversación de la que nace (enlace al chat)
 * @param idPublicacion      publicación
 * @param soyComprador       {@code true} si el usuario de la sesión es el comprador
 * @param contraparte        la otra parte
 * @param cultivo            cultivo
 * @param variedad           variedad
 * @param distrito           distrito de la cosecha
 * @param idUnidad           unidad acordada
 * @param unidad             unidad en singular
 * @param unidadPlural       unidad en plural
 * @param cantidad           cantidad acordada
 * @param pesoUnidadKg       kilos por unidad
 * @param cantidadKg         kilos acordados
 * @param precioUnitario     soles por unidad
 * @param precioKg           soles por kilo
 * @param montoTotal         total en soles
 * @param fechaRecojo        día de recojo
 * @param estado             estado
 * @param fechaCreacion      fecha y hora del acuerdo
 * @param fechaActualizacion fecha y hora del último cambio
 * @param lineaDeTiempo      pasos de la venta, del primero al último
 * @param acciones           acciones que el usuario de la sesión puede hacer ahora
 * @param calificaciones     calificaciones dada y recibida, y si puede calificar (RF-05)
 * @param idPropuestaGrupal  propuesta conjunta de la que nace, o {@code null} si nace del chat (RF-10)
 */
public record VentaDetalleDto(Long id, Long idConversacion, Long idPublicacion, boolean soyComprador,
        ContraparteDto contraparte, String cultivo, String variedad, String distrito, Integer idUnidad, String unidad,
        String unidadPlural, BigDecimal cantidad, BigDecimal pesoUnidadKg, BigDecimal cantidadKg,
        BigDecimal precioUnitario, BigDecimal precioKg, BigDecimal montoTotal, LocalDate fechaRecojo, String estado,
        LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion, List<PasoDto> lineaDeTiempo,
        List<AccionVenta> acciones, CalificacionesVentaDto calificaciones, Long idPropuestaGrupal) {
}
