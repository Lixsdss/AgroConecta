package pe.agroconecta.precio.dominio;

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
 * Precio de referencia del mercado real, en soles por kilo, de un cultivo en una zona de precio y un día
 * (tabla {@code precios_mercado}, RF-02). Uno por cultivo, zona y fecha.
 *
 * <p>El cultivo, el distrito y el administrador son de otros módulos y se referencian por id.
 * {@code trg_precios_mercado_bi} y {@code _bu} exigen un ADMINISTRADOR y una zona de precio.</p>
 */
@Entity
@Table(name = "precios_mercado")
public class PrecioMercado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_precio")
    private Long id;

    @Column(name = "id_cultivo", nullable = false, updatable = false)
    private Integer idCultivo;

    @Column(name = "id_distrito", nullable = false, updatable = false)
    private Integer idDistrito;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "precio_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioKg;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "fuente", nullable = false, length = 25)
    private FuentePrecio fuente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "metodo", nullable = false, length = 10, updatable = false)
    private MetodoCarga metodo;

    @Column(name = "id_administrador", nullable = false)
    private Long idAdministrador;

    @Column(name = "fecha_registro", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    /** Constructor requerido por JPA. */
    protected PrecioMercado() {
    }

    /**
     * Crea un precio de referencia.
     *
     * @param idCultivo       id del cultivo
     * @param idDistrito      id de la zona de precio
     * @param fecha           día del precio
     * @param precioKg        soles por kilo
     * @param fuente          fuente
     * @param metodo          formulario o archivo
     * @param idAdministrador id del administrador que carga
     */
    public PrecioMercado(Integer idCultivo, Integer idDistrito, LocalDate fecha, BigDecimal precioKg,
            FuentePrecio fuente, MetodoCarga metodo, Long idAdministrador) {
        this.idCultivo = idCultivo;
        this.idDistrito = idDistrito;
        this.fecha = fecha;
        this.precioKg = precioKg;
        this.fuente = fuente;
        this.metodo = metodo;
        this.idAdministrador = idAdministrador;
    }

    /**
     * Corrige el precio ("Editar"). Quien edita queda como responsable del precio vigente.
     *
     * @param nuevoPrecioKg   soles por kilo
     * @param nuevaFuente     fuente
     * @param idAdministrador id del administrador que edita
     */
    public void editar(BigDecimal nuevoPrecioKg, FuentePrecio nuevaFuente, Long idAdministrador) {
        this.precioKg = nuevoPrecioKg;
        this.fuente = nuevaFuente;
        this.idAdministrador = idAdministrador;
    }

    /** @return id del precio */
    public Long getId() {
        return id;
    }

    /** @return id del cultivo */
    public Integer getIdCultivo() {
        return idCultivo;
    }

    /** @return id de la zona de precio */
    public Integer getIdDistrito() {
        return idDistrito;
    }

    /** @return día del precio */
    public LocalDate getFecha() {
        return fecha;
    }

    /** @return soles por kilo */
    public BigDecimal getPrecioKg() {
        return precioKg;
    }

    /** @return fuente */
    public FuentePrecio getFuente() {
        return fuente;
    }

    /** @return método de carga */
    public MetodoCarga getMetodo() {
        return metodo;
    }

    /** @return id del administrador responsable */
    public Long getIdAdministrador() {
        return idAdministrador;
    }

    /** @return fecha y hora de registro (la fija la base de datos) */
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    /** @return fecha y hora de la última edición, o {@code null} */
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
