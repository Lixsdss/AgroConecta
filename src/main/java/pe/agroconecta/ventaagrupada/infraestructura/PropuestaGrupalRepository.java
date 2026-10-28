package pe.agroconecta.ventaagrupada.infraestructura;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.ventaagrupada.dominio.PropuestaGrupal;

/**
 * Acceso a la tabla {@code propuestas_grupales}.
 */
public interface PropuestaGrupalRepository extends JpaRepository<PropuestaGrupal, Long> {

    /**
     * Propuestas de un comprador, la más reciente primero.
     *
     * @param idComprador comprador
     * @param pagina      página pedida
     * @return página de propuestas
     */
    Page<PropuestaGrupal> findByIdCompradorOrderByFechaCreacionDescIdDesc(Long idComprador, Pageable pagina);
}
