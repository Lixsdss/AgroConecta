package pe.agroconecta.administracion.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import pe.agroconecta.administracion.dominio.DecisionModeracion;
import pe.agroconecta.administracion.dominio.MotivoModeracion;

/**
 * Cuerpo de {@code POST /api/v1/moderacion/publicaciones/{id}/decisiones}.
 *
 * @param decision decisión
 * @param motivo   motivo de catálogo (ADVERTENCIA y BLOQUEO)
 * @param mensaje  mensaje para el agricultor (ADVERTENCIA y BLOQUEO; opcional en DESBLOQUEO)
 */
public record DecisionRequest(
        @NotNull(message = "Elija una decisión.")
        DecisionModeracion decision,

        MotivoModeracion motivo,

        @Size(max = 300, message = "El mensaje puede tener hasta 300 letras.")
        String mensaje) {

    /**
     * Advertir y ocultar exigen un motivo de catálogo.
     *
     * @return {@code true} si la decisión no lo exige o si hay motivo
     */
    @AssertTrue(message = "Elija el motivo.")
    public boolean isMotivoPresente() {
        return decision == null || !decision.exigeMotivo() || motivo != null;
    }

    /**
     * Advertir y ocultar exigen un mensaje para el agricultor.
     *
     * @return {@code true} si la decisión no lo exige o si hay mensaje
     */
    @AssertTrue(message = "Escriba el mensaje para el agricultor.")
    public boolean isMensajePresente() {
        return decision == null || !decision.exigeMotivo() || StringUtils.isNotBlank(mensaje);
    }
}
