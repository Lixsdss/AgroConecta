# CLAUDE.md — AgroConecta (v3.1)

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
- Conventional technical suffixes stay in English: `Controller`, `Service`, `ServiceImpl`, `Dao`, `Mapper`, `Request`, `Response`, `Dto`, `Event`, `Config`.

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
- Spring Web (REST), Spring Data JPA (Hibernate), Validation, Actuator, Flyway (+ `flyway-mysql`), Spring Modulith.
- Security: Spring Security + **OAuth2 Resource Server** with JWT signed with HMAC (HS256). Login is **phone number + password** (BCrypt). Tokens are issued by the `usuarios` module with `NimbusJwtEncoder`: `sub` = user ID, claim `roles`. The secret comes from the `JWT_SECRET` environment variable. No hand-written JWT filter and no JJWT.
- On every authenticated request, the user must still be ACTIVO (SRS §9.1). Implement it as a JWT validator in `compartido.security` that calls an interface `VerificadorEstadoUsuario` defined in `compartido`; the `usuarios` module implements it (primary-key lookup, no cache unless load tests require it). A SUSPENDIDO or INACTIVO user gets 401.
- Libraries required by the course: Apache POI (`poi-ooxml`), Google Guava, Apache Commons Lang3, Logback (included by default).
- springdoc-openapi (Swagger UI), version compatible with Spring Boot 4.1.
- Tests: JUnit 5, Mockito, `spring-modulith-starter-test`, JaCoCo plugin.
- Database: MySQL in production, MariaDB in local development (Arch Linux). JDBC driver: MySQL Connector/J.
- Frontend: static HTML5 + CSS3 + vanilla JavaScript + Bootstrap 5, served from `src/main/resources/static/`. No React, no Thymeleaf, no Node build step.
- Do not add: Lombok, H2, Testcontainers, Thymeleaf, JJWT, `spring-modulith-starter-jpa`, payment libraries.

### 4.1 Zero-cost rule

- Only free and open-source software. No paid services, paid plans, commercial API keys or dependencies that require them.
- No external infrastructure services (no Redis, message brokers, e-mail/SMS providers, cloud storage). If something needs a cache, use in-memory Spring Cache.
- Deployment target and external monitoring are chosen among free options and recorded in an ADR before week 15.
- If a task seems to require something that costs money, stop and ask.

## 5. Architecture: modular monolith

One deployable WAR, split into **business modules** under `pe.agroconecta`. Each module has its own internal layers (MVC + DAO). Spring Modulith verifies the boundaries in `ArquitecturaModularTest`.

### 5.1 Modules

| Module | RF | Responsibility | Allowed dependencies |
|---|---|---|---|
| `compartido` | — | Config, security (incl. `VerificadorEstadoUsuario` interface), global exception handling, utils. OPEN module, no business logic | none |
| `catalogo` | Support for RF-01, RF-02, RF-03, RF-10 | Read-only catalogs: categorias, cultivos, variedades, unidades_medida, provincias, distritos | compartido |
| `usuarios` | RF-08 | Registration, phone login, JWT issuing, profile, own password change, roles; implements `VerificadorEstadoUsuario` | compartido, catalogo::api |
| `publicaciones` | RF-01, RF-03, RF-07 (quick actions) | Harvest listing in max. 3 steps, public multi-criteria search, lower price / extend deadline / withdraw | compartido, catalogo::api, usuarios::api |
| `precios` | RF-02 | Public price board with history; admin upload via form or CSV/Excel | compartido, catalogo::api |
| `mensajeria` | RF-04 | Internal chat per listing and buyer; WhatsApp lead | compartido, usuarios::api, publicaciones::api, notificaciones::api |
| `transacciones` | RF-06, RF-11 | Propose, accept, reject, cancel, confirm reception; history + Excel export; deliveries and delivery events | compartido, usuarios::api, publicaciones::api, notificaciones::api |
| `calificaciones` | RF-05 | 1–5 stars + comment, only for COMPLETADA transactions; reputation | compartido, usuarios::api, transacciones::api, notificaciones::api |
| `ventaagrupada` | RF-10 | Buyer creates a group proposal; farmers contribute (each contribution is a transaction created through `transacciones::api`); consolidated offer view | compartido, catalogo::api, usuarios::api, publicaciones::api, transacciones::api, notificaciones::api |
| `notificaciones` | RF-07 + in-app notifications of other RFs | Lists notifications and marks them as read. Its `api` exposes a service that other modules call to create notifications (MENSAJE, PROPUESTA, TRANSACCION, ENTREGA, CALIFICACION, MODERACION). VENCIMIENTO notifications are created by `ev_tareas_diarias` | compartido, usuarios::api |
| `administracion` | RF-09 | User management, moderation, metrics, Excel export | compartido, catalogo::api, usuarios::api, publicaciones::api, precios::api, transacciones::api, calificaciones::api, notificaciones::api |

