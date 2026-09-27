# CLAUDE.md — AgroConecta (v3.3)

Guide for Claude Code. Read this file completely before any task.

## 1. Project

AgroConecta is a web platform that connects small farmers in Piura (Peru) directly with buyers (wholesale markets, restaurants, hotels, food processors) to reduce dependence on middlemen. University capstone project: course "Integrador I: Sistemas – Software" (100000I58N), August–November 2026.

- Actors (only these three): **Agricultor**, **Comprador**, **Administrador**. Cooperatives/associations and carriers are NOT system users.
- Scope: RF-01 to RF-11. RF-12 (subscriptions) is documented as future work only — no code.
- No payment gateway. Payment happens between the parties outside the platform. A "transacción" only records the agreement between buyer and farmer.
- Web application only (no native mobile app).
- **Zero cost.** The project must not generate any expense (see section 4.1).

## 2. Language rules

- Everything user-facing or human-readable is in **Spanish**: Javadoc, code comments, UI text, API error messages, log messages, commit messages, documentation in `docs/`.
- Domain identifiers are in **Spanish** (classes, fields, methods, tables): `Publicacion`, `cantidad`, `buscarPorCultivo()`.
- Conventional technical suffixes stay in English: `Controller`, `Service`, `Repository`, `Request`, `Response`, `Dto`, `Config`. Layer packages are in Spanish: `api`, `aplicacion`, `dominio`, `infraestructura`.

## 3. Sources of truth

Read these before implementing anything. When two sources conflict, the higher one wins:

1. `docs/06-decisiones/` (ADRs) — approved decisions, never reopen them unless asked.
2. `docs/04-diseno/base-datos/modelo-bd.sql` — the database schema. Wins over `modelo-logico.md`.
3. `docs/02-requisitos/SRS-AgroConecta.md` — functional/non-functional requirements and business rules. Wins over use cases.
4. `docs/02-requisitos/casos-de-uso/` and `docs/02-requisitos/matriz-trazabilidad.md`.
5. `docs/03-procesos/to-be.md` — TO-BE business process.
6. `docs/04-diseno/ux-ui/` — style guide and screen ↔ endpoint mapping.

Never read or modify `docs/academico/` (course deliverables for the professor, often outdated). Never modify files in `docs/` unless the task explicitly asks for it.

If a requirement is ambiguous or the schema lacks something the code needs: **stop and ask**. Do not invent business rules, columns or tables.

## 4. Tech stack

- Java 21, Spring Boot 4.1.1, Maven Wrapper (`./mvnw`), packaging **WAR** (deployed on Tomcat; `ServletInitializer` must stay).
- Spring Web (REST), Spring Data JPA (Hibernate), Validation, Actuator, Flyway (+ `flyway-mysql`). Architecture rules are verified only with ArchUnit (section 5).
- Security: Spring Security + **OAuth2 Resource Server** with JWT signed with HMAC (HS256). Login is **phone number + password** (BCrypt). Tokens are issued by the `usuario` module with `NimbusJwtEncoder`: `sub` = user ID, claim `roles`. The secret comes from the `JWT_SECRET` environment variable. No hand-written JWT filter and no JJWT.
- On every authenticated request, the user must still be ACTIVO (SRS §9.1). Implement it as a JWT validator in `compartido.seguridad` that calls an interface `VerificadorEstadoUsuario` defined in `compartido.seguridad`; the `usuario` module implements it (primary-key lookup, no cache unless load tests require it). A SUSPENDIDO or INACTIVO user gets 401.
- Libraries required by the course: Apache POI (`poi-ooxml`), Google Guava, Apache Commons Lang3, Logback (included by default).
- springdoc-openapi (Swagger UI), version compatible with Spring Boot 4.1.
- Tests: JUnit 5, Mockito, ArchUnit (`archunit-junit5`), JaCoCo plugin.
- Database: MySQL in production, MariaDB in local development (Arch Linux). JDBC driver: MySQL Connector/J.
- Frontend: static HTML5 + CSS3 + vanilla JavaScript + Bootstrap 5, served from `src/main/resources/static/`. No React, no Thymeleaf, no Node build step.
- Do not add: Lombok, H2, Testcontainers, Thymeleaf, JJWT, payment libraries.

