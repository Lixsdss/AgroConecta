package pe.agroconecta.usuario.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Datos propios del agricultor (tabla {@code agricultores}); comparte el id con su {@link Usuario}.
 * {@code trg_agricultores_bi} rechaza la fila si el usuario no tiene rol AGRICULTOR.
 */
@Entity
@Table(name = "agricultores")
public class Agricultor {

    @Id
    @Column(name = "id_usuario")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "nombre_chacra", length = 80)
    private String nombreChacra;

    /** Constructor requerido por JPA. */
    protected Agricultor() {
    }

    /**
     * Crea los datos de agricultor de un usuario.
     *
     * @param usuario      usuario con rol AGRICULTOR
     * @param nombreChacra nombre de la chacra (opcional)
     */
    public Agricultor(Usuario usuario, String nombreChacra) {
        this.usuario = usuario;
        this.nombreChacra = nombreChacra;
    }

    /**
     * Actualiza el nombre de la chacra.
     *
     * @param nombreChacra nuevo nombre (opcional)
     */
    public void actualizar(String nombreChacra) {
        this.nombreChacra = nombreChacra;
    }

    /**
     * Devuelve el id (el mismo del usuario).
     *
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el nombre de la chacra.
     *
     * @return nombre, o {@code null}
     */
    public String getNombreChacra() {
        return nombreChacra;
    }
}
