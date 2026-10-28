package pe.agroconecta.catalogo.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.catalogo.dominio.Variedad;

/**
 * Acceso de solo lectura a la tabla {@code variedades}; siempre carga el cultivo en la misma consulta.
 */
public interface VariedadRepository extends JpaRepository<Variedad, Integer> {

    /**
     * Lista todas las variedades ordenadas por cultivo y nombre.
     *
     * @return variedades con su cultivo
     */
    @Query("select v from Variedad v join fetch v.cultivo c order by c.nombre, v.nombre")
    List<Variedad> listarTodas();

    /**
     * Lista las variedades de un cultivo ordenadas por nombre.
     *
     * @param idCultivo id del cultivo
     * @return variedades con su cultivo
     */
    @Query("select v from Variedad v join fetch v.cultivo c where c.id = :idCultivo order by v.nombre")
    List<Variedad> listarPorCultivo(Integer idCultivo);

    /**
     * Busca una variedad con su cultivo.
     *
     * @param id id de la variedad
     * @return variedad, si existe
     */
    @Query("select v from Variedad v join fetch v.cultivo where v.id = :id")
    Optional<Variedad> buscarConCultivo(Integer id);
}
