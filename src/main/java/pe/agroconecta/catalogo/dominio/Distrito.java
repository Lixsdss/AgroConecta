package pe.agroconecta.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Distrito de la región Piura (tabla {@code distritos}). Catálogo de solo lectura cargado por V2; los
 * distritos con {@code es_zona_precio} tienen precio de referencia diario (RF-02).
 */
@Entity
@Immutable
@Table(name = "distritos")
public class Distrito {

    @Id
    @Column(name = "id_distrito")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_provincia", nullable = false)
    private Provincia provincia;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ubigeo", nullable = false, length = 6)
    private String ubigeo;

    @Column(name = "es_zona_precio", nullable = false)
    private boolean esZonaPrecio;

    /** Constructor requerido por JPA. */
    protected Distrito() {
    }

    /**
     * Devuelve el id del distrito.
     *
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Devuelve la provincia del distrito.
     *
     * @return provincia
     */
    public Provincia getProvincia() {
        return provincia;
    }

    /**
     * Devuelve el nombre del distrito.
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve el código ubigeo del INEI (6 dígitos).
     *
     * @return ubigeo
     */
    public String getUbigeo() {
        return ubigeo;
    }

    /**
     * Indica si el distrito es una zona de precio (RF-02).
     *
     * @return {@code true} si tiene precio de referencia diario
     */
    public boolean isEsZonaPrecio() {
        return esZonaPrecio;
    }
}
