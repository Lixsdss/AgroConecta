package pe.agroconecta.venta.infraestructura;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.venta.dominio.EstadoTransaccion;
import pe.agroconecta.venta.dominio.Transaccion;

/**
 * Acceso a la tabla {@code transacciones}.
 */
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    /**
     * Lee la venta y la bloquea hasta el fin de la transacción (evita dos cambios de estado simultáneos).
     *
     * @param id id de la venta
     * @return la venta bloqueada, si existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaccion t where t.id = :id")
    Optional<Transaccion> bloquear(Long id);

    /**
     * Ids de las ventas de una conversación.
     *
     * @param idConversacion conversación
     * @return ids
     */
    @Query("select t.id from Transaccion t where t.idConversacion = :idConversacion")
    List<Long> idsDeConversacion(Long idConversacion);

    /**
     * Partes de una propuesta conjunta en un estado (RF-10).
     *
     * @param idPropuestaGrupal propuesta conjunta
     * @param estado            estado buscado
     * @return ventas
     */
    List<Transaccion> findByIdPropuestaGrupalAndEstado(Long idPropuestaGrupal, EstadoTransaccion estado);
}
