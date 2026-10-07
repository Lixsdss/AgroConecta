package pe.agroconecta.venta.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.EntityManager;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.mensajeria.aplicacion.ConversacionService;
import pe.agroconecta.mensajeria.aplicacion.ParticipantesDto;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.publicacion.aplicacion.PublicacionResumenDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;
import pe.agroconecta.venta.dominio.Calificacion;
import pe.agroconecta.venta.dominio.EstadoTransaccion;
import pe.agroconecta.venta.dominio.HistorialTransaccion;
import pe.agroconecta.venta.dominio.Transaccion;
import pe.agroconecta.venta.infraestructura.CalificacionRepository;
import pe.agroconecta.venta.infraestructura.HistorialRepository;
import pe.agroconecta.venta.infraestructura.TransaccionRepository;

/**
 * Acuerdo de venta y seguimiento de la entrega (RF-06, RF-11; UC-05, UC-06). Valida con mensajes amigables lo mismo
 * que {@code trg_transacciones_bi/bu} (la base de datos es la última barrera); los triggers escriben la línea de
 * tiempo y {@code sp_sincronizar_publicacion} cambia el estado de la publicación. Avisa a la otra parte (VENTA) en
 * la misma transacción.
 */
@Service
public class VentaService {

    static final String MENSAJE_NO_EXISTE = "La venta no existe.";
    static final String MENSAJE_NO_DISPONIBLE = "La cosecha ya no está disponible.";
    static final String MENSAJE_UNIDAD = "La unidad elegida no existe.";
    static final String MENSAJE_PESO = "Indique cuántos kilos pesa cada jaba o saco.";
    static final String MENSAJE_EXCEDE = "La cantidad supera lo disponible (≈ %s kg).";
    static final String MENSAJE_TERMINADA = "La venta ya terminó y no se modifica.";
    static final String MENSAJE_CANCELAR_TARDE =
            "Una venta recogida ya no se cancela. Si hay un problema, podrá registrar un reclamo.";
    static final String MENSAJE_SOLO_COMPRADOR = "Solo el comprador confirma que recibió.";
    static final String MENSAJE_FUERA_DE_SECUENCIA = "Ese cambio no corresponde al estado actual de la venta.";

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Logger LOG = LoggerFactory.getLogger(VentaService.class);

    private final TransaccionRepository transacciones;
    private final HistorialRepository historial;
    private final CalificacionRepository calificaciones;
    private final ConversacionService conversaciones;
    private final PublicacionConsultaService publicaciones;
    private final UsuarioConsultaService usuarios;
    private final CatalogoService catalogo;
    private final NotificacionService notificaciones;
    private final EntityManager entityManager;

    /**
     * Crea el servicio.
     *
     * @param transacciones  repositorio de ventas
     * @param historial      línea de tiempo (solo lectura)
     * @param calificaciones calificaciones de la venta (RF-05)
     * @param conversaciones partes de la conversación
     * @param publicaciones  publicación (bloqueo y kilos disponibles)
     * @param usuarios       nombres, negocio y celular
     * @param catalogo       unidades
     * @param notificaciones avisos VENTA
     * @param entityManager  contexto de persistencia (para leer lo que calcula la base de datos)
     */
    public VentaService(TransaccionRepository transacciones, HistorialRepository historial,
            CalificacionRepository calificaciones, ConversacionService conversaciones, PublicacionConsultaService publicaciones,
            UsuarioConsultaService usuarios, CatalogoService catalogo, NotificacionService notificaciones,
            EntityManager entityManager) {
        this.transacciones = transacciones;
        this.historial = historial;
        this.calificaciones = calificaciones;
        this.conversaciones = conversaciones;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.notificaciones = notificaciones;
        this.entityManager = entityManager;
    }

