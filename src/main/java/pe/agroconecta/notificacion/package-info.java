/**
 * Módulo <b>notificacion</b>.
 *
 * Centro de notificaciones del usuario: listado, marcado como leídas y creación de avisos a pedido de
 * otros módulos. Las alertas de vencimiento las crea el evento ev_tareas_diarias.
 *
 * <p>Requisitos: RF-07 (alertas).</p>
 *
 * <p>Capas: {@code api}, {@code aplicacion}, {@code dominio} e {@code infraestructura}. Otros módulos
 * solo usan su capa {@code aplicacion}.</p>
 */
package pe.agroconecta.notificacion;
