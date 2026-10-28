# Presentación APF3 — Contenido de las diapositivas

**Proyecto:** AgroConecta · Integrador I: Sistemas – Software · Semana 12
**Duración:** 10 minutos · 6 integrantes · 15 diapositivas (formato 16:9)
**Fuente de verdad:** Capítulo III del informe y `docs/academico/apf3-auditoria.md`. Todo lo marcado como **[VERIFICAR]** debe confirmarse contra el código antes de diseñar.

---

## Indicaciones generales de diseño

- Una idea por diapositiva y un máximo de 5 viñetas cortas. Lo que se explica de palabra va en las notas del orador, no en la diapositiva.
- Cada diapositiva de arquitectura muestra **cómo se aplicó en el código**: un diagrama o un fragmento real (máximo 8 líneas), no la definición del patrón.
- Guía de estilos del proyecto:
  - Títulos en Plus Jakarta Sans, texto en Inter y código en JetBrains Mono.
  - Paleta: verde bosque #0B2E16, verde agrícola #15803D, verde acento #4ADE80, ámbar #D97706 y gris pizarra #0F172A.
- Numerar las diapositivas y poner en el pie el nombre de quien expone.

## Reparto del tiempo

| Bloque | Diapositivas | Expone | Tiempo |
|---|---|---|---|
| Contexto y estado | 1–2 | Integrante 1 | 1:00 |
| Arquitectura: estructura, MVC y DAO | 3–5 | Integrante 2 | 2:00 |
| SOLID y TDD | 6–7 | Integrante 3 | 1:30 |
| Seguridad y librerías | 8–10 | Integrante 4 | 1:30 |
| Avance y demo | 11–12 | Integrante 5 | 2:20 |
| GitHub y cierre | 13–15 | Integrante 6 | 1:40 |
| **Total** | | | **10:00** |

---

## Diapositiva 1 — Portada (Integrante 1)

- Título: **AgroConecta**
- Subtítulo: Avance de Proyecto Final 3 — Desarrollo de la solución
- Curso, docente (Richard Doan Asencios Vilchez), sección y los 6 integrantes
- Visual: logo o ícono de hoja y paleta verde

## Diapositiva 2 — Problema, solución y estado del avance (Integrante 1)

- **Problema:** el pequeño agricultor de Piura vende a intermediarios sin conocer el precio real del mercado.
- **Solución:** plataforma web que conecta directamente a agricultores y compradores, con precios de mercado públicos.
- **Actores:** Agricultor · Comprador · Administrador
- **Indicadores del avance** (4 tarjetas grandes):
  - 11 / 11 requisitos funcionales
  - 238 pruebas en verde
  - 96,2 % de cobertura
  - 67 endpoints REST

> **Notas del orador:** "En este avance construimos la solución: los once requisitos funcionan de extremo a extremo, con pantalla, API y pruebas. Ahora les mostramos cómo está construida."

## Diapositiva 3 — Arquitectura: monolito modular (Integrante 2)

- Visual principal: **diagrama de módulos** (Figura 3.1 del informe), simplificado.
- 9 módulos de negocio más `compartido` (seguridad, errores y configuración).
- Cada módulo tiene 4 capas: `api` → `aplicacion` → `dominio` / `infraestructura`.
- Las reglas se verifican automáticamente con **ArchUnit (5 reglas en verde)**:
  - los módulos solo se comunican por `aplicacion`;
  - no hay ciclos;
  - `compartido` no depende de ningún módulo.

> **Notas del orador:** "Elegimos un monolito modular: un solo despliegue, pero cada módulo es independiente como un microservicio. Y no depende de la disciplina: una prueba falla si alguien rompe una regla."

## Diapositiva 4 — Patrón MVC (Integrante 2)

- Visual: flujo horizontal de una petición real (registrar un precio, RF-02):
  **Vista** (`precios.html` + `precios.js`) → `api.js` con JWT → **Controlador** (`PrecioController`) → **Servicio** (`CargaPrecioService`) → **Modelo** (`PrecioMercado`)
- Fragmento de código: método `registrar` de `PrecioController`, donde se ven `@PreAuthorize` y `@Valid`. **[VERIFICAR / EXTRAER]**
- Datos: 17 controladores REST, 28 páginas + portada, 0 lógica de negocio en los controladores.

> **Notas del orador:** "La vista es HTML con Bootstrap y JavaScript; el controlador solo recibe y valida; la lógica de negocio está en el servicio."

## Diapositiva 5 — Patrón DAO (Integrante 2)

- Visual: las capas `aplicacion` → `infraestructura` (repositorio) → MariaDB.
- Fragmento: la interfaz `PrecioMercadoRepository extends JpaRepository`, de pocas líneas. **[VERIFICAR / EXTRAER]**
- Tres ideas:
  - El servicio no conoce SQL; Spring Data genera la implementación con consultas parametrizadas.
  - Los repositorios solo se usan dentro de su propio módulo (regla de ArchUnit).
  - Defensa en profundidad: el *trigger* `trg_precios_mercado_bi` vuelve a validar en la base de datos.

