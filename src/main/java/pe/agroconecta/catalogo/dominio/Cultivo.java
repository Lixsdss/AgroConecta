package pe.agroconecta.catalogo.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Cultivo (tabla {@code cultivos}). Catálogo de solo lectura cargado por V2. Su vida útil sugiere la fecha
 * límite de las publicaciones (RF-07) y su unidad local es la unidad en que se comercializa en Piura (RF-02).
 */
@Entity
@Immutable
@Table(name = "cultivos")
public class Cultivo {

    @Id
    @Column(name = "id_cultivo")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "vida_util_dias", nullable = false)
    private Short vidaUtilDias;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad_local", nullable = false)
    private UnidadMedida unidadLocal;

    @Column(name = "peso_sugerido_kg", precision = 10, scale = 3)
    private BigDecimal pesoSugeridoKg;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    /** Constructor requerido por JPA. */
    protected Cultivo() {
    }

    /**
     * Devuelve el id del cultivo.
     *
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Devuelve el nombre del cultivo.
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve cuántos días dura el producto a la venta.
     *
     * @return vida útil en días
     */
    public Short getVidaUtilDias() {
        return vidaUtilDias;
    }

    /**
     * Devuelve la unidad en que se comercializa en Piura.
     *
     * @return unidad local
     */
    public UnidadMedida getUnidadLocal() {
        return unidadLocal;
    }

    /**
     * Devuelve el peso típico de la unidad local si es de peso variable.
     *
     * @return kilos sugeridos, o {@code null}
     */
    public BigDecimal getPesoSugeridoKg() {
        return pesoSugeridoKg;
    }

    /**
     * Indica si el cultivo se ofrece en la plataforma.
     *
     * @return {@code true} si está activo
     */
    public boolean isActivo() {
        return activo;
    }
}
