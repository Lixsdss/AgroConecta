package pe.agroconecta.mensajeria.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.mensajeria.dominio.CanalInicio;
import pe.agroconecta.mensajeria.dominio.Conversacion;
import pe.agroconecta.mensajeria.dominio.Mensaje;
import pe.agroconecta.mensajeria.dominio.TipoMensaje;
import pe.agroconecta.mensajeria.infraestructura.ConteoPorConversacion;
import pe.agroconecta.mensajeria.infraestructura.ConversacionRepository;
import pe.agroconecta.mensajeria.infraestructura.MensajeRepository;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.publicacion.aplicacion.PublicacionResumenDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;

/**
 * Chat entre comprador y agricultor (RF-04, UC-04): abrir la conversación, contacto por WhatsApp, mensajes de
 * texto y ofertas, leídos y no leídos. Valida con mensajes amigables lo mismo que {@code trg_conversaciones_bi} y
 * {@code trg_mensajes_bi} (la base de datos es la última barrera) y avisa a la otra parte con
 * {@code notificacion.aplicacion} en la misma transacción.
 */
@Service
public class ConversacionService {

    /** Conversaciones por página en "Mensajes". */
    public static final int TAMANO_PAGINA = 20;

    static final String MENSAJE_PUBLICACION_NO_EXISTE = "La publicación no existe.";
    static final String MENSAJE_NO_DISPONIBLE = "Solo se puede contactar una publicación disponible.";
    static final String MENSAJE_CONVERSACION_NO_EXISTE = "La conversación no existe.";
    static final String MENSAJE_OFERTA_NO_DISPONIBLE = "Solo se hacen ofertas mientras la publicación está disponible.";
    static final String MENSAJE_OFERTA_EXCEDE = "La oferta supera lo disponible (≈ %s %s).";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

    private static final String COMPRADOR = "COMPRADOR";
    private static final Logger LOG = LoggerFactory.getLogger(ConversacionService.class);

    private final ConversacionRepository conversaciones;
    private final MensajeRepository mensajes;
    private final PublicacionConsultaService publicaciones;
    private final UsuarioConsultaService usuarios;
    private final NotificacionService notificaciones;
    private final EntityManager entityManager;

    /**
     * Crea el servicio.
     *
     * @param conversaciones repositorio de conversaciones
     * @param mensajes       repositorio de mensajes
     * @param publicaciones  consultas de publicaciones
     * @param usuarios       consultas de usuarios (nombres, negocio y celular)
     * @param notificaciones avisos a la otra parte
     * @param entityManager  contexto de persistencia (para leer la fecha que pone la base de datos)
     */
    public ConversacionService(ConversacionRepository conversaciones, MensajeRepository mensajes,
            PublicacionConsultaService publicaciones, UsuarioConsultaService usuarios,
            NotificacionService notificaciones, EntityManager entityManager) {
        this.conversaciones = conversaciones;
        this.mensajes = mensajes;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.entityManager = entityManager;
    }

    /**
     * "Escribir": abre la conversación del comprador con una publicación, o devuelve la que ya tenía.
     *
     * @param idComprador   comprador de la sesión
     * @param idPublicacion publicación
     * @return la conversación y si se creó ahora
     * @throws RecursoNoEncontradoException si la publicación no existe
     * @throws ReglaNegocioException        si no había conversación y la publicación no está PUBLICADA
     */
    @Transactional
    public AperturaDto abrir(long idComprador, long idPublicacion) {
        PublicacionResumenDto publicacion = publicacion(idPublicacion);
        Conversacion existente = conversaciones.findByIdPublicacionAndIdComprador(idPublicacion, idComprador)
                .orElse(null);
        if (existente != null) {
            return new AperturaDto(aDto(existente, idComprador, publicacion), false);
        }
        Conversacion nueva = crear(idComprador, publicacion, CanalInicio.CHAT);
        return new AperturaDto(aDto(nueva, idComprador, publicacion), true);
    }

