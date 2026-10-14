package pe.agroconecta.administracion.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.administracion.dominio.PanelAdmin;

/**
 * Acceso a la vista {@code v_panel_admin} (una sola fila).
 */
public interface PanelAdminRepository extends JpaRepository<PanelAdmin, Long> {
}
