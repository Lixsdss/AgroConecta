package pe.agroconecta.catalogo.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.catalogo.dominio.Provincia;

/**
 * Acceso de solo lectura a la tabla {@code provincias}.
 */
public interface ProvinciaRepository extends JpaRepository<Provincia, Integer> {

    /**
     * Lista todas las provincias ordenadas por nombre.
     *
     * @return provincias
     */
    List<Provincia> findAllByOrderByNombreAsc();
}