### 5.2 Internal structure of every business module

```
pe.agroconecta.<modulo>/
├── package-info.java   @ApplicationModule(displayName, allowedDependencies)
├── api/                PUBLIC  → XxxService (interface), XxxDto (records), XxxEvent (records)
│                                 package-info.java with @NamedInterface("api")
├── web/                internal → XxxController (@RestController)
│   └── dto/            internal → XxxRequest / XxxResponse (records + Bean Validation)
├── service/            internal → XxxServiceImpl implements api.XxxService
├── dao/                internal → XxxDao extends JpaRepository
├── model/              internal → JPA entities and enums
└── mapper/             internal → XxxMapper (manual, entity ↔ DTO)
```

`compartido` has: `config/`, `security/`, `exception/`, `util/`.

Request flow: `web` → `api` interface → `service` → `dao` → database.

### 5.3 Decoupling rules (mandatory)

1. Other modules may only import a module's `api` package. Never `web`, `service`, `dao`, `model` or `mapper` of another module.
2. No JPA relationships to entities of another module. Reference them by ID (`Long agricultorId`, not `@ManyToOne Usuario`). Foreign keys still exist in the database.
3. Cross-module reads go through the other module's `api` service and return DTOs, never entities.
4. Many state changes happen in the database (triggers and `ev_tareas_diarias`), not in Java, so Java code cannot observe them as events. Do **not** create domain events or `@Scheduled` jobs for them; modules read the current state through the owner module's `api`. Application events (`ApplicationEventPublisher` + `@ApplicationModuleListener`, records in the publisher's `api` package) are allowed only for side effects initiated in Java, and only with approval. None are defined at the moment. Notifications are **not** events: the module that performs the action calls `notificaciones::api` inside the same transaction, using only the notification types defined in `modelo-bd.sql`.
5. `compartido` never imports business modules. It defines interfaces (such as `VerificadorEstadoUsuario`) that business modules implement; Spring injects them by type.
6. `administracion` reports read the SQL views with its own read-only DAOs.
7. `ArquitecturaModularTest` must always pass. Never relax `allowedDependencies` to make it pass — fix the dependency instead.

### 5.4 Coding rules

- Controllers never contain business logic and never return entities.
- Business rules live only in `service/`. Use `@Transactional` on service methods that write.
- Service implementations are package-private when possible; expose only the `api` interface.
- DTOs are Java records. Validate input with Bean Validation in `web/dto`.
- Every public class and public method has Javadoc in Spanish (RNF-04).
- Apply SOLID; prefer constructor injection; no field injection.
- Use Guava / Commons Lang3 where they simplify code; Apache POI for Excel import in `precios`.

## 6. Database

- The schema is defined by SQL, not by JPA: `spring.jpa.hibernate.ddl-auto=validate`.
- Migrations live in `src/main/resources/db/migration/`:
  - `V1__esquema_inicial.sql` = sections 2–11 of `modelo-bd.sql` (no `CREATE DATABASE` / `USE`).
  - `V2__datos_iniciales.sql` = section 12 of `modelo-bd.sql` (roles, catalogs, units).
- Schema changes: new migration `V3__...` onward AND update `modelo-bd.sql`. Only with explicit approval. Never edit an applied migration.
- Tables are in plural Spanish. The schema has 24 tables, 15 triggers, 7 views and 1 daily event (`ev_tareas_diarias`). Before writing service logic, check which triggers already enforce a rule — do not duplicate it in Java.
- The database user needs privileges for CREATE, ALTER, INDEX, REFERENCES, TRIGGER, EVENT, CREATE VIEW, CREATE ROUTINE and ALTER ROUTINE (function `fn_rol_usuario`). The server needs `event_scheduler=ON`. Both are configured outside the migrations.
- Alerts, expiry and group-proposal closing are done by `ev_tareas_diarias`. Never reimplement them with `@Scheduled`.
- Lock the listing with `PESSIMISTIC_WRITE` when a transaction is accepted (RNF-02).
- `spring.jpa.open-in-view=false`.

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
- Errors (handled in `compartido.exception.GlobalExceptionHandler`) use one JSON format: `{ timestamp, estado, error, mensaje, ruta, errores[] }`. `errores` holds field details only for 400; otherwise it is an empty list.
- Status mapping (SRS §9.2; check the engine error code before the SQLSTATE):
  - 400: Bean Validation.
  - 422: trigger error (SQLSTATE 45000, return the trigger message), CHECK violation (3819 MySQL / 4025 MariaDB) or service business rule.
  - 409: duplicate key or foreign key (1062, 1451, 1452).
  - 401: missing/invalid token or user not ACTIVO. 403: wrong role or not the owner. 404: not found.
