package pe.agroconecta.administracion.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.administracion.dominio.ConversionPublicacion;

/**
 * Acceso a la vista {@code v_conversion_publicacion}.
 */
public interface ConversionPublicacionRepository extends JpaRepository<ConversionPublicacion, Long> {

    /**
     * Las 10 publicaciones con más contactos (al menos uno).
     *
     * @param minimo contactos mínimos (exclusivo)
     * @return filas del embudo
     */
    List<ConversionPublicacion> findTop10ByContactosGreaterThanOrderByContactosDescOfertasDescIdPublicacionDesc(
            long minimo);

    /**
     * Todas las publicaciones con contactos, para el Excel.
     *
     * @param minimo contactos mínimos (exclusivo)
     * @return filas del embudo
     */
    List<ConversionPublicacion> findByContactosGreaterThanOrderByContactosDescOfertasDescIdPublicacionDesc(
            long minimo);
}
