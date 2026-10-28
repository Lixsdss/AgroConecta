/**
 * Módulo <b>usuario</b> (RF-08) — capa <b>dominio</b>.
 *
 * Capa de dominio: entidades JPA y enums del módulo. Las entidades de otros módulos se referencian por
 * su id ({@code Long}), nunca con {@code @ManyToOne}; las vistas se mapean como entidades
 * {@code @Immutable}.
 *
 * Tablas y vistas que mapea este módulo:
 * <ul>
 * <li>usuarios</li>
 * <li>agricultores</li>
 * <li>compradores</li>
 * </ul>
 */
package pe.agroconecta.usuario.dominio;
