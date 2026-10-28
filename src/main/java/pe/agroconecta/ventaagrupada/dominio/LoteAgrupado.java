package pe.agroconecta.ventaagrupada.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Lote de la venta agrupada (vista {@code v_lotes_agrupados}, solo lectura): cosechas PUBLICADA con kilos disponibles
 * del mismo cultivo, variedad y distrito, de 2 o más agricultores.
 */
@Entity
@Immutable
@IdClass(ClaveLote.class)
@Table(name = "v_lotes_agrupados")
public class LoteAgrupado {

    @Id
    @Column(name = "id_variedad")
    private Integer idVariedad;

    @Id
    @Column(name = "id_distrito")
    private Integer idDistrito;

    @Column(name = "id_cultivo")
    private Integer idCultivo;

    @Column(name = "cultivo")
    private String cultivo;

    @Column(name = "variedad")
    private String variedad;

    @Column(name = "distrito")
    private String distrito;

    @Column(name = "total_agricultores")
    private Long totalAgricultores;

    @Column(name = "total_publicaciones")
    private Long totalPublicaciones;

    @Column(name = "volumen_kg")
    private BigDecimal volumenKg;

    @Column(name = "precio_promedio_kg")
    private BigDecimal precioPromedioKg;

    @Column(name = "fecha_limite_proxima")
    private LocalDate fechaLimiteProxima;

    /** Constructor requerido por JPA. */
    protected LoteAgrupado() {
    }

    /**
     * Devuelve la variedad.
     *
     * @return variedad
     */
    public Integer getIdVariedad() {
        return idVariedad;
    }

    /**
     * Devuelve el distrito.
     *
     * @return distrito
     */
    public Integer getIdDistrito() {
        return idDistrito;
    }

    /**
     * Devuelve el cultivo.
     *
     * @return cultivo
     */
    public Integer getIdCultivo() {
        return idCultivo;
    }

    /**
     * Devuelve el nombre del cultivo.
     *
     * @return nombre del cultivo
     */
    public String getCultivo() {
        return cultivo;
    }

    /**
     * Devuelve el nombre de la variedad.
     *
     * @return nombre de la variedad
     */
    public String getVariedad() {
        return variedad;
    }

    /**
     * Devuelve el nombre del distrito.
     *
     * @return nombre del distrito
     */
    public String getDistrito() {
        return distrito;
    }

    /**
     * Devuelve los agricultores distintos del lote.
     *
     * @return agricultores distintos del lote
     */
    public Long getTotalAgricultores() {
        return totalAgricultores;
    }

    /**
     * Devuelve las publicaciones del lote.
     *
     * @return publicaciones del lote
     */
    public Long getTotalPublicaciones() {
        return totalPublicaciones;
    }

    /**
     * Devuelve los kilos disponibles del lote.
     *
     * @return kilos disponibles del lote
     */
    public BigDecimal getVolumenKg() {
        return volumenKg;
    }

    /**
     * Devuelve el precio por kilo promedio, ponderado por los kilos.
     *
     * @return precio por kilo promedio, ponderado por los kilos
     */
    public BigDecimal getPrecioPromedioKg() {
        return precioPromedioKg;
    }

    /**
     * Devuelve la fecha límite más próxima del lote.
     *
     * @return la fecha límite más próxima del lote
     */
    public LocalDate getFechaLimiteProxima() {
        return fechaLimiteProxima;
    }
}
