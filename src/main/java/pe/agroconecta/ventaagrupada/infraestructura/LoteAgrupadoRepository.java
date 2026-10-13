package pe.agroconecta.ventaagrupada.infraestructura;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.ventaagrupada.dominio.ClaveLote;
import pe.agroconecta.ventaagrupada.dominio.LoteAgrupado;

/**
 * Acceso a la vista {@code v_lotes_agrupados} (solo lectura).
 */
public interface LoteAgrupadoRepository extends JpaRepository<LoteAgrupado, ClaveLote> {

    /**
     * Lotes con filtros opcionales de cultivo y distrito, el de más kilos primero.
     *
     * @param idCultivo  cultivo, o {@code null}
     * @param idDistrito distrito, o {@code null}
     * @param pagina     página pedida
     * @return página de lotes
     */
    @Query("select l from LoteAgrupado l where (:idCultivo is null or l.idCultivo = :idCultivo) "
            + "and (:idDistrito is null or l.idDistrito = :idDistrito) "
            + "order by l.volumenKg desc, l.idVariedad, l.idDistrito")
    Page<LoteAgrupado> buscar(Integer idCultivo, Integer idDistrito, Pageable pagina);
}
