/**
 * Módulo <b>catalogo</b> (Soporte de RF-01, RF-02, RF-03 y RF-10) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>provincias</li>
 * <li>distritos</li>
 * <li>cultivos</li>
 * <li>variedades</li>
 * <li>unidades_medida</li>
 * </ul>
 */
package pe.agroconecta.catalogo.dominio;
