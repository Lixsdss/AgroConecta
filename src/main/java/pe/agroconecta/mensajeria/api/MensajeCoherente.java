package pe.agroconecta.mensajeria.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Un mensaje TEXTO necesita {@code contenido}; una OFERTA necesita {@code ofertaCantidad} y
 * {@code ofertaPrecioUnitario} ({@code chk_mensaje_tipo}). Los errores se informan en el campo que falta.
 */
@Documented
@Constraint(validatedBy = ValidadorMensajeCoherente.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MensajeCoherente {

    /**
     * Devuelve el mensaje por defecto.
     *
     * @return mensaje por defecto
     */
    String message() default "El mensaje no es válido.";

    /**
     * Devuelve los grupos de validación.
     *
     * @return grupos de validación
     */
    Class<?>[] groups() default {};

    /**
     * Devuelve la carga útil.
     *
     * @return carga útil
     */
    Class<? extends Payload>[] payload() default {};
}
