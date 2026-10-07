package pe.agroconecta.venta.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Venta con sus partes, cultivo, unidad, kilos, precios y monto (vista {@code v_historial_transacciones}, solo
 * lectura). Base de "Mis ventas" y "Mis compras" (UC-08) y de la exportación a Excel.
 */
@Entity
@Immutable
@Table(name = "v_historial_transacciones")
public class VentaHistorial {

    @Id
    @Column(name = "id_transaccion")
    private Long id;

    @Column(name = "id_comprador")
    private Long idComprador;

    /** Nombre del negocio del comprador. */
    @Column(name = "comprador")
    private String comprador;

    @Column(name = "id_agricultor")
    private Long idAgricultor;

    @Column(name = "agricultor")
    private String agricultor;

    @Column(name = "id_propuesta_grupal")
    private Long idPropuestaGrupal;

    @Column(name = "cultivo")
    private String cultivo;

    @Column(name = "variedad")
    private String variedad;

    @Column(name = "cantidad")
    private BigDecimal cantidad;

    /** Nombre en plural de la unidad acordada (la vista lo llama {@code unidad}). */
    @Column(name = "unidad")
    private String unidadPlural;

    @Column(name = "cantidad_kg")
    private BigDecimal cantidadKg;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "precio_kg")
    private BigDecimal precioKg;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    @Column(name = "fecha_recojo")
    private LocalDate fechaRecojo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado")
    private EstadoTransaccion estado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /** Constructor requerido por JPA. */
    protected VentaHistorial() {
    }

    /** @return id de la venta */
    public Long getId() {
        return id;
    }

    /** @return id del comprador */
    public Long getIdComprador() {
        return idComprador;
    }

    /** @return nombre del negocio del comprador */
    public String getComprador() {
        return comprador;
    }

    /** @return id del agricultor */
    public Long getIdAgricultor() {
        return idAgricultor;
    }

    /** @return nombre del agricultor */
    public String getAgricultor() {
        return agricultor;
    }

    /** @return propuesta conjunta, o {@code null} */
    public Long getIdPropuestaGrupal() {
        return idPropuestaGrupal;
    }

    /** @return cultivo */
    public String getCultivo() {
        return cultivo;
    }

    /** @return variedad */
    public String getVariedad() {
        return variedad;
    }

    /** @return cantidad en la unidad acordada */
    public BigDecimal getCantidad() {
        return cantidad;
    }

    /** @return unidad acordada en plural */
    public String getUnidadPlural() {
        return unidadPlural;
    }

    /** @return kilos acordados */
    public BigDecimal getCantidadKg() {
        return cantidadKg;
    }

    /** @return soles por unidad */
    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    /** @return soles por kilo */
    public BigDecimal getPrecioKg() {
        return precioKg;
    }

    /** @return monto total */
    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    /** @return día de recojo */
    public LocalDate getFechaRecojo() {
        return fechaRecojo;
    }

    /** @return estado */
    public EstadoTransaccion getEstado() {
        return estado;
    }

    /** @return fecha y hora del acuerdo */
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /** @return fecha y hora del último cambio */
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
