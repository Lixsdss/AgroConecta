package pe.agroconecta.ventaagrupada.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.catalogo.aplicacion.VariedadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.mensajeria.aplicacion.ConversacionService;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.publicacion.aplicacion.PublicacionResumenDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;
import pe.agroconecta.venta.aplicacion.ParteDto;
import pe.agroconecta.venta.aplicacion.VentaService;
import pe.agroconecta.ventaagrupada.dominio.ClaveLote;
import pe.agroconecta.ventaagrupada.dominio.EstadoPropuesta;
import pe.agroconecta.ventaagrupada.dominio.LoteAgrupado;
import pe.agroconecta.ventaagrupada.dominio.PropuestaGrupal;
import pe.agroconecta.ventaagrupada.infraestructura.LoteAgrupadoRepository;
import pe.agroconecta.ventaagrupada.infraestructura.PropuestaGrupalRepository;

/**
 * Propuestas conjuntas (RF-10, UC-12): el comprador propone comprar parte de un lote; el servicio reparte los kilos en
 * proporción a lo disponible de cada cosecha ({@link Reparto}) y crea una venta PROPUESTA por cosecha a través del
 * módulo {@code venta}. Cerrar o cancelar la propuesta cancela las partes pendientes. Vencer las propuestas lo hace
 * {@code ev_tareas_diarias}.
 */
@Service
public class PropuestaService {

    /** Propuestas por página. */
    public static final int TAMANO_PAGINA = 20;

    static final String MENSAJE_SIN_LOTE =
            "Ese lote ya no está disponible: necesita 2 o más agricultores con cosechas a la venta.";
    static final String MENSAJE_EXCEDE_LOTE = "La cantidad supera lo disponible en el lote (≈ %s kg).";
    static final String MENSAJE_MUY_POCO = "La cantidad es muy pequeña: a ningún agricultor le toca una unidad entera.";
    static final String MENSAJE_NO_EXISTE = "La propuesta no existe.";
    static final String MENSAJE_TERMINADA = "La propuesta ya está cerrada o cancelada.";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

    /** Estados de una parte que ya es una venta acordada (o la siguió). */
    private static final Set<String> ACORDADAS = Set.of("ACORDADA", "RECOGIDO", "EN_CAMINO", "ENTREGADO", "COMPLETADA");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Logger LOG = LoggerFactory.getLogger(PropuestaService.class);

    private final PropuestaGrupalRepository propuestas;
    private final LoteAgrupadoRepository lotes;
    private final PublicacionConsultaService publicaciones;
    private final ConversacionService conversaciones;
    private final VentaService ventas;
    private final CatalogoService catalogo;
    private final UsuarioConsultaService usuarios;
    private final NotificacionService notificaciones;

    /**
     * Crea el servicio.
     *
     * @param propuestas     repositorio de propuestas
     * @param lotes          vista de lotes
     * @param publicaciones  cosechas del lote
     * @param conversaciones conversación de cada parte
     * @param ventas         partes como ventas PROPUESTA
     * @param catalogo       nombres de variedad y distrito
     * @param usuarios       nombre del comprador
     * @param notificaciones avisos PROPUESTA_CONJUNTA
     */
    public PropuestaService(PropuestaGrupalRepository propuestas, LoteAgrupadoRepository lotes,
            PublicacionConsultaService publicaciones, ConversacionService conversaciones, VentaService ventas,
            CatalogoService catalogo, UsuarioConsultaService usuarios, NotificacionService notificaciones) {
        this.propuestas = propuestas;
        this.lotes = lotes;
        this.publicaciones = publicaciones;
        this.conversaciones = conversaciones;
        this.ventas = ventas;
        this.catalogo = catalogo;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
    }

