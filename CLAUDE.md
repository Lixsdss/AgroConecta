# CLAUDE.md — AgroConecta (v3.4)

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
- Security: Spring Security + **OAuth2 Resource Server** with JWT signed with HMAC (HS256). Login is **mobile number (`celular`) + password** (BCrypt). Tokens are issued by the `usuario` module with `NimbusJwtEncoder`: `sub` = user ID, claim `roles`. The secret comes from the `JWT_SECRET` environment variable. No hand-written JWT filter and no JJWT.
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

## 5. Modular architecture

One WAR split into **business modules** under `pe.agroconecta`. Every module has the same four layers. The rules are verified with **ArchUnit** in `src/test/java/pe/agroconecta/ArquitecturaTest.java` (one test per rule).

### 5.1 Module map

A module owns **only** the tables and views in its row; no other module maps them.

| Module | Requirements | Tables and views it owns |
|---|---|---|
| `catalogo` | shared catalogs | provincias, distritos, cultivos, variedades, unidades_medida |
| `usuario` | RF-08 | usuarios, agricultores, compradores |
| `publicacion` | RF-01, RF-03, RF-07 (quick actions) | publicaciones, v_publicaciones_disponibles |
| `precio` | RF-02 | precios_mercado, v_precios_vigentes, v_rango_precio_plataforma |
| `mensajeria` | RF-04 | conversaciones, mensajes |
| `venta` | RF-05, RF-06, RF-11 | transacciones, transacciones_historial, calificaciones, v_historial_transacciones, v_reputacion_usuario |
| `ventaagrupada` | RF-10 | propuestas_grupales, v_lotes_agrupados |
| `notificacion` | RF-07 (alerts) | notificaciones |
| `administracion` | RF-09 | revisiones_publicacion, reclamos, suspensiones, v_panel_admin, v_resumen_usuario, v_ventas_mensuales_cultivo, v_conversion_publicacion |

`pe.agroconecta.compartido` is not a business module: it holds `config/` (OpenAPI, CORS), `seguridad/` (Spring Security, JWT, the `VerificadorEstadoUsuario` interface) and `error/` (`ErrorResponse`, `ManejadorGlobalExcepciones`, `ReglaNegocioException`). It has no business logic.

### 5.2 Layers of every module

```
pe.agroconecta.<modulo>/
├── package-info.java       responsibility and requirements of the module
├── api/                    XxxController (@RestController) + XxxRequest / XxxResponse (records with Bean Validation)
├── aplicacion/             XxxService (use cases, business rules, @Transactional) + XxxDto (records it returns)
├── dominio/                JPA entities and enums (views as @Immutable entities)
└── infraestructura/        XxxRepository (Spring Data JPA)
```

Request flow: `api` → `aplicacion` → `infraestructura` → database. Every layer has a `package-info.java` with its responsibility (in `dominio`, the tables it maps).

### 5.3 Rules verified with ArchUnit (`ArquitecturaTest`)

1. A module uses another module **only through that module's `aplicacion` layer**. It never touches the other module's `api`, `dominio` or `infraestructura`.
2. Repositories (`infraestructura`) are used only by the `aplicacion` layer of their own module.
3. Controllers (`api`) depend on `aplicacion`, never directly on `infraestructura`.
4. `compartido` does not depend on any module. It defines interfaces (such as `VerificadorEstadoUsuario`) that modules implement; Spring injects them by type.
5. There are no dependency cycles between modules.

The rules use `allowEmptyShould(true)` so they pass while a layer is still empty. `ArquitecturaTest.MODULOS` must match the module map. Never relax a rule to make it pass: fix the dependency.

### 5.4 Mandatory rules that ArchUnit cannot fully check

