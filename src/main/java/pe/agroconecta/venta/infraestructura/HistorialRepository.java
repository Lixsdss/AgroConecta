package pe.agroconecta.venta.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.venta.dominio.HistorialTransaccion;

/**
 * Lectura de {@code transacciones_historial} (índice {@code idx_historial_transaccion}).
 */
public interface HistorialRepository extends JpaRepository<HistorialTransaccion, Long> {

    /**
     * Línea de tiempo de una venta, del primer paso al último.
     *
     * @param idTransaccion venta
     * @return pasos
     */
    List<HistorialTransaccion> findByIdTransaccionOrderByFechaAscIdAsc(Long idTransaccion);
}