    /**
     * "Enviar propuesta conjunta" (UC-12 pasos 2–6): registra la propuesta, reparte los kilos y crea una parte por
     * cosecha, con aviso a cada agricultor.
     *
     * @param idComprador comprador de la sesión
     * @param nueva       lote, kilos, precio por kilo y día de recojo
     * @return la propuesta con sus partes
     * @throws ReglaNegocioException si ya no hay lote, la cantidad supera el lote o a nadie le toca una unidad
     */
    @Transactional
    public PropuestaDto crear(long idComprador, NuevaPropuestaDto nueva) {
        Preconditions.checkNotNull(nueva, "nueva");
        LoteAgrupado lote = lotes.findById(new ClaveLote(nueva.idVariedad(), nueva.idDistrito()))
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_SIN_LOTE));
        if (nueva.cantidadKg().compareTo(lote.getVolumenKg()) > 0) {
            throw new ReglaNegocioException(MENSAJE_EXCEDE_LOTE.formatted(numero(lote.getVolumenKg())));
        }
        List<PublicacionResumenDto> cosechas = publicaciones.cosechasDeLote(nueva.idVariedad(), nueva.idDistrito());
        Map<Long, PublicacionResumenDto> porId = cosechas.stream()
                .collect(Collectors.toMap(PublicacionResumenDto::id, Function.identity()));
        List<Reparto.Parte> reparto = Reparto.repartir(nueva.cantidadKg(), cosechas.stream()
                .map(c -> new Reparto.Cosecha(c.id(), c.disponibleKg(), c.pesoUnidadKg())).toList());
        if (reparto.isEmpty()) {
            throw new ReglaNegocioException(MENSAJE_MUY_POCO);
        }

        PropuestaGrupal propuesta = propuestas.saveAndFlush(new PropuestaGrupal(idComprador, nueva.idVariedad(),
                nueva.idDistrito(), nueva.cantidadKg(), nueva.precioKg(), nueva.fechaRecojo()));
        UsuarioResumenDto comprador = usuarios.buscarResumenes(List.of(idComprador)).get(idComprador);
        String quien = StringUtils.defaultIfBlank(comprador.nombreNegocio(), comprador.nombreCompleto());
        for (Reparto.Parte parte : reparto) {
            PublicacionResumenDto cosecha = porId.get(parte.idPublicacion());
            BigDecimal precioUnitario = nueva.precioKg().multiply(cosecha.pesoUnidadKg()).setScale(2, RoundingMode.HALF_UP);
            long idConversacion = conversaciones.abrirParaPropuesta(idComprador, cosecha.id());
            long idVenta = ventas.registrarPropuesta(idComprador, idConversacion, propuesta.getId(),
                    cosecha.idUnidad(), parte.unidades(), cosecha.pesoUnidadKg(), precioUnitario, nueva.fechaRecojo());
            String unidad = parte.unidades().compareTo(BigDecimal.ONE) == 0
                    ? cosecha.unidad().toLowerCase() : cosecha.unidadPlural();
            notificaciones.crear(new NuevaNotificacionDto(cosecha.idAgricultor(), "PROPUESTA_CONJUNTA",
                    "Propuesta conjunta", "%s le propone comprar %s %s de su %s a S/ %s por %s. Responda antes del %s."
                            .formatted(quien, numero(parte.unidades()), unidad, producto(cosecha),
                                    precioUnitario.toPlainString(), cosecha.unidad().toLowerCase(),
                                    DIA.format(nueva.fechaRecojo())), idVenta));
        }
        LOG.info("Propuesta conjunta {}: comprador={}, lote=({}, {}), kg={}, partes={}", propuesta.getId(),
                idComprador, nueva.idVariedad(), nueva.idDistrito(), nueva.cantidadKg(), reparto.size());
        return aDto(propuesta);
    }

    /**
     * "Mis propuestas", la más reciente primero (20 por página).
     *
     * @param idComprador comprador de la sesión
     * @param pagina      página desde 0
     * @return página de propuestas con sus partes
     * @throws SolicitudInvalidaException si la página es negativa
     */
    @Transactional(readOnly = true)
    public Page<PropuestaDto> listar(long idComprador, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        return propuestas.findByIdCompradorOrderByFechaCreacionDescIdDesc(idComprador,
                PageRequest.of(pagina, TAMANO_PAGINA)).map(this::aDto);
    }

    /**
     * Avance de una propuesta (UC-12 paso 8).
     *
     * @param idComprador comprador de la sesión
     * @param idPropuesta propuesta
     * @return la propuesta con sus partes
     */
    @Transactional(readOnly = true)
    public PropuestaDto obtener(long idComprador, long idPropuesta) {
        return aDto(propia(idComprador, idPropuesta));
    }

    /**
     * Cierra ("ya tengo lo que necesitaba") o cancela ("ya no la quiero") la propuesta y cancela las partes
     * pendientes; las acordadas siguen su ciclo (UC-12 paso 9 y A3).
     *
     * @param idComprador comprador de la sesión
     * @param idPropuesta propuesta
     * @param cerrar      {@code true} para cerrar; {@code false} para cancelar
     * @return la propuesta actualizada
     * @throws ReglaNegocioException si ya no está ABIERTA
     */
    @Transactional
    public PropuestaDto terminar(long idComprador, long idPropuesta, boolean cerrar) {
        PropuestaGrupal propuesta = propia(idComprador, idPropuesta);
        if (propuesta.getEstado() != EstadoPropuesta.ABIERTA) {
            throw new ReglaNegocioException(MENSAJE_TERMINADA);
        }
        propuesta.terminar(cerrar ? EstadoPropuesta.CERRADA : EstadoPropuesta.CANCELADA);
        propuestas.saveAndFlush(propuesta);
        ventas.cancelarPartesPendientes(idPropuesta, idComprador, cerrar);
        return aDto(propuesta);
    }

    private PropuestaGrupal propia(long idComprador, long idPropuesta) {
        PropuestaGrupal propuesta = propuestas.findById(idPropuesta)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        if (propuesta.getIdComprador() != idComprador) {
            LOG.warn("Acceso negado a la propuesta {} para el usuario {}", idPropuesta, idComprador);
            throw new AccessDeniedException("No es dueño de la propuesta");
        }
        return propuesta;
    }

    private PropuestaDto aDto(PropuestaGrupal p) {
        List<ParteDto> partes = ventas.partesDePropuesta(p.getId());
        VariedadDto variedad = catalogo.buscarVariedad(p.getIdVariedad()).orElseThrow();
        DistritoDto distrito = catalogo.buscarDistrito(p.getIdDistrito()).orElseThrow();
        return new PropuestaDto(p.getId(), p.getIdVariedad(), variedad.cultivo(), variedad.nombre(), p.getIdDistrito(),
                distrito.nombre(), p.getCantidadKg(), p.getPrecioKg(), p.getFechaRecojo(), p.getEstado().name(),
                p.getFechaCreacion(), sumar(partes, null), sumar(partes, ACORDADAS), sumar(partes, Set.of("PROPUESTA")),
                partes);
    }

    /** Suma los kilos de las partes en esos estados ({@code null}: todas). */
    private static BigDecimal sumar(List<ParteDto> partes, Set<String> estados) {
        return partes.stream().filter(p -> estados == null || estados.contains(p.estado()))
                .map(ParteDto::cantidadKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String producto(PublicacionResumenDto p) {
        return "Común".equals(p.variedad()) ? p.cultivo() + " común" : p.cultivo() + " " + p.variedad();
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
