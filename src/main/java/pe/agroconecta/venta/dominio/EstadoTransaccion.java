package pe.agroconecta.venta.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una venta ({@code chk_transaccion_estado}). Las transiciones las valida {@code trg_transacciones_bu}.
 */
public enum EstadoTransaccion {
    /** Parte de una propuesta conjunta, pendiente del agricultor (RF-10). */
    PROPUESTA,
    /** Acuerdo registrado. */
    ACORDADA,
    /** Ya recogieron la carga. */
    RECOGIDO,
    /** La carga va en camino. */
    EN_CAMINO,
    /** El comprador recibió la carga. */
    ENTREGADO,
    /** Venta terminada. */
    COMPLETADA,
    /** El agricultor rechazó la propuesta conjunta (RF-10). */
    RECHAZADA,
    /** Venta cancelada. */
    CANCELADA;

    /** Estados de una venta en curso ("En curso"). */
    public static final Set<EstadoTransaccion> EN_CURSO = EnumSet.of(PROPUESTA, ACORDADA, RECOGIDO, EN_CAMINO,
            ENTREGADO);

    /** Estados finales: la venta ya no se modifica ("Terminadas"). */
    public static final Set<EstadoTransaccion> TERMINADAS = EnumSet.of(COMPLETADA, RECHAZADA, CANCELADA);

    /**
     * Devuelve {@code true} si la venta ya no se modifica.
     *
     * @return {@code true} si la venta ya no se modifica
     */
    public boolean esFinal() {
        return TERMINADAS.contains(this);
    }
}
