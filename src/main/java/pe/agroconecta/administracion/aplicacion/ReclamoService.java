package pe.agroconecta.administracion.aplicacion;

import java.util.List;

import jakarta.persistence.EntityManager;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.administracion.dominio.EstadoReclamo;
import pe.agroconecta.administracion.dominio.MotivoReclamo;
import pe.agroconecta.administracion.dominio.Reclamo;
import pe.agroconecta.administracion.infraestructura.ReclamoRepository;
import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.venta.aplicacion.VentaParaAdministracionDto;
import pe.agroconecta.venta.aplicacion.VentaService;

/**
 * Reclamos (RF-09, UC-11 parte 3): una parte reclama sobre una venta que llegó a ACORDADA y el administrador lo
 * resuelve una sola vez como FUNDADO o INFUNDADO. Valida con mensajes amigables lo mismo que
 * {@code trg_reclamos_bi/bu} y {@code uk_reclamo_trans_reclamante}; avisa a las partes (RECLAMO).
 */
@Service
public class ReclamoService {

    /** Reclamos por página. */
    public static final int TAMANO_PAGINA = 20;

    static final String MENSAJE_NO_ACORDADA = "Solo se reclama sobre una venta que llegó a acordarse.";
    static final String MENSAJE_DUPLICADO = "Ya registró un reclamo sobre esta venta.";
    static final String MENSAJE_NO_EXISTE = "El reclamo no existe.";
    static final String MENSAJE_RESUELTO = "El reclamo ya fue resuelto.";
    static final String MENSAJE_RESULTADO = "El resultado debe ser FUNDADO o INFUNDADO.";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

    private static final Logger LOG = LoggerFactory.getLogger(ReclamoService.class);

    private final ReclamoRepository reclamos;
    private final VentaService ventas;
    private final NotificacionService notificaciones;
    private final EntityManager entityManager;

    /**
     * Crea el servicio.
     *
     * @param reclamos       repositorio de reclamos
     * @param ventas         venta reclamada (partes, estado e historial)
     * @param notificaciones avisos RECLAMO
     * @param entityManager  para releer las fechas que fija la base de datos
     */
    public ReclamoService(ReclamoRepository reclamos, VentaService ventas, NotificacionService notificaciones,
            EntityManager entityManager) {
        this.reclamos = reclamos;
        this.ventas = ventas;
        this.notificaciones = notificaciones;
        this.entityManager = entityManager;
    }

    /**
     * Registra un reclamo PENDIENTE y avisa a la otra parte.
     *
     * @param idUsuario   parte que reclama
     * @param idVenta     venta
     * @param motivo      motivo de catálogo
     * @param descripcion descripción
     * @return el reclamo
     * @throws AccessDeniedException si no es parte de la venta (403)
     * @throws ReglaNegocioException si la venta nunca llegó a ACORDADA
     * @throws ConflictoException    si ya reclamó esa venta
     */
    @Transactional
    public ReclamoDto registrar(long idUsuario, long idVenta, MotivoReclamo motivo, String descripcion) {
        Preconditions.checkNotNull(motivo, "motivo");
        VentaParaAdministracionDto venta = ventas.paraAdministracion(idVenta);
        if (!venta.participa(idUsuario)) {
            LOG.warn("Reclamo negado sobre la venta {} para el usuario {}", idVenta, idUsuario);
            throw new AccessDeniedException("No es parte de la venta");
        }
        if (!venta.llegoAAcordada()) {
            throw new ReglaNegocioException(MENSAJE_NO_ACORDADA);
        }
        if (reclamos.existsByIdTransaccionAndIdReclamante(idVenta, idUsuario)) {
            throw new ConflictoException(MENSAJE_DUPLICADO);
        }
        Reclamo reclamo = reclamos.saveAndFlush(new Reclamo(idVenta, idUsuario, motivo, descripcion.strip()));
        entityManager.refresh(reclamo); // fecha_registro la fija la base de datos
        long idOtra = idUsuario == venta.idComprador() ? venta.idAgricultor() : venta.idComprador();
        notificaciones.crear(new NuevaNotificacionDto(idOtra, "RECLAMO", "Reclamo sobre una venta",
                "Se registró un reclamo sobre su venta de %s. AgroConecta lo revisará y les avisará."
                        .formatted(venta.producto()), idVenta));
        LOG.info("Reclamo {} registrado: venta={}, reclamante={}, motivo={}", reclamo.getId(), idVenta, idUsuario,
                motivo);
        return aDto(reclamo, venta);
    }

