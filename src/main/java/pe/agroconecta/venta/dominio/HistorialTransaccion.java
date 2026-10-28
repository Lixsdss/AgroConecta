package pe.agroconecta.venta.dominio;

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
 * Paso de la línea de tiempo de una venta (tabla {@code transacciones_historial}). Lo escriben
 * {@code trg_transacciones_ai/au}; la aplicación solo lo lee y nunca lo corrige.
 */
@Entity
@Immutable
@Table(name = "transacciones_historial")
public class HistorialTransaccion {

    @Id
    @Column(name = "id_historial")
    private Long id;

    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado")
    private EstadoTransaccion estado;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    /** Constructor requerido por JPA. */
    protected HistorialTransaccion() {
    }

    /**
     * Devuelve el id del paso.
     *
     * @return id del paso
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve la venta.
     *
     * @return venta
     */
    public Long getIdTransaccion() {
        return idTransaccion;
    }

    /**
     * Devuelve el estado al que pasó.
     *
     * @return estado al que pasó
     */
    public EstadoTransaccion getEstado() {
        return estado;
    }

    /**
     * Devuelve el autor ({@code null}: proceso automático).
     *
     * @return autor ({@code null}: proceso automático)
     */
    public Long getIdUsuario() {
        return idUsuario;
    }

    /**
     * Devuelve la fecha y hora.
     *
     * @return fecha y hora
     */
    public LocalDateTime getFecha() {
        return fecha;
    }
}
