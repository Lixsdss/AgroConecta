package pe.agroconecta.publicacion.infraestructura;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.publicacion.dominio.Publicacion;

/**
 * Acceso a la tabla {@code publicaciones}.
 */
public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    /**
     * Cuenta las publicaciones de unas variedades en un distrito desde una fecha y promedia sus kilos.
     *
     * @param idsVariedad ids de las variedades del cultivo
     * @param idDistrito  id del distrito
     * @param desde       fecha y hora de publicación mínima
     * @return cantidad y promedio de kilos
     */
    @Query("select new pe.agroconecta.publicacion.infraestructura.EstadisticaCantidad(count(p), avg(p.cantidadKg)) "
            + "from Publicacion p where p.idVariedad in :idsVariedad and p.idDistrito = :idDistrito "
            + "and p.fechaPublicacion >= :desde")
    EstadisticaCantidad estadisticaCantidad(Collection<Integer> idsVariedad, Integer idDistrito, LocalDateTime desde);

    /**
     * Cola de moderación: publicaciones visibles u ocultas que tienen marca de revisión, la más antigua primero.
     *
     * @param pagina página pedida
     * @return página de publicaciones por revisar
     */
    @Query("select p from Publicacion p where p.marcaRevision is not null "
            + "and p.estado in (pe.agroconecta.publicacion.dominio.EstadoPublicacion.PUBLICADA, "
            + "pe.agroconecta.publicacion.dominio.EstadoPublicacion.BLOQUEADA) order by p.fechaPublicacion asc, p.id asc")
    Page<Publicacion> buscarPorRevisar(Pageable pagina);

    /**
     * Publicaciones ocultas por la moderación, la más reciente primero.
     *
     * @param pagina página pedida
     * @return página de publicaciones BLOQUEADAS
     */
    @Query("select p from Publicacion p where p.estado = pe.agroconecta.publicacion.dominio.EstadoPublicacion.BLOQUEADA "
            + "order by p.fechaActualizacion desc, p.id desc")
    Page<Publicacion> buscarOcultas(Pageable pagina);
}
