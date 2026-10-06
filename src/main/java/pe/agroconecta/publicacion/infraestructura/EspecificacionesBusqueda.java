package pe.agroconecta.publicacion.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import pe.agroconecta.publicacion.dominio.EstadoPublicacion;
import pe.agroconecta.publicacion.dominio.PublicacionDisponible;

/**
 * Condiciones de la búsqueda multicriterio (RF-03) sobre {@code v_publicaciones_disponibles}. Cada filtro
 * opcional devuelve una condición vacía si su valor es {@code null}, para combinarlos con {@code and}.
 */
public final class EspecificacionesBusqueda {

    private EspecificacionesBusqueda() {
    }

    /**
     * Condición fija de la búsqueda: PUBLICADA, con kilos disponibles y con fecha límite hoy o después.
     *
     * @param hoy fecha de hoy
     * @return condición
     */
    public static Specification<PublicacionDisponible> disponibles(LocalDate hoy) {
        return (raiz, consulta, cb) -> cb.and(
                cb.equal(raiz.get("estado"), EstadoPublicacion.PUBLICADA),
                cb.greaterThan(raiz.get("disponibleKg"), BigDecimal.ZERO),
                cb.greaterThanOrEqualTo(raiz.get("fechaLimite"), hoy));
    }

    /**
     * Filtro por igualdad de un atributo, o ninguno si el valor es {@code null}.
     *
     * @param atributo nombre del atributo de {@link PublicacionDisponible}
     * @param valor    valor buscado
     * @return condición
     */
    public static Specification<PublicacionDisponible> igual(String atributo, Object valor) {
        return (raiz, consulta, cb) -> valor == null ? cb.conjunction() : cb.equal(raiz.get(atributo), valor);
    }

    /**
     * Fecha de cosecha dentro del rango; cada extremo es opcional.
     *
     * @param desde fecha mínima, o {@code null}
     * @param hasta fecha máxima, o {@code null}
     * @return condición
     */
    public static Specification<PublicacionDisponible> cosechaEntre(LocalDate desde, LocalDate hasta) {
        return (raiz, consulta, cb) -> cb.and(
                desde == null ? cb.conjunction() : cb.greaterThanOrEqualTo(raiz.get("fechaCosecha"), desde),
                hasta == null ? cb.conjunction() : cb.lessThanOrEqualTo(raiz.get("fechaCosecha"), hasta));
    }
}