- **Entities never cross modules:** an entity references an entity of another module by its id (`Long idAgricultor`), never with `@ManyToOne`. Foreign keys still exist in the database.
- **`api` exposes DTOs (Java records), never entities.** `aplicacion` services used by other modules also return DTOs (records in `aplicacion`), never entities.
- **Each module maps only its own tables** (section 5.1). Views are read-only entities (`@Immutable`). To read another module's data, call its `aplicacion` service.
- Many state changes happen in the database (triggers and `ev_tareas_diarias`), not in Java. Do not create domain events or `@Scheduled` jobs for them. Application events (`ApplicationEventPublisher`) are used only with approval; none exist today.
- Notifications are not events: the module that performs the action calls the `notificacion.aplicacion` service inside the same transaction, using only the types in the `CHECK` of `notificaciones.tipo`: MENSAJE, OFERTA, VENTA, PROPUESTA_CONJUNTA, CALIFICACION, RECLAMO, CUENTA. POR_VENCER is created by `ev_tareas_diarias` and MODERACION by `trg_revisiones_ai`.
- `administracion` reads the views of its row with its own `@Immutable` entities.

### 5.5 Coding rules

- Controllers contain no business logic and never return entities.
- Business rules live only in `aplicacion`. Use `@Transactional` on methods that write.
- DTOs are records. Input is validated with Bean Validation in the `api` `Request` records.
- A service rejects a business rule with `compartido.error.ReglaNegocioException` (422, message in Spanish).
- Every public class and public method has Javadoc in Spanish (RNF-04).
- SOLID; constructor injection; never field injection.
- Guava / Commons Lang3 where they simplify code; Apache POI for Excel in `precio` (import) and `venta` / `administracion` (export).

### 5.6 How to build a vertical slice

For each RF, inside its module (section 5.1):

1. **`dominio`:** entities for the module's tables and enums for their `CHECK` constraints, following section 6.2 (read-only generated columns, `@Enumerated(EnumType.STRING)`, `id_actualizado_por` on every `transacciones` update, references to other modules by id).
2. **`infraestructura`:** one `XxxRepository extends JpaRepository` per entity that needs it; `PESSIMISTIC_WRITE` lock where section 6.2 requires it.
3. **`aplicacion`:** `XxxService` with the use cases, the validations with Spanish messages (the database is the last barrier), the rules of section 6.3 and the `XxxDto` records it returns. For another module's data, use its `aplicacion` service.
4. **`api`:** `XxxController` with `@PreAuthorize`, `XxxRequest` / `XxxResponse` records and the contract in `docs/05-api/endpoints.md`.
5. **Tests:** service unit tests (Mockito), controller tests (MockMvc) and integration tests against MariaDB (`agroconecta_test`, `test` profile) for anything that depends on triggers; `ArquitecturaTest` must pass.
6. **Screen:** `src/main/resources/static/pages/<modulo>/` + `static/js/<modulo>/`, connected to the real endpoints through `js/api.js` (section 10).

## 6. Database (data model v4.1)

- The schema is defined by SQL, not by JPA: `spring.jpa.hibernate.ddl-auto=validate`. `docs/04-diseno/base-datos/modelo-bd.sql` (v4.1) is the single source of truth; `cambios-v4.md` explains the changes from v3 and v4, `modelo-logico.md` has one ER diagram per module and `notas-integridad-bd.md` lists what the database does not guarantee.
- Migrations live in `src/main/resources/db/migration/`:
  - `V1__esquema_inicial.sql` = sections 2–11 of `modelo-bd.sql` (no `DROP/CREATE DATABASE` / `USE`). It starts with `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;`: triggers and routines keep the collation of the session that created them.
  - `V2__datos_iniciales.sql` = section 12 of `modelo-bd.sql` (provinces, the 65 districts of Piura, units, crops, varieties). The first ADMINISTRADOR is created by the application, never by SQL.
  - Flyway (`flyway-mysql`) runs the `DELIMITER $$` blocks of V1 as they are.
