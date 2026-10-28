package pe.agroconecta.administracion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import pe.agroconecta.venta.aplicacion.VentaEnCursoDto;

/**
 * Detalle de un usuario para el administrador: la fila de "Usuarios" más el último acceso, la suspensión vigente y
 * las ventas en curso (aviso de UC-11 A1).
 *
 * @param id                     id del usuario
 * @param rol                    rol
 * @param nombre                 nombre completo
 * @param chacra                 chacra, o {@code null}
 * @param negocio                negocio, o {@code null}
 * @param tipoComprador          tipo de comprador, o {@code null}
 * @param celular                celular
 * @param distrito               distrito
 * @param estado                 estado
 * @param motivoSuspension       motivo de la suspensión vigente, o {@code null}
 * @param operacionesCompletadas ventas completadas
 * @param calificacion           promedio de estrellas, o {@code null}
 * @param totalCalificaciones    calificaciones recibidas
 * @param reclamosFundados       reclamos fundados en su contra
 * @param fechaRegistro          fecha de registro
 * @param ultimoAcceso           último inicio de sesión, o {@code null}
 * @param suspension             suspensión vigente, o {@code null}
 * @param ventasEnCurso          ventas de PROPUESTA a ENTREGADO
 */
public record UsuarioDetalleDto(Long id, String rol, String nombre, String chacra, String negocio,
        String tipoComprador, String celular, String distrito, String estado, String motivoSuspension,
        long operacionesCompletadas, BigDecimal calificacion, long totalCalificaciones, long reclamosFundados,
        LocalDateTime fechaRegistro, LocalDateTime ultimoAcceso, SuspensionDto suspension,
        List<VentaEnCursoDto> ventasEnCurso) {

    /**
     * Arma el detalle a partir de la fila.
     *
     * @param u             fila de "Usuarios"
     * @param ultimoAcceso  último acceso
     * @param suspension    suspensión vigente
     * @param ventasEnCurso ventas en curso
     * @return el detalle
     */
    static UsuarioDetalleDto de(UsuarioAdminDto u, LocalDateTime ultimoAcceso, SuspensionDto suspension,
            List<VentaEnCursoDto> ventasEnCurso) {
        return new UsuarioDetalleDto(u.id(), u.rol(), u.nombre(), u.chacra(), u.negocio(), u.tipoComprador(),
                u.celular(), u.distrito(), u.estado(), u.motivoSuspension(), u.operacionesCompletadas(),
                u.calificacion(), u.totalCalificaciones(), u.reclamosFundados(), u.fechaRegistro(), ultimoAcceso,
                suspension, ventasEnCurso);
    }
}
