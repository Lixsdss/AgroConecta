/**
 * Módulo <b>notificacion</b> (RF-07 (alertas)) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>notificaciones</li>
 * </ul>
 */
package pe.agroconecta.notificacion.dominio;
