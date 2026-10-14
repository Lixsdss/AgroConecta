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
 * Suspensión de un usuario (tabla {@code suspensiones}, RF-09). Al insertarla, {@code trg_suspensiones_ai} pasa al
 * usuario a SUSPENDIDO; al registrar {@code fecha_fin}, {@code trg_suspensiones_au} lo devuelve a ACTIVO. La fecha
 * de fin la registra {@code SuspensionRepository.levantar} con el reloj de la base de datos.
 */
@Entity
@Table(name = "suspensiones")
public class Suspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suspension")
    private Long id;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long idUsuario;

    @Column(name = "id_administrador", nullable = false, updatable = false)
    private Long idAdministrador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "motivo", nullable = false, length = 25, updatable = false)
    private MotivoSuspension motivo;

    @Column(name = "detalle", nullable = false, length = 300, updatable = false)
    private String detalle;

    @Column(name = "fecha_inicio", insertable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", insertable = false, updatable = false)
    private LocalDateTime fechaFin;

    /** Constructor requerido por JPA. */
    protected Suspension() {
    }

    /**
     * Crea una suspensión abierta.
     *
     * @param idUsuario       usuario suspendido
     * @param idAdministrador administrador que suspende
     * @param motivo          motivo de catálogo
     * @param detalle         justificación
     */
    public Suspension(Long idUsuario, Long idAdministrador, MotivoSuspension motivo, String detalle) {
        this.idUsuario = idUsuario;
        this.idAdministrador = idAdministrador;
        this.motivo = motivo;
        this.detalle = detalle;
    }

    /** @return id de la suspensión */
    public Long getId() {
        return id;
    }

    /** @return usuario suspendido */
    public Long getIdUsuario() {
        return idUsuario;
    }

    /** @return administrador que suspendió */
    public Long getIdAdministrador() {
        return idAdministrador;
    }

    /** @return motivo */
    public MotivoSuspension getMotivo() {
        return motivo;
    }

    /** @return justificación */
    public String getDetalle() {
        return detalle;
    }

    /** @return inicio de la suspensión */
    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    /** @return fecha de reactivación, o {@code null} si sigue vigente */
    public LocalDateTime getFechaFin() {
        return fechaFin;
    }
}
