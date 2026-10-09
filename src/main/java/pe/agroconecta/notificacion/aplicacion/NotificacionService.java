package pe.agroconecta.notificacion.aplicacion;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.notificacion.dominio.Notificacion;
import pe.agroconecta.notificacion.dominio.TipoNotificacion;
import pe.agroconecta.notificacion.infraestructura.NotificacionRepository;

/**
 * Avisos: creación por los demás módulos, dentro de su misma transacción (los avisos no son eventos), y el centro
 * de avisos del usuario (RF-07, UC-09 paso 2): lista, no leídos y marcar como leídos. Los avisos no se borran.
 */
@Service
public class NotificacionService {

    /** Avisos por página. */
    public static final int TAMANO_PAGINA = 20;

    static final String MENSAJE_NO_EXISTE = "El aviso no existe.";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

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

    /**
     * Avisos del usuario, el más reciente primero.
     *
     * @param idUsuario usuario de la sesión
     * @param pagina    página desde 0 (20 por página)
     * @return página de avisos
     * @throws SolicitudInvalidaException si la página es negativa
     */
    @Transactional(readOnly = true)
    public Page<NotificacionDto> listar(long idUsuario, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        return notificaciones.findByIdUsuarioOrderByFechaCreacionDescIdDesc(idUsuario,
                PageRequest.of(pagina, TAMANO_PAGINA)).map(NotificacionService::aDto);
    }

    /**
     * Cantidad de avisos sin leer del usuario (campana del inicio).
     *
     * @param idUsuario usuario de la sesión
     * @return cantidad
     */
    @Transactional(readOnly = true)
    public long noLeidas(long idUsuario) {
        return notificaciones.countByIdUsuarioAndLeidaFalse(idUsuario);
    }

    /**
     * Marca un aviso del usuario como leído.
     *
     * @param idUsuario      usuario de la sesión
     * @param idNotificacion aviso
     * @throws RecursoNoEncontradoException si no existe o es de otro usuario
     */
    @Transactional
    public void marcarLeida(long idUsuario, long idNotificacion) {
        notificaciones.findByIdAndIdUsuario(idNotificacion, idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE))
                .marcarLeida();
    }

    /**
     * Marca como leídos todos los avisos del usuario.
     *
     * @param idUsuario usuario de la sesión
     * @return avisos marcados
     */
    @Transactional
    public int marcarTodasLeidas(long idUsuario) {
        return notificaciones.marcarTodasLeidas(idUsuario);
    }

    private static NotificacionDto aDto(Notificacion n) {
        return new NotificacionDto(n.getId(), n.getTipo().name(), n.getTitulo(), n.getContenido(),
                n.getIdReferencia(), n.isLeida(), n.getFechaCreacion());
    }

    private static TipoNotificacion tipo(NuevaNotificacionDto aviso) {
        return TipoNotificacion.valueOf(aviso.tipo());
    }
}