    /**
     * Conversación de una parte de la venta agrupada (RF-10): la del comprador con esa publicación, o una nueva con
     * {@code canal_inicio} = PROPUESTA_CONJUNTA si no existía. Se usa dentro de la transacción de la propuesta.
     *
     * @param idComprador   comprador de la propuesta
     * @param idPublicacion publicación de la parte
     * @return id de la conversación
     * @throws RecursoNoEncontradoException si la publicación no existe
     * @throws ReglaNegocioException        si no había conversación y la publicación no está PUBLICADA
     */
    @Transactional
    public long abrirParaPropuesta(long idComprador, long idPublicacion) {
        return conversaciones.findByIdPublicacionAndIdComprador(idPublicacion, idComprador)
                .orElseGet(() -> crear(idComprador, publicacion(idPublicacion), CanalInicio.PROPUESTA_CONJUNTA))
                .getId();
    }

    /**
     * "WhatsApp" (UC-04 A1): crea la conversación si no existía, suma un clic y devuelve el enlace {@code wa.me}
     * con el celular del agricultor y un texto sugerido.
     *
     * @param idComprador   comprador de la sesión
     * @param idPublicacion publicación
     * @return conversación y enlace
     * @throws RecursoNoEncontradoException si la publicación no existe
     * @throws ReglaNegocioException        si no había conversación y la publicación no está PUBLICADA
     */
    @Transactional
    public EnlaceWhatsappDto whatsapp(long idComprador, long idPublicacion) {
        PublicacionResumenDto publicacion = publicacion(idPublicacion);
        Conversacion conversacion = conversaciones.findByIdPublicacionAndIdComprador(idPublicacion, idComprador)
                .orElseGet(() -> crear(idComprador, publicacion, CanalInicio.WHATSAPP));
        conversacion.sumarClicWhatsapp();
        conversaciones.save(conversacion);
        UsuarioResumenDto agricultor = usuarios.buscarResumenes(List.of(publicacion.idAgricultor()))
                .get(publicacion.idAgricultor());
        LOG.info("Contacto por WhatsApp: conversacion={}, publicacion={}, clics={}", conversacion.getId(),
                idPublicacion, conversacion.getClicsWhatsapp());
        return new EnlaceWhatsappDto(conversacion.getId(), enlaceWhatsapp(agricultor, publicacion));
    }

