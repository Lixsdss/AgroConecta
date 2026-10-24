package pe.agroconecta.mensajeria.dominio;

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
 * Conversación entre el comprador y el agricultor de una publicación (tabla {@code conversaciones}). Una por
 * publicación y comprador ({@code uk_conversacion_pub_comprador}); nunca se cierra. {@code trg_conversaciones_bi}
 * exige que la publicación esté PUBLICADA al crearla.
 */
@Entity
@Table(name = "conversaciones")
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Long id;

    @Column(name = "id_publicacion", nullable = false, updatable = false)
    private Long idPublicacion;

    @Column(name = "id_comprador", nullable = false, updatable = false)
    private Long idComprador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "canal_inicio", nullable = false, length = 18, updatable = false)
    private CanalInicio canalInicio;

    @Column(name = "clics_whatsapp", nullable = false)
    private int clicsWhatsapp;

    @Column(name = "fecha_inicio", insertable = false, updatable = false)
    private LocalDateTime fechaInicio;

    /** Constructor requerido por JPA. */
    protected Conversacion() {
    }

    /**
     * Crea una conversación.
     *
     * @param idPublicacion publicación
     * @param idComprador   comprador que inicia el contacto
     * @param canalInicio   canal por el que empezó
     */
    public Conversacion(Long idPublicacion, Long idComprador, CanalInicio canalInicio) {
        this.idPublicacion = idPublicacion;
        this.idComprador = idComprador;
        this.canalInicio = canalInicio;
    }

    /** Suma un clic en "WhatsApp" (lead, RF-04). */
    public void sumarClicWhatsapp() {
        clicsWhatsapp++;
    }

    /**
     * Devuelve el id de la conversación.
     *
     * @return id de la conversación
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el id de la publicación.
     *
     * @return id de la publicación
     */
    public Long getIdPublicacion() {
        return idPublicacion;
    }

    /**
     * Devuelve el id del comprador.
     *
     * @return id del comprador
     */
    public Long getIdComprador() {
        return idComprador;
    }

    /**
     * Devuelve el canal por el que empezó.
     *
     * @return canal por el que empezó
     */
    public CanalInicio getCanalInicio() {
        return canalInicio;
    }

    /**
     * Devuelve los clics en "WhatsApp".
     *
     * @return clics en "WhatsApp"
     */
    public int getClicsWhatsapp() {
        return clicsWhatsapp;
    }

    /**
     * Devuelve la fecha y hora de inicio (la pone la base de datos).
     *
     * @return fecha y hora de inicio (la pone la base de datos)
     */
    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }
}
