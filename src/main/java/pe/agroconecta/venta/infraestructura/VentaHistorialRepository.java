package pe.agroconecta.venta.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.agroconecta.venta.dominio.VentaHistorial;

/**
 * Acceso a la vista {@code v_historial_transacciones} (solo lectura). Los filtros se arman con
 * {@link EspecificacionesVentas}.
 */
public interface VentaHistorialRepository
        extends JpaRepository<VentaHistorial, Long>, JpaSpecificationExecutor<VentaHistorial> {

    /**
     * Partes de una propuesta conjunta (RF-10), en el orden en que se crearon.
     *
     * @param idPropuestaGrupal propuesta conjunta
     * @return ventas de la propuesta
     */
    List<VentaHistorial> findByIdPropuestaGrupalOrderByIdAsc(Long idPropuestaGrupal);
}
