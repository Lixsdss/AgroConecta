package pe.agroconecta.publicacion.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.publicacion.dominio.Publicacion;

/**
 * Acceso a la tabla {@code publicaciones}.
 */
public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
}
