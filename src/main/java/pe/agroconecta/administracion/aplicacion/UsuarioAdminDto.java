package pe.agroconecta.administracion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.agroconecta.administracion.dominio.ResumenUsuario;

/**
 * Fila de la pantalla "Usuarios" (11e, {@code v_resumen_usuario}).
 *
 * @param id                     id del usuario
 * @param rol                    rol
 * @param nombre                 nombre completo
 * @param chacra                 chacra (agricultor), o {@code null}
 * @param negocio                negocio (comprador), o {@code null}
 * @param tipoComprador          tipo de comprador, o {@code null}
 * @param celular                celular
 * @param distrito               distrito
 * @param estado                 {@code ACTIVO}, {@code SUSPENDIDO} o {@code INACTIVO}
 * @param motivoSuspension       motivo de la suspensión vigente, o {@code null}
 * @param operacionesCompletadas ventas completadas
 * @param calificacion           promedio de estrellas, o {@code null}
 * @param totalCalificaciones    calificaciones recibidas
 * @param reclamosFundados       reclamos fundados en su contra
 * @param fechaRegistro          fecha de registro
 */
public record UsuarioAdminDto(Long id, String rol, String nombre, String chacra, String negocio,
        String tipoComprador, String celular, String distrito, String estado, String motivoSuspension,
        long operacionesCompletadas, BigDecimal calificacion, long totalCalificaciones, long reclamosFundados,
        LocalDateTime fechaRegistro) {

    /**
     * Arma la fila a partir de la vista.
     *
     * @param r fila de {@code v_resumen_usuario}
     * @return la fila
     */
    static UsuarioAdminDto de(ResumenUsuario r) {
        return new UsuarioAdminDto(r.getId(), r.getRol(), r.getNombreCompleto(), r.getNombreChacra(),
                r.getNombreNegocio(), r.getTipoComprador(), r.getCelular(), r.getDistrito(), r.getEstado(),
                r.getMotivoSuspension(), valor(r.getOperacionesCompletadas()), r.getPromedioPuntuacion(),
                valor(r.getTotalCalificaciones()), valor(r.getReclamosFundados()), r.getFechaRegistro());
    }

    private static long valor(Long numero) {
        return numero == null ? 0 : numero;
    }
}
