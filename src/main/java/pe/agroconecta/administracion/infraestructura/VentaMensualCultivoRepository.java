package pe.agroconecta.administracion.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.administracion.dominio.VentaMensualCultivo;
import pe.agroconecta.administracion.dominio.VentaMensualCultivoId;

/**
 * Acceso a la vista {@code v_ventas_mensuales_cultivo}.
 */
public interface VentaMensualCultivoRepository extends JpaRepository<VentaMensualCultivo, VentaMensualCultivoId> {

    /**
     * Ventas de un mes por cultivo, el de más kilos primero.
     *
     * @param mes mes {@code yyyy-MM}
     * @return filas del mes
     */
    List<VentaMensualCultivo> findByIdMesOrderByKilosVendidosDescCultivoAsc(String mes);
}
