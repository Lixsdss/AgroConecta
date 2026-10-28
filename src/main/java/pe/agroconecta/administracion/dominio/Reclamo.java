package pe.agroconecta.administracion.dominio;

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
 * Reclamo de una parte sobre una venta que llegó a ACORDADA (tabla {@code reclamos}, RF-09). Nace PENDIENTE
 * ({@code trg_reclamos_bi}) y el administrador lo resuelve una sola vez; {@code trg_reclamos_bu} fija la fecha.
 */
@Entity
@Table(name = "reclamos")
public class Reclamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reclamo")
    private Long id;

    @Column(name = "id_transaccion", nullable = false, updatable = false)
    private Long idTransaccion;

    @Column(name = "id_reclamante", nullable = false, updatable = false)
    private Long idReclamante;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "motivo", nullable = false, length = 25, updatable = false)
    private MotivoReclamo motivo;

    @Column(name = "descripcion", nullable = false, length = 300, updatable = false)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoReclamo estado;

    @Column(name = "id_administrador")
    private Long idAdministrador;

    @Column(name = "respuesta", length = 300)
    private String respuesta;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_resolucion", insertable = false, updatable = false)
    private LocalDateTime fechaResolucion;

    /** Constructor requerido por JPA. */
    protected Reclamo() {
    }

    /**
     * Crea un reclamo PENDIENTE.
     *
     * @param idTransaccion venta reclamada
     * @param idReclamante  parte que reclama
     * @param motivo        motivo de catálogo
     * @param descripcion   descripción
     */
    public Reclamo(Long idTransaccion, Long idReclamante, MotivoReclamo motivo, String descripcion) {
        this.idTransaccion = idTransaccion;
        this.idReclamante = idReclamante;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.estado = EstadoReclamo.PENDIENTE;
    }

    /**
     * Registra la resolución ({@code trg_reclamos_bu} fija la fecha de resolución).
     *
     * @param resultado       FUNDADO o INFUNDADO
     * @param idAdministrador administrador que resuelve
     * @param respuesta       respuesta para las partes
     */
    public void resolver(EstadoReclamo resultado, Long idAdministrador, String respuesta) {
        this.estado = resultado;
        this.idAdministrador = idAdministrador;
        this.respuesta = respuesta;
    }

    /**
     * Devuelve el id del reclamo.
     *
     * @return id del reclamo
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve la venta reclamada.
     *
     * @return venta reclamada
     */
    public Long getIdTransaccion() {
        return idTransaccion;
    }

    /**
     * Devuelve la parte que reclama.
     *
     * @return parte que reclama
     */
    public Long getIdReclamante() {
        return idReclamante;
    }

    /**
     * Devuelve el motivo.
     *
     * @return motivo
     */
    public MotivoReclamo getMotivo() {
        return motivo;
    }

    /**
     * Devuelve la descripción.
     *
     * @return descripción
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Devuelve el estado.
     *
     * @return estado
     */
    public EstadoReclamo getEstado() {
        return estado;
    }

    /**
     * Devuelve el administrador que resolvió, o {@code null}.
     *
     * @return administrador que resolvió, o {@code null}
     */
    public Long getIdAdministrador() {
        return idAdministrador;
    }

    /**
     * Devuelve la respuesta, o {@code null}.
     *
     * @return respuesta, o {@code null}
     */
    public String getRespuesta() {
        return respuesta;
    }

    /**
     * Devuelve la fecha y hora de registro.
     *
     * @return fecha y hora de registro
     */
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Devuelve la fecha y hora de resolución, o {@code null}.
     *
     * @return fecha y hora de resolución, o {@code null}
     */
    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }
}
