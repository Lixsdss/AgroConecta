package pe.agroconecta.administracion.infraestructura;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.agroconecta.administracion.dominio.ResumenUsuario;

/**
 * Acceso a la vista {@code v_resumen_usuario} (solo lectura).
 */
public interface ResumenUsuarioRepository extends JpaRepository<ResumenUsuario, Long> {

    /**
     * Busca usuarios con filtros opcionales.
     *
     * @param rol    rol, o {@code null}
     * @param estado estado, o {@code null}
     * @param texto  patrón {@code %texto%} en minúsculas sobre nombre, negocio o celular, o {@code null}
     * @param pagina página y orden
     * @return página de usuarios
     */
    @Query("""
            select r from ResumenUsuario r
            where (:rol is null or r.rol = :rol)
              and (:estado is null or r.estado = :estado)
              and (:texto is null or lower(r.nombreCompleto) like :texto or lower(r.nombreNegocio) like :texto
                   or r.celular like :texto)
            """)
    Page<ResumenUsuario> buscar(@Param("rol") String rol, @Param("estado") String estado,
            @Param("texto") String texto, Pageable pagina);

    /**
     * Las 5 mejores reputaciones (al menos una calificación).
     *
     * @param minimo calificaciones mínimas (exclusivo)
     * @return usuarios
     */
    List<ResumenUsuario> findTop5ByTotalCalificacionesGreaterThanOrderByPromedioPuntuacionDescTotalCalificacionesDescIdAsc(
            long minimo);

    /**
     * Las 5 peores reputaciones (al menos una calificación).
     *
     * @param minimo calificaciones mínimas (exclusivo)
     * @return usuarios
     */
    List<ResumenUsuario> findTop5ByTotalCalificacionesGreaterThanOrderByPromedioPuntuacionAscTotalCalificacionesDescIdAsc(
            long minimo);
}