- Schema changes: new migration `V3__...` onward AND update `modelo-bd.sql`. Only with explicit approval. Never edit an applied migration.
- Tables are in plural Spanish. v4.1 has **20 tables**, 36 foreign keys, **10 views**, 21 triggers, 1 function (`fn_rol_usuario`), 1 procedure (`sp_sincronizar_publicacion`) and 1 daily event (`ev_tareas_diarias`, 06:00).
- Roles, states, reasons, sources, moderation decisions and notification types are fixed lists enforced with `CHECK` constraints, not catalog tables. Only `provincias`, `distritos`, `unidades_medida`, `cultivos` and `variedades` are catalog tables.
- The database user needs privileges for CREATE, ALTER, INDEX, REFERENCES, TRIGGER, EVENT, CREATE VIEW, CREATE ROUTINE and ALTER ROUTINE (`fn_rol_usuario`, `sp_sincronizar_publicacion`). The server needs `event_scheduler=ON`; it is a server setting, not part of the migrations. Both are configured outside the migrations.
- Local MariaDB 11+ with MySQL Connector/J: JDBC metadata fails (`INFORMATION_SCHEMA.KEYWORDS` has no `RESERVED` column), so the `dev` and `test` profiles set `spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect`. `prod` (MySQL) detects the dialect.
- `spring.jpa.open-in-view=false`.
- **Database safety:** connect only through the application as `agroconecta_app`, only to `agroconecta` and `agroconecta_test`. Never use sudo, root or another MariaDB user, and never run DROP, CREATE or GRANT. If a database must be dropped or recreated, stop and give the user the exact command.

### 6.1 Business rules enforced by triggers

Before writing service logic, read the triggers of the tables involved. The database already enforces these rules and performs these side effects:

| Trigger / object | Rule or side effect |
|---|---|
| `trg_agricultores_bi`, `trg_compradores_bi` | The subtype row matches `usuarios.rol` |
| `trg_publicaciones_bi` | Every listing starts **PUBLICADA** (post-moderation); fixed units (kilo, quintal, tonelada) take `peso_unidad_kg` from the catalog, variable units (jaba, saco) require it; if `fecha_limite` is NULL it is `fecha_cosecha + cultivos.vida_util_dias`; `fecha_limite` ≥ today; the first listing of a farmer gets `marca_revision = PRIMERA_PUBLICACION` (moderation queue) |
| `trg_publicaciones_bu` | Allowed transitions: PUBLICADA → BLOQUEADA/VENDIDA/VENCIDA/RETIRADA, BLOQUEADA → PUBLICADA/VENCIDA/RETIRADA, VENDIDA → PUBLICADA/VENCIDA, VENCIDA → PUBLICADA/RETIRADA; "Extender" a VENCIDA listing returns it to PUBLICADA; a farmer's correction of a BLOQUEADA listing sets `marca_revision = CORREGIDA`; quantity cannot drop below what is already agreed; the farmer never changes |
| `trg_revisiones_bi`, `trg_revisiones_ai` | Only an ADMINISTRADOR decides; CONFORME, ADVERTENCIA and BLOQUEO only on a PUBLICADA listing, DESBLOQUEO only on a BLOQUEADA one; the decision sets BLOQUEADA or PUBLICADA/VENCIDA, clears `marca_revision` and creates the MODERACION notification (except CONFORME) |
| `trg_precios_mercado_bi`, `trg_precios_mercado_bu` | Only an ADMINISTRADOR loads or edits prices, only for districts with `es_zona_precio` |
| `trg_conversaciones_bi`, `trg_mensajes_bi` | Only PUBLICADA listings are contacted; the sender takes part in the conversation; OFERTA messages only while the listing is PUBLICADA (text messages always) |
| `trg_transacciones_bi` | A sale starts ACORDADA (chat agreement) or PROPUESTA (only with `id_propuesta_grupal`); the listing is PUBLICADA; `id_actualizado_por` is one of the parties; weight of the agreed unit; quantity in kg ≤ available; coherence with the group proposal |
| `trg_transacciones_bu` | Finished sales and terms (unit, quantity, weight, price) never change; allowed transitions per user: the farmer accepts/rejects a PROPUESTA, the buyer cancels it; ACORDADA → RECOGIDO/CANCELADA and RECOGIDO → EN_CAMINO by either party; ACORDADA → ENTREGADO (farm pick-up), EN_CAMINO → ENTREGADO and ENTREGADO → COMPLETADA only by the buyer; only ACORDADA can be cancelled; `id_actualizado_por = NULL` only for the automatic cancellation of expired proposals |
| `trg_transacciones_ai`, `trg_transacciones_au` | Insert the row in `transacciones_historial` (timeline) and call `sp_sincronizar_publicacion` (listing VENDIDA when fully agreed, back to PUBLICADA/VENCIDA when an agreement is cancelled) |
| `trg_calificaciones_bi` | Only COMPLETADA sales, only by one of the parties (one per evaluator: unique key) |
| `trg_reclamos_bi`, `trg_reclamos_bu` | Only on sales that reached ACORDADA (checked in `transacciones_historial`), only by a party, starts PENDIENTE; only an ADMINISTRADOR resolves it, once; sets `fecha_resolucion` |
| `trg_suspensiones_*` | Only an ADMINISTRADOR suspends, never another administrator; one open suspension per user; `RECLAMOS_FUNDADOS` requires ≥ 3 founded complaints; insert sets `usuarios.estado = SUSPENDIDO`, setting `fecha_fin` sets it back to ACTIVO |
| `ev_tareas_diarias` | POR_VENCER notifications 3 days and 1 day before `fecha_limite`; PUBLICADA → VENCIDA after the deadline; cancels group PROPUESTA sales whose `fecha_recojo` has passed and closes their proposals |

