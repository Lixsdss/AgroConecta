# ADR-005: Destino de despliegue

**Estado:** Pendiente  
**Fecha de registro:** 27 de septiembre de 2026  
**Fecha límite de decisión:** antes de la semana 15 del curso  
**Responsable:** equipo AgroConecta

## Contexto

AgroConecta se entrega como un único WAR (Spring Boot 4.1.1, Java 21) que se despliega en Tomcat. La base de datos no es un simple almacén de tablas: `modelo-bd.sql` v4.1 define 21 triggers, 1 función (`fn_rol_usuario`), 1 procedimiento (`sp_sincronizar_publicacion`) y 1 evento diario (`ev_tareas_diarias`) que vence publicaciones, crea las alertas de RF-07 y cierra las propuestas conjuntas de RF-10. Sin esos objetos el sistema no cumple sus reglas de negocio.

El proyecto tiene la restricción de **costo cero** (SRS §4.1): no se puede pagar hosting, base de datos gestionada ni servicios externos.

El walking skeleton (RF-08) debía quedar desplegado apenas funcionara, pero la decisión se pospone hasta tener el primer slice terminado.

## Requisitos que debe cumplir el destino

| # | Requisito | Motivo |
|---|---|---|
| 1 | Gratuito durante todo el curso (sin tarjeta que genere cargos, o con límites que lo garanticen) | Costo cero |
| 2 | MySQL 8.0+ o MariaDB 10.6+ con permiso para crear **triggers, funciones, procedimientos y eventos** | Reglas de negocio en la BD |
| 3 | `event_scheduler=ON` de forma persistente | `ev_tareas_diarias` (RF-07, RF-10) |
| 4 | Usuario de aplicación con CREATE, ALTER, INDEX, REFERENCES, TRIGGER, EVENT, CREATE VIEW, CREATE ROUTINE y ALTER ROUTINE; con binlog activo, `log_bin_trust_function_creators=1` o privilegio equivalente | Flyway aplica V1 con esos objetos |
| 5 | Java 21 y un Tomcat compatible con Spring Boot 4.1 (Jakarta Servlet 6.1, Tomcat 11) | Empaquetado WAR |
| 6 | Variables de entorno para `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` | Sin secretos en Git |
| 7 | HTTPS con certificado gratuito (Let's Encrypt) | SRS §9.3 |
| 8 | Acceso para verificar `/actuator/health` con un monitor externo gratuito | RNF-06 |
| 9 | Zona horaria configurable a America/Lima | El evento corre a las 06:00 hora del servidor |
| 10 | Compresión gzip en Tomcat para HTML, CSS, JS, JSON y SVG (`compression="on"` en el conector, o en el proxy HTTPS delante de Tomcat) | RNF-01: carga en menos de 3 s con 4G. En un WAR la propiedad `server.compression` de Spring Boot no se aplica; la configura el servidor |

## Alternativas a evaluar

1. **Máquina virtual gratuita con Tomcat y MariaDB instalados por el equipo** (capa gratuita de un proveedor cloud o créditos académicos). Es la opción más probable: el equipo controla el servidor de BD, los privilegios y `event_scheduler`. Hay que verificar los límites de la capa gratuita, que no expire antes de noviembre de 2026 y el trabajo de administración (actualizaciones, respaldos, firewall).
2. **Servidor de la universidad.** Sin costo, pero depende de que la universidad otorgue acceso con privilegios suficientes sobre MariaDB y permita exponer el servicio.
3. **Plataforma gestionada (PaaS) con base de datos gestionada gratuita.** Solo es válida si la base de datos permite triggers, rutinas y eventos con `event_scheduler=ON`; muchas bases gestionadas o compartidas no lo permiten o no dan los privilegios necesarios. Debe comprobarse antes de considerarla.

Queda descartado cualquier destino que no permita los objetos del punto 2 o el evento del punto 3, porque obligaría a reimplementar reglas de la BD en Java, contra lo decidido en el SRS.

## Decisión

**Pendiente.** Se tomará antes de la semana 15, después de probar el WAR y las migraciones V1 y V2 en el candidato elegido.

## Consecuencias

- Hasta decidir, el sistema solo corre en local y en CI (`.github/workflows/ci.yml`, MariaDB en contenedor).
- Cuando se decida, este ADR se actualiza a **Aceptado** con el destino, la versión de base de datos, los pasos de instalación y los de respaldo, y se documenta el despliegue en `docs/08-operacion/`.
- El perfil `prod` ya lee toda la configuración de variables de entorno. Si el destino usa MariaDB 11 o superior con MySQL Connector/J, `prod` necesitará también fijar `spring.jpa.database-platform` (ver CLAUDE.md §6).
