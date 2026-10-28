/**
 * Módulo <b>precio</b> (RF-02) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>precios_mercado</li>
 * <li>v_precios_vigentes (vista, solo lectura)</li>
 * <li>v_rango_precio_plataforma (vista, solo lectura)</li>
 * </ul>
 */
package pe.agroconecta.precio.dominio;
