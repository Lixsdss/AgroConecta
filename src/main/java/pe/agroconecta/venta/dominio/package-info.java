/**
 * Módulo <b>venta</b> (RF-05, RF-06 y RF-11) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>transacciones</li>
 * <li>transacciones_historial (solo lectura: la llenan los triggers)</li>
 * <li>calificaciones</li>
 * <li>v_historial_transacciones (vista, solo lectura)</li>
 * <li>v_reputacion_usuario (vista, solo lectura)</li>
 * </ul>
 */
package pe.agroconecta.venta.dominio;
