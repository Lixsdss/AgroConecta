package pe.agroconecta.venta.aplicacion;

import java.util.List;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;
import pe.agroconecta.venta.dominio.Calificacion;
import pe.agroconecta.venta.dominio.EstadoTransaccion;
import pe.agroconecta.venta.dominio.ReputacionUsuario;
import pe.agroconecta.venta.dominio.VentaHistorial;
import pe.agroconecta.venta.infraestructura.CalificacionRecibida;
import pe.agroconecta.venta.infraestructura.CalificacionRepository;
import pe.agroconecta.venta.infraestructura.ReputacionRepository;
import pe.agroconecta.venta.infraestructura.VentaHistorialRepository;

/**
 * Calificaciones y reputación (RF-05, UC-07). Valida con mensajes amigables lo mismo que
 * {@code trg_calificaciones_bi} y {@code uk_calificacion_trans_evaluador}; la reputación la calcula
 * {@code v_reputacion_usuario} al consultarla (no se guarda). Las calificaciones no se editan ni se eliminan.
 */
@Service
public class CalificacionService {

    /** Calificaciones recibidas por página. */
    public static final int TAMANO_PAGINA = 10;

    static final String MENSAJE_NO_TERMINADA = "Solo se califican ventas terminadas.";
    static final String MENSAJE_YA_CALIFICADA = "Ya calificó esta venta. Las calificaciones no se cambian.";
    static final String MENSAJE_USUARIO_NO_EXISTE = "El usuario no existe.";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

    private static final Logger LOG = LoggerFactory.getLogger(CalificacionService.class);

    private final CalificacionRepository calificaciones;
    private final ReputacionRepository reputaciones;
    private final VentaHistorialRepository ventas;
    private final VentaService ventaService;
    private final UsuarioConsultaService usuarios;
    private final NotificacionService notificaciones;

    /**
     * Crea el servicio.
     *
     * @param calificaciones repositorio de calificaciones
     * @param reputaciones   vista de reputación
     * @param ventas         vista del historial (partes y estado de la venta)
     * @param ventaService   detalle de la venta que se devuelve
     * @param usuarios       existencia y rol del usuario
     * @param notificaciones aviso CALIFICACION
     */
    public CalificacionService(CalificacionRepository calificaciones, ReputacionRepository reputaciones,
            VentaHistorialRepository ventas, VentaService ventaService, UsuarioConsultaService usuarios,
            NotificacionService notificaciones) {
        this.calificaciones = calificaciones;
        this.reputaciones = reputaciones;
        this.ventas = ventas;
        this.ventaService = ventaService;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
    }

    /**
     * Califica a la otra parte de una venta COMPLETADA (una sola vez) y le avisa.
     *
     * @param idUsuario  parte que califica
     * @param idVenta    venta
     * @param puntuacion estrellas (1 a 5)
     * @param comentario comentario opcional
     * @return la venta con sus calificaciones
     * @throws RecursoNoEncontradoException si la venta no existe
     * @throws AccessDeniedException        si el usuario no es parte de la venta (403)
     * @throws ReglaNegocioException        si la venta no está COMPLETADA
     * @throws ConflictoException           si ya la calificó
     */
    @Transactional
    public VentaDetalleDto calificar(long idUsuario, long idVenta, int puntuacion, String comentario) {
        Preconditions.checkArgument(puntuacion >= 1 && puntuacion <= 5, "puntuacion");
        VentaHistorial venta = ventas.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(VentaService.MENSAJE_NO_EXISTE));
        boolean comprador = idUsuario == venta.getIdComprador();
        if (!comprador && idUsuario != venta.getIdAgricultor()) {
            LOG.warn("Calificación negada en la venta {} para el usuario {}", idVenta, idUsuario);
            throw new AccessDeniedException("No es parte de la venta");
        }
        if (venta.getEstado() != EstadoTransaccion.COMPLETADA) {
            throw new ReglaNegocioException(MENSAJE_NO_TERMINADA);
        }
        if (calificaciones.existsByIdTransaccionAndIdEvaluador(idVenta, idUsuario)) {
            throw new ConflictoException(MENSAJE_YA_CALIFICADA);
        }
        calificaciones.saveAndFlush(new Calificacion(idVenta, idUsuario, puntuacion,
                StringUtils.trimToNull(comentario)));
        LOG.info("Venta {} calificada por el usuario {} con {} estrellas", idVenta, idUsuario, puntuacion);

        String quien = comprador ? venta.getComprador() : venta.getAgricultor();
        String producto = "Común".equals(venta.getVariedad()) ? venta.getCultivo() + " común"
                : venta.getCultivo() + " " + venta.getVariedad();
        notificaciones.crear(new NuevaNotificacionDto(comprador ? venta.getIdAgricultor() : venta.getIdComprador(),
                "CALIFICACION", "Nueva calificación", "%s le dio %d %s por la venta de %s.".formatted(quien,
                        puntuacion, puntuacion == 1 ? "estrella" : "estrellas", producto), idVenta));
        return ventaService.obtener(idUsuario, idVenta);
    }

    /**
     * Reputación de un agricultor o comprador: promedio, total y calificaciones recibidas (10 por página).
     *
     * @param idUsuario usuario
     * @param pagina    página desde 0
     * @return reputación
     * @throws RecursoNoEncontradoException si el usuario no existe o es administrador
     * @throws SolicitudInvalidaException   si la página es negativa
     */
    @Transactional(readOnly = true)
    public ReputacionDto reputacion(long idUsuario, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        UsuarioResumenDto usuario = usuarios.buscarResumenes(List.of(idUsuario)).get(idUsuario);
        if (usuario == null || "ADMINISTRADOR".equals(usuario.rol())) {
            throw new RecursoNoEncontradoException(MENSAJE_USUARIO_NO_EXISTE);
        }
        ReputacionUsuario reputacion = reputaciones.findById(idUsuario).orElse(null);
        return new ReputacionDto(idUsuario, reputacion == null ? null : reputacion.getPromedio(),
                reputacion == null ? 0 : reputacion.getTotal(),
                calificaciones.recibidas(idUsuario, PageRequest.of(pagina, TAMANO_PAGINA)).map(CalificacionService::aDto));
    }

    private static CalificacionDto aDto(CalificacionRecibida c) {
        return new CalificacionDto(c.puntuacion(), c.comentario(), c.fecha(), c.autor());
    }
}
