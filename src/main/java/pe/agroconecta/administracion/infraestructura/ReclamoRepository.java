package pe.agroconecta.administracion.infraestructura;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.administracion.dominio.EstadoReclamo;
import pe.agroconecta.administracion.dominio.Reclamo;

/**
 * Acceso a la tabla {@code reclamos}.
 */
public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {

    /**
     * Indica si la persona ya reclamó esa venta ({@code uk_reclamo_trans_reclamante}).
     *
     * @param idTransaccion venta
     * @param idReclamante  persona
     * @return {@code true} si ya reclamó
     */
    boolean existsByIdTransaccionAndIdReclamante(Long idTransaccion, Long idReclamante);

    /**
     * Reclamos de una persona, el más reciente primero.
     *
     * @param idReclamante persona
     * @return reclamos
     */
    List<Reclamo> findByIdReclamanteOrderByFechaRegistroDescIdDesc(Long idReclamante);

    /**
     * Reclamos de una persona sobre una venta.
     *
     * @param idReclamante  persona
     * @param idTransaccion venta
     * @return reclamos (uno como máximo)
     */
    List<Reclamo> findByIdReclamanteAndIdTransaccion(Long idReclamante, Long idTransaccion);

    /**
     * Reclamos en un estado, con el orden de la página pedida.
     *
     * @param estado estado
     * @param pagina página y orden
     * @return página de reclamos
     */
    Page<Reclamo> findByEstado(EstadoReclamo estado, Pageable pagina);
}
