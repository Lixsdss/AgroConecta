package pe.agroconecta.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Provincia de la región Piura (tabla {@code provincias}). Catálogo de solo lectura cargado por V2.
 */
@Entity
@Immutable
@Table(name = "provincias")
public class Provincia {

    @Id
    @Column(name = "id_provincia")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    /** Constructor requerido por JPA. */
    protected Provincia() {
    }

    /**
     * Devuelve el id de la provincia.
     *
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Devuelve el nombre de la provincia.
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }
}
