package pe.agroconecta.administracion.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.administracion.dominio.MotivoSuspension;
import pe.agroconecta.administracion.dominio.ResumenUsuario;
import pe.agroconecta.administracion.dominio.Suspension;
import pe.agroconecta.administracion.infraestructura.ResumenUsuarioRepository;
import pe.agroconecta.administracion.infraestructura.SuspensionRepository;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.usuario.aplicacion.CuentaDto;
import pe.agroconecta.usuario.aplicacion.GestionCuentasService;
import pe.agroconecta.usuario.aplicacion.NuevoAdministradorDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;
import pe.agroconecta.venta.aplicacion.VentaEnCursoDto;
import pe.agroconecta.venta.aplicacion.VentaService;

/**
 * Pantalla "Usuarios" (RF-09, UC-11 parte 1): lista, detalle, suspender y reactivar ({@code suspensiones}), y las
 * acciones de cuenta del módulo {@code usuario} (contraseña temporal, desactivar, activar, crear administrador).
 * Valida con mensajes amigables lo mismo que {@code trg_suspensiones_bi}. Al suspender avisa al usuario
 * (CUENTA) y a la otra parte de sus ventas ACORDADA o PROPUESTA (VENTA, UC-11 A1); las ventas no cambian solas.
 */
@Service
public class UsuarioAdminService {

    /** Usuarios por página. */
    public static final int TAMANO_PAGINA = 20;

    static final String MENSAJE_NO_EXISTE = "El usuario no existe.";
    static final String MENSAJE_SUSPENDER_ADMIN = "No se suspende a un administrador.";
    static final String MENSAJE_YA_SUSPENDIDO = "El usuario ya está suspendido.";
    static final String MENSAJE_SOLO_ACTIVA = "Solo se suspende una cuenta activa.";
    static final String MENSAJE_FUNDADOS = "Se requieren al menos 3 reclamos fundados (tiene %d).";
    static final String MENSAJE_NO_SUSPENDIDO = "El usuario no está suspendido.";
    static final String MENSAJE_FILTRO = "El filtro de rol o estado no es válido.";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";

    /** Reclamos fundados que exige el motivo RECLAMOS_FUNDADOS. */
    static final int MINIMO_FUNDADOS = 3;

