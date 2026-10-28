package pe.agroconecta.notificacion.infraestructura;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Avisos de un usuario, el más reciente primero (índice {@code idx_notificacion_usuario}).
     *
     * @param idUsuario usuario
     * @param pagina    página pedida
     * @return página de avisos
     */
    Page<Notificacion> findByIdUsuarioOrderByFechaCreacionDescIdDesc(Long idUsuario, Pageable pagina);

    /**
     * Cuenta los avisos sin leer de un usuario.
     *
     * @param idUsuario usuario
     * @return cantidad
     */
    long countByIdUsuarioAndLeidaFalse(Long idUsuario);

    /**
     * Busca un aviso de un usuario.
     *
     * @param id        id del aviso
     * @param idUsuario usuario dueño
     * @return el aviso, si existe y es suyo
     */
    Optional<Notificacion> findByIdAndIdUsuario(Long id, Long idUsuario);

    /**
     * Marca como leídos todos los avisos de un usuario.
     *
     * @param idUsuario usuario
     * @return avisos marcados
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notificacion n set n.leida = true where n.idUsuario = :idUsuario and n.leida = false")
    int marcarTodasLeidas(Long idUsuario);
}
