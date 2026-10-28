package pe.agroconecta.venta.infraestructura;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.agroconecta.venta.dominio.Calificacion;

/**
 * Acceso a la tabla {@code calificaciones}. Las partes de cada venta se leen de {@code v_historial_transacciones}
 * (vista del mismo módulo).
 */
public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    /**
     * Calificaciones de una venta (como máximo una por parte).
     *
     * @param idTransaccion venta
     * @return calificaciones
     */
    List<Calificacion> findByIdTransaccion(Long idTransaccion);

    /**
     * Calificaciones que un usuario dio en varias ventas.
     *
     * @param idsTransaccion ventas
     * @param idEvaluador    usuario que calificó
     * @return calificaciones
     */
    List<Calificacion> findByIdTransaccionInAndIdEvaluador(Collection<Long> idsTransaccion, Long idEvaluador);

    /**
     * Indica si el usuario ya calificó la venta.
     *
     * @param idTransaccion venta
     * @param idEvaluador   usuario
     * @return {@code true} si ya la calificó
     */
    boolean existsByIdTransaccionAndIdEvaluador(Long idTransaccion, Long idEvaluador);

    /**
     * Calificaciones que recibió un usuario (las que dio la otra parte de sus ventas), la más reciente primero.
     *
     * @param idUsuario usuario evaluado
     * @param pagina    página pedida
     * @return página de calificaciones con el nombre de quien calificó
     */
    @Query(value = "select new pe.agroconecta.venta.infraestructura.CalificacionRecibida(c.puntuacion, c.comentario, "
            + "c.fecha, case when c.idEvaluador = v.idComprador then v.comprador else v.agricultor end) "
            + "from Calificacion c, VentaHistorial v where v.id = c.idTransaccion "
            + "and ((c.idEvaluador = v.idComprador and v.idAgricultor = :idUsuario) "
            + "or (c.idEvaluador = v.idAgricultor and v.idComprador = :idUsuario)) "
            + "order by c.fecha desc, c.id desc",
            countQuery = "select count(c) from Calificacion c, VentaHistorial v where v.id = c.idTransaccion "
                    + "and ((c.idEvaluador = v.idComprador and v.idAgricultor = :idUsuario) "
                    + "or (c.idEvaluador = v.idAgricultor and v.idComprador = :idUsuario))")
    Page<CalificacionRecibida> recibidas(Long idUsuario, Pageable pagina);
}
