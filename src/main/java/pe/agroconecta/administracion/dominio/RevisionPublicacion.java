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
 * Decisión de moderación sobre una publicación (tabla {@code revisiones_publicacion}, RF-09). Nunca se modifica.
 *
 * <p>La publicación y el administrador son de otros módulos y se referencian por id. Al insertarla,
 * {@code trg_revisiones_ai} aplica la decisión a la publicación, borra su marca y avisa al agricultor.</p>
 */
@Entity
@Table(name = "revisiones_publicacion")
public class RevisionPublicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_revision")
    private Long id;

    @Column(name = "id_publicacion", nullable = false, updatable = false)
    private Long idPublicacion;

    @Column(name = "id_administrador", nullable = false, updatable = false)
    private Long idAdministrador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "decision", nullable = false, length = 12, updatable = false)
    private DecisionModeracion decision;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "motivo", length = 25, updatable = false)
    private MotivoModeracion motivo;

    @Column(name = "mensaje", length = 300, updatable = false)
    private String mensaje;

    @Column(name = "fecha", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fecha;

    /** Constructor requerido por JPA. */
    protected RevisionPublicacion() {
    }

    /**
     * Crea una decisión de moderación.
     *
     * @param idPublicacion   id de la publicación (módulo {@code publicacion})
     * @param idAdministrador id del administrador (módulo {@code usuario})
     * @param decision        decisión
     * @param motivo          motivo de catálogo, o {@code null} en CONFORME y DESBLOQUEO
     * @param mensaje         mensaje para el agricultor, o {@code null}
     */
    public RevisionPublicacion(Long idPublicacion, Long idAdministrador, DecisionModeracion decision,
            MotivoModeracion motivo, String mensaje) {
        this.idPublicacion = idPublicacion;
        this.idAdministrador = idAdministrador;
        this.decision = decision;
        this.motivo = motivo;
        this.mensaje = mensaje;
    }

    /** @return id de la revisión */
    public Long getId() {
        return id;
    }

    /** @return id de la publicación */
    public Long getIdPublicacion() {
        return idPublicacion;
    }

    /** @return id del administrador */
    public Long getIdAdministrador() {
        return idAdministrador;
    }

    /** @return decisión */
    public DecisionModeracion getDecision() {
        return decision;
    }

    /** @return motivo, o {@code null} */
    public MotivoModeracion getMotivo() {
        return motivo;
    }

    /** @return mensaje al agricultor, o {@code null} */
    public String getMensaje() {
        return mensaje;
    }

    /** @return fecha y hora de la decisión (la fija la base de datos) */
    public LocalDateTime getFecha() {
        return fecha;
    }
}
