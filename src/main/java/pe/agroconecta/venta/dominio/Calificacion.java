package pe.agroconecta.venta.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Calificación de 1 a 5 estrellas que una parte da a la otra después de una venta COMPLETADA (tabla
 * {@code calificaciones}). El evaluado no se guarda: es la otra parte de la venta. No se edita ni se elimina.
 */
@Entity
@Table(name = "calificaciones")
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calificacion")
    private Long id;

    @Column(name = "id_transaccion", nullable = false, updatable = false)
    private Long idTransaccion;

    @Column(name = "id_evaluador", nullable = false, updatable = false)
    private Long idEvaluador;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "puntuacion", nullable = false, updatable = false)
    private Integer puntuacion;

    @Column(name = "comentario", length = 500, updatable = false)
    private String comentario;

    @Column(name = "fecha", insertable = false, updatable = false)
    private LocalDateTime fecha;

    /** Constructor requerido por JPA. */
    protected Calificacion() {
    }

    /**
     * Crea una calificación.
     *
     * @param idTransaccion venta calificada
     * @param idEvaluador   parte que califica
     * @param puntuacion    estrellas (1 a 5)
     * @param comentario    comentario opcional, o {@code null}
     */
    public Calificacion(Long idTransaccion, Long idEvaluador, Integer puntuacion, String comentario) {
        this.idTransaccion = idTransaccion;
        this.idEvaluador = idEvaluador;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
    }

    /**
     * Devuelve el id de la calificación.
     *
     * @return id de la calificación
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve la venta calificada.
     *
     * @return venta calificada
     */
    public Long getIdTransaccion() {
        return idTransaccion;
    }

    /**
     * Devuelve la parte que califica.
     *
     * @return parte que califica
     */
    public Long getIdEvaluador() {
        return idEvaluador;
    }

    /**
     * Devuelve las estrellas (1 a 5).
     *
     * @return estrellas (1 a 5)
     */
    public Integer getPuntuacion() {
        return puntuacion;
    }

    /**
     * Devuelve el comentario, o {@code null}.
     *
     * @return comentario, o {@code null}
     */
    public String getComentario() {
        return comentario;
    }

    /**
     * Devuelve la fecha y hora (la pone la base de datos).
     *
     * @return fecha y hora (la pone la base de datos)
     */
    public LocalDateTime getFecha() {
        return fecha;
    }
}
