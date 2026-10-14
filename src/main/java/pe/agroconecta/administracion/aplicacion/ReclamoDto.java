package pe.agroconecta.administracion.aplicacion;

import java.time.LocalDateTime;

/**
 * Reclamo sobre una venta (RF-09).
 *
 * @param id              id del reclamo
 * @param idVenta         venta reclamada
 * @param motivo          motivo de catálogo
 * @param descripcion     descripción
 * @param estado          {@code PENDIENTE}, {@code FUNDADO} o {@code INFUNDADO}
 * @param respuesta       respuesta del administrador, o {@code null}
 * @param fechaRegistro   fecha y hora de registro
 * @param fechaResolucion fecha y hora de resolución, o {@code null}
 * @param producto        "Mango Kent"
 * @param reclamante      quien reclama (negocio del comprador o nombre del agricultor)
 * @param contraparte     la otra parte de la venta
 */
public record ReclamoDto(Long id, Long idVenta, String motivo, String descripcion, String estado, String respuesta,
        LocalDateTime fechaRegistro, LocalDateTime fechaResolucion, String producto, String reclamante,
        String contraparte) {
}
