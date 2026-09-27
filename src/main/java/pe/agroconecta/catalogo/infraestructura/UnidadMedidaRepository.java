package pe.agroconecta.catalogo.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.catalogo.dominio.UnidadMedida;

/**
 * Acceso de solo lectura a la tabla {@code unidades_medida}.
 */
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Integer> {

    /**
     * Lista las unidades en el orden del catálogo.
     *
     * @return unidades ordenadas por id
     */
    List<UnidadMedida> findAllByOrderByIdAsc();
}
