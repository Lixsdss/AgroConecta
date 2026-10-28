/**
 * Módulo <b>demo</b>.
 *
 * Datos de demostración para presentar la aplicación: usuarios, precios, cosechas, conversaciones, ventas,
 * calificaciones y una propuesta conjunta, generados con Datafaker. Solo actúa con el perfil {@code demo} y nunca
 * con {@code prod} (ADR-006).
 *
 * <p>No es dueño de ninguna tabla: crea todo a través de la capa {@code aplicacion} de los demás módulos, con sus
 * mismas reglas y triggers. Ningún módulo depende de él.</p>
 */
package pe.agroconecta.demo;
