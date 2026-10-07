package pe.agroconecta.venta.api;

import java.math.BigDecimal;

import org.springframework.data.web.PagedModel;

import pe.agroconecta.venta.aplicacion.CalificacionDto;
import pe.agroconecta.venta.aplicacion.ReputacionDto;

/**
 * Reputación pública de un usuario.
 *
 * @param idUsuario      usuario
 * @param promedio       promedio de estrellas, o {@code null} si no tiene calificaciones
 * @param total          cantidad de calificaciones recibidas
 * @param calificaciones página de calificaciones recibidas
 */
public record ReputacionResponse(Long idUsuario, BigDecimal promedio, long total,
        PagedModel<CalificacionDto> calificaciones) {

    /**
     * Convierte la reputación del servicio.
     *
     * @param r reputación
     * @return respuesta
     */
    public static ReputacionResponse de(ReputacionDto r) {
        return new ReputacionResponse(r.idUsuario(), r.promedio(), r.total(), new PagedModel<>(r.calificaciones()));
    }
}