- Public endpoints: `POST /api/v1/auth/registro`, `POST /api/v1/auth/login`, `GET /api/v1/catalogo/**`, `GET /api/v1/publicaciones/**`, `GET /api/v1/precios/**`, static files, `/actuator/health` (the only exposed Actuator endpoint), Swagger UI. Everything else requires JWT.
- Roles in the token claim `roles`: `AGRICULTOR`, `COMPRADOR`, `ADMINISTRADOR`. Use `@PreAuthorize` in controllers.
- Keep `docs/05-api/endpoints.md` updated when adding endpoints.

## 9. Key business rules (full list in the SRS)

- RF-01: publishing takes max. 3 steps (variety → crop, quantity and **unit**; price and district; availability date and deadline). Units come from `unidades_medida` (seed data in V2); never hardcode them. Selling by area is out of scope. If no deadline is given, the trigger calculates it.
- RF-02: public price board; the admin uploads by form or CSV/Excel.
- RF-04: one conversation per listing and buyer; the WhatsApp button records a lead.
- RF-05: rating only for COMPLETADA transactions, once per evaluator.
- RF-06: either party proposes; the other party accepts or rejects; either party cancels; without delivery, the buyer confirms reception → COMPLETADA.
- RF-07: notifications at 3 days and 1 day before the deadline are created by `ev_tareas_diarias`; quick actions live in `publicaciones`.
- RF-08: phone is the login identifier; email is optional; the role never changes after registration.
- RF-10: the **buyer** creates the group proposal; farmers contribute.
- RF-11: RECOGIDO → EN_CAMINO → ENTREGADO; only the buyer confirms reception, and the trigger sets COMPLETADA. Delivery events are never edited, deleted or "corrected".
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

- Unit tests for services with Mockito; module tests with `@ApplicationModuleTest`; controller tests with MockMvc.
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
3. **Backend.** `model` → `dao` → `api` interface and DTOs → `service` → `web` controller, with unit and module tests.
4. **Frontend.** Connect the Claude Design screen in `static/pages/<modulo>/` to the real endpoints through `js/api.js`. While an endpoint is not ready, the screen may use mock data in a clearly marked `mock` object, removed before merge.
5. **End-to-end check.** Run the application and verify the main flow of the use case from the browser.
6. **Pull request** to `develop`.

### 12.2 Definition of Done

An RF is done only when all of these are true:

- The main flow and the alternative flows of its use case work end to end (browser → API → database).
- Unit and module tests exist and pass; `ArquitecturaModularTest` passes; service coverage ≥ 70 %.
- CI is green on the pull request.
- Javadoc in Spanish on every public class and method.
- `docs/05-api/endpoints.md` and `docs/02-requisitos/matriz-trazabilidad.md` are updated.
- No mock data, no `TODO` without an issue, no secrets in the code.
- Error cases return the standard `ApiError` format with messages in Spanish.

### 12.3 Walking skeleton

RF-08 is the walking skeleton: registration and login working end to end (login page → API → database → JWT) **and deployed** on the target server as soon as it works, before starting RF-01. From then on, every merged RF is deployed, so deployment is practiced continuously and not left for week 15.

### 12.4 Git and team

- Branches: `main` (stable, deployable), `develop` (integration), `feature/rf-XX-nombre-corto` created from `develop`.
- One feature branch per RF. Merge to `develop` only through a pull request with green CI and one team review.
- Commits in Spanish, conventional format: `feat(publicaciones): ...`, `fix(usuarios): ...`, `docs: ...`, `test: ...`, `refactor: ...`, `ci: ...`.
- The team may build two RFs in parallel only if they belong to different modules.
- Within a Claude Code session, work on **one RF at a time**. Do not implement anything outside the requested RF and do not create placeholder classes for future RFs.

### 12.5 Order and milestones

Order by dependency first, then value (same as SRS §11):

| Order | RF | Module | Milestone |
|---|---|---|---|
| 1 | RF-08 (walking skeleton) | usuarios | — |
| 2 | RF-01 (+ catalog read endpoints) | publicaciones, catalogo | 30 % (week 10) |
| 3 | RF-03 | publicaciones | 30 % (week 10) |
| 4 | RF-02 | precios | 40 % (week 11) |
| 5 | RF-04 | mensajeria | 40 % (week 11) |
| 6 | RF-06 | transacciones | 60 % (week 13) |
| 7 | RF-11 | transacciones | 60 % (week 13) |
| 8 | RF-05 | calificaciones | 70 % (week 14) |
| 9 | RF-07 | notificaciones, publicaciones | 70 % (week 14) |
| 10 | RF-10 | ventaagrupada | 80 % (week 15) |
| 11 | RF-09 | administracion | 90 % (week 16) |

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
- It starts a MariaDB service container with the `agroconecta_test` database, sets up Java 21 and runs `./mvnw -B verify` (compile, tests, `ArquitecturaModularTest`, JaCoCo report).
- The JaCoCo report is uploaded as a build artifact.
- A red CI blocks the merge. Fix the cause; never skip or disable tests to make CI pass.
- Do not modify `ci.yml` without explicit approval.