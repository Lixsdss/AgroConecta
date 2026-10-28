package pe.agroconecta.venta.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Reputación de un usuario: promedio y total de las calificaciones que recibió (vista {@code v_reputacion_usuario},
 * solo lectura). Solo aparecen los usuarios con al menos una calificación.
 */
@Entity
@Immutable
@Table(name = "v_reputacion_usuario")
public class ReputacionUsuario {

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "total_calificaciones")
    private Long total;

    @Column(name = "promedio_puntuacion")
    private BigDecimal promedio;

    /** Constructor requerido por JPA. */
    protected ReputacionUsuario() {
    }

    /**
     * Devuelve el id del usuario.
     *
     * @return id del usuario
     */
    public Long getIdUsuario() {
        return idUsuario;
    }

    /**
     * Devuelve la cantidad de calificaciones recibidas.
     *
     * @return cantidad de calificaciones recibidas
     */
    public Long getTotal() {
        return total;
    }

    /**
     * Devuelve el promedio de estrellas, con un decimal.
     *
     * @return promedio de estrellas, con un decimal
     */
    public BigDecimal getPromedio() {
        return promedio;
    }
}
