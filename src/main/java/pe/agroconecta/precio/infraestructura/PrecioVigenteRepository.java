package pe.agroconecta.precio.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.precio.dominio.ClaveCultivoDistrito;
import pe.agroconecta.precio.dominio.PrecioVigente;

/**
 * Lectura de la vista {@code v_precios_vigentes}.
 */
public interface PrecioVigenteRepository extends JpaRepository<PrecioVigente, ClaveCultivoDistrito> {

    /**
     * Precios vigentes ordenados por cultivo y zona.
     *
     * @return todos los precios vigentes
     */
    List<PrecioVigente> findAllByOrderByCultivoAscZonaAsc();

    /**
     * Precios vigentes de un cultivo en todas las zonas.
     *
     * @param idCultivo id del cultivo
     * @return precios vigentes del cultivo
     */
    List<PrecioVigente> findByIdCultivoOrderByZonaAsc(Integer idCultivo);
}
