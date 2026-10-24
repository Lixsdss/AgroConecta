package pe.agroconecta.administracion.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Ventas completadas por mes y cultivo (vista {@code v_ventas_mensuales_cultivo}, solo lectura). El mes es el de la
 * fecha en que la venta pasó a COMPLETADA.
 */
@Entity
@Immutable
@Table(name = "v_ventas_mensuales_cultivo")
public class VentaMensualCultivo {

    @EmbeddedId
    private VentaMensualCultivoId id;

    @Column(name = "cultivo")
    private String cultivo;

    @Column(name = "ventas_completadas")
    private Long ventasCompletadas;

    @Column(name = "kilos_vendidos")
    private BigDecimal kilosVendidos;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    /** Constructor requerido por JPA. */
    protected VentaMensualCultivo() {
    }

    /**
     * Devuelve el mes y cultivo.
     *
     * @return mes y cultivo
     */
    public VentaMensualCultivoId getId() {
        return id;
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
     * Devuelve las ventas completadas.
     *
     * @return ventas completadas
     */
    public Long getVentasCompletadas() {
        return ventasCompletadas;
    }

    /**
     * Devuelve los kilos vendidos.
     *
     * @return kilos vendidos
     */
    public BigDecimal getKilosVendidos() {
        return kilosVendidos;
    }

    /**
     * Devuelve el monto total en soles.
     *
     * @return monto total en soles
     */
    public BigDecimal getMontoTotal() {
        return montoTotal;
    }
}