### 4.1 Zero-cost rule

- Only free and open-source software. No paid services, paid plans, commercial API keys or dependencies that require them.
- No external infrastructure services (no Redis, message brokers, e-mail/SMS providers, cloud storage). If something needs a cache, use in-memory Spring Cache.
- Deployment target and external monitoring are chosen among free options and recorded in an ADR before week 15.
- If a task seems to require something that costs money, stop and ask.

## 5. Arquitectura modular

Un solo WAR dividido en **módulos de negocio** bajo `pe.agroconecta`. Todos los módulos tienen las mismas cuatro capas. Las reglas se verifican con **ArchUnit** en `src/test/java/pe/agroconecta/ArquitecturaTest.java` (una prueba por regla).

### 5.1 Mapa de módulos

Un módulo es dueño **solo** de las tablas y vistas de su fila; ningún otro módulo las mapea.

| Módulo | Requisitos | Tablas y vistas que le pertenecen |
|---|---|---|
| `catalogo` | catálogos compartidos | provincias, distritos, cultivos, variedades, unidades_medida |
| `usuario` | RF-08 | usuarios, agricultores, compradores |
| `publicacion` | RF-01, RF-03, RF-07 | publicaciones, v_publicaciones_disponibles |
| `precio` | RF-02 | precios_mercado, v_precios_vigentes, v_rango_precio_plataforma |
| `mensajeria` | RF-04 | conversaciones, mensajes |
| `venta` | RF-05, RF-06, RF-11 | transacciones, transacciones_historial, calificaciones, v_historial_transacciones, v_reputacion_usuario |
| `ventaagrupada` | RF-10 | propuestas_grupales, v_lotes_agrupados |
| `notificacion` | RF-07 (alertas) | notificaciones |
| `administracion` | RF-09 | revisiones_publicacion, reclamos, suspensiones, v_panel_admin, v_resumen_usuario, v_ventas_mensuales_cultivo, v_conversion_publicacion |

`pe.agroconecta.compartido` no es un módulo de negocio: contiene `config/` (OpenAPI, CORS), `seguridad/` (Spring Security, JWT, interfaz `VerificadorEstadoUsuario`) y `error/` (`ErrorResponse`, `ManejadorGlobalExcepciones`). No tiene lógica de negocio.

### 5.2 Capas de cada módulo

```
pe.agroconecta.<modulo>/
├── package-info.java       responsabilidad y requisitos del módulo
├── api/                    XxxController (@RestController) + XxxRequest / XxxResponse (records con Bean Validation)
├── aplicacion/             XxxService (casos de uso, reglas de negocio, @Transactional) + XxxDto (records que devuelve)
├── dominio/                entidades JPA y enums (las vistas, como entidades @Immutable)
└── infraestructura/        XxxRepository (Spring Data JPA)
```

Flujo de una petición: `api` → `aplicacion` → `infraestructura` → base de datos. Cada capa tiene un `package-info.java` con su responsabilidad (en `dominio`, las tablas que mapea).

### 5.3 Reglas verificadas con ArchUnit (`ArquitecturaTest`)

1. Un módulo usa a otro **solo a través de la capa `aplicacion`** de ese otro módulo. Nunca toca su `api`, `dominio` ni `infraestructura`.
2. Los repositorios (`infraestructura`) solo los usa la capa `aplicacion` de su propio módulo.
3. Los controladores (`api`) dependen de `aplicacion`, nunca directamente de `infraestructura`.
4. `compartido` no depende de ningún módulo. Define interfaces (como `VerificadorEstadoUsuario`) que los módulos implementan; Spring las inyecta por tipo.
5. No hay ciclos de dependencias entre módulos.

Las reglas usan `allowEmptyShould(true)` para pasar mientras una capa esté vacía. `ArquitecturaTest.MODULOS` debe coincidir con el mapa de módulos. Nunca se relaja una regla para que pase: se corrige la dependencia.

### 5.4 Reglas que ArchUnit no verifica por completo (obligatorias)

