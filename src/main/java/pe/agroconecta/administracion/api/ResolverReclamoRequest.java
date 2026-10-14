package pe.agroconecta.administracion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.agroconecta.administracion.dominio.EstadoReclamo;

/**
 * Cuerpo de {@code POST /api/v1/admin/reclamos/{id}/resolucion}.
 *
 * @param estado    FUNDADO o INFUNDADO
 * @param respuesta respuesta para las partes
 */
public record ResolverReclamoRequest(
        @NotNull(message = "Elija el resultado.")
        EstadoReclamo estado,

        @NotBlank(message = "Escriba la respuesta para las partes.")
        @Size(max = 300, message = "La respuesta puede tener hasta 300 letras.")
        String respuesta) {
}
