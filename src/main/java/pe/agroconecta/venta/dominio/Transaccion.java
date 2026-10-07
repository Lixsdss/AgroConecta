package pe.agroconecta.venta.dominio;

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
 * Venta entre el comprador y el agricultor de una conversación (tabla {@code transacciones}). Los términos (unidad,
 * cantidad, peso y precio) no cambian; solo avanza el estado, y cada cambio fija {@code id_actualizado_por}.
 * {@code cantidad_kg} y {@code monto_total} son columnas generadas; {@code trg_transacciones_bi} puede fijar el peso
 * por unidad.
 */
@Entity
@Table(name = "transacciones")
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long id;

    @Column(name = "id_conversacion", nullable = false, updatable = false)
    private Long idConversacion;

    @Column(name = "id_propuesta_grupal", updatable = false)
    private Long idPropuestaGrupal;

    @Column(name = "id_unidad", nullable = false, updatable = false)
    private Integer idUnidad;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "peso_unidad_kg", nullable = false, precision = 10, scale = 3, updatable = false)
    private BigDecimal pesoUnidadKg;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal precioUnitario;

    @Column(name = "cantidad_kg", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal cantidadKg;

    @Column(name = "monto_total", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal montoTotal;

    @Column(name = "fecha_recojo", nullable = false, updatable = false)
    private LocalDate fechaRecojo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoTransaccion estado;

    @Column(name = "id_actualizado_por")
    private Long idActualizadoPor;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    /** Constructor requerido por JPA. */
    protected Transaccion() {
    }

    /**
     * Crea el acuerdo del chat, que nace ACORDADA.
     *
     * @param idConversacion conversación
     * @param idUnidad       unidad acordada
     * @param cantidad       cantidad en esa unidad
     * @param pesoUnidadKg   kilos por unidad
     * @param precioUnitario soles por unidad
     * @param fechaRecojo    día de recojo
     * @param idRegistra     parte que registra el acuerdo
     * @return la venta, sin guardar
     */
    public static Transaccion acordada(Long idConversacion, Integer idUnidad, BigDecimal cantidad,
            BigDecimal pesoUnidadKg, BigDecimal precioUnitario, LocalDate fechaRecojo, Long idRegistra) {
        Transaccion t = new Transaccion();
        t.idConversacion = idConversacion;
        t.idUnidad = idUnidad;
        t.cantidad = cantidad;
        t.pesoUnidadKg = pesoUnidadKg;
        t.precioUnitario = precioUnitario;
        t.fechaRecojo = fechaRecojo;
        t.estado = EstadoTransaccion.ACORDADA;
        t.idActualizadoPor = idRegistra;
        return t;
    }

    /**
     * Cambia el estado y registra quién lo hizo ({@code trg_transacciones_bu} valida el cambio).
     *
     * @param nuevo     nuevo estado
     * @param idUsuario parte que hace el cambio
     */
    public void cambiarEstado(EstadoTransaccion nuevo, Long idUsuario) {
        this.estado = nuevo;
        this.idActualizadoPor = idUsuario;
    }

    /** @return id de la venta */
    public Long getId() {
        return id;
    }

    /** @return conversación de la que nace */
    public Long getIdConversacion() {
        return idConversacion;
    }

    /** @return propuesta conjunta, o {@code null} si nace del chat */
    public Long getIdPropuestaGrupal() {
        return idPropuestaGrupal;
    }

    /** @return unidad acordada */
    public Integer getIdUnidad() {
        return idUnidad;
    }

    /** @return cantidad en la unidad acordada */
    public BigDecimal getCantidad() {
        return cantidad;
    }

    /** @return kilos por unidad */
    public BigDecimal getPesoUnidadKg() {
        return pesoUnidadKg;
    }

    /** @return soles por unidad */
    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    /** @return kilos acordados (los calcula la base de datos) */
    public BigDecimal getCantidadKg() {
        return cantidadKg;
    }

    /** @return monto total (lo calcula la base de datos) */
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

    /** @return autor del último cambio ({@code null}: proceso automático) */
    public Long getIdActualizadoPor() {
        return idActualizadoPor;
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
