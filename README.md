# AgroConecta

Plataforma web que conecta directamente a **pequeños agricultores de Piura (Perú)** con compradores (mercados mayoristas, restaurantes, hoteles y agroindustrias) para reducir la dependencia de intermediarios. El agricultor publica su cosecha en tres pasos con un precio de referencia, negocia por chat con ofertas, registra el acuerdo y sigue la entrega hasta que el comprador la confirma. La plataforma **no cobra ni procesa pagos**: el pago se hace entre las partes, fuera del sistema (ADR-002).

Proyecto integrador del curso *Integrador I: Sistemas – Software* (100000I58N), UTP, agosto–noviembre de 2026.

## Actores

| Actor | Qué hace |
|---|---|
| **Agricultor** | Publica cosechas (RF-01), ve precios de referencia (RF-02), conversa y acuerda ventas (RF-04, RF-06), sigue la entrega (RF-11), recibe avisos de vencimiento con acciones rápidas (RF-07) y responde a propuestas de venta agrupada (RF-10). |
| **Comprador** | Busca y filtra cosechas (RF-03), envía ofertas por chat o WhatsApp (RF-04), registra acuerdos (RF-06), confirma la recepción (RF-11), califica (RF-05) y envía propuestas conjuntas a lotes de varios agricultores (RF-10). |
| **Administrador** | Modera publicaciones, carga precios de mercado, resuelve reclamos, suspende cuentas y revisa el panel con métricas (RF-02, RF-09). |

Las cooperativas y los transportistas no son usuarios del sistema.

## Tecnologías

| Capa | Tecnología (versión) |
|---|---|
| Lenguaje y build | Java 21, Maven Wrapper (`./mvnw`), empaquetado **WAR** para Tomcat |
| Framework | Spring Boot 4.1.1 (Spring Framework 7.0.9): Web MVC, Data JPA (Hibernate 7.4.5), Validation, Actuator |
| Seguridad | Spring Security 7.1.1 + OAuth2 Resource Server, JWT HS256 (`NimbusJwtEncoder`/`NimbusJwtDecoder`), BCrypt |
| Base de datos | MySQL en producción, MariaDB 11 en desarrollo; Flyway 12.4.0 (`flyway-mysql`); MySQL Connector/J 9.7.0 |
| Librerías del curso | Google Guava 33.7.1-jre, Apache Commons Lang3 3.20.0, Apache POI (`poi-ooxml`) 5.5.1, Logback 1.5.38 (SLF4J 2.0.18) |
| Documentación de la API | springdoc-openapi 3.1.1 (Swagger UI) |
| Pruebas | JUnit 6.0.3 (Jupiter), Mockito 5.23.0, Spring MockMvc, ArchUnit 1.5.1, JaCoCo 0.8.15 |
| Frontend | HTML5 + CSS3 + JavaScript sin frameworks + Bootstrap 5.3.8 (servido localmente), íconos Lucide en un sprite SVG local |

Todo es software libre y gratuito: el proyecto no genera costos (CLAUDE.md §4.1).

## Arquitectura

Monolito modular (ADR-001): un único WAR dividido en **módulos de negocio** bajo `pe.agroconecta`, cada uno con las mismas cuatro capas.

```
pe.agroconecta.<modulo>/
├── api/              controladores REST + Request/Response (records con Bean Validation)
├── aplicacion/       servicios con los casos de uso y las reglas de negocio + DTO (records)
├── dominio/          entidades JPA y enums (las vistas, como entidades @Immutable)
└── infraestructura/  repositorios Spring Data JPA
```