Never reimplement these side effects in Java and never use `@Scheduled` for the daily tasks.

### 6.2 Rules for the vertical slices (JPA and services)

- Generated columns (`cantidad_kg`, `precio_kg`, `monto_total`) are mapped with `@Column(insertable = false, updatable = false)`. Refresh the entity after saving when their value or a trigger-set value (`peso_unidad_kg`, `fecha_limite`, `estado`, `marca_revision`) is needed.
- Roles, states and reasons are `CHECK`-constrained strings: map them as Java enums with `@Enumerated(EnumType.STRING)` **plus `@JdbcTypeCode(SqlTypes.VARCHAR)`** (otherwise Hibernate expects a native `enum` column and `validate` fails), with exactly the values of the `CHECK`. `CHAR(n)` columns (`celular`, `ruc`, `ubigeo`) use `@JdbcTypeCode(SqlTypes.CHAR)`.
- Integration tests that change rows with `JdbcTemplate` inside a `@Transactional` test must call `EntityManager.flush()` before and `clear()` after, because the test shares the persistence context with the services.
- Every update to `transacciones` must set `id_actualizado_por` to the user making the change; the triggers use it for the timeline and for permissions.
- "Confirmar que recibí" performs two updates (ENTREGADO, then COMPLETADA) in one `@Transactional` method, from EN_CAMINO or from ACORDADA (farm pick-up).
- The application never inserts into `transacciones_historial`; the triggers do.
- Services also validate business rules to return friendly messages in Spanish; the database is the last barrier (its errors are mapped as described in section 8).
- Lock the listing with `PESSIMISTIC_WRITE` before a sale becomes ACORDADA (chat agreement or accepted group proposal) (RNF-02).
- The `publicacion` module never changes the state of a BLOQUEADA listing (only its data); unblocking always goes through `revisiones_publicacion` (DESBLOQUEO).

### 6.3 Business rules the services must implement (not in the database)

- **Moderation flags (RF-01, RF-09), thresholds approved in slice 3:** when a listing is created, the `publicacion` service sets `marca_revision`, with priority PRECIO_FUERA_RANGO > CANTIDAD_INUSUAL > PRIMERA_PUBLICACION (the trigger sets the last one only if there is no other flag):
  - `CANTIDAD_INUSUAL` (implemented): the listing's kilos exceed **3 ×** the average kilos of listings of the same crop in the same district in the last **90 days**, with at least **3** listings to compare.
  - `PRECIO_FUERA_RANGO` (slice 4, needs `precio`): the price per kilo differs by more than **±15 %** from today's market price per kilo (`v_precios_vigentes`) of the listing's district if it is a price zone, otherwise the average of the 4 zones. No reference price → no flag.