    private static final Set<String> ROLES = Set.of("AGRICULTOR", "COMPRADOR", "ADMINISTRADOR");
    private static final Set<String> ESTADOS = Set.of("ACTIVO", "SUSPENDIDO", "INACTIVO");
    private static final Set<String> AVISAR_CONTRAPARTE = Set.of("ACORDADA", "PROPUESTA");
    private static final Map<MotivoSuspension, String> MOTIVOS = ImmutableMap.of(
            MotivoSuspension.RECLAMOS_FUNDADOS, "reclamos fundados",
            MotivoSuspension.DATOS_FALSOS, "datos falsos",
            MotivoSuspension.PUBLICACIONES_ENGANOSAS, "publicaciones engañosas",
            MotivoSuspension.USO_INDEBIDO, "uso indebido");

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioAdminService.class);

    private final ResumenUsuarioRepository resumenes;
    private final SuspensionRepository suspensiones;
    private final GestionCuentasService cuentas;
    private final UsuarioConsultaService usuarios;
    private final VentaService ventas;
    private final NotificacionService notificaciones;
    private final EntityManager entityManager;

    /**
     * Crea el servicio.
     *
     * @param resumenes      vista {@code v_resumen_usuario}
     * @param suspensiones   repositorio de suspensiones
     * @param cuentas        acciones de cuenta del módulo {@code usuario}
     * @param usuarios       nombre del administrador que suspendió
     * @param ventas         ventas en curso del usuario
     * @param notificaciones avisos CUENTA y VENTA
     * @param entityManager  para releer la vista después de los triggers
     */
    public UsuarioAdminService(ResumenUsuarioRepository resumenes, SuspensionRepository suspensiones,
            GestionCuentasService cuentas, UsuarioConsultaService usuarios, VentaService ventas,
            NotificacionService notificaciones, EntityManager entityManager) {
        this.resumenes = resumenes;
        this.suspensiones = suspensiones;
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.ventas = ventas;
        this.notificaciones = notificaciones;
        this.entityManager = entityManager;
    }

    /**
     * Lista usuarios por nombre con filtros opcionales.
     *
     * @param rol    rol, o {@code null}
     * @param estado estado, o {@code null}
     * @param q      texto en el nombre, el negocio o el celular, o {@code null}
     * @param pagina página desde 0 (20 por página)
     * @return página de usuarios
     */
    @Transactional(readOnly = true)
    public Page<UsuarioAdminDto> listar(String rol, String estado, String q, int pagina) {
        String filtroRol = StringUtils.trimToNull(rol);
        String filtroEstado = StringUtils.trimToNull(estado);
        if ((filtroRol != null && !ROLES.contains(filtroRol))
                || (filtroEstado != null && !ESTADOS.contains(filtroEstado))) {
            throw new SolicitudInvalidaException(MENSAJE_FILTRO);
        }
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        String texto = StringUtils.trimToNull(StringUtils.normalizeSpace(q));
        String patron = texto == null ? null : "%" + texto.toLowerCase() + "%";
        PageRequest pedido = PageRequest.of(pagina, TAMANO_PAGINA,
                Sort.by(Sort.Order.asc("nombreCompleto"), Sort.Order.asc("id")));
        return resumenes.buscar(filtroRol, filtroEstado, patron, pedido).map(UsuarioAdminDto::de);
    }

    /**
     * Detalle de un usuario.
     *
     * @param idUsuario usuario
     * @return fila, último acceso, suspensión vigente y ventas en curso
     */
    @Transactional(readOnly = true)
    public UsuarioDetalleDto obtener(long idUsuario) {
        UsuarioAdminDto fila = UsuarioAdminDto.de(buscar(idUsuario));
        CuentaDto cuenta = cuentas.cuenta(idUsuario);
        SuspensionDto suspension = suspensiones.findFirstByIdUsuarioAndFechaFinIsNull(idUsuario)
                .map(this::aDto).orElse(null);
        List<VentaEnCursoDto> enCurso = "ADMINISTRADOR".equals(fila.rol()) ? List.of() : ventas.enCursoDe(idUsuario);
        return UsuarioDetalleDto.de(fila, cuenta.ultimoAcceso(), suspension, enCurso);
    }

    /**
     * Suspende a un agricultor o comprador activo ({@code trg_suspensiones_ai} lo pasa a SUSPENDIDO: pierde el
     * acceso en su siguiente petición) y avisa al usuario y a sus contrapartes.
     *
     * @param idAdministrador administrador de la sesión
     * @param idUsuario       usuario
     * @param motivo          motivo de catálogo
     * @param detalle         justificación
     * @return el detalle actualizado
     */
    @Transactional
    public UsuarioDetalleDto suspender(long idAdministrador, long idUsuario, MotivoSuspension motivo,
            String detalle) {
        ResumenUsuario usuario = buscar(idUsuario);
        if ("ADMINISTRADOR".equals(usuario.getRol())) {
            throw new ReglaNegocioException(MENSAJE_SUSPENDER_ADMIN);
        }
        if ("SUSPENDIDO".equals(usuario.getEstado())) {
            throw new ReglaNegocioException(MENSAJE_YA_SUSPENDIDO);
        }
        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new ReglaNegocioException(MENSAJE_SOLO_ACTIVA);
        }
        long fundados = usuario.getReclamosFundados() == null ? 0 : usuario.getReclamosFundados();
        if (motivo == MotivoSuspension.RECLAMOS_FUNDADOS && fundados < MINIMO_FUNDADOS) {
            throw new ReglaNegocioException(MENSAJE_FUNDADOS.formatted(fundados));
        }
        String texto = StringUtils.normalizeSpace(detalle);
        Suspension suspension = suspensiones.saveAndFlush(new Suspension(idUsuario, idAdministrador, motivo, texto));
        entityManager.refresh(suspension); // fecha_inicio la fija la base de datos
        notificaciones.crear(new NuevaNotificacionDto(idUsuario, "CUENTA", "Su cuenta fue suspendida",
                "Motivo: %s. %s".formatted(MOTIVOS.get(motivo), texto), null));
        for (VentaEnCursoDto venta : ventas.enCursoDe(idUsuario)) {
            if (AVISAR_CONTRAPARTE.contains(venta.estado())) {
                notificaciones.crear(new NuevaNotificacionDto(venta.idContraparte(), "VENTA",
                        "La otra parte de su venta fue suspendida",
                        "La cuenta de la otra parte de su venta de %s fue suspendida. Puede cancelarla si lo necesita."
                                .formatted(venta.producto()), venta.idVenta()));
            }
        }
        LOG.info("Usuario {} suspendido por el administrador {}: {}", idUsuario, idAdministrador, motivo);
        entityManager.refresh(usuario); // trg_suspensiones_ai cambió el estado
        return obtener(idUsuario);
    }

    /**
     * Levanta la suspensión vigente ({@code trg_suspensiones_au} lo devuelve a ACTIVO) y avisa al usuario.
     *
     * @param idUsuario usuario
     * @return el detalle actualizado
     */
    @Transactional
    public UsuarioDetalleDto reactivar(long idUsuario) {
        buscar(idUsuario);
        Suspension vigente = suspensiones.findFirstByIdUsuarioAndFechaFinIsNull(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_NO_SUSPENDIDO));
        suspensiones.levantar(vigente.getId()); // Limpia el contexto: la vista se vuelve a leer
        notificaciones.crear(new NuevaNotificacionDto(idUsuario, "CUENTA", "Su cuenta fue reactivada",
                "Ya puede ingresar a AgroConecta de nuevo.", null));
        LOG.info("Suspensión {} levantada: usuario {}", vigente.getId(), idUsuario);
        return obtener(idUsuario);
    }

    /**
     * Contraseña temporal de un agricultor o comprador.
     *
     * @param idUsuario usuario
     * @return la contraseña temporal (se muestra una sola vez)
     */
    @Transactional
    public String contrasenaTemporal(long idUsuario) {
        return cuentas.reiniciarContrasena(idUsuario);
    }

    /**
     * Desactiva la cuenta.
     *
     * @param idUsuario usuario
     * @return el detalle actualizado
     */
    @Transactional
    public UsuarioDetalleDto desactivar(long idUsuario) {
        cuentas.desactivar(idUsuario);
        entityManager.flush();
        refrescar(idUsuario);
        return obtener(idUsuario);
    }

    /**
     * Vuelve a activar la cuenta.
     *
     * @param idUsuario usuario
     * @return el detalle actualizado
     */
    @Transactional
    public UsuarioDetalleDto activar(long idUsuario) {
        cuentas.activar(idUsuario);
        entityManager.flush();
        refrescar(idUsuario);
        return obtener(idUsuario);
    }

    /**
     * Crea otro administrador.
     *
     * @param datos nombre, celular, distrito y contraseña
     * @return el detalle del administrador creado
     */
    @Transactional
    public UsuarioDetalleDto crearAdministrador(NuevoAdministradorDto datos) {
        long id = cuentas.crearAdministrador(datos);
        LOG.info("Nuevo administrador {}", id);
        return obtener(id);
    }

    private void refrescar(long idUsuario) {
        ResumenUsuario cargado = entityManager.find(ResumenUsuario.class, idUsuario);
        if (cargado != null) {
            entityManager.refresh(cargado);
        }
    }

    private ResumenUsuario buscar(long idUsuario) {
        return resumenes.findById(idUsuario).orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
    }

    private SuspensionDto aDto(Suspension s) {
        UsuarioResumenDto admin = usuarios.buscarResumenes(List.of(s.getIdAdministrador())).get(s.getIdAdministrador());
        return new SuspensionDto(s.getMotivo().name(), s.getDetalle(), s.getFechaInicio(),
                admin == null ? null : admin.nombreCompleto());
    }

}
