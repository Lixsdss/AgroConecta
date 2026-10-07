package pe.agroconecta.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.agroconecta.venta.dominio.VentaHistorial;

/**
 * Acceso a la vista {@code v_historial_transacciones} (solo lectura). Los filtros se arman con
 * {@link EspecificacionesVentas}.
 */
public interface VentaHistorialRepository
        extends JpaRepository<VentaHistorial, Long>, JpaSpecificationExecutor<VentaHistorial> {
}