- **Las entidades no cruzan módulos:** una entidad referencia una entidad de otro módulo por su id (`Long idAgricultor`), nunca con `@ManyToOne`. Las FK siguen existiendo en la base de datos.
- **`api` expone DTO (records de Java), nunca entidades.** Los servicios de `aplicacion` que usan otros módulos también devuelven DTO (records en `aplicacion`), nunca entidades.
- **Cada módulo mapea solo sus tablas** (sección 5.1). Las vistas son entidades de solo lectura (`@Immutable`). Para leer datos de otro módulo se llama a su servicio de `aplicacion`.
- Muchos cambios de estado ocurren en la base de datos (triggers y `ev_tareas_diarias`), no en Java. No se crean eventos de dominio ni tareas `@Scheduled` para ellos. Los eventos de aplicación (`ApplicationEventPublisher`) solo se usan con aprobación; hoy no hay ninguno.
- Las notificaciones no son eventos: el módulo que ejecuta la acción llama al servicio de `notificacion.aplicacion` dentro de la misma transacción, usando solo los tipos del `CHECK` de `notificaciones.tipo` (MENSAJE, OFERTA, VENTA, PROPUESTA_CONJUNTA, CALIFICACION, RECLAMO, CUENTA). POR_VENCER la crea `ev_tareas_diarias` y PUBLICACION_APROBADA/RECHAZADA, `trg_revisiones_ai`.
- `administracion` lee las vistas de su fila con sus propias entidades `@Immutable`.

### 5.5 Reglas de código

- Los controladores no tienen lógica de negocio y nunca devuelven entidades.
- Las reglas de negocio viven solo en `aplicacion`. `@Transactional` en los métodos que escriben.
- Los DTO son records. La entrada se valida con Bean Validation en los `Request` de `api`.
- Toda clase y todo método públicos tienen Javadoc en español (RNF-04).
- SOLID; inyección por constructor; nunca inyección por campo.
- Guava / Commons Lang3 cuando simplifiquen el código; Apache POI para Excel en `precio` y `venta`.

### 5.6 Cómo construir un slice vertical

Para cada RF, en su módulo (sección 5.1):

1. **`dominio`:** entidades de las tablas del módulo y enums de sus `CHECK`, siguiendo la sección 6.2 (columnas generadas de solo lectura, `@Enumerated(EnumType.STRING)`, `id_actualizado_por` en cada update de `transacciones`, referencias a otros módulos por id).
2. **`infraestructura`:** un `XxxRepository extends JpaRepository` por entidad que lo necesite; bloqueo `PESSIMISTIC_WRITE` donde lo indica la sección 6.2.
3. **`aplicacion`:** `XxxService` con los casos de uso, las validaciones con mensajes en español (la BD es la última barrera) y los `XxxDto` que devuelve. Para datos de otro módulo, su servicio de `aplicacion`.
4. **`api`:** `XxxController` con `@PreAuthorize`, `XxxRequest` / `XxxResponse` (records) y el contrato en `docs/05-api/endpoints.md`.
5. **Pruebas:** unitarias del servicio (Mockito), del controlador (MockMvc) e integración contra MariaDB (`agroconecta_test`, perfil `test`) para lo que dependa de triggers; `ArquitecturaTest` debe pasar.
6. **Pantalla:** `src/main/resources/static/pages/<modulo>/` + `static/js/<modulo>/`, conectada a los endpoints reales mediante `js/api.js` (sección 10).

## 6. Database (data model v4)

- The schema is defined by SQL, not by JPA: `spring.jpa.hibernate.ddl-auto=validate`. `docs/04-diseno/base-datos/modelo-bd.sql` (v4) is the single source of truth; `cambios-v4.md` explains the changes from v3, `modelo-logico.md` has one ER diagram per module and `notas-integridad-bd.md` lists what the database does not guarantee.
- Migrations live in `src/main/resources/db/migration/`:
  - `V1__esquema_inicial.sql` = sections 2–11 of `modelo-bd.sql` (no `DROP/CREATE DATABASE` / `USE`). It starts with `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;`: triggers and routines keep the collation of the session that created them.
  - `V2__datos_iniciales.sql` = section 12 of `modelo-bd.sql` (provinces, the 65 districts of Piura, units, crops, varieties). The first ADMINISTRADOR is created by the application, never by SQL.
  - Flyway (`flyway-mysql`) runs the `DELIMITER $$` blocks of V1 as they are.