## Diapositiva 6 — Principios SOLID (Integrante 3)

- Tabla compacta de 5 filas (principio → ejemplo real):
  - **S:** `LectorArchivoPrecios` solo lee el archivo; `CargaPrecioService` solo valida y guarda.
  - **O:** `ValidadorUsuarioActivo` extiende la validación del JWT sin modificar Spring.
  - **L:** `VerificadorEstadoUsuarioBd` sustituye a su interfaz (y un *mock* en las pruebas).
  - **I:** `VerificadorEstadoUsuario` tiene un solo método: `estaActivo(long)`.
  - **D:** `compartido` define la interfaz y `usuario` la implementa; se inyecta por constructor.
- Visual destacado: mini diagrama de la inversión de dependencias (D), en el que `compartido` define la interfaz `VerificadorEstadoUsuario` y `usuario` provee la implementación.

> **Notas del orador:** explicar solo la D con el diagrama; las otras cuatro se leen en la tabla.

## Diapositiva 7 — TDD y pruebas (Integrante 3)

- Visual: pirámide de pruebas de 3 niveles:
  - unitarias con Mockito (133, junto con ArchUnit);
  - integración contra MariaDB real (105);
  - arquitectura con ArchUnit (5 reglas).
- **Ciclo rojo → verde** con un caso real, el límite de intentos de inicio de sesión:
  - commit `test:` con la prueba que falla (5 intentos → 429);
  - commit `feat:` con la implementación usando Guava Cache;
  - suite en verde.

  **[VERIFICAR: hashes de los dos commits]**
- Resultado: 238/238 · 0 fallas · 96,2 % de líneas · todos los módulos ≥ 95 % en la capa de servicios. **[VERIFICAR cifras finales]**
- Visual secundario: captura del informe JaCoCo.

## Diapositiva 8 — Seguridad en la arquitectura (Integrante 4)

- Íconos con una línea cada uno:
  - **Autenticación:** JWT HS256 con vigencia de 12 h; contraseñas con BCrypt.
  - **Autorización:** roles con `@PreAuthorize` y control de dueño (solo las partes ven su venta o chat).
  - **Cuenta activa:** un usuario suspendido queda bloqueado en la siguiente petición (401).
  - **Fuerza bruta:** 5 intentos fallidos en 15 minutos → 429. **[VERIFICAR]**
  - **Datos personales:** celular enmascarado en los logs (`98*****81`), Ley N.° 29733.
  - **Secretos:** solo en variables de entorno; ninguno en el historial de Git.
- Pie: relación con el OWASP Top 10 (A01, A02, A03, A07, A09).

## Diapositiva 9 — Librerías Java (Integrante 4)

Cuatro tarjetas, cada una con **para qué** y **cómo protege**:

| Librería | Para qué | Seguridad |
|---|---|---|
| Google Guava 33.7.1 | `Preconditions` en servicios, colecciones inmutables, `Splitter` para CSV, `Cache` de intentos de login | Falla rápido ante datos inválidos; resultados que nadie puede alterar |
| Apache Commons Lang3 3.20.0 | Limpieza y normalización de texto (`StringUtils`) | Sanitiza entradas; enmascara el celular |
| Apache POI 5.5.1 | Importar precios desde Excel; exportar historial y panel a Excel | Máximo 1 MB, solo .xlsx/.csv, 500 filas, protección contra *zip bombs* |
| Logback 1.5.38 | Auditoría de acciones, accesos negados y errores; archivo rotativo en producción | Nunca registra contraseñas, tokens ni mensajes |

**[VERIFICAR: uso de `Cache` y configuración de `logback-spring.xml`]**

> **Notas del orador:** mostrar un ejemplo concreto de POI: "El administrador sube un Excel con precios y el comprador descarga su historial en Excel."

## Diapositiva 10 — Otras herramientas del proyecto (Integrante 4, 15 s)

Franja de "badges" sin explicación detallada:

- **Seguridad:** Spring Security con OAuth2 Resource Server (Nimbus JWT)
- **Base de datos:** Flyway (migraciones V1–V3), MariaDB 11
- **Calidad:** JUnit 5, Mockito, ArchUnit, JaCoCo
- **Documentación:** springdoc-openapi (Swagger UI), Javadoc
- **Interfaz:** Bootstrap 5

**[VERIFICAR: lista y versiones contra el pom.xml]**

## Diapositiva 11 — Desarrollo por rebanadas verticales (Integrante 5)

- Visual 1: una "rebanada" como columna que atraviesa todas las capas:
  base de datos (migración + *trigger*) → repositorio → servicio → controlador → pantalla → pruebas
