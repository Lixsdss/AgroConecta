package pe.agroconecta.precio.infraestructura;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.precio.dominio.PrecioMercado;

/**
 * Acceso a la tabla {@code precios_mercado}.
 */
public interface PrecioMercadoRepository extends JpaRepository<PrecioMercado, Long> {

    /**
     * Indica si ya hay precio de ese cultivo, zona y fecha (restricción única).
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id de la zona
     * @param fecha      día
     * @return {@code true} si existe
     */
    boolean existsByIdCultivoAndIdDistritoAndFecha(Integer idCultivo, Integer idDistrito, LocalDate fecha);

    /**
     * Serie de precios de un cultivo en una zona desde una fecha, del más antiguo al más reciente.
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id de la zona
     * @param desde      primer día incluido
     * @return precios ordenados por fecha
     */
    List<PrecioMercado> findByIdCultivoAndIdDistritoAndFechaGreaterThanEqualOrderByFechaAsc(Integer idCultivo,
            Integer idDistrito, LocalDate desde);

    /**
     * Precios de unas fechas (para completar el id de los precios vigentes).
     *
     * @param fechas días buscados
     * @return precios de esos días
     */
    List<PrecioMercado> findByFechaIn(Collection<LocalDate> fechas);
}
