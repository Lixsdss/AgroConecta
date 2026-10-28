package pe.agroconecta.administracion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.agroconecta.administracion.dominio.MotivoReclamo;

/**
 * Cuerpo de {@code POST /api/v1/reclamos}.
 *
 * @param idVenta     venta reclamada
 * @param motivo      motivo de catálogo
 * @param descripcion qué pasó
 */
public record RegistrarReclamoRequest(
        @NotNull(message = "Indique la venta.")
        Long idVenta,

        @NotNull(message = "Elija el motivo.")
        MotivoReclamo motivo,

        @NotBlank(message = "Cuente qué pasó.")
        @Size(max = 300, message = "La descripción puede tener hasta 300 letras.")
        String descripcion) {
}
