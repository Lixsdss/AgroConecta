package pe.agroconecta.publicacion.infraestructura;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.agroconecta.publicacion.dominio.EstadoPublicacion;
import pe.agroconecta.publicacion.dominio.PublicacionDisponible;

/**
 * Acceso a la vista {@code v_publicaciones_disponibles} (solo lectura). Los filtros de la búsqueda se arman con
 * {@link EspecificacionesBusqueda}.
 */
public interface PublicacionDisponibleRepository
        extends JpaRepository<PublicacionDisponible, Long>, JpaSpecificationExecutor<PublicacionDisponible> {

    /**
     * Publicaciones de un agricultor en unos estados ("Mis ventas", RF-07).
     *
     * @param idAgricultor agricultor
     * @param estados      estados aceptados
     * @param pagina       página y orden pedidos
     * @return página de publicaciones
     */
    Page<PublicacionDisponible> findByIdAgricultorAndEstadoIn(Long idAgricultor, Collection<EstadoPublicacion> estados,
            Pageable pagina);
}
