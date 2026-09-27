/**
 * Módulo <b>ventaagrupada</b> (RF-10) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>propuestas_grupales</li>
 * <li>v_lotes_agrupados (vista, solo lectura)</li>
 * </ul>
 */
package pe.agroconecta.ventaagrupada.dominio;
