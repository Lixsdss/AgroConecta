package pe.agroconecta.administracion.infraestructura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.agroconecta.administracion.dominio.Suspension;

/**
 * Acceso a la tabla {@code suspensiones}.
 */
public interface SuspensionRepository extends JpaRepository<Suspension, Long> {

    /**
     * Suspensión vigente de un usuario (como máximo una, {@code trg_suspensiones_bi}).
     *
     * @param idUsuario usuario
     * @return la suspensión sin fecha de fin, si existe
     */
    Optional<Suspension> findFirstByIdUsuarioAndFechaFinIsNull(Long idUsuario);

    /**
     * Levanta una suspensión con la hora de la base de datos (así {@code fecha_fin >= fecha_inicio} aunque la
     * aplicación y la base tengan otra zona horaria). {@code trg_suspensiones_au} devuelve al usuario a ACTIVO.
     *
     * @param id suspensión
     * @return filas actualizadas (0 si ya estaba levantada)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Suspension s set s.fechaFin = current_timestamp where s.id = :id and s.fechaFin is null")
    int levantar(@Param("id") Long id);
}
