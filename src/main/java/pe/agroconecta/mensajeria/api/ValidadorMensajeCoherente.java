package pe.agroconecta.mensajeria.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

/**
 * Valida {@link MensajeCoherente}: informa el error en el campo que falta.
 */
public class ValidadorMensajeCoherente implements ConstraintValidator<MensajeCoherente, EnviarMensajeRequest> {

    /** Crea el validador. */
    public ValidadorMensajeCoherente() {
    }

    /**
     * Comprueba que el mensaje tenga los datos de su tipo.
     *
     * @param m        mensaje
     * @param contexto contexto de validación
     * @return {@code true} si es coherente (o si el tipo falta, que ya lo informa {@code @NotNull})
     */
    @Override
    public boolean isValid(EnviarMensajeRequest m, ConstraintValidatorContext contexto) {
        if (m == null || m.tipo() == null) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        boolean valido = true;
        if ("TEXTO".equals(m.tipo()) && StringUtils.isBlank(m.contenido())) {
            valido = error(contexto, "contenido", "Escriba un mensaje.");
        }
        if ("OFERTA".equals(m.tipo())) {
            if (m.ofertaCantidad() == null) {
                valido = error(contexto, "ofertaCantidad", "Indique la cantidad.");
            }
            if (m.ofertaPrecioUnitario() == null) {
                valido = error(contexto, "ofertaPrecioUnitario", "Indique el precio por unidad.");
            }
        }
        return valido;
    }

    private static boolean error(ConstraintValidatorContext contexto, String campo, String mensaje) {
        contexto.buildConstraintViolationWithTemplate(mensaje).addPropertyNode(campo).addConstraintViolation();
        return false;
    }
}
