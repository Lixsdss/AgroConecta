package pe.agroconecta.mensajeria.infraestructura;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.mensajeria.dominio.Conversacion;

/**
 * Acceso a la tabla {@code conversaciones}. Las listas se ordenan por la última actividad: el último mensaje o,
 * si no hay, el inicio de la conversación.
 */
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    /**
     * Busca la conversación de un comprador sobre una publicación.
     *
     * @param idPublicacion publicación
     * @param idComprador   comprador
     * @return conversación, si existe
     */
    Optional<Conversacion> findByIdPublicacionAndIdComprador(Long idPublicacion, Long idComprador);

    /**
     * Conversaciones de un comprador, con actividad más reciente primero.
     *
     * @param idComprador comprador
     * @param pagina      página pedida
     * @return página de conversaciones
     */
    @Query(value = "select c from Conversacion c where c.idComprador = :idComprador order by "
            + "coalesce((select max(m.fechaEnvio) from Mensaje m where m.idConversacion = c.id), c.fechaInicio) desc, "
            + "c.id desc",
            countQuery = "select count(c) from Conversacion c where c.idComprador = :idComprador")
    Page<Conversacion> deComprador(Long idComprador, Pageable pagina);

    /**
     * Conversaciones sobre unas publicaciones (las de un agricultor), con actividad más reciente primero.
     *
     * @param idsPublicacion publicaciones del agricultor (no vacío)
     * @param pagina         página pedida
     * @return página de conversaciones
     */
    @Query(value = "select c from Conversacion c where c.idPublicacion in :idsPublicacion order by "
            + "coalesce((select max(m.fechaEnvio) from Mensaje m where m.idConversacion = c.id), c.fechaInicio) desc, "
            + "c.id desc",
            countQuery = "select count(c) from Conversacion c where c.idPublicacion in :idsPublicacion")
    Page<Conversacion> dePublicaciones(Collection<Long> idsPublicacion, Pageable pagina);
}
