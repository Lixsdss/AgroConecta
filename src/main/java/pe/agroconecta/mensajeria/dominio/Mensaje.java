package pe.agroconecta.mensajeria.dominio;

import java.math.BigDecimal;
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
 * Mensaje de una conversación (tabla {@code mensajes}): texto u oferta ({@code chk_mensaje_tipo}).
 * {@code trg_mensajes_bi} exige que el remitente participe y que las ofertas se hagan sobre una publicación
 * PUBLICADA.
 */
@Entity
@Table(name = "mensajes")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long id;

    @Column(name = "id_conversacion", nullable = false, updatable = false)
    private Long idConversacion;

    @Column(name = "id_remitente", nullable = false, updatable = false)
    private Long idRemitente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 6, updatable = false)
    private TipoMensaje tipo;

    @Column(name = "contenido", length = 1000, updatable = false)
    private String contenido;

    @Column(name = "oferta_cantidad", precision = 12, scale = 2, updatable = false)
    private BigDecimal ofertaCantidad;

    @Column(name = "oferta_precio_unitario", precision = 10, scale = 2, updatable = false)
    private BigDecimal ofertaPrecioUnitario;

    @Column(name = "leido", nullable = false)
    private boolean leido;

    @Column(name = "fecha_envio", insertable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    /** Constructor requerido por JPA. */
    protected Mensaje() {
    }

    private Mensaje(Long idConversacion, Long idRemitente, TipoMensaje tipo, String contenido,
            BigDecimal ofertaCantidad, BigDecimal ofertaPrecioUnitario) {
        this.idConversacion = idConversacion;
        this.idRemitente = idRemitente;
        this.tipo = tipo;
        this.contenido = contenido;
        this.ofertaCantidad = ofertaCantidad;
        this.ofertaPrecioUnitario = ofertaPrecioUnitario;
    }

    /**
     * Crea un mensaje de texto.
     *
     * @param idConversacion conversación
     * @param idRemitente    quien escribe
     * @param contenido      texto
     * @return mensaje
     */
    public static Mensaje texto(Long idConversacion, Long idRemitente, String contenido) {
        return new Mensaje(idConversacion, idRemitente, TipoMensaje.TEXTO, contenido, null, null);
    }

    /**
     * Crea una oferta.
     *
     * @param idConversacion conversación
     * @param idRemitente    quien oferta
     * @param cantidad       cantidad en la unidad de la publicación
     * @param precioUnitario soles por unidad
     * @return mensaje
     */
    public static Mensaje oferta(Long idConversacion, Long idRemitente, BigDecimal cantidad,
            BigDecimal precioUnitario) {
        return new Mensaje(idConversacion, idRemitente, TipoMensaje.OFERTA, null, cantidad, precioUnitario);
    }

    /**
     * Devuelve el id del mensaje.
     *
     * @return id del mensaje
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el id de la conversación.
     *
     * @return id de la conversación
     */
    public Long getIdConversacion() {
        return idConversacion;
    }

    /**
     * Devuelve el id de quien lo envió.
     *
     * @return id de quien lo envió
     */
    public Long getIdRemitente() {
        return idRemitente;
    }

    /**
     * Devuelve el tipo de mensaje.
     *
     * @return tipo de mensaje
     */
    public TipoMensaje getTipo() {
        return tipo;
    }

    /**
     * Devuelve el texto (solo TEXTO).
     *
     * @return texto (solo TEXTO)
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * Devuelve la cantidad ofertada (solo OFERTA).
     *
     * @return cantidad ofertada (solo OFERTA)
     */
    public BigDecimal getOfertaCantidad() {
        return ofertaCantidad;
    }

    /**
     * Devuelve el precio por unidad ofertado (solo OFERTA).
     *
     * @return precio por unidad ofertado (solo OFERTA)
     */
    public BigDecimal getOfertaPrecioUnitario() {
        return ofertaPrecioUnitario;
    }

    /**
     * Devuelve {@code true} si la otra parte ya lo leyó.
     *
     * @return {@code true} si la otra parte ya lo leyó
     */
    public boolean isLeido() {
        return leido;
    }

    /**
     * Devuelve la fecha y hora de envío (la pone la base de datos).
     *
     * @return fecha y hora de envío (la pone la base de datos)
     */
    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }
}
