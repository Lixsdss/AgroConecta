package pe.agroconecta.administracion.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fila de la pantalla "Usuarios" (vista {@code v_resumen_usuario}, solo lectura): datos del usuario, suspensión
 * vigente, operaciones completadas, reputación y reclamos fundados en su contra. Rol, estado y motivo son textos
 * (sus enums son del módulo {@code usuario}).
 */
@Entity
@Immutable
@Table(name = "v_resumen_usuario")
public class ResumenUsuario {

    @Id
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "rol")
    private String rol;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column(name = "nombre_chacra")
    private String nombreChacra;

    @Column(name = "nombre_negocio")
    private String nombreNegocio;

    @Column(name = "tipo_comprador")
    private String tipoComprador;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "celular")
    private String celular;

    @Column(name = "distrito")
    private String distrito;

    @Column(name = "estado")
    private String estado;

    @Column(name = "motivo_suspension")
    private String motivoSuspension;

    @Column(name = "operaciones_completadas")
    private Long operacionesCompletadas;

    @Column(name = "promedio_puntuacion")
    private BigDecimal promedioPuntuacion;

    @Column(name = "total_calificaciones")
    private Long totalCalificaciones;

    @Column(name = "reclamos_fundados")
    private Long reclamosFundados;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    /** Constructor requerido por JPA. */
    protected ResumenUsuario() {
    }

    /**
     * Devuelve el id del usuario.
     *
     * @return id del usuario
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el rol.
     *
     * @return rol
     */
    public String getRol() {
        return rol;
    }

    /**
     * Devuelve el nombre completo.
     *
     * @return nombre completo
     */
    public String getNombreCompleto() {
        return nombreCompleto;
    }

    /**
     * Devuelve la chacra (agricultor), o {@code null}.
     *
     * @return chacra (agricultor), o {@code null}
     */
    public String getNombreChacra() {
        return nombreChacra;
    }

    /**
     * Devuelve el negocio (comprador), o {@code null}.
     *
     * @return negocio (comprador), o {@code null}
     */
    public String getNombreNegocio() {
        return nombreNegocio;
    }

    /**
     * Devuelve el tipo de comprador, o {@code null}.
     *
     * @return tipo de comprador, o {@code null}
     */
    public String getTipoComprador() {
        return tipoComprador;
    }

    /**
     * Devuelve el celular.
     *
     * @return celular
     */
    public String getCelular() {
        return celular;
    }

    /**
     * Devuelve el distrito.
     *
     * @return distrito
     */
    public String getDistrito() {
        return distrito;
    }

    /**
     * Devuelve el estado.
     *
     * @return estado
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Devuelve el motivo de la suspensión vigente, o {@code null}.
     *
     * @return motivo de la suspensión vigente, o {@code null}
     */
    public String getMotivoSuspension() {
        return motivoSuspension;
    }

    /**
     * Devuelve las ventas COMPLETADA en las que participó.
     *
     * @return ventas COMPLETADA en las que participó
     */
    public Long getOperacionesCompletadas() {
        return operacionesCompletadas;
    }

    /**
     * Devuelve el promedio de estrellas, o {@code null} sin calificaciones.
     *
     * @return promedio de estrellas, o {@code null} sin calificaciones
     */
    public BigDecimal getPromedioPuntuacion() {
        return promedioPuntuacion;
    }

    /**
     * Devuelve las calificaciones recibidas.
     *
     * @return calificaciones recibidas
     */
    public Long getTotalCalificaciones() {
        return totalCalificaciones;
    }

    /**
     * Devuelve las reclamos FUNDADO en su contra.
     *
     * @return reclamos FUNDADO en su contra
     */
    public Long getReclamosFundados() {
        return reclamosFundados;
    }

    /**
     * Devuelve la fecha de registro.
     *
     * @return fecha de registro
     */
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