    /**
     * "Registrar acuerdo" (UC-05): bloquea la publicación, valida los kilos y registra la venta como ACORDADA.
     *
     * @param idUsuario parte que registra
     * @param nueva     términos del acuerdo
     * @return la venta registrada
     * @throws RecursoNoEncontradoException si la conversación no existe
     * @throws AccessDeniedException        si el usuario no participa en la conversación (403)
     * @throws ReglaNegocioException        si la publicación no está PUBLICADA, la unidad no existe, falta el peso o
     *                                      los kilos superan lo disponible
     */
    @Transactional
    public VentaDetalleDto registrar(long idUsuario, NuevaVentaDto nueva) {
        Preconditions.checkNotNull(nueva, "nueva");
        ParticipantesDto partes = conversaciones.participantes(nueva.idConversacion());
        exigirParte(partes, idUsuario);
        UnidadDto unidad = catalogo.buscarUnidad(nueva.idUnidad())
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_UNIDAD));
        PublicacionResumenDto publicacion = publicaciones.bloquearParaVenta(partes.idPublicacion())
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_NO_DISPONIBLE));
        if (!publicacion.estaPublicada()) {
            throw new ReglaNegocioException(MENSAJE_NO_DISPONIBLE);
        }
        BigDecimal peso = pesoPorUnidad(unidad, nueva.pesoUnidadKg(), publicacion);
        if (nueva.cantidad().multiply(peso).compareTo(publicacion.disponibleKg()) > 0) {
            throw new ReglaNegocioException(MENSAJE_EXCEDE.formatted(numero(publicacion.disponibleKg())));
        }

        Transaccion venta = transacciones.saveAndFlush(Transaccion.acordada(partes.idConversacion(), unidad.id(),
                nueva.cantidad(), peso, nueva.precioUnitario(), nueva.fechaRecojo(), idUsuario));
        entityManager.refresh(venta);
        LOG.info("Venta acordada: id={}, conversacion={}, registra={}, kg={}", venta.getId(),
                partes.idConversacion(), idUsuario, venta.getCantidadKg());

        Map<Long, UsuarioResumenDto> personas = usuarios.buscarResumenes(
                List.of(partes.idComprador(), partes.idAgricultor()));
        UsuarioResumenDto quien = personas.get(idUsuario);
        avisar(otraParte(partes, idUsuario), "Venta acordada", "%s registró la venta de %s de %s a S/ %s por %s. "
                .formatted(nombre(quien), cantidadConUnidad(venta.getCantidad(), unidad), producto(publicacion),
                        soles(venta.getPrecioUnitario()), unidad.nombre().toLowerCase())
                + "Recojo: %s. Celular: %s.".formatted(DIA.format(venta.getFechaRecojo()), quien.celular()),
                venta.getId());
        return detalle(venta, partes, publicacion, idUsuario);
    }

    /**
     * Cambia el estado de la venta (UC-05 A1, UC-06). "Confirmar que recibí" pasa a ENTREGADO y luego a COMPLETADA
     * en esta misma transacción.
     *
     * @param idUsuario parte que hace el cambio
     * @param idVenta   venta
     * @param accion    cambio pedido
     * @return la venta actualizada
     * @throws RecursoNoEncontradoException si la venta no existe
     * @throws AccessDeniedException        si el usuario no es parte de la venta (403)
     * @throws ReglaNegocioException        si el cambio no corresponde al estado o al usuario
     */
    @Transactional
    public VentaDetalleDto cambiarEstado(long idUsuario, long idVenta, AccionVenta accion) {
        Preconditions.checkNotNull(accion, "accion");
        Transaccion venta = transacciones.bloquear(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        ParticipantesDto partes = conversaciones.participantes(venta.getIdConversacion());
        exigirParte(partes, idUsuario);
        boolean comprador = idUsuario == partes.idComprador();
        validarAccion(venta.getEstado(), comprador, accion);

        switch (accion) {
            case RECOGIDO -> guardar(venta, EstadoTransaccion.RECOGIDO, idUsuario);
            case EN_CAMINO -> guardar(venta, EstadoTransaccion.EN_CAMINO, idUsuario);
            case CANCELAR -> guardar(venta, EstadoTransaccion.CANCELADA, idUsuario);
            case CONFIRMAR_RECEPCION -> {
                if (venta.getEstado() != EstadoTransaccion.ENTREGADO) {
                    guardar(venta, EstadoTransaccion.ENTREGADO, idUsuario);
                }
                guardar(venta, EstadoTransaccion.COMPLETADA, idUsuario);
            }
        }
        entityManager.refresh(venta);
        LOG.info("Venta {}: {} por el usuario {}", idVenta, venta.getEstado(), idUsuario);

        PublicacionResumenDto publicacion = publicaciones.buscar(partes.idPublicacion()).orElseThrow();
        UsuarioResumenDto quien = usuarios.buscarResumenes(List.of(idUsuario)).get(idUsuario);
        avisar(otraParte(partes, idUsuario), TITULOS.get(venta.getEstado()),
                textoCambio(venta.getEstado(), nombre(quien), producto(publicacion)), venta.getId());
        return detalle(venta, partes, publicacion, idUsuario);
    }

    /**
     * Detalle de una venta con su línea de tiempo y las acciones del usuario (UC-06, UC-08 paso 6).
     *
     * @param idUsuario usuario de la sesión
     * @param idVenta   venta
     * @return la venta
     * @throws RecursoNoEncontradoException si no existe
     * @throws AccessDeniedException        si el usuario no es parte de la venta (403)
     */
    @Transactional(readOnly = true)
    public VentaDetalleDto obtener(long idUsuario, long idVenta) {
        Transaccion venta = transacciones.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        ParticipantesDto partes = conversaciones.participantes(venta.getIdConversacion());
        exigirParte(partes, idUsuario);
        PublicacionResumenDto publicacion = publicaciones.buscar(partes.idPublicacion()).orElseThrow();
        return detalle(venta, partes, publicacion, idUsuario);
    }

    /**
     * Acciones que una parte puede hacer en un estado (las mismas transiciones que {@code trg_transacciones_bu}
     * permite para el acuerdo del chat).
     *
     * @param estado    estado actual
     * @param comprador {@code true} si la parte es el comprador
     * @return acciones permitidas, en el orden en que se muestran
     */
    static List<AccionVenta> accionesPermitidas(EstadoTransaccion estado, boolean comprador) {
        return switch (estado) {
            case ACORDADA -> comprador
                    ? List.of(AccionVenta.CONFIRMAR_RECEPCION, AccionVenta.RECOGIDO, AccionVenta.CANCELAR)
                    : List.of(AccionVenta.RECOGIDO, AccionVenta.CANCELAR);
            case RECOGIDO -> List.of(AccionVenta.EN_CAMINO);
            case EN_CAMINO, ENTREGADO -> comprador ? List.of(AccionVenta.CONFIRMAR_RECEPCION) : List.of();
            default -> List.of();
        };
    }

    private static void validarAccion(EstadoTransaccion estado, boolean comprador, AccionVenta accion) {
        if (estado.esFinal()) {
            throw new ReglaNegocioException(MENSAJE_TERMINADA);
        }
        if (accionesPermitidas(estado, comprador).contains(accion)) {
            return;
        }
        if (accion == AccionVenta.CANCELAR && estado != EstadoTransaccion.PROPUESTA) {
            throw new ReglaNegocioException(MENSAJE_CANCELAR_TARDE);
        }
        if (accion == AccionVenta.CONFIRMAR_RECEPCION && !comprador) {
            throw new ReglaNegocioException(MENSAJE_SOLO_COMPRADOR);
        }
        throw new ReglaNegocioException(MENSAJE_FUERA_DE_SECUENCIA);
    }

    private void guardar(Transaccion venta, EstadoTransaccion estado, long idUsuario) {
        venta.cambiarEstado(estado, idUsuario);
        transacciones.saveAndFlush(venta); // Un UPDATE por estado: el trigger valida y registra cada paso
    }

    private static void exigirParte(ParticipantesDto partes, long idUsuario) {
        if (!partes.participa(idUsuario)) {
            LOG.warn("Acceso negado a la conversación {} para el usuario {}", partes.idConversacion(), idUsuario);
            throw new AccessDeniedException("No es parte de la venta");
        }
    }

    /** Peso fijo del catálogo; el indicado para jaba o saco; o el de la publicación si es la misma unidad. */
    private static BigDecimal pesoPorUnidad(UnidadDto unidad, BigDecimal indicado, PublicacionResumenDto publicacion) {
        if (!unidad.esPesoVariable()) {
            return unidad.factorKg();
        }
        if (indicado != null) {
            return indicado;
        }
        if (Objects.equals(unidad.id(), publicacion.idUnidad())) {
            return publicacion.pesoUnidadKg();
        }
        throw new ReglaNegocioException(MENSAJE_PESO);
    }

    private VentaDetalleDto detalle(Transaccion venta, ParticipantesDto partes, PublicacionResumenDto publicacion,
            long idUsuario) {
        boolean comprador = idUsuario == partes.idComprador();
        List<HistorialTransaccion> pasos = historial.findByIdTransaccionOrderByFechaAscIdAsc(venta.getId());
        Set<Long> personas = new HashSet<>(List.of(partes.idComprador(), partes.idAgricultor()));
        pasos.stream().map(HistorialTransaccion::getIdUsuario).filter(Objects::nonNull).forEach(personas::add);
        Map<Long, UsuarioResumenDto> resumenes = usuarios.buscarResumenes(personas);
        UsuarioResumenDto otra = resumenes.get(otraParte(partes, idUsuario));
        UnidadDto unidad = catalogo.buscarUnidad(venta.getIdUnidad()).orElseThrow();
        List<PasoDto> linea = pasos.stream()
                .map(p -> new PasoDto(p.getEstado().name(), p.getFecha(),
                        p.getIdUsuario() == null ? null : resumenes.get(p.getIdUsuario()).nombreCompleto(),
                        p.getIdUsuario() != null && p.getIdUsuario() == idUsuario))
                .toList();
        return new VentaDetalleDto(venta.getId(), venta.getIdConversacion(), partes.idPublicacion(), comprador,
                new ContraparteDto(otra.id(), otra.nombreCompleto(), comprador ? null : otra.nombreNegocio(),
                        otra.celular()),
                publicacion.cultivo(), publicacion.variedad(), publicacion.distrito(), unidad.id(), unidad.nombre(),
                unidad.nombrePlural(), venta.getCantidad(), venta.getPesoUnidadKg(), venta.getCantidadKg(),
                venta.getPrecioUnitario(), venta.getPrecioUnitario().divide(venta.getPesoUnidadKg(), 2,
                        RoundingMode.HALF_UP),
                venta.getMontoTotal(), venta.getFechaRecojo(), venta.getEstado().name(), venta.getFechaCreacion(),
                venta.getFechaActualizacion(), linea,
                venta.getIdPropuestaGrupal() == null ? accionesPermitidas(venta.getEstado(), comprador) : List.of(),
                calificaciones(venta, idUsuario, resumenes));
    }

    /** La calificación que dio el usuario, la que recibió y si todavía puede calificar (RF-05). */
    private CalificacionesVentaDto calificaciones(Transaccion venta, long idUsuario,
            Map<Long, UsuarioResumenDto> resumenes) {
        CalificacionDto mia = null;
        CalificacionDto recibida = null;
        for (Calificacion c : calificaciones.findByIdTransaccion(venta.getId())) {
            UsuarioResumenDto autor = resumenes.get(c.getIdEvaluador());
            CalificacionDto dto = new CalificacionDto(c.getPuntuacion(), c.getComentario(), c.getFecha(),
                    StringUtils.defaultIfBlank(autor.nombreNegocio(), autor.nombreCompleto()));
            if (c.getIdEvaluador() == idUsuario) {
                mia = dto;
            } else {
                recibida = dto;
            }
        }
        return new CalificacionesVentaDto(venta.getEstado() == EstadoTransaccion.COMPLETADA && mia == null, mia,
                recibida);
    }

    private void avisar(long idDestinatario, String titulo, String texto, long idVenta) {
        notificaciones.crear(new NuevaNotificacionDto(idDestinatario, "VENTA", titulo, texto, idVenta));
    }

    private static final Map<EstadoTransaccion, String> TITULOS = Map.of(
            EstadoTransaccion.RECOGIDO, "Ya recogieron",
            EstadoTransaccion.EN_CAMINO, "En camino",
            EstadoTransaccion.COMPLETADA, "Venta terminada",
            EstadoTransaccion.CANCELADA, "Venta cancelada");

    private static String textoCambio(EstadoTransaccion estado, String quien, String producto) {
        return switch (estado) {
            case RECOGIDO -> "%s marcó que ya recogieron %s.".formatted(quien, producto);
            case EN_CAMINO -> "%s marcó que %s va en camino.".formatted(quien, producto);
            case COMPLETADA -> "%s confirmó que recibió %s. La venta terminó.".formatted(quien, producto);
            case CANCELADA -> "%s canceló la venta de %s.".formatted(quien, producto);
            default -> "La venta de %s cambió de estado.".formatted(producto);
        };
    }

    private static long otraParte(ParticipantesDto partes, long idUsuario) {
        return idUsuario == partes.idComprador() ? partes.idAgricultor() : partes.idComprador();
    }

    private static String nombre(UsuarioResumenDto u) {
        return StringUtils.isBlank(u.nombreNegocio()) ? u.nombreCompleto()
                : "%s (%s)".formatted(u.nombreCompleto(), u.nombreNegocio());
    }

    private static String producto(PublicacionResumenDto p) {
        return "Común".equals(p.variedad()) ? p.cultivo() + " común" : p.cultivo() + " " + p.variedad();
    }

    private static String cantidadConUnidad(BigDecimal cantidad, UnidadDto unidad) {
        return numero(cantidad) + " " + (cantidad.compareTo(BigDecimal.ONE) == 0
                ? unidad.nombre().toLowerCase() : unidad.nombrePlural());
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static String soles(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