    /**
     * Reclamos que registró el usuario, el más reciente primero.
     *
     * @param idUsuario usuario de la sesión
     * @param idVenta   solo los de esa venta, o {@code null} para todos
     * @return reclamos
     */
    @Transactional(readOnly = true)
    public List<ReclamoDto> mios(long idUsuario, Long idVenta) {
        List<Reclamo> encontrados = idVenta == null
                ? reclamos.findByIdReclamanteOrderByFechaRegistroDescIdDesc(idUsuario)
                : reclamos.findByIdReclamanteAndIdTransaccion(idUsuario, idVenta);
        return encontrados.stream().map(r -> aDto(r, ventas.paraAdministracion(r.getIdTransaccion()))).toList();
    }

    /**
     * Reclamos para el administrador: pendientes, el más antiguo primero; resueltos o todos, el más reciente primero.
     *
     * @param estado {@code PENDIENTE}, {@code FUNDADO}, {@code INFUNDADO} o {@code null} para todos
     * @param pagina página desde 0 (20 por página)
     * @return página de reclamos
     */
    @Transactional(readOnly = true)
    public Page<ReclamoDto> listar(EstadoReclamo estado, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        Sort orden = estado == EstadoReclamo.PENDIENTE
                ? Sort.by(Sort.Order.asc("fechaRegistro"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("fechaRegistro"), Sort.Order.desc("id"));
        PageRequest pedido = PageRequest.of(pagina, TAMANO_PAGINA, orden);
        Page<Reclamo> encontrados = estado == null ? reclamos.findAll(pedido) : reclamos.findByEstado(estado, pedido);
        return encontrados.map(r -> aDto(r, ventas.paraAdministracion(r.getIdTransaccion())));
    }

    /**
     * Reclamo con su venta y la línea de tiempo.
     *
     * @param idReclamo reclamo
     * @return el reclamo y la venta
     */
    @Transactional(readOnly = true)
    public ReclamoDetalleDto obtener(long idReclamo) {
        Reclamo reclamo = reclamos.findById(idReclamo)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        VentaParaAdministracionDto venta = ventas.paraAdministracion(reclamo.getIdTransaccion());
        return new ReclamoDetalleDto(aDto(reclamo, venta), venta);
    }

    /**
     * Resuelve un reclamo PENDIENTE (una sola vez) y avisa a las dos partes.
     *
     * @param idAdministrador administrador de la sesión
     * @param idReclamo       reclamo
     * @param resultado       FUNDADO o INFUNDADO
     * @param respuesta       respuesta para las partes
     * @return el reclamo resuelto
     * @throws ReglaNegocioException si ya estaba resuelto
     */
    @Transactional
    public ReclamoDto resolver(long idAdministrador, long idReclamo, EstadoReclamo resultado, String respuesta) {
        if (resultado == null || resultado == EstadoReclamo.PENDIENTE) {
            throw new SolicitudInvalidaException(MENSAJE_RESULTADO);
        }
        Reclamo reclamo = reclamos.findById(idReclamo)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        if (reclamo.getEstado() != EstadoReclamo.PENDIENTE) {
            throw new ReglaNegocioException(MENSAJE_RESUELTO);
        }
        String texto = StringUtils.normalizeSpace(respuesta);
        reclamo.resolver(resultado, idAdministrador, texto);
        reclamos.saveAndFlush(reclamo);
        entityManager.refresh(reclamo); // trg_reclamos_bu fija fecha_resolucion
        VentaParaAdministracionDto venta = ventas.paraAdministracion(reclamo.getIdTransaccion());
        String resultadoTexto = resultado == EstadoReclamo.FUNDADO ? "fundado" : "infundado";
        long idOtra = reclamo.getIdReclamante().equals(venta.idComprador()) ? venta.idAgricultor()
                : venta.idComprador();
        notificaciones.crear(new NuevaNotificacionDto(reclamo.getIdReclamante(), "RECLAMO", "Reclamo resuelto",
                "Su reclamo sobre %s fue resuelto: %s. Respuesta: %s".formatted(venta.producto(), resultadoTexto, texto),
                venta.id()));
        notificaciones.crear(new NuevaNotificacionDto(idOtra, "RECLAMO", "Reclamo resuelto",
                "El reclamo sobre su venta de %s fue resuelto: %s. Respuesta: %s".formatted(venta.producto(),
                        resultadoTexto, texto), venta.id()));
        LOG.info("Reclamo {} resuelto: {} por el administrador {}", idReclamo, resultado, idAdministrador);
        return aDto(reclamo, venta);
    }

    private static ReclamoDto aDto(Reclamo r, VentaParaAdministracionDto venta) {
        boolean comprador = r.getIdReclamante().equals(venta.idComprador());
        return new ReclamoDto(r.getId(), r.getIdTransaccion(), r.getMotivo().name(), r.getDescripcion(),
                r.getEstado().name(), r.getRespuesta(), r.getFechaRegistro(), r.getFechaResolucion(), venta.producto(),
                comprador ? venta.comprador() : venta.agricultor(), comprador ? venta.agricultor() : venta.comprador());
    }
}
