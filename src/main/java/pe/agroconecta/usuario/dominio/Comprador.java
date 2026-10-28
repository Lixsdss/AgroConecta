package pe.agroconecta.usuario.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Datos propios del comprador (tabla {@code compradores}); comparte el id con su {@link Usuario}.
 * {@code trg_compradores_bi} rechaza la fila si el usuario no tiene rol COMPRADOR.
 */
@Entity
@Table(name = "compradores")
public class Comprador {

    @Id
    @Column(name = "id_usuario")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_comprador", nullable = false, length = 20)
    private TipoComprador tipoComprador;

    @Column(name = "nombre_negocio", nullable = false, length = 120)
    private String nombreNegocio;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ruc", length = 11)
    private String ruc;

    /** Constructor requerido por JPA. */
    protected Comprador() {
    }

    /**
     * Crea los datos de comprador de un usuario.
     *
     * @param usuario       usuario con rol COMPRADOR
     * @param tipoComprador tipo de negocio
     * @param nombreNegocio nombre del negocio
     * @param ruc           RUC (opcional)
     */
    public Comprador(Usuario usuario, TipoComprador tipoComprador, String nombreNegocio, String ruc) {
        this.usuario = usuario;
        this.tipoComprador = tipoComprador;
        this.nombreNegocio = nombreNegocio;
        this.ruc = ruc;
    }

    /**
     * Actualiza los datos del negocio.
     *
     * @param tipoComprador tipo de negocio
     * @param nombreNegocio nombre del negocio
     * @param ruc           RUC (opcional)
     */
    public void actualizar(TipoComprador tipoComprador, String nombreNegocio, String ruc) {
        this.tipoComprador = tipoComprador;
        this.nombreNegocio = nombreNegocio;
        this.ruc = ruc;
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
     * Devuelve el tipo de negocio.
     *
     * @return tipo
     */
    public TipoComprador getTipoComprador() {
        return tipoComprador;
    }

    /**
     * Devuelve el nombre del negocio.
     *
     * @return nombre
     */
    public String getNombreNegocio() {
        return nombreNegocio;
    }

    /**
     * Devuelve el RUC.
     *
     * @return RUC, o {@code null}
     */
    public String getRuc() {
        return ruc;
    }
}
