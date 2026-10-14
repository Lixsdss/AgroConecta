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

    /** @return publicaciones PUBLICADA */
    public Long getPublicacionesActivas() {
        return publicacionesActivas;
    }

    /** @return publicadas en los últimos 7 días que siguen PUBLICADA */
    public Long getPublicadasUltimos7Dias() {
        return publicadasUltimos7Dias;
    }

    /** @return publicaciones en la cola "Por revisar" */
    public Long getPorRevisar() {
        return porRevisar;
    }

    /** @return publicación más antigua de la cola, o {@code null} */
    public LocalDateTime getPorRevisarMasAntigua() {
        return porRevisarMasAntigua;
    }

    /** @return decisiones CONFORME de hoy */
    public Long getConformesHoy() {
        return conformesHoy;
    }

    /** @return decisiones BLOQUEO de hoy */
    public Long getBloqueadasHoy() {
        return bloqueadasHoy;
    }

    /** @return precios de referencia cargados hoy */
    public Long getPreciosCargadosHoy() {
        return preciosCargadosHoy;
    }

    /** @return precios esperados hoy (cultivos activos × zonas de precio) */
    public Long getPreciosEsperadosHoy() {
        return preciosEsperadosHoy;
    }

    /** @return reclamos PENDIENTE */
    public Long getReclamosPendientes() {
        return reclamosPendientes;
    }
}