- **Moderation decisions (RF-09):** CONFORME only for listings in the queue (`marca_revision` not NULL); ADVERTENCIA and BLOQUEO for any PUBLICADA listing; DESBLOQUEO only for BLOQUEADA. Cross-module DTOs carry states and flags as `String` (never another module's `dominio` enums).
- **Proportional split (RF-10):** the `ventaagrupada` service splits the joint proposal's kilos among the farmers of the lot in proportion to each farmer's available kilos, rounded down to whole units of each farmer's listing, and creates one PROPUESTA sale per farmer through `venta.aplicacion`, in the farmer's unit. A farmer whose share is less than one unit receives no proposal.
- **Offers (RF-04):** reject OFERTA messages on a listing that is not PUBLICADA before inserting (friendly 422).
- **Cancellation (RF-06):** only an ACORDADA sale can be cancelled; after pick-up, point the user to a complaint (`reclamos`).

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

- RF-01: publishing takes max. 3 steps (variety → crop, **unit** (kilo, jaba, saco, quintal, tonelada) and quantity, with the kilos per jaba/saco; price per unit, with the price per kilo calculated, and district; harvest date and optional deadline). Units come from `unidades_medida` (seed data in V2); never hardcode them. Selling by area is out of scope. If no deadline is given, the trigger calculates it from the crop's shelf life. Listings are born **PUBLICADA** (post-moderation).
- RF-02: reference prices are per kilo, per price zone (Piura, Tambogrande, Chulucanas, Sullana), with source and administrator; the board shows them in the crop's local unit with a trend (`v_precios_vigentes`). The range shown to the farmer comes from completed platform sales in the last 30 days (`v_rango_precio_plataforma`). The admin uploads by form or CSV/Excel.
- RF-03: searches PUBLICADA listings with available kilos; the "Categoría" filter is the crop; filters include the unit; sort by price per kilo, harvest date or rating.
- RF-04: one conversation per listing and buyer, never closed (used to coordinate delivery); structured OFERTA messages only while the listing is PUBLICADA; the WhatsApp button increments `clics_whatsapp`.
- RF-05: rating only for COMPLETADA sales, once per evaluator.
- RF-06: parties negotiate with offers; either party registers the agreement directly as ACORDADA (the agreed unit may differ from the listing's); either party can cancel while ACORDADA. PROPUESTA exists only in group sales (RF-10). After pick-up there is no cancellation: problems go through a complaint.
- RF-07: POR_VENCER notifications 3 days and 1 day before the deadline are created by `ev_tareas_diarias`; quick actions (lower price, extend, withdraw) live in `publicacion`; extending moves VENCIDA back to PUBLICADA.
- RF-08: the 9-digit mobile number starting with 9 (`celular`) is the login identifier; there is no email; district is mandatory; buyers register business name and type; the role never changes after registration.
- RF-09: post-moderation queue (`marca_revision` not NULL); decisions CONFORME, ADVERTENCIA, BLOQUEO, DESBLOQUEO (warning and blocking need a catalog reason and a message); a corrected blocked listing returns to the queue as CORREGIDA. Complaints only on sales that reached ACORDADA, resolved by the admin. Justified suspensions; RECLAMOS_FUNDADOS needs ≥ 3 founded complaints. Metrics come from views.
- RF-10: the system groups PUBLICADA listings by crop, variety and district (≥ 2 farmers, `v_lotes_agrupados`); the **buyer** sends a joint proposal (kg, price per kg, pick-up date); each farmer receives a PROPUESTA in their own unit and accepts or rejects it; proportional split (section 6.3); pending proposals are cancelled by `ev_tareas_diarias` when the pick-up date passes.
- RF-11: no carrier or plate. ACORDADA → RECOGIDO → EN_CAMINO → ENTREGADO → COMPLETADA, or ACORDADA → ENTREGADO → COMPLETADA (farm pick-up). Only the buyer confirms reception ("Confirmar que recibí": ENTREGADO and COMPLETADA in one transaction). The timeline is `transacciones_historial`, recorded by triggers; it is never edited, deleted or "corrected".
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
- Formats: agricultor, comprador and public screens are **mobile** (design 360×800, `.ac-pantalla` max 480 px); the 5 administrador screens are **desktop** (design 1440×900).
- Icons: Lucide, served from the local sprite `static/img/iconos.svg` (use `js/comun/iconos.js`); fonts Work Sans and Cormorant Garamond from `static/vendor/fuentes/`. No CDN. Details in `docs/04-diseno/ux-ui/guia-estilos.md`.
- The `templates/` folder is not used (no server-side rendering).

## 11. Testing

- Unit tests for services with Mockito; controller tests with MockMvc; integration tests against MariaDB (`agroconecta_test`) for anything that depends on triggers; architecture tests in `ArquitecturaTest` (ArchUnit).
- Test packages mirror the modules: `src/test/java/pe/agroconecta/<modulo>/`.
- Every RF is finished only when its tests pass. Run `./mvnw test` before finishing any task.
- Minimum coverage: 70 % of lines in every `pe.agroconecta.*.aplicacion` package. The JaCoCo `check` goal enforces it in `./mvnw verify` and fails the build below it; packages without classes are not evaluated.
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

Order by dependency first, then value. Course milestones map to slices:

| Order | Slice | Module | RF | Milestone |
|---|---|---|---|---|
| 1 | Registration, login with `celular`, JWT and roles (walking skeleton) | catalogo + usuario | RF-08 | Week 10 (30 %) |
| 2 | Publish harvest | publicacion | RF-01 | Week 10 (30 %) |
| 3 | Post-moderation queue only | administracion | RF-09 | Week 10 (30 %) |
| 4 | Reference prices | precio | RF-02 | Week 11 (40 %) |
| 5 | Search and filters | publicacion | RF-03 | Week 11 (40 %) |
| 6 | Messaging and offers | mensajeria | RF-04 | Week 12 (APF3) |
| 7 | Agreement and delivery tracking | venta | RF-06, RF-11 | Week 12 (APF3) |
| 8 | Ratings | venta | RF-05 | — |
| 9 | Notifications and expiry alerts | notificacion (+ quick actions in publicacion) | RF-07 | — |
| 10 | Group sale | ventaagrupada | RF-10 | — |
| 11 | Complaints, suspensions and dashboard | administracion | RF-09 | — |

- **Week 10 (30 %):** slices 1 to 3, and the demo must show the top Java libraries in use: Guava, Apache Commons, Apache POI and Logback.
- **Week 11 (40 %):** slices 4 and 5, integrated with Git and GitHub (branches, pull requests, CI).
- **Week 12 (APF3):** slices 6 and 7.

## 13. Documentation structure

```
docs/
├── README.md                  index and source-of-truth order
├── 01-negocio/                lean-canvas.md, project-charter.md
├── 02-requisitos/             SRS-AgroConecta.md, matriz-trazabilidad.md, casos-de-uso/casos-de-uso.md
├── 03-procesos/               to-be.md, AgroConecta_TO-BE.drawio, to-be.png, as-is.md
├── 04-diseno/                 base-datos/ (modelo-bd.sql, cambios-v4.md, modelo-logico.md, notas-integridad-bd.md), ux-ui/
├── 05-api/                    endpoints.md
├── 06-decisiones/             ADR-XXX-*.md (context, decision, alternatives, consequences)
├── 07-calidad/                test plan, security report (Unit 4)
├── 08-operacion/              deployment, monitoring, maintenance plans (Unit 4)
└── academico/                 course deliverables — do not read
```

## 14. Continuous integration

- Workflow file: `.github/workflows/ci.yml` (GitHub Actions). It runs on every push and pull request to `main` and `develop`.
- It starts a MariaDB service container (`mariadb:11.8`) with the `agroconecta_test` database and the `agroconecta_app` user, sets up Java 21 (Temurin) with the Maven cache and runs `./mvnw -B clean verify` (compile, tests against the real database, Flyway, `ArquitecturaTest`, JaCoCo report and check). No H2.
- Required GitHub Actions repository secrets: `DB_PASSWORD` (password of `agroconecta_app` in the CI container) and `JWT_SECRET` (HS256 key, at least 32 bytes). Pull requests from forks do not receive secrets.
- The JaCoCo report is uploaded as the `informe-jacoco` artifact; test reports are uploaded when the build fails.
- A red CI blocks the merge. Fix the cause; never skip or disable tests to make CI pass.
- Do not modify `ci.yml` without explicit approval.