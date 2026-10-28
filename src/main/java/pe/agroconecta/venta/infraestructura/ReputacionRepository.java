package pe.agroconecta.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.venta.dominio.ReputacionUsuario;

/**
 * Acceso a la vista {@code v_reputacion_usuario} (solo lectura).
 */
public interface ReputacionRepository extends JpaRepository<ReputacionUsuario, Long> {
}