    /**
     * Partes de una conversación, para otros módulos (por ejemplo, {@code venta} al registrar un acuerdo).
     *
     * @param idConversacion conversación
     * @return publicación, comprador y agricultor
     * @throws RecursoNoEncontradoException si la conversación no existe
     */
    @Transactional(readOnly = true)
    public ParticipantesDto participantes(long idConversacion) {
        Conversacion c = conversaciones.findById(idConversacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_CONVERSACION_NO_EXISTE));
        PublicacionResumenDto publicacion = publicacion(c.getIdPublicacion());
        return new ParticipantesDto(c.getId(), c.getIdPublicacion(), c.getIdComprador(), publicacion.idAgricultor());
    }

    /**
     * "Mensajes": conversaciones del usuario, con actividad más reciente primero.
     *
     * @param idUsuario usuario de la sesión
     * @param rol       {@code COMPRADOR} o {@code AGRICULTOR}
     * @param pagina    página desde 0 (20 por página)
     * @return página de conversaciones
     * @throws SolicitudInvalidaException si la página es negativa
     */
    @Transactional(readOnly = true)
    public Page<ConversacionDto> listar(long idUsuario, String rol, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        PageRequest pedido = PageRequest.of(pagina, TAMANO_PAGINA);
        Page<Conversacion> encontradas;
        if (COMPRADOR.equals(rol)) {
            encontradas = conversaciones.deComprador(idUsuario, pedido);
        } else {
            List<Long> propias = publicaciones.idsDeAgricultor(idUsuario);
            encontradas = propias.isEmpty() ? Page.empty(pedido) : conversaciones.dePublicaciones(propias, pedido);
        }
        if (encontradas.isEmpty()) {
            return encontradas.map(c -> null);
        }
        List<Long> ids = encontradas.map(Conversacion::getId).getContent();
        Map<Long, Mensaje> ultimos = mensajes.ultimos(ids).stream()
                .collect(Collectors.toMap(Mensaje::getIdConversacion, Function.identity()));
        Map<Long, Long> noLeidos = mensajes.noLeidos(ids, idUsuario).stream()
                .collect(Collectors.toMap(ConteoPorConversacion::idConversacion, ConteoPorConversacion::total));
        Map<Long, PublicacionResumenDto> productos = publicaciones.buscarVarias(
                encontradas.map(Conversacion::getIdPublicacion).toSet());
        Set<Long> personas = new HashSet<>();
        encontradas.forEach(c -> {
            personas.add(c.getIdComprador());
            personas.add(productos.get(c.getIdPublicacion()).idAgricultor());
        });
        Map<Long, UsuarioResumenDto> resumenes = usuarios.buscarResumenes(personas);
        return encontradas.map(c -> aDto(c, idUsuario, productos.get(c.getIdPublicacion()), resumenes,
                ultimos.get(c.getId()), noLeidos.getOrDefault(c.getId(), 0L)));
    }

    /**
     * Total de mensajes no leídos del usuario (aviso del inicio).
     *
     * @param idUsuario usuario de la sesión
     * @param rol       {@code COMPRADOR} o {@code AGRICULTOR}
     * @return total
     */
    @Transactional(readOnly = true)
    public long noLeidos(long idUsuario, String rol) {
        if (COMPRADOR.equals(rol)) {
            return mensajes.noLeidosDeComprador(idUsuario);
        }
        List<Long> propias = publicaciones.idsDeAgricultor(idUsuario);
        return propias.isEmpty() ? 0 : mensajes.noLeidosDeAgricultor(propias, idUsuario);
    }

    /**
     * Cabecera del chat.
     *
     * @param idUsuario      usuario de la sesión
     * @param idConversacion conversación
     * @return la conversación
     * @throws RecursoNoEncontradoException si no existe
     * @throws AccessDeniedException        si el usuario no participa (403)
     */
    @Transactional(readOnly = true)
    public ConversacionDto obtener(long idUsuario, long idConversacion) {
        Participacion p = participacion(idUsuario, idConversacion);
        return aDto(p.conversacion(), idUsuario, p.publicacion());
    }

    /**
     * Mensajes de la conversación, del más antiguo al más reciente, y marca como leídos los de la otra parte.
     *
     * @param idUsuario      usuario de la sesión
     * @param idConversacion conversación
     * @param despuesDe      id del último mensaje que ya se tiene, o {@code null} para todos
     * @return mensajes
     * @throws RecursoNoEncontradoException si no existe
     * @throws AccessDeniedException        si el usuario no participa (403)
     */
    @Transactional
    public List<MensajeDto> mensajes(long idUsuario, long idConversacion, Long despuesDe) {
        Participacion p = participacion(idUsuario, idConversacion);
        mensajes.marcarLeidos(idConversacion, idUsuario);
        return mensajes.findByIdConversacionAndIdGreaterThanOrderByIdAsc(idConversacion,
                        despuesDe == null ? 0L : despuesDe).stream()
                .map(m -> aDto(m, idUsuario, p.publicacion()))
                .toList();
    }

    /**
     * Envía un mensaje de texto o una oferta y avisa a la otra parte.
     *
     * @param idUsuario      usuario de la sesión
     * @param idConversacion conversación
     * @param nuevo          texto u oferta
     * @return el mensaje guardado
     * @throws RecursoNoEncontradoException si la conversación no existe
     * @throws AccessDeniedException        si el usuario no participa (403)
     * @throws ReglaNegocioException        si es una oferta y la publicación no está PUBLICADA o la cantidad
     *                                      supera lo disponible
     */
    @Transactional
    public MensajeDto enviar(long idUsuario, long idConversacion, NuevoMensajeDto nuevo) {
        Preconditions.checkNotNull(nuevo, "nuevo");
        Participacion p = participacion(idUsuario, idConversacion);
        PublicacionResumenDto publicacion = p.publicacion();
        Mensaje mensaje;
        if (TipoMensaje.OFERTA.name().equals(nuevo.tipo())) {
            validarOferta(publicacion, nuevo.ofertaCantidad());
            mensaje = Mensaje.oferta(idConversacion, idUsuario, nuevo.ofertaCantidad(), nuevo.ofertaPrecioUnitario());
        } else {
            mensaje = Mensaje.texto(idConversacion, idUsuario, nuevo.contenido().strip());
        }
        Mensaje guardado = mensajes.saveAndFlush(mensaje);
        entityManager.refresh(guardado);
        avisar(p, idUsuario, guardado);
        LOG.info("Mensaje enviado: conversacion={}, remitente={}, tipo={}", idConversacion, idUsuario,
                guardado.getTipo());
        return aDto(guardado, idUsuario, publicacion);
    }

    private PublicacionResumenDto publicacion(long idPublicacion) {
        return publicaciones.buscar(idPublicacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_PUBLICACION_NO_EXISTE));
    }

    private Conversacion crear(long idComprador, PublicacionResumenDto publicacion, CanalInicio canal) {
        if (!publicacion.estaPublicada()) {
            throw new ReglaNegocioException(MENSAJE_NO_DISPONIBLE);
        }
        Conversacion nueva = conversaciones.saveAndFlush(new Conversacion(publicacion.id(), idComprador, canal));
        entityManager.refresh(nueva);
        LOG.info("Conversación abierta: id={}, publicacion={}, comprador={}, canal={}", nueva.getId(),
                publicacion.id(), idComprador, canal);
        return nueva;
    }

    private void validarOferta(PublicacionResumenDto publicacion, BigDecimal cantidad) {
        if (!publicacion.estaPublicada()) {
            throw new ReglaNegocioException(MENSAJE_OFERTA_NO_DISPONIBLE);
        }
        if (cantidad.compareTo(publicacion.unidadesDisponibles()) > 0) {
            throw new ReglaNegocioException(MENSAJE_OFERTA_EXCEDE.formatted(
                    numero(publicacion.unidadesDisponibles()), publicacion.unidadPlural()));
        }
    }

    /** Conversación y publicación, si el usuario es el comprador o el agricultor. */
    private Participacion participacion(long idUsuario, long idConversacion) {
        Conversacion conversacion = conversaciones.findById(idConversacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_CONVERSACION_NO_EXISTE));
        PublicacionResumenDto publicacion = publicacion(conversacion.getIdPublicacion());
        if (idUsuario != conversacion.getIdComprador() && idUsuario != publicacion.idAgricultor()) {
            LOG.warn("Acceso negado a la conversación {} para el usuario {}", idConversacion, idUsuario);
            throw new AccessDeniedException("No participa en la conversación");
        }
        return new Participacion(conversacion, publicacion);
    }

    /** OFERTA: siempre. MENSAJE: solo si la otra parte no tiene ya un aviso sin leer de esta conversación. */
    private void avisar(Participacion p, long idRemitente, Mensaje mensaje) {
        Conversacion c = p.conversacion();
        PublicacionResumenDto publicacion = p.publicacion();
        long idDestinatario = idRemitente == c.getIdComprador() ? publicacion.idAgricultor() : c.getIdComprador();
        UsuarioResumenDto remitente = usuarios.buscarResumenes(List.of(idRemitente)).get(idRemitente);
        String quien = StringUtils.defaultIfBlank(remitente.nombreNegocio(), remitente.nombreCompleto());
        String producto = producto(publicacion);
        if (mensaje.getTipo() == TipoMensaje.OFERTA) {
            notificaciones.crear(new NuevaNotificacionDto(idDestinatario, "OFERTA", "Nueva oferta",
                    "%s le ofrece %s %s a S/ %s por %s (%s).".formatted(quien, numero(mensaje.getOfertaCantidad()),
                            mensaje.getOfertaCantidad().compareTo(BigDecimal.ONE) == 0
                                    ? publicacion.unidad().toLowerCase() : publicacion.unidadPlural(),
                            soles(mensaje.getOfertaPrecioUnitario()), publicacion.unidad().toLowerCase(), producto),
                    c.getId()));
        } else {
            notificaciones.crearSiNoHayPendiente(new NuevaNotificacionDto(idDestinatario, "MENSAJE",
                    "Nuevo mensaje", "%s le escribió sobre %s.".formatted(quien, producto), c.getId()));
        }
    }

    private ConversacionDto aDto(Conversacion c, long idUsuario, PublicacionResumenDto publicacion) {
        Map<Long, UsuarioResumenDto> resumenes = usuarios.buscarResumenes(
                List.of(c.getIdComprador(), publicacion.idAgricultor()));
        Mensaje ultimo = mensajes.ultimos(List.of(c.getId())).stream().findFirst().orElse(null);
        long noLeidos = mensajes.noLeidos(List.of(c.getId()), idUsuario).stream()
                .mapToLong(ConteoPorConversacion::total).sum();
        return aDto(c, idUsuario, publicacion, resumenes, ultimo, noLeidos);
    }

    private static ConversacionDto aDto(Conversacion c, long idUsuario, PublicacionResumenDto publicacion,
            Map<Long, UsuarioResumenDto> resumenes, Mensaje ultimo, long noLeidos) {
        boolean soyAgricultor = idUsuario == publicacion.idAgricultor();
        UsuarioResumenDto otra = resumenes.get(soyAgricultor ? c.getIdComprador() : publicacion.idAgricultor());
        OtraParteDto otraParte = new OtraParteDto(otra.id(), otra.nombreCompleto(),
                soyAgricultor ? otra.nombreNegocio() : null);
        ProductoChatDto producto = new ProductoChatDto(publicacion.cultivo(), publicacion.variedad(),
                publicacion.distrito(), publicacion.estado(), publicacion.unidad(), publicacion.unidadPlural(),
                publicacion.pesoUnidadKg(), publicacion.precioUnitario(), publicacion.precioKg(),
                publicacion.disponibleKg(), publicacion.unidadesDisponibles(), publicacion.diasRestantes());
        UltimoMensajeDto ultimoDto = ultimo == null ? null : new UltimoMensajeDto(ultimo.getTipo().name(),
                ultimo.getContenido(), ultimo.getOfertaCantidad(), ultimo.getOfertaPrecioUnitario(),
                ultimo.getIdRemitente() == idUsuario, ultimo.getFechaEnvio());
        return new ConversacionDto(c.getId(), c.getIdPublicacion(), c.getCanalInicio().name(), c.getFechaInicio(),
                soyAgricultor, otraParte, producto, ultimoDto, noLeidos);
    }

    private static MensajeDto aDto(Mensaje m, long idUsuario, PublicacionResumenDto publicacion) {
        boolean oferta = m.getTipo() == TipoMensaje.OFERTA;
        return new MensajeDto(m.getId(), m.getIdRemitente(), m.getIdRemitente() == idUsuario, m.getTipo().name(),
                m.getContenido(), m.getOfertaCantidad(), m.getOfertaPrecioUnitario(),
                oferta ? m.getOfertaCantidad().multiply(publicacion.pesoUnidadKg()).setScale(2, RoundingMode.HALF_UP)
                        : null,
                oferta ? m.getOfertaCantidad().multiply(m.getOfertaPrecioUnitario()).setScale(2, RoundingMode.HALF_UP)
                        : null,
                m.getFechaEnvio());
    }

    /** "Hola, {agricultor}. Vi su {producto} en AgroConecta ({cantidad} {unidad} a S/ {precio} por {unidad})." */
    static String enlaceWhatsapp(UsuarioResumenDto agricultor, PublicacionResumenDto p) {
        String texto = "Hola, %s. Vi su %s en AgroConecta (%s %s a S/ %s por %s). ¿Sigue disponible?".formatted(
                agricultor.nombreCompleto(), producto(p), numero(p.unidadesDisponibles()), p.unidadPlural(),
                soles(p.precioUnitario()), p.unidad().toLowerCase());
        return "https://wa.me/51" + agricultor.celular() + "?text="
                + URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String producto(PublicacionResumenDto p) {
        return "Común".equals(p.variedad()) ? p.cultivo() + " común" : p.cultivo() + " " + p.variedad();
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static String soles(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Conversación con su publicación, ya comprobado que el usuario participa. */
    private record Participacion(Conversacion conversacion, PublicacionResumenDto publicacion) {
    }
}
