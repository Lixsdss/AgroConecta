package pe.agroconecta.publicacion.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Cosecha publicada por un agricultor (tabla {@code publicaciones}, RF-01).
 *
 * <p>El agricultor, la variedad, el distrito y la unidad son de otros módulos y se referencian por id.
 * {@code cantidad_kg} y {@code precio_kg} son columnas generadas por la base de datos. {@code trg_publicaciones_bi}
 * fija el peso de las unidades de peso fijo, calcula la fecha límite si llega vacía y marca la primera
 * publicación; por eso el servicio refresca la entidad después de guardarla.</p>
 */
@Entity
@Table(name = "publicaciones")
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion")
    private Long id;

    @Column(name = "id_agricultor", nullable = false, updatable = false)
    private Long idAgricultor;

    @Column(name = "id_variedad", nullable = false)
    private Integer idVariedad;

    @Column(name = "id_distrito", nullable = false)
    private Integer idDistrito;

    @Column(name = "id_unidad", nullable = false)
    private Integer idUnidad;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "peso_unidad_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal pesoUnidadKg;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "cantidad_kg", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal cantidadKg;

    @Column(name = "precio_kg", precision = 10, scale = 4, insertable = false, updatable = false)
    private BigDecimal precioKg;

    @Column(name = "fecha_cosecha", nullable = false)
    private LocalDate fechaCosecha;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoPublicacion estado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "marca_revision", length = 20)
    private MarcaRevision marcaRevision;

    @Column(name = "fecha_publicacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    /** Constructor requerido por JPA. */
    protected Publicacion() {
    }

    /**
     * Crea una publicación nueva en estado PUBLICADA (moderación posterior).
     *
     * @param idAgricultor   id del agricultor (módulo {@code usuario})
     * @param idVariedad     id de la variedad (módulo {@code catalogo})
     * @param idDistrito     id del distrito donde está la cosecha
     * @param idUnidad       id de la unidad elegida
     * @param cantidad       cantidad en esa unidad
     * @param pesoUnidadKg   kilos por unidad
     * @param precioUnitario soles por unidad
     * @param fechaCosecha   fecha de cosecha
     * @param fechaLimite    fecha límite de venta, o {@code null} para que la calcule la base de datos
     */
    public Publicacion(Long idAgricultor, Integer idVariedad, Integer idDistrito, Integer idUnidad,
            BigDecimal cantidad, BigDecimal pesoUnidadKg, BigDecimal precioUnitario, LocalDate fechaCosecha,
            LocalDate fechaLimite) {
        this.idAgricultor = idAgricultor;
        this.idVariedad = idVariedad;
        this.idDistrito = idDistrito;
        this.idUnidad = idUnidad;
        this.cantidad = cantidad;
        this.pesoUnidadKg = pesoUnidadKg;
        this.precioUnitario = precioUnitario;
        this.fechaCosecha = fechaCosecha;
        this.fechaLimite = fechaLimite;
        this.estado = EstadoPublicacion.PUBLICADA;
    }

    /**
     * Pone la publicación en la cola de moderación con una marca calculada por el servicio (RF-09). Si no se
     * llama, {@code trg_publicaciones_bi} marca PRIMERA_PUBLICACION cuando corresponde.
     *
     * @param marca marca de revisión
     */
    public void marcarParaRevision(MarcaRevision marca) {
        this.marcaRevision = marca;
    }

    /**
     * Indica si la publicación es de ese agricultor.
     *
     * @param idUsuario id del usuario
     * @return {@code true} si es su dueño
     */
    public boolean esDe(Long idUsuario) {
        return idAgricultor.equals(idUsuario);
    }

    /**
     * "Bajar el precio" (RF-07): nuevo precio por unidad; el precio por kilo lo recalcula la base de datos.
     *
     * @param precioUnitario nuevo precio por unidad
     */
    public void cambiarPrecio(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    /**
     * "Extender" (RF-07): nueva fecha límite. En una VENCIDA, {@code trg_publicaciones_bu} la devuelve a PUBLICADA.
     *
     * @param fechaLimite nueva fecha límite (hoy o después)
     */
    public void extenderHasta(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    /** "Retirar" (RF-07): la publicación pasa a RETIRADA. */
    public void retirar() {
        this.estado = EstadoPublicacion.RETIRADA;
    }

    /**
     * Corrección del agricultor sobre una publicación oculta (RF-09): {@code trg_publicaciones_bu} la marca
     * CORREGIDA para que vuelva a la cola de moderación.
     *
     * @param cantidad       cantidad en la unidad publicada
     * @param pesoUnidadKg   kilos por unidad (jaba o saco), o {@code null} para no cambiarlo
     * @param precioUnitario precio por unidad
     */
    public void corregir(BigDecimal cantidad, BigDecimal pesoUnidadKg, BigDecimal precioUnitario) {
        this.cantidad = cantidad;
        if (pesoUnidadKg != null) {
            this.pesoUnidadKg = pesoUnidadKg;
        }
        this.precioUnitario = precioUnitario;
    }

    /**
     * Devuelve el id de la publicación.
     *
     * @return id de la publicación
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el id del agricultor.
     *
     * @return id del agricultor
     */
    public Long getIdAgricultor() {
        return idAgricultor;
    }

    /**
     * Devuelve el id de la variedad.
     *
     * @return id de la variedad
     */
    public Integer getIdVariedad() {
        return idVariedad;
    }

    /**
     * Devuelve el id del distrito.
     *
     * @return id del distrito
     */
    public Integer getIdDistrito() {
        return idDistrito;
    }

    /**
     * Devuelve el id de la unidad.
     *
     * @return id de la unidad
     */
    public Integer getIdUnidad() {
        return idUnidad;
    }

    /**
     * Devuelve la cantidad en la unidad elegida.
     *
     * @return cantidad en la unidad elegida
     */
    public BigDecimal getCantidad() {
        return cantidad;
    }

    /**
     * Devuelve los kilos por unidad.
     *
     * @return kilos por unidad
     */
    public BigDecimal getPesoUnidadKg() {
        return pesoUnidadKg;
    }

    /**
     * Devuelve los soles por unidad.
     *
     * @return soles por unidad
     */
    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    /**
     * Devuelve los kilos totales (columna generada).
     *
     * @return kilos totales (columna generada)
     */
    public BigDecimal getCantidadKg() {
        return cantidadKg;
    }

    /**
     * Devuelve los soles por kilo (columna generada).
     *
     * @return soles por kilo (columna generada)
     */
    public BigDecimal getPrecioKg() {
        return precioKg;
    }

    /**
     * Devuelve la fecha de cosecha.
     *
     * @return fecha de cosecha
     */
    public LocalDate getFechaCosecha() {
        return fechaCosecha;
    }

    /**
     * Devuelve la fecha límite de venta.
     *
     * @return fecha límite de venta
     */
    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    /**
     * Devuelve el estado.
     *
     * @return estado
     */
    public EstadoPublicacion getEstado() {
        return estado;
    }

    /**
     * Devuelve la marca de la cola de moderación, o {@code null}.
     *
     * @return marca de la cola de moderación, o {@code null}
     */
    public MarcaRevision getMarcaRevision() {
        return marcaRevision;
    }

    /**
     * Devuelve la fecha y hora de publicación (la fija la base de datos).
     *
     * @return fecha y hora de publicación (la fija la base de datos)
     */
    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    /**
     * Devuelve la fecha y hora del último cambio, o {@code null}.
     *
     * @return fecha y hora del último cambio, o {@code null}
     */
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
