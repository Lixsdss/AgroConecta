package pe.agroconecta.precio.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Último precio del mercado por cultivo y zona, en kilo y en la unidad local, con tendencia frente a la carga
 * anterior (vista {@code v_precios_vigentes}, solo lectura).
 */
@Entity
@Immutable
@IdClass(ClaveCultivoDistrito.class)
@Table(name = "v_precios_vigentes")
public class PrecioVigente {

    @Id
    @Column(name = "id_cultivo")
    private Integer idCultivo;

    @Id
    @Column(name = "id_distrito")
    private Integer idDistrito;

    @Column(name = "cultivo")
    private String cultivo;

    @Column(name = "zona")
    private String zona;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "precio_kg")
    private BigDecimal precioKg;

    @Column(name = "unidad_local")
    private String unidadLocal;

    @Column(name = "precio_unidad_local")
    private BigDecimal precioUnidadLocal;

    @Column(name = "tendencia")
    private String tendencia;

    @Column(name = "fuente")
    private String fuente;

    @Column(name = "actualizado")
    private LocalDateTime actualizado;

    /** Constructor requerido por JPA. */
    protected PrecioVigente() {
    }

    /** @return id del cultivo */
    public Integer getIdCultivo() {
        return idCultivo;
    }

    /** @return id de la zona */
    public Integer getIdDistrito() {
        return idDistrito;
    }

    /** @return nombre del cultivo */
    public String getCultivo() {
        return cultivo;
    }

    /** @return nombre de la zona */
    public String getZona() {
        return zona;
    }

    /** @return día del último precio */
    public LocalDate getFecha() {
        return fecha;
    }

    /** @return soles por kilo */
    public BigDecimal getPrecioKg() {
        return precioKg;
    }

    /** @return unidad local del cultivo */
    public String getUnidadLocal() {
        return unidadLocal;
    }

    /** @return soles por unidad local, o {@code null} si no hay peso de referencia */
    public BigDecimal getPrecioUnidadLocal() {
        return precioUnidadLocal;
    }

    /** @return SUBE, BAJA, IGUAL o SIN_DATO */
    public String getTendencia() {
        return tendencia;
    }

    /** @return fuente del precio */
    public String getFuente() {
        return fuente;
    }

    /** @return última carga o edición */
    public LocalDateTime getActualizado() {
        return actualizado;
    }
}
