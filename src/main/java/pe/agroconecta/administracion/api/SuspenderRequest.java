package pe.agroconecta.administracion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.agroconecta.administracion.dominio.MotivoSuspension;

/**
 * Cuerpo de {@code POST /api/v1/admin/usuarios/{id}/suspension}.
 *
 * @param motivo  motivo de catálogo
 * @param detalle justificación
 */
public record SuspenderRequest(
        @NotNull(message = "Elija el motivo.")
        MotivoSuspension motivo,

        @NotBlank(message = "Escriba el detalle de la suspensión.")
        @Size(max = 300, message = "El detalle puede tener hasta 300 letras.")
        String detalle) {
}