- Definición de terminado de cada RF:
  - pruebas en verde;
  - ≥ 70 % de cobertura del servicio;
  - pantalla conectada a la API real;
  - documentación del contrato.
- Visual 2: línea de tiempo con las ramas por RF y sus fechas, en el orden en que se construyeron:
  RF-08 → RF-01 → RF-09 → RF-02 → RF-03 → RF-04 → RF-06 → RF-05 → RF-07 → RF-10 → RF-09 (administración)

  **[VERIFICAR: fechas de cada rama]**

> **Notas del orador:** "No construimos primero toda la base de datos y luego toda la interfaz: cada requisito se terminó completo antes de pasar al siguiente. Por eso cada avance del sílabo tuvo funcionalidades usables."

## Diapositiva 12 — Demostración en vivo (Integrante 5, 1:40)

Diapositiva de transición con el guion de la demo (el expositor cambia al navegador):

| Paso | Pestaña | Acción | RF |
|---|---|---|---|
| 1 | Sin sesión | Portada con precios de mercado públicos | RF-02 |
| 2 | Agricultor | Publicar una cosecha en 3 pasos | RF-01 |
| 3 | Comprador | Buscar con filtros y abrir el detalle | RF-03 |
| 4 | Comprador | Enviar un mensaje y proponer el acuerdo | RF-04, RF-06 |
| 5 | Agricultor | Ver el aviso, aceptar y marcar "Recogido" | RF-07, RF-11 |
| 6 | Comprador | Confirmar recepción y calificar | RF-11, RF-05 |
| 7 | Administrador | Panel de métricas y exportar a Excel | RF-09 |

**Preparación:**
- Abrir **una pestaña por actor**. La sesión se guarda en `sessionStorage`, que es independiente en cada pestaña, así que los tres actores pueden tener sesión iniciada a la vez.
- Cargar antes los datos de prueba, para que existan avisos, ventas y lotes que mostrar.
- **Plan B:** un video de 90 s del mismo recorrido, por si falla la conexión o el servidor.
- RF-08 (registro e inicio de sesión) y RF-10 (venta agrupada) no entran en la demo: se muestran en una diapositiva de respaldo con capturas (12b).

## Diapositiva 12b — Galería de interfaces (respaldo)

- Capturas de RF-08 (registro, elegir rol, inicio) y RF-10 (lotes y propuesta conjunta).
- Se usa solo si sobra tiempo o si el docente pregunta por esos requisitos.

## Diapositiva 13 — Control de versiones con Git y GitHub (Integrante 6)

- Visual: esquema de ramas `main` ← `develop` ← `feature/rf-xx`, con *pull requests* y la etiqueta `apf3`.
- Tres datos:
  - 70+ commits con Conventional Commits en español;
  - CI con GitHub Actions en cada *pull request*;
  - secretos fuera del repositorio.
- **Luego, en el navegador**, este recorrido:
  1. Ramas del repositorio
  2. *Pull requests* fusionados
  3. Historial de commits (señalar un par `test:` → `feat:`)
  4. Pestaña Actions en verde
  5. Etiqueta / release `apf3`
  6. README
- **Respaldo:** las mismas 6 vistas en capturas, por si no hay internet.

## Diapositiva 14 — Conclusiones y próximos pasos (Integrante 6)

- Los cuatro principios del curso están aplicados y se verifican con pruebas automáticas.
- 11 de 11 requisitos funcionando: supera el mínimo del avance.
- Unidad 4:
  - semana 13: pruebas;
  - semana 14: pruebas de seguridad;
  - semana 15: despliegue;
  - semanas 16–17: monitoreo y mantenimiento.

## Diapositiva 15 — Preguntas

- "¿Preguntas?" con el logo y el enlace al repositorio.

---

## Preguntas probables del docente (preparar respuesta de una línea)

| Pregunta | Respuesta corta | Quién |
|---|---|---|
| ¿Por qué monolito y no microservicios? | Un solo despliegue a costo cero, con módulos aislados que se verifican con ArchUnit | Integrante 2 |
| ¿Dónde está el DAO si no hay clases DAO? | Spring Data JPA: las interfaces `Repository` son los DAO; Spring genera la implementación | Integrante 2 |
| ¿Cómo saben que hicieron TDD? | Commits `test:` antes de `feat:` y la cobertura medida en cada build | Integrante 3 |
| ¿Por qué pruebas contra MariaDB y no H2? | Para probar también los *triggers*, vistas y restricciones reales | Integrante 3 |
| ¿Qué pasa si roban el token? | Vence en 12 h y deja de servir si el usuario se suspende | Integrante 4 |
| ¿Por qué no hay pagos? | Decisión de costo cero (ADR-002); el pago se negocia fuera de la plataforma | Integrante 1 |
| ¿Cómo se integró el trabajo de todos? | Ramas por RF, *pull requests* revisados y CI en verde | Integrante 6 |
