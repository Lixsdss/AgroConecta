package pe.agroconecta.usuario.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.usuario.dominio.Agricultor;

/**
 * Acceso a la tabla {@code agricultores}.
 */
public interface AgricultorRepository extends JpaRepository<Agricultor, Long> {
}
