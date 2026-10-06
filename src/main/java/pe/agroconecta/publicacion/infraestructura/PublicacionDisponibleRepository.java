package pe.agroconecta.publicacion.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.agroconecta.publicacion.dominio.PublicacionDisponible;

/**
 * Acceso a la vista {@code v_publicaciones_disponibles} (solo lectura). Los filtros de la búsqueda se arman con
 * {@link EspecificacionesBusqueda}.
 */
public interface PublicacionDisponibleRepository
        extends JpaRepository<PublicacionDisponible, Long>, JpaSpecificationExecutor<PublicacionDisponible> {
}
