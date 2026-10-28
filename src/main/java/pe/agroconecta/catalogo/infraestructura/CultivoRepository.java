package pe.agroconecta.catalogo.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.catalogo.dominio.Cultivo;

/**
 * Acceso de solo lectura a la tabla {@code cultivos}; siempre carga la unidad local en la misma consulta.
 */
public interface CultivoRepository extends JpaRepository<Cultivo, Integer> {

    /**
     * Lista los cultivos activos ordenados por nombre.
     *
     * @return cultivos activos con su unidad local
     */
    @Query("select c from Cultivo c join fetch c.unidadLocal where c.activo = true order by c.nombre")
    List<Cultivo> listarActivos();
}
