package pe.agroconecta.venta.infraestructura;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import pe.agroconecta.venta.dominio.EstadoTransaccion;
import pe.agroconecta.venta.dominio.VentaHistorial;

/**
 * Condiciones de "Mis ventas" y "Mis compras" (UC-08) sobre {@code v_historial_transacciones}. Cada filtro opcional
 * devuelve una condición vacía si su valor es {@code null}.
 */
public final class EspecificacionesVentas {

    private EspecificacionesVentas() {
    }

    /**
     * Ventas en las que participa el usuario (como comprador o como agricultor).
     *
     * @param idUsuario usuario
     * @return condición
     */
    public static Specification<VentaHistorial> deUsuario(long idUsuario) {
        return (raiz, consulta, cb) -> cb.or(cb.equal(raiz.get("idComprador"), idUsuario),
                cb.equal(raiz.get("idAgricultor"), idUsuario));
    }

    /**
     * Estado dentro de un grupo.
     *
     * @param estados estados aceptados, o {@code null} para todos
     * @return condición
     */
    public static Specification<VentaHistorial> estadoEn(Collection<EstadoTransaccion> estados) {
        return (raiz, consulta, cb) -> estados == null ? cb.conjunction() : raiz.get("estado").in(estados);
    }

    /**
     * Fecha del acuerdo dentro del rango (días completos); cada extremo es opcional.
     *
     * @param desde primer día, o {@code null}
     * @param hasta último día, o {@code null}
     * @return condición
     */
    public static Specification<VentaHistorial> acordadaEntre(LocalDate desde, LocalDate hasta) {
        return (raiz, consulta, cb) -> cb.and(
                desde == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(raiz.get("fechaCreacion"), desde.atStartOfDay()),
                hasta == null ? cb.conjunction()
                        : cb.lessThan(raiz.get("fechaCreacion"), hasta.plusDays(1).atStartOfDay()));
    }

    /**
     * Nombre de la otra parte que contiene el texto (sin distinguir mayúsculas): el negocio del comprador si el
     * usuario es el agricultor, o el nombre del agricultor si es el comprador.
     *
     * @param idUsuario usuario
     * @param texto     texto buscado, o {@code null}
     * @return condición
     */
    public static Specification<VentaHistorial> contraparteContiene(long idUsuario, String texto) {
        return (raiz, consulta, cb) -> {
            if (texto == null) {
                return cb.conjunction();
            }
            // "!" escapa los comodines del texto: "50%" busca literalmente "50%"
            String patron = "%" + texto.toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            return cb.or(
                    cb.and(cb.equal(raiz.get("idAgricultor"), idUsuario),
                            cb.like(cb.lower(raiz.get("comprador")), patron, '!')),
                    cb.and(cb.equal(raiz.get("idComprador"), idUsuario),
                            cb.like(cb.lower(raiz.get("agricultor")), patron, '!')));
        };
    }

    /**
     * Solo las ventas indicadas.
     *
     * @param ids ids de ventas, o {@code null} para no filtrar
     * @return condición
     */
    public static Specification<VentaHistorial> idEn(Collection<Long> ids) {
        return (raiz, consulta, cb) -> ids == null ? cb.conjunction() : raiz.get("id").in(ids);
    }
}
