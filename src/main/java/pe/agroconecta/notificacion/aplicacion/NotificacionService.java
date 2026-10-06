package pe.agroconecta.notificacion.aplicacion;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.notificacion.dominio.Notificacion;
import pe.agroconecta.notificacion.dominio.TipoNotificacion;
import pe.agroconecta.notificacion.infraestructura.NotificacionRepository;

/**
 * Creación de avisos por los demás módulos, dentro de su misma transacción (los avisos no son eventos). El centro
 * de avisos del usuario (lista y marcar como leídos) llega con RF-07.
 */
@Service
public class NotificacionService {

    /** Largo máximo del título ({@code notificaciones.titulo}). */
    static final int MAXIMO_TITULO = 120;
    /** Largo máximo del texto ({@code notificaciones.contenido}). */
    static final int MAXIMO_CONTENIDO = 300;

    private final NotificacionRepository notificaciones;

    /**
     * Crea el servicio.
     *
     * @param notificaciones repositorio de avisos
     */
    public NotificacionService(NotificacionRepository notificaciones) {
        this.notificaciones = notificaciones;
    }

    /**
     * Crea un aviso sin leer. El título y el texto se recortan a su largo máximo.
     *
     * @param aviso datos del aviso
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void crear(NuevaNotificacionDto aviso) {
        Preconditions.checkNotNull(aviso, "aviso");
        notificaciones.save(new Notificacion(aviso.idUsuario(), tipo(aviso), StringUtils.abbreviate(aviso.titulo(),
                MAXIMO_TITULO), StringUtils.abbreviate(aviso.contenido(), MAXIMO_CONTENIDO), aviso.idReferencia()));
    }

    /**
     * Crea el aviso solo si el usuario no tiene ya uno sin leer del mismo tipo y referencia (por ejemplo, un
     * MENSAJE de la misma conversación), para no repetir avisos.
     *
     * @param aviso datos del aviso
     * @return {@code true} si se creó
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean crearSiNoHayPendiente(NuevaNotificacionDto aviso) {
        Preconditions.checkNotNull(aviso, "aviso");
        if (notificaciones.existsByIdUsuarioAndTipoAndIdReferenciaAndLeidaFalse(aviso.idUsuario(), tipo(aviso),
                aviso.idReferencia())) {
            return false;
        }
        crear(aviso);
        return true;
    }

    private static TipoNotificacion tipo(NuevaNotificacionDto aviso) {
        return TipoNotificacion.valueOf(aviso.tipo());
    }
}
