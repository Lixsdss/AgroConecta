package pe.agroconecta.administracion.infraestructura;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.administracion.dominio.RevisionPublicacion;

/**
 * Acceso a la tabla {@code revisiones_publicacion}.
 */
public interface RevisionPublicacionRepository extends JpaRepository<RevisionPublicacion, Long> {

    /**
     * Lista las decisiones de un intervalo, la más reciente primero.
     *
     * @param desde inicio (incluido)
     * @param hasta fin (excluido)
     * @return decisiones del intervalo
     */
    List<RevisionPublicacion> findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(LocalDateTime desde,
            LocalDateTime hasta);
}
