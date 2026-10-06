package pe.agroconecta.notificacion.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.notificacion.dominio.Notificacion;
import pe.agroconecta.notificacion.dominio.TipoNotificacion;

/**
 * Acceso a la tabla {@code notificaciones}.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Indica si el usuario tiene un aviso sin leer de ese tipo y referencia.
     *
     * @param idUsuario    usuario
     * @param tipo         tipo de aviso
     * @param idReferencia id del objeto del aviso
     * @return {@code true} si existe
     */
    boolean existsByIdUsuarioAndTipoAndIdReferenciaAndLeidaFalse(Long idUsuario, TipoNotificacion tipo,
            Long idReferencia);
}
