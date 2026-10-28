package pe.agroconecta.notificacion.dominio;

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
 * Aviso para un usuario (tabla {@code notificaciones}). {@code idReferencia} apunta al objeto del aviso (por
 * ejemplo, la conversación en MENSAJE y OFERTA).
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 22)
    private TipoNotificacion tipo;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "contenido", nullable = false, length = 300)
    private String contenido;

    @Column(name = "id_referencia")
    private Long idReferencia;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Constructor requerido por JPA. */
    protected Notificacion() {
    }

    /**
     * Crea un aviso sin leer.
     *
     * @param idUsuario    usuario que lo recibe
     * @param tipo         tipo de aviso
     * @param titulo       título (hasta 120 caracteres)
     * @param contenido    texto (hasta 300 caracteres)
     * @param idReferencia id del objeto del aviso, o {@code null}
     */
    public Notificacion(Long idUsuario, TipoNotificacion tipo, String titulo, String contenido, Long idReferencia) {
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.titulo = titulo;
        this.contenido = contenido;
        this.idReferencia = idReferencia;
    }

    /** Marca el aviso como leído. */
    public void marcarLeida() {
        this.leida = true;
    }

    /**
     * Devuelve la fecha y hora de creación (la pone la base de datos).
     *
     * @return fecha y hora de creación (la pone la base de datos)
     */
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Devuelve el id del aviso.
     *
     * @return id del aviso
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el usuario que lo recibe.
     *
     * @return usuario que lo recibe
     */
    public Long getIdUsuario() {
        return idUsuario;
    }

    /**
     * Devuelve el tipo de aviso.
     *
     * @return tipo de aviso
     */
    public TipoNotificacion getTipo() {
        return tipo;
    }

    /**
     * Devuelve el título.
     *
     * @return título
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Devuelve el texto.
     *
     * @return texto
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * Devuelve el id del objeto del aviso.
     *
     * @return id del objeto del aviso
     */
    public Long getIdReferencia() {
        return idReferencia;
    }

    /**
     * Devuelve {@code true} si ya se leyó.
     *
     * @return {@code true} si ya se leyó
     */
    public boolean isLeida() {
        return leida;
    }
}
