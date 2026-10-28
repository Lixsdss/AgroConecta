package pe.agroconecta.administracion.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.administracion.dominio.DecisionModeracion;
import pe.agroconecta.administracion.dominio.MotivoModeracion;
import pe.agroconecta.administracion.dominio.RevisionPublicacion;
import pe.agroconecta.administracion.infraestructura.RevisionPublicacionRepository;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.publicacion.aplicacion.PublicacionModeracionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionService;
import pe.agroconecta.publicacion.aplicacion.VistaModeracion;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;

/**
 * Moderación posterior de publicaciones (RF-09, UC-11 parte 2). Valida con mensajes amigables lo mismo que
 * {@code trg_revisiones_bi} y {@code chk_revision_coherencia}; al insertar la revisión, {@code trg_revisiones_ai}
 * cambia la publicación, borra su marca y avisa al agricultor (este servicio no lo repite).
 */
@Service
public class ModeracionService {

    static final String MENSAJE_NO_VISIBLE = "Solo se revisa, advierte u oculta una publicación visible.";
    static final String MENSAJE_NO_OCULTA = "Solo se vuelve a mostrar una publicación oculta.";
    static final String MENSAJE_FUERA_DE_COLA = "La publicación no está en la cola de revisión.";
    static final String MENSAJE_FALTA_MOTIVO = "Elija un motivo y escriba el mensaje para el agricultor.";

    private static final String PUBLICADA = "PUBLICADA";
    private static final String BLOQUEADA = "BLOQUEADA";
    private static final String CORREGIDA = "CORREGIDA";

    private static final Logger LOG = LoggerFactory.getLogger(ModeracionService.class);

    private final RevisionPublicacionRepository revisiones;
    private final PublicacionService publicaciones;
    private final UsuarioConsultaService usuarios;

    /**
     * Crea el servicio.
     *
     * @param revisiones    repositorio de revisiones
     * @param publicaciones consultas de publicaciones del módulo {@code publicacion}
     * @param usuarios      consultas de usuarios del módulo {@code usuario}
     */
    public ModeracionService(RevisionPublicacionRepository revisiones, PublicacionService publicaciones,
            UsuarioConsultaService usuarios) {
        this.revisiones = revisiones;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
    }

    /**
     * Lista la cola "Por revisar" o las publicaciones ocultas, con los datos del agricultor.
     *
     * @param vista  listado pedido
     * @param pagina página pedida
     * @return página de publicaciones en revisión
     */
    @Transactional(readOnly = true)
    public Page<PublicacionEnRevisionDto> listar(VistaModeracion vista, Pageable pagina) {
        Page<PublicacionModeracionDto> encontradas = publicaciones.listarParaModeracion(vista, pagina);
        Set<Long> idsAgricultor = encontradas.stream().map(PublicacionModeracionDto::idAgricultor)
                .collect(Collectors.toSet());
        Map<Long, UsuarioResumenDto> agricultores = usuarios.buscarResumenes(idsAgricultor);
        return encontradas.map(p -> PublicacionEnRevisionDto.de(p, agricultores.get(p.idAgricultor())));
    }

    /**
     * Lista las decisiones de un día, la más reciente primero.
     *
     * @param dia día consultado
     * @return decisiones con producto y administrador
     */
    @Transactional(readOnly = true)
    public List<DecisionDelDiaDto> decisionesDelDia(LocalDate dia) {
        List<RevisionPublicacion> delDia = revisiones.findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(
                dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
        Map<Long, UsuarioResumenDto> administradores = usuarios.buscarResumenes(
                delDia.stream().map(RevisionPublicacion::getIdAdministrador).collect(Collectors.toSet()));
        return delDia.stream().map(r -> {
            PublicacionModeracionDto p = publicaciones.obtenerParaModeracion(r.getIdPublicacion());
            UsuarioResumenDto admin = administradores.get(r.getIdAdministrador());
            return new DecisionDelDiaDto(r.getId(), r.getIdPublicacion(), p.cultivo() + " " + p.variedad(),
                    r.getDecision(), r.getMotivo(), r.getMensaje(), r.getFecha(),
                    admin == null ? null : admin.nombreCompleto());
        }).toList();
    }

    /**
     * Registra una decisión sobre una publicación.
     *
     * @param idAdministrador id del administrador de la sesión
     * @param idPublicacion   id de la publicación
     * @param decision        decisión
     * @param motivo          motivo (ADVERTENCIA y BLOQUEO; se ignora en las demás)
     * @param mensaje         mensaje al agricultor (ADVERTENCIA y BLOQUEO; opcional en DESBLOQUEO)
     * @return revisión registrada y estado resultante de la publicación
     * @throws ReglaNegocioException si la decisión no corresponde al estado de la publicación o falta el motivo
     */
    @Transactional
    public DecisionRegistradaDto decidir(long idAdministrador, long idPublicacion, DecisionModeracion decision,
            MotivoModeracion motivo, String mensaje) {
        Preconditions.checkNotNull(decision, "decision");
        PublicacionModeracionDto publicacion = publicaciones.obtenerParaModeracion(idPublicacion);
        validar(publicacion, decision);

        String texto = StringUtils.trimToNull(StringUtils.normalizeSpace(mensaje));
        MotivoModeracion motivoGuardado = decision.exigeMotivo() ? motivo : null;
        String mensajeGuardado = decision == DecisionModeracion.CONFORME ? null : texto;
        if (decision.exigeMotivo() && (motivoGuardado == null || mensajeGuardado == null)) {
            throw new ReglaNegocioException(MENSAJE_FALTA_MOTIVO);
        }

        RevisionPublicacion revision = revisiones.saveAndFlush(new RevisionPublicacion(idPublicacion, idAdministrador,
                decision, motivoGuardado, mensajeGuardado));
        String estado = publicaciones.obtenerParaModeracion(idPublicacion).estado();
        LOG.info("Moderación: publicacion={}, decision={}, motivo={}, administrador={}, estado={}", idPublicacion,
                decision, motivoGuardado, idAdministrador, estado);
        return new DecisionRegistradaDto(revision.getId(), idPublicacion, decision, estado);
    }

    private static void validar(PublicacionModeracionDto publicacion, DecisionModeracion decision) {
        if (decision == DecisionModeracion.DESBLOQUEO) {
            if (!BLOQUEADA.equals(publicacion.estado())) {
                throw new ReglaNegocioException(MENSAJE_NO_OCULTA);
            }
            return;
        }
        // V3: "Mantener oculta" una publicación oculta que el agricultor corrigió (BLOQUEO sobre BLOQUEADA + CORREGIDA)
        boolean mantenerOculta = decision == DecisionModeracion.BLOQUEO && BLOQUEADA.equals(publicacion.estado())
                && CORREGIDA.equals(publicacion.marcaRevision());
        if (!PUBLICADA.equals(publicacion.estado()) && !mantenerOculta) {
            throw new ReglaNegocioException(MENSAJE_NO_VISIBLE);
        }
        if (decision == DecisionModeracion.CONFORME && publicacion.marcaRevision() == null) {
            throw new ReglaNegocioException(MENSAJE_FUERA_DE_COLA);
        }
    }
}