| Módulo | Requisitos |
|---|---|
| `catalogo` | Catálogos compartidos (provincias, distritos, cultivos, variedades, unidades) |
| `usuario` | RF-08 registro, inicio de sesión con celular, perfil |
| `publicacion` | RF-01 publicar, RF-03 buscar, RF-07 acciones rápidas |
| `precio` | RF-02 precios de referencia (formulario e importación Excel/CSV) |
| `mensajeria` | RF-04 conversaciones y ofertas |
| `venta` | RF-05 calificaciones, RF-06 acuerdos e historial, RF-11 entrega |
| `ventaagrupada` | RF-10 lotes y propuestas conjuntas |
| `notificacion` | RF-07 centro de avisos |
| `administracion` | RF-09 moderación, reclamos, suspensiones y panel |

`pe.agroconecta.compartido` contiene la configuración de seguridad, OpenAPI y CORS, y el manejo unificado de errores; no tiene lógica de negocio. Un módulo usa otro **solo a través de su capa `aplicacion`**, y esas reglas las verifica ArchUnit en `src/test/java/pe/agroconecta/ArquitecturaTest.java`.

Muchas reglas de negocio viven en la base de datos (21 triggers, vistas, un procedimiento y el evento diario `ev_tareas_diarias`), definida por las migraciones de Flyway en `src/main/resources/db/migration/`. Hibernate solo valida el esquema (`ddl-auto=validate`).

Diagramas: [`docs/04-diseno/arquitectura/`](docs/04-diseno/arquitectura/).

## Requisitos previos

- **JDK 21**. No hace falta instalar Maven: se usa `./mvnw`.
- **MariaDB 11+** (desarrollo) o **MySQL 8+** (producción) con:
  - las bases `agroconecta` (desarrollo) y `agroconecta_test` (pruebas);
  - el usuario `agroconecta_app` con privilegios CREATE, ALTER, INDEX, REFERENCES, TRIGGER, EVENT, CREATE VIEW, CREATE ROUTINE y ALTER ROUTINE sobre ambas bases;
  - `event_scheduler=ON` en el servidor (lo usa `ev_tareas_diarias`).

  Estas tareas las hace el administrador de la base de datos; no forman parte de las migraciones.

## Variables de entorno

Los secretos nunca se guardan en Git. En local se pueden poner en un archivo `.env` en la raíz, que está ignorado por `.gitignore`.

| Variable | Obligatoria | Uso |
|---|---|---|
| `DB_PASSWORD` | Sí | Contraseña de `agroconecta_app` |
| `JWT_SECRET` | Sí | Clave HS256 para firmar los tokens (mínimo 32 bytes; la aplicación no arranca sin ella) |
| `DB_URL` | En `prod` | URL JDBC (en `dev`/`test` hay un valor local por defecto) |
| `DB_USER` | En `prod` | Usuario de la base de datos (por defecto `agroconecta_app`) |
| `ADMIN_CELULAR`, `ADMIN_NOMBRE`, `ADMIN_PASSWORD` | No | Crean el primer ADMINISTRADOR al arrancar si todavía no existe ninguno |
| `LOG_PATH` | No | Carpeta de los archivos de log en `prod` (por defecto `logs/`) |

## Cómo ejecutar

```bash
set -a; . ./.env; set +a      # cargar las variables (o exportarlas a mano)
./mvnw spring-boot:run        # perfil dev por defecto, en http://localhost:8080
```

Al arrancar, Flyway aplica las migraciones pendientes. La portada pública está en `http://localhost:8080/` y el inicio de sesión en `http://localhost:8080/pages/usuario/ingresar.html`.

Para producción: `./mvnw -B clean package` genera `target/agroconecta-0.0.1-SNAPSHOT.war`, que se despliega en Tomcat con `SPRING_PROFILES_ACTIVE=prod` y las variables anteriores. El destino de despliegue todavía se está definiendo ([ADR-005](docs/06-decisiones/ADR-005-destino-de-despliegue.md), pendiente).

## Pruebas, cobertura y Javadoc

