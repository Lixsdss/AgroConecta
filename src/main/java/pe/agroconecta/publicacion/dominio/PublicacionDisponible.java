package pe.agroconecta.publicacion.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Publicación con sus nombres de catálogo, el agricultor y su calificación, los kilos aún disponibles y los días
 * que faltan para la fecha límite (vista {@code v_publicaciones_disponibles}, solo lectura). La usa la búsqueda
 * (RF-03) y el detalle de una publicación.
 */
@Entity
@Immutable
@Table(name = "v_publicaciones_disponibles")
public class PublicacionDisponible {

    @Id
    @Column(name = "id_publicacion")
    private Long id;

    @Column(name = "id_agricultor")
    private Long idAgricultor;

    @Column(name = "agricultor")
    private String agricultor;

    @Column(name = "calificacion_agricultor")
    private BigDecimal calificacionAgricultor;

    @Column(name = "id_cultivo")
    private Integer idCultivo;

    @Column(name = "cultivo")
    private String cultivo;

    @Column(name = "id_variedad")
    private Integer idVariedad;

    @Column(name = "variedad")
    private String variedad;

    @Column(name = "id_distrito")
    private Integer idDistrito;

    @Column(name = "distrito")
    private String distrito;

    @Column(name = "id_provincia")
    private Integer idProvincia;

    @Column(name = "provincia")
    private String provincia;

    @Column(name = "id_unidad")
    private Integer idUnidad;

    /** Nombre en plural de la unidad (la vista lo llama {@code unidad}). */
    @Column(name = "unidad")
    private String unidadPlural;

    @Column(name = "cantidad")
    private BigDecimal cantidad;

    @Column(name = "peso_unidad_kg")
    private BigDecimal pesoUnidadKg;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "precio_kg")
    private BigDecimal precioKg;

    @Column(name = "cantidad_kg")
    private BigDecimal cantidadKg;

    @Column(name = "disponible_kg")
    private BigDecimal disponibleKg;

    @Column(name = "fecha_cosecha")
    private LocalDate fechaCosecha;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "dias_restantes")
    private Integer diasRestantes;

    @Column(name = "por_vencer")
    private Integer porVencer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado")
    private EstadoPublicacion estado;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    /** Constructor requerido por JPA. */
    protected PublicacionDisponible() {
    }

    /** @return id de la publicación */
    public Long getId() {
        return id;
    }

    /** @return id del agricultor */
    public Long getIdAgricultor() {
        return idAgricultor;
    }

    /** @return nombre completo del agricultor */
    public String getAgricultor() {
        return agricultor;
    }

    /** @return promedio de calificaciones del agricultor (1 a 5), o {@code null} si no tiene */
    public BigDecimal getCalificacionAgricultor() {
        return calificacionAgricultor;
    }

    /** @return id del cultivo */
    public Integer getIdCultivo() {
        return idCultivo;
    }

    /** @return nombre del cultivo */
    public String getCultivo() {
        return cultivo;
    }

    /** @return id de la variedad */
    public Integer getIdVariedad() {
        return idVariedad;
    }

    /** @return nombre de la variedad */
    public String getVariedad() {
        return variedad;
    }

    /** @return id del distrito */
    public Integer getIdDistrito() {
        return idDistrito;
    }

    /** @return nombre del distrito */
    public String getDistrito() {
        return distrito;
    }

    /** @return id de la provincia */
    public Integer getIdProvincia() {
        return idProvincia;
    }

    /** @return nombre de la provincia */
    public String getProvincia() {
        return provincia;
    }

    /** @return id de la unidad */
    public Integer getIdUnidad() {
        return idUnidad;
    }

    /** @return nombre de la unidad en plural */
    public String getUnidadPlural() {
        return unidadPlural;
    }

    /** @return cantidad publicada en la unidad elegida */
    public BigDecimal getCantidad() {
        return cantidad;
    }

    /** @return kilos por unidad */
    public BigDecimal getPesoUnidadKg() {
        return pesoUnidadKg;
    }

    /** @return soles por unidad */
    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    /** @return soles por kilo */
    public BigDecimal getPrecioKg() {
        return precioKg;
    }

    /** @return kilos publicados */
    public BigDecimal getCantidadKg() {
        return cantidadKg;
    }

    /** @return kilos aún no comprometidos en ventas */
    public BigDecimal getDisponibleKg() {
        return disponibleKg;
    }

    /** @return fecha de cosecha */
    public LocalDate getFechaCosecha() {
        return fechaCosecha;
    }

    /** @return fecha límite de venta */
    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    /** @return días que faltan para la fecha límite */
    public Integer getDiasRestantes() {
        return diasRestantes;
    }

    /** @return {@code true} si está PUBLICADA y faltan 3 días o menos */
    public boolean isPorVencer() {
        return porVencer != null && porVencer != 0;
    }

    /** @return estado de la publicación */
    public EstadoPublicacion getEstado() {
        return estado;
    }

    /** @return fecha y hora de publicación */
    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }
}
