package pe.agroconecta.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Variedad de un cultivo (tabla {@code variedades}). Catálogo de solo lectura cargado por V2; las publicaciones
 * guardan la variedad y de ella se obtiene el cultivo (3FN).
 */
@Entity
@Immutable
@Table(name = "variedades")
public class Variedad {

    @Id
    @Column(name = "id_variedad")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cultivo", nullable = false)
    private Cultivo cultivo;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    /** Constructor requerido por JPA. */
    protected Variedad() {
    }

    /**
     * Devuelve el id de la variedad.
     *
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Devuelve el cultivo de la variedad.
     *
     * @return cultivo
     */
    public Cultivo getCultivo() {
        return cultivo;
    }

    /**
     * Devuelve el nombre de la variedad.
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }
}
