package pe.agroconecta.catalogo.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Unidad de venta (tabla {@code unidades_medida}). Catálogo de solo lectura cargado por V2: kilo, jaba, saco,
 * quintal y tonelada. {@code factor_kg} es {@code null} en las de peso variable (jaba, saco).
 */
@Entity
@Immutable
@Table(name = "unidades_medida")
public class UnidadMedida {

    @Id
    @Column(name = "id_unidad")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    @Column(name = "nombre_plural", nullable = false, length = 30)
    private String nombrePlural;

    @Column(name = "factor_kg", precision = 10, scale = 3)
    private BigDecimal factorKg;

    /** Constructor requerido por JPA. */
    protected UnidadMedida() {
    }

    /**
     * Devuelve el id de la unidad.
     *
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Devuelve el nombre en singular (por ejemplo, "Jaba").
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve el nombre en plural (por ejemplo, "jabas").
     *
     * @return nombre en plural
     */
    public String getNombrePlural() {
        return nombrePlural;
    }

    /**
     * Devuelve cuántos kilos pesa una unidad.
     *
     * @return kilos por unidad, o {@code null} si el peso es variable
     */
    public BigDecimal getFactorKg() {
        return factorKg;
    }
}
