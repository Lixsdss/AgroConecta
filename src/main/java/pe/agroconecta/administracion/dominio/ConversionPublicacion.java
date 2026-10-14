package pe.agroconecta.administracion.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Embudo de una publicación: clics de WhatsApp, contactos, ofertas, acuerdos y ventas completadas (vista
 * {@code v_conversion_publicacion}, solo lectura).
 */
@Entity
@Immutable
@Table(name = "v_conversion_publicacion")
public class ConversionPublicacion {

    @Id
    @Column(name = "id_publicacion")
    private Long idPublicacion;

    @Column(name = "id_agricultor")
    private Long idAgricultor;

    /** {@code SUM} de un entero: la base de datos lo devuelve como decimal. */
    @Column(name = "clics_whatsapp")
    private BigDecimal clicsWhatsapp;

    @Column(name = "contactos")
    private Long contactos;

    @Column(name = "ofertas")
    private Long ofertas;

    @Column(name = "acuerdos")
    private Long acuerdos;

    @Column(name = "completadas")
    private Long completadas;

    /** Constructor requerido por JPA. */
    protected ConversionPublicacion() {
    }

    /** @return publicación */
    public Long getIdPublicacion() {
        return idPublicacion;
    }

    /** @return agricultor */
    public Long getIdAgricultor() {
        return idAgricultor;
    }

    /** @return clics en el botón de WhatsApp */
    public BigDecimal getClicsWhatsapp() {
        return clicsWhatsapp;
    }

    /** @return conversaciones abiertas */
    public Long getContactos() {
        return contactos;
    }

    /** @return mensajes OFERTA */
    public Long getOfertas() {
        return ofertas;
    }

    /** @return ventas que llegaron a acordarse y siguen en curso o terminaron */
    public Long getAcuerdos() {
        return acuerdos;
    }

    /** @return ventas COMPLETADA */
    public Long getCompletadas() {
        return completadas;
    }
}
