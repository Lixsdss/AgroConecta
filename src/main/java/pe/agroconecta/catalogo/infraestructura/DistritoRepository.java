package pe.agroconecta.catalogo.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.catalogo.dominio.Distrito;

/**
 * Acceso de solo lectura a la tabla {@code distritos}; siempre carga la provincia en la misma consulta.
 */
public interface DistritoRepository extends JpaRepository<Distrito, Integer> {

    /**
     * Lista todos los distritos ordenados por provincia y nombre.
     *
     * @return distritos con su provincia
     */
    @Query("select d from Distrito d join fetch d.provincia p order by p.nombre, d.nombre")
    List<Distrito> listarTodos();

    /**
     * Lista los distritos de una provincia ordenados por nombre.
     *
     * @param idProvincia id de la provincia
     * @return distritos con su provincia
     */
    @Query("select d from Distrito d join fetch d.provincia p where p.id = :idProvincia order by d.nombre")
    List<Distrito> listarPorProvincia(Integer idProvincia);

    /**
     * Busca un distrito con su provincia.
     *
     * @param id id del distrito
     * @return distrito, si existe
     */
    @Query("select d from Distrito d join fetch d.provincia where d.id = :id")
    Optional<Distrito> buscarConProvincia(Integer id);

    /**
     * Busca un distrito por su ubigeo del INEI.
     *
     * @param ubigeo código de 6 dígitos
     * @return distrito, si existe
     */
    @Query("select d from Distrito d join fetch d.provincia where d.ubigeo = :ubigeo")
    Optional<Distrito> buscarPorUbigeo(String ubigeo);
}
