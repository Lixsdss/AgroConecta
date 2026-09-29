package pe.agroconecta.precio.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Rango de precios por kilo de las ventas COMPLETADA de los últimos 30 días, por cultivo y distrito de la
 * publicación (vista {@code v_rango_precio_plataforma}, solo lectura).
 */
@Entity
@Immutable
@IdClass(ClaveCultivoDistrito.class)
@Table(name = "v_rango_precio_plataforma")
public class RangoPrecioPlataforma {

    @Id
    @Column(name = "id_cultivo")
    private Integer idCultivo;

    @Id
    @Column(name = "id_distrito")
    private Integer idDistrito;

    @Column(name = "precio_kg_minimo")
    private BigDecimal minimoKg;

    @Column(name = "precio_kg_maximo")
    private BigDecimal maximoKg;

    @Column(name = "precio_kg_promedio")
    private BigDecimal promedioKg;

    @Column(name = "total_ventas")
    private Long ventas;

    /** Constructor requerido por JPA. */
    protected RangoPrecioPlataforma() {
    }

    /** @return precio por kilo mínimo */
    public BigDecimal getMinimoKg() {
        return minimoKg;
    }

    /** @return precio por kilo máximo */
    public BigDecimal getMaximoKg() {
        return maximoKg;
    }

    /** @return precio por kilo promedio ponderado por kilos */
    public BigDecimal getPromedioKg() {
        return promedioKg;
    }

    /** @return número de ventas consideradas */
    public Long getVentas() {
        return ventas;
    }
}
