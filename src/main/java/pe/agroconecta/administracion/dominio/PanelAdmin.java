package pe.agroconecta.administracion.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Tarjetas del panel (vista {@code v_panel_admin}, solo lectura, una sola fila). La vista no tiene clave: se usa
 * {@code publicaciones_activas} como identificador solo para cumplir con JPA.
 */
@Entity
@Immutable
@Table(name = "v_panel_admin")
public class PanelAdmin {

    @Id
    @Column(name = "publicaciones_activas")
    private Long publicacionesActivas;

    @Column(name = "publicadas_ultimos_7_dias")
    private Long publicadasUltimos7Dias;

    @Column(name = "por_revisar")
    private Long porRevisar;

    @Column(name = "por_revisar_mas_antigua")
    private LocalDateTime porRevisarMasAntigua;

    @Column(name = "conformes_hoy")
    private Long conformesHoy;

    @Column(name = "bloqueadas_hoy")
    private Long bloqueadasHoy;

    @Column(name = "precios_cargados_hoy")
    private Long preciosCargadosHoy;

    @Column(name = "precios_esperados_hoy")
    private Long preciosEsperadosHoy;

    @Column(name = "reclamos_pendientes")
    private Long reclamosPendientes;

    /** Constructor requerido por JPA. */
    protected PanelAdmin() {
    }

    /**
     * Devuelve las publicaciones PUBLICADA.
     *
     * @return publicaciones PUBLICADA
     */
    public Long getPublicacionesActivas() {
        return publicacionesActivas;
    }

    /**
     * Devuelve las publicadas en los últimos 7 días que siguen PUBLICADA.
     *
     * @return publicadas en los últimos 7 días que siguen PUBLICADA
     */
    public Long getPublicadasUltimos7Dias() {
        return publicadasUltimos7Dias;
    }

    /**
     * Devuelve las publicaciones en la cola "Por revisar".
     *
     * @return publicaciones en la cola "Por revisar"
     */
    public Long getPorRevisar() {
        return porRevisar;
    }

    /**
     * Devuelve la publicación más antigua de la cola, o {@code null}.
     *
     * @return publicación más antigua de la cola, o {@code null}
     */
    public LocalDateTime getPorRevisarMasAntigua() {
        return porRevisarMasAntigua;
    }

    /**
     * Devuelve las decisiones CONFORME de hoy.
     *
     * @return decisiones CONFORME de hoy
     */
    public Long getConformesHoy() {
        return conformesHoy;
    }

    /**
     * Devuelve las decisiones BLOQUEO de hoy.
     *
     * @return decisiones BLOQUEO de hoy
     */
    public Long getBloqueadasHoy() {
        return bloqueadasHoy;
    }

    /**
     * Devuelve los precios de referencia cargados hoy.
     *
     * @return precios de referencia cargados hoy
     */
    public Long getPreciosCargadosHoy() {
        return preciosCargadosHoy;
    }

    /**
     * Devuelve los precios esperados hoy (cultivos activos × zonas de precio).
     *
     * @return precios esperados hoy (cultivos activos × zonas de precio)
     */
    public Long getPreciosEsperadosHoy() {
        return preciosEsperadosHoy;
    }

    /**
     * Devuelve las reclamos PENDIENTE.
     *
     * @return reclamos PENDIENTE
     */
    public Long getReclamosPendientes() {
        return reclamosPendientes;
    }
}