| Comando | Qué hace | Resultado |
|---|---|---|
| `./mvnw test` | Pruebas unitarias (Mockito), de controladores (MockMvc), de integración contra MariaDB `agroconecta_test` (perfil `test`, sin H2) y de arquitectura (ArchUnit) | `target/surefire-reports/` |
| `./mvnw -B clean verify` | Lo anterior + informe JaCoCo + exigencia de **≥ 70 % de líneas** en cada paquete `*.aplicacion` (falla si no se cumple) | `target/site/jacoco/index.html` |
| `./mvnw javadoc:javadoc` | Documentación de la API Java (Javadoc en español) | `target/reports/apidocs/index.html` |

Las pruebas de integración necesitan MariaDB en marcha y las variables `DB_PASSWORD` y `JWT_SECRET`. Los resultados más recientes están en [`docs/07-calidad/evidencia-pruebas.md`](docs/07-calidad/evidencia-pruebas.md).

## Documentación de la API (Swagger)

Solo en el perfil **dev**:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI en JSON: `http://localhost:8080/v3/api-docs`

En **prod** están desactivados (`application-prod.properties`). El contrato completo, con los códigos de error y los mensajes, está en [`docs/05-api/endpoints.md`](docs/05-api/endpoints.md).

## Logs

Logback se configura en [`src/main/resources/logback-spring.xml`](src/main/resources/logback-spring.xml):

- `dev` y `test`: solo consola.
- `prod`: consola y archivo `logs/agroconecta.log` (o en `LOG_PATH`), que rota por día y cuando pasa de 10 MB. Se guardan 30 días, con un máximo de 1 GB.
- Niveles: `pe.agroconecta` en INFO; el SQL de Hibernate en WARN.

Nunca se registran contraseñas, hashes, tokens ni el texto de los mensajes, y el celular aparece enmascarado (`98*****21`).

## Ramas y commits

- `main`: estable y desplegable. `develop`: integración.
- `feature/rf-XX-nombre-corto`: una rama por requisito, creada desde `develop`. Se integra a `develop` solo mediante un *pull request* con CI en verde y la revisión de otra persona del equipo.
- CI: [`.github/workflows/ci.yml`](.github/workflows/ci.yml) ejecuta `./mvnw -B clean verify` contra un contenedor MariaDB 11.8 en cada *push* y *pull request* a `main` y `develop`.
- Commits en español con *Conventional Commits*: `feat(publicacion): ...`, `fix(usuario): ...`, `docs: ...`, `test: ...`, `refactor: ...`, `style: ...`, `ci: ...`, `chore: ...`.

## Documentación

| Carpeta | Contenido |
|---|---|
| [`docs/01-negocio/`](docs/01-negocio/) | Lean Canvas |
| [`docs/02-requisitos/`](docs/02-requisitos/) | [SRS](docs/02-requisitos/SRS-AgroConecta.md), [casos de uso](docs/02-requisitos/casos-de-uso/casos-de-uso.md) y [matriz de trazabilidad](docs/02-requisitos/matriz-trazabilidad.md) |
| [`docs/03-procesos/`](docs/03-procesos/) | Proceso de negocio TO-BE (draw.io) |
| [`docs/04-diseno/`](docs/04-diseno/) | [Arquitectura](docs/04-diseno/arquitectura/), [modelo de base de datos](docs/04-diseno/base-datos/modelo-bd.sql) (v4.1) y [guía de estilos](docs/04-diseno/ux-ui/guia-estilos.md) y [pantallas](docs/04-diseno/ux-ui/pantallas.md) |
| [`docs/05-api/`](docs/05-api/) | [Contrato de la API REST](docs/05-api/endpoints.md) |
| [`docs/06-decisiones/`](docs/06-decisiones/) | Decisiones de arquitectura (ADR-001 a ADR-005) |
| [`docs/07-calidad/`](docs/07-calidad/) | [Seguridad](docs/07-calidad/seguridad.md) y [evidencia de pruebas](docs/07-calidad/evidencia-pruebas.md) |
