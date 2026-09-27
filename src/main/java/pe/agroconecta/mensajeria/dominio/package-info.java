/**
 * Módulo <b>mensajeria</b> (RF-04) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>conversaciones</li>
 * <li>mensajes</li>
 * </ul>
 */
package pe.agroconecta.mensajeria.dominio;
