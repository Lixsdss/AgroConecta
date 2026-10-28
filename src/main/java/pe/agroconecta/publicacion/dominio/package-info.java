/**
 * Módulo <b>publicacion</b> (RF-01, RF-03 y RF-07) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>publicaciones</li>
 * <li>v_publicaciones_disponibles (vista, solo lectura)</li>
 * </ul>
 */
package pe.agroconecta.publicacion.dominio;
