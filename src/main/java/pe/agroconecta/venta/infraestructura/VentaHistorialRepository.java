package pe.agroconecta.venta.infraestructura;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.venta.dominio.EstadoTransaccion;
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

    /**
     * Ventas de un usuario (como comprador o agricultor) en unos estados (RF-09: aviso al suspender).
     *
     * @param idUsuario usuario
     * @param estados   estados buscados
     * @return ventas, la más reciente primero
     */
    @Query("select v from VentaHistorial v where (v.idComprador = :idUsuario or v.idAgricultor = :idUsuario) "
            + "and v.estado in :estados order by v.id desc")
    List<VentaHistorial> deUsuarioEnEstados(Long idUsuario, Collection<EstadoTransaccion> estados);
}