- Schema changes: new migration `V3__...` onward AND update `modelo-bd.sql`. Only with explicit approval. Never edit an applied migration.
- Tables are in plural Spanish. v4 has **20 tables**, 36 foreign keys, **10 views**, 21 triggers, 1 function (`fn_rol_usuario`), 1 procedure (`sp_sincronizar_publicacion`) and 1 daily event (`ev_tareas_diarias`, 06:00).
- Roles, states, reasons, sources and notification types are fixed lists enforced with `CHECK` constraints, not catalog tables. Only `provincias`, `distritos`, `unidades_medida`, `cultivos` and `variedades` are catalog tables.
- The database user needs privileges for CREATE, ALTER, INDEX, REFERENCES, TRIGGER, EVENT, CREATE VIEW, CREATE ROUTINE and ALTER ROUTINE (`fn_rol_usuario`, `sp_sincronizar_publicacion`). The server needs `event_scheduler=ON`; it is a server setting, not part of the migrations. Both are configured outside the migrations.
- `spring.jpa.open-in-view=false`.

### 6.1 Business rules enforced by triggers

Before writing service logic, read the triggers of the tables involved. The database already enforces these rules and performs these side effects:

| Trigger / object | Rule or side effect |
|---|---|
| `trg_agricultores_bi`, `trg_compradores_bi` | The subtype row matches `usuarios.rol` |
| `trg_publicaciones_bi` | Every listing starts `EN_REVISION`; fixed units (kilo, quintal, tonelada) take `peso_unidad_kg` from the catalog, variable units (jaba, saco) require it; if `fecha_limite` is NULL it is `fecha_cosecha + cultivos.vida_util_dias`; `fecha_limite` ≥ today; the first listing of a farmer gets `marca_revision = PRIMERA_PUBLICACION` |
| `trg_publicaciones_bu` | Allowed state transitions; "Extender" a VENCIDA listing returns it to PUBLICADA; quantity cannot drop below what is already agreed; the farmer never changes |
| `trg_revisiones_bi`, `trg_revisiones_ai` | Only an ADMINISTRADOR reviews, only `EN_REVISION` listings; the review sets PUBLICADA/RECHAZADA and creates the PUBLICACION_APROBADA/RECHAZADA notification |
| `trg_precios_mercado_bi`, `trg_precios_mercado_bu` | Only an ADMINISTRADOR loads or edits prices, only for districts with `es_zona_precio` |
| `trg_conversaciones_bi`, `trg_mensajes_bi` | Only PUBLICADA listings are contacted; the sender takes part in the conversation |
| `trg_transacciones_bi` | A sale starts ACORDADA (chat agreement) or PROPUESTA (only with `id_propuesta_grupal`); the listing is PUBLICADA; `id_actualizado_por` is one of the parties; weight of the agreed unit; quantity in kg ≤ available; coherence with the group proposal |
| `trg_transacciones_bu` | Finished sales and terms (unit, quantity, weight, price) never change; allowed transitions per user: the farmer accepts/rejects a PROPUESTA, the buyer cancels it; ACORDADA → RECOGIDO/CANCELADA and RECOGIDO → EN_CAMINO by either party; EN_CAMINO → ENTREGADO and ENTREGADO → COMPLETADA only by the buyer; `id_actualizado_por = NULL` only for the automatic cancellation of expired proposals |
| `trg_transacciones_ai`, `trg_transacciones_au` | Insert the row in `transacciones_historial` (timeline) and call `sp_sincronizar_publicacion` (listing VENDIDA when fully agreed, back to PUBLICADA/VENCIDA when an agreement is cancelled) |
| `trg_calificaciones_bi` | Only COMPLETADA sales, only by one of the parties (one per evaluator: unique key) |
| `trg_reclamos_bi`, `trg_reclamos_bu` | Only on agreed sales, only by a party, starts PENDIENTE; only an ADMINISTRADOR resolves it, once; sets `fecha_resolucion` |
| `trg_suspensiones_*` | Only an ADMINISTRADOR suspends, never another administrator; one open suspension per user; `RECLAMOS_FUNDADOS` requires ≥ 3 founded complaints; insert sets `usuarios.estado = SUSPENDIDO`, setting `fecha_fin` sets it back to ACTIVO |
| `ev_tareas_diarias` | POR_VENCER notifications 3 days and 1 day before `fecha_limite`; PUBLICADA → VENCIDA after the deadline; cancels expired group PROPUESTA sales and closes their proposals |

