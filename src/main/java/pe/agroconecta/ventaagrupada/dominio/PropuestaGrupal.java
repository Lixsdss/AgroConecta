package pe.agroconecta.ventaagrupada.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Propuesta conjunta de un comprador a un lote (tabla {@code propuestas_grupales}, RF-10): kilos, precio por kilo y
 * fecha de recojo, que también es el límite para responder. Sus partes son ventas PROPUESTA del módulo {@code venta}.
 */
@Entity
@Table(name = "propuestas_grupales")
public class PropuestaGrupal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propuesta_grupal")
    private Long id;

    @Column(name = "id_comprador", nullable = false, updatable = false)
    private Long idComprador;

    @Column(name = "id_variedad", nullable = false, updatable = false)
    private Integer idVariedad;

    @Column(name = "id_distrito", nullable = false, updatable = false)
    private Integer idDistrito;

    @Column(name = "cantidad_kg", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal cantidadKg;

    @Column(name = "precio_kg", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal precioKg;

    @Column(name = "fecha_recojo", nullable = false, updatable = false)
    private LocalDate fechaRecojo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoPropuesta estado;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Constructor requerido por JPA. */
    protected PropuestaGrupal() {
    }

    /**
     * Crea una propuesta ABIERTA.
     *
     * @param idComprador comprador
     * @param idVariedad  variedad del lote
     * @param idDistrito  distrito del lote
     * @param cantidadKg  kilos pedidos
     * @param precioKg    soles por kilo
     * @param fechaRecojo día de recojo
     */
    public PropuestaGrupal(Long idComprador, Integer idVariedad, Integer idDistrito, BigDecimal cantidadKg,
            BigDecimal precioKg, LocalDate fechaRecojo) {
        this.idComprador = idComprador;
        this.idVariedad = idVariedad;
        this.idDistrito = idDistrito;
        this.cantidadKg = cantidadKg;
        this.precioKg = precioKg;
        this.fechaRecojo = fechaRecojo;
        this.estado = EstadoPropuesta.ABIERTA;
    }

    /**
     * Cierra o cancela la propuesta.
     *
     * @param nuevo {@link EstadoPropuesta#CERRADA} o {@link EstadoPropuesta#CANCELADA}
     */
    public void terminar(EstadoPropuesta nuevo) {
        this.estado = nuevo;
    }

    /** @return id de la propuesta */
    public Long getId() {
        return id;
    }

    /** @return comprador */
    public Long getIdComprador() {
        return idComprador;
    }

    /** @return variedad del lote */
    public Integer getIdVariedad() {
        return idVariedad;
    }

    /** @return distrito del lote */
    public Integer getIdDistrito() {
        return idDistrito;
    }

    /** @return kilos pedidos */
    public BigDecimal getCantidadKg() {
        return cantidadKg;
    }

    /** @return soles por kilo */
    public BigDecimal getPrecioKg() {
        return precioKg;
    }

    /** @return día de recojo */
    public LocalDate getFechaRecojo() {
        return fechaRecojo;
    }

    /** @return estado */
    public EstadoPropuesta getEstado() {
        return estado;
    }

    /** @return fecha y hora de creación (la pone la base de datos) */
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
