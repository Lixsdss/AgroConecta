package pe.agroconecta.mensajeria.infraestructura;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.mensajeria.dominio.Mensaje;

/**
 * Acceso a la tabla {@code mensajes} (índice {@code idx_mensaje_conversacion}).
 */
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /**
     * Mensajes de una conversación posteriores a un id, del más antiguo al más reciente.
     *
     * @param idConversacion conversación
     * @param despuesDe      id del último mensaje que ya se tiene (0 para todos)
     * @return mensajes
     */
    List<Mensaje> findByIdConversacionAndIdGreaterThanOrderByIdAsc(Long idConversacion, Long despuesDe);

    /**
     * Marca como leídos los mensajes de la otra parte.
     *
     * @param idConversacion conversación
     * @param idLector       usuario que lee
     * @return mensajes marcados
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Mensaje m set m.leido = true where m.idConversacion = :idConversacion "
            + "and m.idRemitente <> :idLector and m.leido = false")
    int marcarLeidos(Long idConversacion, Long idLector);

    /**
     * Último mensaje de cada conversación.
     *
     * @param idsConversacion conversaciones (no vacío)
     * @return un mensaje por conversación que tenga mensajes
     */
    @Query("select m from Mensaje m where m.id in (select max(m2.id) from Mensaje m2 "
            + "where m2.idConversacion in :idsConversacion group by m2.idConversacion)")
    List<Mensaje> ultimos(Collection<Long> idsConversacion);

    /**
     * Mensajes no leídos por un usuario en cada conversación.
     *
     * @param idsConversacion conversaciones (no vacío)
     * @param idLector        usuario
     * @return conteo por conversación (solo las que tienen no leídos)
     */
    @Query("select new pe.agroconecta.mensajeria.infraestructura.ConteoPorConversacion(m.idConversacion, count(m)) "
            + "from Mensaje m where m.idConversacion in :idsConversacion and m.idRemitente <> :idLector "
            + "and m.leido = false group by m.idConversacion")
    List<ConteoPorConversacion> noLeidos(Collection<Long> idsConversacion, Long idLector);

    /**
     * Total de mensajes no leídos por un comprador en todas sus conversaciones.
     *
     * @param idComprador comprador
     * @return total
     */
    @Query("select count(m) from Mensaje m, Conversacion c where m.idConversacion = c.id "
            + "and c.idComprador = :idComprador and m.idRemitente <> :idComprador and m.leido = false")
    long noLeidosDeComprador(Long idComprador);

    /**
     * Total de mensajes no leídos por un agricultor en las conversaciones de sus publicaciones.
     *
     * @param idsPublicacion publicaciones del agricultor (no vacío)
     * @param idAgricultor   agricultor
     * @return total
     */
    @Query("select count(m) from Mensaje m, Conversacion c where m.idConversacion = c.id "
            + "and c.idPublicacion in :idsPublicacion and m.idRemitente <> :idAgricultor and m.leido = false")
    long noLeidosDeAgricultor(Collection<Long> idsPublicacion, Long idAgricultor);
}