Never reimplement these side effects in Java and never use `@Scheduled` for the daily tasks.

### 6.2 Rules for the vertical slices (JPA and services)

- Generated columns (`cantidad_kg`, `precio_kg`, `monto_total`) are mapped with `@Column(insertable = false, updatable = false)`. Refresh the entity after saving when their value or a trigger-set value (`peso_unidad_kg`, `fecha_limite`, `estado`) is needed.
- Roles, states and reasons are `CHECK`-constrained strings: map them as Java enums with `@Enumerated(EnumType.STRING)`, with exactly the values of the `CHECK`.
- Every update to `transacciones` must set `id_actualizado_por` to the user making the change; the triggers use it for the timeline and for permissions.
- "Confirmar que recibí" performs two updates (ENTREGADO, then COMPLETADA) in one `@Transactional` method.
- The application never inserts into `transacciones_historial`; the triggers do.
- Services also validate business rules to return friendly messages in Spanish; the database is the last barrier (its errors are mapped as described in section 8).
- Lock the listing with `PESSIMISTIC_WRITE` before a sale becomes ACORDADA (chat agreement or accepted group proposal) (RNF-02).

## 7. Configuration

- `application.properties`: common settings, `spring.profiles.active=dev`.
- `application-dev.properties`: local MariaDB, database `agroconecta`, user `agroconecta_app`.
- `application-test.properties`: local MariaDB, database `agroconecta_test`. Tests that start the Spring context use this profile. Do not use H2 (the schema uses MySQL triggers and events).
- `application-prod.properties`: values from environment variables only.
- Connection settings use environment variables with local defaults, so the same files work on developer machines and in CI:
  - `spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/<database>}`
  - `spring.datasource.username=${DB_USER:agroconecta_app}`
  - `spring.datasource.password=${DB_PASSWORD}`
- Secrets never go into Git: `DB_PASSWORD` and `JWT_SECRET` have no default value.

## 8. REST API conventions

