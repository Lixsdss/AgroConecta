# ADR-001: Monolito modular verificado con ArchUnit

**Estado:** Aceptado  
**Fecha:** 27 de septiembre de 2026 (registra una decisión tomada en septiembre de 2026)

## Contexto

AgroConecta lo construye un equipo de 6 personas (4 programadores) en un curso de un semestre, con 11 requisitos funcionales que se entregan por rebanadas verticales. Se despliega como un único WAR en Tomcat, con costo cero y una sola base de datos MySQL/MariaDB que aplica reglas de negocio con triggers y un evento diario. El docente evalúa, además del funcionamiento, la calidad de la arquitectura (SOLID, bajo acoplamiento, documentación).

## Decisión

Construir un **monolito modular**: un solo WAR con un módulo de negocio por dominio bajo `pe.agroconecta` (`catalogo`, `usuario`, `publicacion`, `precio`, `mensajeria`, `venta`, `ventaagrupada`, `notificacion`, `administracion`) más `compartido`, cada uno con las capas `api`, `aplicacion`, `dominio` e `infraestructura`.

- Un módulo solo usa a otro a través de su capa `aplicacion`; las entidades no cruzan módulos (se referencian por id).
- Cada módulo es dueño exclusivo de sus tablas y vistas.
- Las reglas se verifican con **ArchUnit** en `ArquitecturaTest` (una prueba por regla), que corre en `./mvnw verify` y en la CI.

El detalle vive en `CLAUDE.md` §5.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Capas técnicas globales** (`controller/`, `service/`, `repository/` para todo el sistema) | Es la estructura más conocida, pero no marca límites entre dominios: cualquier servicio puede usar cualquier repositorio y el acoplamiento crece con cada RF. Dificulta que dos personas trabajen en RF distintos sin pisarse. |
| **Microservicios** | Exigen varios despliegues, comunicación por red, datos distribuidos y más infraestructura. Incompatibles con el costo cero, con el tamaño del equipo y con reglas de negocio que dependen de triggers sobre una sola base de datos. |
| **Spring Modulith** para verificar los módulos | Se usó al inicio. Se retiró el 27/09/2026: sus convenciones (módulos abiertos, interfaces nombradas, eventos) no encajaban con las capas elegidas, y ArchUnit expresa las mismas reglas de forma explícita con menos dependencias. |

## Consecuencias

- **Positivas:** un solo despliegue y una sola base de datos; límites claros entre dominios verificados en cada build; cada RF se implementa dentro de un módulo, lo que permite trabajo en paralelo; migrar un módulo a un servicio aparte sería posible más adelante.
- **Negativas:** más paquetes y DTO que en una estructura por capas; las lecturas entre módulos pasan por servicios de `aplicacion` en lugar de un JOIN directo; algunas reglas (entidades que no cruzan módulos, tablas por dueño) no se pueden verificar del todo con ArchUnit y dependen de la revisión de código.
