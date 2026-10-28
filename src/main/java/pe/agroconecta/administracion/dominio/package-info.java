/**
 * Módulo <b>administracion</b> (RF-09) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>revisiones_publicacion</li>
 * <li>reclamos</li>
 * <li>suspensiones</li>
 * <li>v_panel_admin (vista, solo lectura)</li>
 * <li>v_resumen_usuario (vista, solo lectura)</li>
 * <li>v_ventas_mensuales_cultivo (vista, solo lectura)</li>
 * <li>v_conversion_publicacion (vista, solo lectura)</li>
 * </ul>
 */
package pe.agroconecta.administracion.dominio;