- Base path: `/api/v1/<recurso-en-plural>` (e.g. `/api/v1/publicaciones`).
- Verbs: GET read, POST create (201 + Location), PUT/PATCH update, DELETE delete.
- Lists are paginated (`page`, `size`, `sort`).
- Errors are handled in `compartido.error.ManejadorGlobalExcepciones` and use one JSON format (SRS §9.2), `compartido.error.ErrorResponse`: `{ timestamp, estado, error, mensaje, ruta, errores[] }`. `error` is the standard HTTP reason phrase; each item of `errores` is `{ campo, mensaje }` and only appears for 400 (otherwise it is an empty list). SQL details are never exposed.
- Status mapping (the engine error code is checked before the SQLSTATE):
  - 400: Bean Validation (field messages).
  - 422: trigger error (SQLSTATE 45000, with the trigger's message text), CHECK violation (3819 MySQL / 4025 MariaDB, generic Spanish message) and business rules rejected by a service (`compartido.error.ReglaNegocioException`, with its message).
  - 409: unique key duplicate (1062) or foreign key violation (1451, 1452), with a generic Spanish message.
  - 500: anything else, with a generic message; the detail is logged with Logback.
  - 401: missing/invalid token or user not ACTIVO. 403: wrong role or not the owner (Spring Security; the handler rethrows its exceptions). 404: not found.
- Public endpoints: `POST /api/v1/auth/registro`, `POST /api/v1/auth/login`, `GET /api/v1/catalogo/**`, `GET /api/v1/publicaciones/**`, `GET /api/v1/precios/**`, static files, `/actuator/health` (the only exposed Actuator endpoint), Swagger UI. Everything else requires JWT.
- Roles in the token claim `roles`: `AGRICULTOR`, `COMPRADOR`, `ADMINISTRADOR`. Use `@PreAuthorize` in controllers.
- Keep `docs/05-api/endpoints.md` updated when adding endpoints.

## 9. Key business rules (full list in the SRS)

- RF-01: publishing takes max. 3 steps (variety → crop, quantity and **unit** with the weight of a jaba/saco; price per unit and district; harvest date and deadline). Units come from `unidades_medida` (seed data in V2); never hardcode them. Selling by area is out of scope. If no deadline is given, the trigger calculates it. Every listing starts EN_REVISION and is published after the administrator review (RF-09).
- RF-02: public price board; the admin uploads by form or CSV/Excel.
- RF-04: one conversation per listing and buyer; the WhatsApp button records a lead.
- RF-05: rating only for COMPLETADA transactions, once per evaluator.
- RF-06: either party registers the agreement reached in the chat, and the sale starts ACORDADA; PROPUESTA exists only for group sales (RF-10). Either party cancels an ACORDADA sale.
- RF-07: notifications at 3 days and 1 day before the deadline are created by `ev_tareas_diarias`; quick actions live in `publicacion`.
- RF-08: phone is the login identifier; email is optional; the role never changes after registration.
- RF-10: the **buyer** creates the joint proposal (price per kg, kg, pickup date); each farmer accepts or rejects their PROPUESTA; `ev_tareas_diarias` cancels the ones not answered by the pickup date.
- RF-11: ACORDADA → RECOGIDO → EN_CAMINO → ENTREGADO → COMPLETADA. Only the buyer confirms reception ("Confirmar que recibí": ENTREGADO and COMPLETADA in one transaction, section 6.2). The timeline is `transacciones_historial`; it is never edited, deleted or "corrected".
- Use cases: UC-01 to UC-12 (UC-12 = RF-10). RF-06 is covered by UC-05, UC-06 A1 and UC-08.

## 10. Frontend

```
src/main/resources/static/
├── index.html
├── css/estilos.css           palette and components from docs/04-diseno/ux-ui/guia-estilos.md
├── vendor/bootstrap/         Bootstrap 5 served locally (rural 4G, RNF-01)
├── img/
├── js/
│   ├── api.js                HTTP client: base URL, JWT in sessionStorage + Authorization header, error handling
│   ├── comun/                shared UI helpers
│   └── <modulo>/             one JS file per screen
└── pages/<modulo>/           one HTML file per screen (exported from Claude Design)
```

- RNF-03: large buttons, high contrast, linear navigation. RNF-01: page load under 3 s on 4G — keep pages light, no heavy libraries.
- Screens call only documented endpoints (`docs/04-diseno/ux-ui/pantallas.md`).
- Render user data with `textContent`, never `innerHTML`. Never log passwords, tokens or message content.
- The `templates/` folder is not used (no server-side rendering).

## 11. Testing

- Unit tests for services with Mockito; controller tests with MockMvc; integration tests against MariaDB (`agroconecta_test`) for anything that depends on triggers; architecture tests in `ArquitecturaTest` (ArchUnit).
- Test packages mirror the modules: `src/test/java/pe/agroconecta/<modulo>/`.
- Every RF is finished only when its tests pass. Run `./mvnw test` before finishing any task.
- Minimum coverage: 70 % in the service layer (JaCoCo).
- JaCoCo report: `./mvnw verify` → `target/site/jacoco/`.
- The same command runs in CI on every push and pull request (section 14). Code that only passes locally is not finished.

## 12. Workflow

### 12.1 Vertical slices

Each RF is built as a **vertical slice**: database → backend → API contract → frontend → tests, working end to end before it is considered done. Never build "all the backend" or "all the frontend" first.

Steps of a slice:

1. **Plan.** Read the RF's SRS section, use case and traceability row. Present a short plan (classes per layer, endpoints, tables, triggers involved, events, screen) and wait for approval.
2. **Contract first.** Add the RF's endpoints to `docs/05-api/endpoints.md` (method, path, role, request, response, status codes) before writing code. Backend and frontend may then be built in parallel against that contract.
3. **Backend.** `dominio` → `infraestructura` → `aplicacion` → `api`, with unit and integration tests (section 5.6).
4. **Frontend.** Connect the Claude Design screen in `static/pages/<modulo>/` to the real endpoints through `js/api.js`. While an endpoint is not ready, the screen may use mock data in a clearly marked `mock` object, removed before merge.
5. **End-to-end check.** Run the application and verify the main flow of the use case from the browser.
6. **Pull request** to `develop`.

### 12.2 Definition of Done

An RF is done only when all of these are true:

- The main flow and the alternative flows of its use case in `docs/02-requisitos/casos-de-uso/casos-de-uso.md` work end to end (browser → API → database).
- The code compiles; unit and integration tests exist and pass; `ArquitecturaTest` (ArchUnit) passes; service coverage ≥ 70 %.
- CI is green on the pull request.
- Javadoc in Spanish on every public class and method.
- `docs/05-api/endpoints.md` and `docs/02-requisitos/matriz-trazabilidad.md` are updated.
- No mock data, no `TODO` without an issue, no secrets in the code.
- Error cases return the standard `ErrorResponse` format with messages in Spanish.

### 12.3 Walking skeleton

RF-08 is the walking skeleton: registration and login working end to end (login page → API → database → JWT) **and deployed** on the target server as soon as it works, before starting RF-01. From then on, every merged RF is deployed, so deployment is practiced continuously and not left for week 15.

### 12.4 Git and team

- Branches: `main` (stable, deployable), `develop` (integration), `feature/rf-XX-nombre-corto` created from `develop`.
- One feature branch per RF. Merge to `develop` only through a pull request with green CI and one team review.
- Commits in Spanish, conventional format: `feat(publicacion): ...`, `fix(usuario): ...`, `docs: ...`, `test: ...`, `refactor: ...`, `ci: ...`.
- The team may build two RFs in parallel only if they belong to different modules.
- Within a Claude Code session, work on **one RF at a time**. Do not implement anything outside the requested RF and do not create placeholder classes for future RFs.

### 12.5 Order and milestones

Orden sugerido de los slices (por dependencia y luego por valor). Hitos del curso: 30 % en la semana 10, 40 % en la semana 11 y APF3 en la semana 12.

| Orden | Slice | Módulo | RF |
|---|---|---|---|
| 1 | Registro, inicio de sesión con celular, JWT y roles (walking skeleton) | catalogo + usuario | RF-08 |
| 2 | Publicar cosecha | publicacion | RF-01 |
| 3 | Moderación de publicaciones (solo la cola EN_REVISION) | administracion | RF-09 |
| 4 | Precios de referencia | precio | RF-02 |
| 5 | Búsqueda y filtros | publicacion | RF-03 |
| 6 | Mensajería | mensajeria | RF-04 |
| 7 | Acuerdo y seguimiento de la entrega | venta | RF-06, RF-11 |
| 8 | Calificaciones | venta | RF-05 |
| 9 | Notificaciones y alertas de vencimiento | notificacion (+ acciones rápidas en publicacion) | RF-07 |
| 10 | Venta agrupada | ventaagrupada | RF-10 |
| 11 | Reclamos, suspensiones y panel | administracion | RF-09 |

## 13. Documentation structure

```
docs/
├── README.md                  index and source-of-truth order
├── 01-negocio/                lean-canvas.md, project-charter.md
├── 02-requisitos/             SRS-AgroConecta.md, matriz-trazabilidad.md, casos-de-uso/UC-XX-*.md
├── 03-procesos/               to-be.md, AgroConecta_TO-BE.drawio, to-be.png, as-is.md
├── 04-diseno/                 arquitectura-modular.md, base-datos/, ux-ui/
├── 05-api/                    endpoints.md
├── 06-decisiones/             ADR-XXX-*.md (context, decision, alternatives, consequences)
├── 07-calidad/                test plan, security report (Unit 4)
├── 08-operacion/              deployment, monitoring, maintenance plans (Unit 4)
└── academico/                 course deliverables — do not read
```

## 14. Continuous integration

- Workflow file: `.github/workflows/ci.yml` (GitHub Actions). It runs on every push and pull request to `main` and `develop`.
- It starts a MariaDB service container with the `agroconecta_test` database, sets up Java 21 and runs `./mvnw -B verify` (compile, tests, `ArquitecturaTest`, JaCoCo report).
- The JaCoCo report is uploaded as a build artifact.
- A red CI blocks the merge. Fix the cause; never skip or disable tests to make CI pass.
- Do not modify `ci.yml` without explicit approval.