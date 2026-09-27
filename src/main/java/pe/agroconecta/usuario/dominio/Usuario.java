package pe.agroconecta.usuario.dominio;

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
 * Usuario de la plataforma (tabla {@code usuarios}). Sus datos de agricultor o comprador están en
 * {@link Agricultor} o {@link Comprador}; el administrador no tiene subtipo.
 *
 * <p>El distrito es del módulo {@code catalogo} y se referencia por id. El estado lo cambian también los
 * triggers de {@code suspensiones}.</p>
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rol", nullable = false, length = 13, updatable = false)
    private Rol rol;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "celular", nullable = false, length = 9)
    private String celular;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "id_distrito", nullable = false)
    private Integer idDistrito;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoUsuario estado;

    @Column(name = "fecha_registro", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    /** Constructor requerido por JPA. */
    protected Usuario() {
    }

    /**
     * Crea un usuario nuevo en estado ACTIVO.
     *
     * @param rol            rol del usuario
     * @param nombreCompleto nombre completo
     * @param celular        celular de 9 dígitos (usuario de inicio de sesión)
     * @param passwordHash   hash BCrypt de la contraseña
     * @param idDistrito     id del distrito (módulo {@code catalogo})
     */
    public Usuario(Rol rol, String nombreCompleto, String celular, String passwordHash, Integer idDistrito) {
        this.rol = rol;
        this.nombreCompleto = nombreCompleto;
        this.celular = celular;
        this.passwordHash = passwordHash;
        this.idDistrito = idDistrito;
        this.estado = EstadoUsuario.ACTIVO;
    }

    /**
     * Actualiza los datos editables del perfil.
     *
     * @param nombreCompleto nuevo nombre completo
     * @param idDistrito     nuevo distrito
     */
    public void actualizarDatos(String nombreCompleto, Integer idDistrito) {
        this.nombreCompleto = nombreCompleto;
        this.idDistrito = idDistrito;
    }

    /**
     * Reemplaza el hash de la contraseña.
     *
     * @param nuevoHash hash BCrypt de la nueva contraseña
     */
    public void cambiarPasswordHash(String nuevoHash) {
        this.passwordHash = nuevoHash;
    }

    /**
     * Registra el momento del último inicio de sesión.
     *
     * @param momento fecha y hora del acceso
     */
    public void registrarAcceso(LocalDateTime momento) {
        this.ultimoAcceso = momento;
    }

    /**
     * Indica si el usuario puede usar la plataforma.
     *
     * @return {@code true} si está ACTIVO
     */
    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    /**
     * Devuelve el id del usuario.
     *
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el rol.
     *
     * @return rol
     */
    public Rol getRol() {
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
     * Devuelve el celular.
     *
     * @return celular de 9 dígitos
     */
    public String getCelular() {
        return celular;
    }

    /**
     * Devuelve el hash BCrypt de la contraseña.
     *
     * @return hash
     */
    public String getPasswordHash() {
        return passwordHash;
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
     * Devuelve el estado.
     *
     * @return estado
     */
    public EstadoUsuario getEstado() {
        return estado;
    }

    /**
     * Devuelve la fecha de registro (la fija la base de datos).
     *
     * @return fecha de registro
     */
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Devuelve el último inicio de sesión.
     *
     * @return fecha y hora, o {@code null} si nunca inició sesión
     */
    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }
}
