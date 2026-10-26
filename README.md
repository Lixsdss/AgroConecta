<div align="center">

# 🌱 AgroConecta

**Del campo de Piura directo al comprador, sin intermediarios.**

[![CI](https://github.com/Lixsdss/AgroConecta/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/Lixsdss/AgroConecta/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot 4.1.1](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![MariaDB / MySQL](https://img.shields.io/badge/MariaDB%20%2F%20MySQL-Flyway-003545?logo=mariadb&logoColor=white)
![Bootstrap 5](https://img.shields.io/badge/Bootstrap-5.3.8-7952B3?logo=bootstrap&logoColor=white)
![Cobertura](https://img.shields.io/badge/cobertura%20aplicacion-%E2%89%A570%25-brightgreen)

[Inicio rápido](#-inicio-rápido) ·
[Funcionalidades](#-funcionalidades) ·
[Arquitectura](#-arquitectura) ·
[Pruebas](#-pruebas-cobertura-y-javadoc) ·
[API](#-documentación-de-la-api) ·
[Documentación](#-documentación)

</div>

---

AgroConecta es una plataforma web que conecta directamente a **pequeños agricultores de Piura (Perú)** con compradores (mercados mayoristas, restaurantes, hoteles y agroindustrias) para reducir la dependencia de intermediarios.

El agricultor publica su cosecha en tres pasos con un precio de referencia, negocia por chat con ofertas, registra el acuerdo y sigue la entrega hasta que el comprador la confirma. La plataforma **no cobra ni procesa pagos**: el pago se hace entre las partes, fuera del sistema (ADR-002).

> Proyecto integrador del curso *Integrador I: Sistemas – Software* (100000I58N), UTP, agosto–noviembre de 2026.

## 👥 Actores

| Actor | Qué hace |
|---|---|
| 🧑‍🌾 **Agricultor** | Publica cosechas, ve precios de referencia, conversa y acuerda ventas, sigue la entrega, recibe avisos de vencimiento con acciones rápidas y responde a propuestas de venta agrupada. |
| 🛒 **Comprador** | Busca y filtra cosechas, envía ofertas por chat o WhatsApp, registra acuerdos, confirma la recepción, califica y envía propuestas conjuntas a lotes de varios agricultores. |
| 🛡️ **Administrador** | Modera publicaciones, carga precios de mercado, resuelve reclamos, suspende cuentas y revisa el panel con métricas. |

Las cooperativas y los transportistas no son usuarios del sistema.

## ✅ Funcionalidades

| RF | Funcionalidad | Módulo |
|---|---|---|
| RF-01 | Publicar una cosecha en 3 pasos (variedad, unidad y cantidad, precio y fechas) | `publicacion` |
| RF-02 | Precios de referencia por zona, con tendencia; carga por formulario o Excel/CSV | `precio` |
| RF-03 | Búsqueda con filtros (cultivo, unidad, distrito) y orden por precio, fecha o calificación | `publicacion` |
| RF-04 | Chat por publicación con ofertas estructuradas y botón de WhatsApp | `mensajeria` |
| RF-05 | Calificación mutua al completar una venta | `venta` |
| RF-06 | Registro del acuerdo, cancelación e historial exportable a Excel | `venta` |
| RF-07 | Avisos de vencimiento y acciones rápidas (bajar precio, extender, retirar) | `notificacion`, `publicacion` |
| RF-08 | Registro e inicio de sesión con celular, roles y perfil | `usuario` |
| RF-09 | Moderación posterior, reclamos, suspensiones y panel con métricas | `administracion` |
| RF-10 | Venta agrupada: lotes de varios agricultores y propuestas conjuntas con reparto proporcional | `ventaagrupada` |
| RF-11 | Seguimiento de la entrega con línea de tiempo y "Confirmar que recibí" | `venta` |

Las suscripciones (RF-12) quedan como trabajo futuro.

## 🧰 Tecnologías

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

Todo es software libre y gratuito: el proyecto no genera costos.

## 🏗️ Arquitectura

Monolito modular (ADR-001): un único WAR dividido en **módulos de negocio** bajo `pe.agroconecta`, cada uno con las mismas cuatro capas.

![Arquitectura general de AgroConecta](docs/04-diseno/arquitectura/arquitectura-general.png)

```
pe.agroconecta.<modulo>/
├── api/              controladores REST + Request/Response (records con Bean Validation)
├── aplicacion/       servicios con los casos de uso y las reglas de negocio + DTO (records)
├── dominio/          entidades JPA y enums (las vistas, como entidades @Immutable)
└── infraestructura/  repositorios Spring Data JPA
```

| Módulo | Responsabilidad |
|---|---|
| `catalogo` | Catálogos compartidos (provincias, distritos, cultivos, variedades, unidades) |
| `usuario` | Registro, inicio de sesión con celular y perfil (RF-08) |
| `publicacion` | Publicar, buscar y acciones rápidas (RF-01, RF-03, RF-07) |
| `precio` | Precios de referencia (RF-02) |
| `mensajeria` | Conversaciones y ofertas (RF-04) |
| `venta` | Calificaciones, acuerdos, historial y entrega (RF-05, RF-06, RF-11) |
| `ventaagrupada` | Lotes y propuestas conjuntas (RF-10) |
| `notificacion` | Centro de avisos (RF-07) |
| `administracion` | Moderación, reclamos, suspensiones y panel (RF-09) |
| `compartido` | Seguridad, OpenAPI, CORS y manejo unificado de errores; **sin lógica de negocio** |

- Un módulo usa otro **solo a través de su capa `aplicacion`**. Estas reglas las verifica ArchUnit en [`ArquitecturaTest`](src/test/java/pe/agroconecta/ArquitecturaTest.java).
- Muchas reglas de negocio viven en la base de datos (21 triggers, 10 vistas, un procedimiento y el evento diario `ev_tareas_diarias`), definida por las migraciones de Flyway en [`src/main/resources/db/migration/`](src/main/resources/db/migration/). Hibernate solo valida el esquema (`ddl-auto=validate`).

Más diagramas (dependencias entre módulos, capas y clases): [`docs/04-diseno/arquitectura/`](docs/04-diseno/arquitectura/).

## 🚀 Inicio rápido

### 1. Requisitos previos

- **JDK 21**. No hace falta instalar Maven: se usa `./mvnw`.
- **MariaDB 11+** (desarrollo) o **MySQL 8+** (producción) con:
  - las bases `agroconecta` (desarrollo) y `agroconecta_test` (pruebas);
  - el usuario `agroconecta_app` con privilegios CREATE, ALTER, INDEX, REFERENCES, TRIGGER, EVENT, CREATE VIEW, CREATE ROUTINE y ALTER ROUTINE sobre ambas bases;
  - `event_scheduler=ON` en el servidor (lo usa `ev_tareas_diarias`).

  Estas tareas las hace el administrador de la base de datos; no forman parte de las migraciones.

### 2. Clonar

```bash
git clone git@github.com:Lixsdss/AgroConecta.git
cd AgroConecta
```

### 3. Variables de entorno

> [!IMPORTANT]
> Los secretos **nunca** se suben a GitHub. Guárdalos en un archivo `.env` en la raíz del proyecto, que ya está ignorado por `.gitignore`.

| Variable | Obligatoria | Uso |
|---|---|---|
| `DB_PASSWORD` | Sí | Contraseña de `agroconecta_app` |
| `JWT_SECRET` | Sí | Clave HS256 para firmar los tokens (mínimo 32 bytes; la aplicación no arranca sin ella) |
| `DB_URL` | En `prod` | URL JDBC (en `dev`/`test` hay un valor local por defecto) |
| `DB_USER` | En `prod` | Usuario de la base de datos (por defecto `agroconecta_app`) |
| `ADMIN_CELULAR`, `ADMIN_NOMBRE`, `ADMIN_PASSWORD` | No | Crean el primer ADMINISTRADOR al arrancar si todavía no existe ninguno |
| `LOG_PATH` | No | Carpeta de los archivos de log en `prod` (por defecto `logs/`) |

Ejemplo de `.env` (reemplaza cada valor por el tuyo):

```bash
DB_PASSWORD=<contraseña de agroconecta_app>
JWT_SECRET=<clave aleatoria de al menos 32 bytes>
# Solo la primera vez, para crear el administrador inicial:
ADMIN_CELULAR=<9 dígitos que empiezan por 9>
ADMIN_NOMBRE=<nombre completo>
ADMIN_PASSWORD=<contraseña segura>
```

Para generar una `JWT_SECRET` aleatoria:

```bash
openssl rand -base64 48
```

### 4. Ejecutar

```bash
set -a; . ./.env; set +a      # cargar las variables (o exportarlas a mano)
./mvnw spring-boot:run        # perfil dev por defecto, en http://localhost:8080
```

Al arrancar, Flyway aplica las migraciones pendientes.

| Pantalla | URL |
|---|---|
| Portada pública (precios de referencia) | http://localhost:8080/ |
| Iniciar sesión | http://localhost:8080/pages/usuario/ingresar.html |
| Swagger UI (solo `dev`) | http://localhost:8080/swagger-ui.html |

### 5. Primer administrador

El primer ADMINISTRADOR no se crea por SQL. Lo crea la aplicación al arrancar, **solo si no existe ninguno** y las tres variables `ADMIN_*` tienen valor. La contraseña se guarda cifrada con BCrypt y nunca aparece en el log. Después del primer arranque puedes quitar esas variables del `.env`. Los demás administradores se crean desde la aplicación.

### Producción

`./mvnw -B clean package` genera `target/agroconecta-0.0.1-SNAPSHOT.war`, que se despliega en Tomcat con `SPRING_PROFILES_ACTIVE=prod` y las variables anteriores. El destino de despliegue todavía se está definiendo ([ADR-005](docs/06-decisiones/ADR-005-destino-de-despliegue.md)).

## 🧪 Pruebas, cobertura y Javadoc

| Comando | Qué hace | Resultado |
|---|---|---|
| `./mvnw test` | Pruebas unitarias (Mockito), de controladores (MockMvc), de integración contra MariaDB `agroconecta_test` (perfil `test`, sin H2) y de arquitectura (ArchUnit) | `target/surefire-reports/` |
| `./mvnw -B clean verify` | Lo anterior + informe JaCoCo + exigencia de **≥ 70 % de líneas** en cada paquete `*.aplicacion` (falla si no se cumple) | `target/site/jacoco/index.html` |
| `./mvnw javadoc:javadoc` | Documentación de la API Java (Javadoc en español) | `target/reports/apidocs/index.html` |

Las pruebas de integración necesitan MariaDB en marcha y las variables `DB_PASSWORD` y `JWT_SECRET`. Los resultados más recientes están en [`docs/07-calidad/evidencia-pruebas.md`](docs/07-calidad/evidencia-pruebas.md).

## 📖 Documentación de la API

Swagger UI (`/swagger-ui.html`) y OpenAPI en JSON (`/v3/api-docs`) están disponibles **solo en el perfil `dev`**; en `prod` están desactivados.

El contrato completo de los endpoints, con los códigos de estado y los mensajes de error, está en [`docs/05-api/endpoints.md`](docs/05-api/endpoints.md). Todos los errores usan el mismo formato JSON:

```json
{ "timestamp": "...", "estado": 422, "error": "Unprocessable Content", "mensaje": "...", "ruta": "/api/v1/...", "errores": [] }
```

## 🔒 Seguridad

- Inicio de sesión con celular y contraseña (BCrypt); token JWT HS256 de 12 horas.
- En cada petición se comprueba que el usuario siga ACTIVO: una cuenta suspendida recibe 401.
- Tras 5 intentos fallidos de inicio de sesión, el celular se bloquea 15 minutos (429).
- Roles con `@PreAuthorize`; solo `/actuator/health` está expuesto.
- Nunca se registran contraseñas, tokens ni el texto de los mensajes, y el celular aparece enmascarado (`98*****21`).

Detalle y riesgos pendientes: [`docs/07-calidad/seguridad.md`](docs/07-calidad/seguridad.md).

## 📝 Logs

Logback se configura en [`logback-spring.xml`](src/main/resources/logback-spring.xml):

- `dev` y `test`: solo consola.
- `prod`: consola y archivo `logs/agroconecta.log` (o en `LOG_PATH`), que rota por día y cuando pasa de 10 MB. Se guardan 30 días, con un máximo de 1 GB.

## 🌿 Ramas, commits y CI

- `main`: estable y desplegable. `develop`: integración.
- `feature/rf-XX-nombre-corto`: una rama por requisito, creada desde `develop`. Se integra a `develop` solo mediante un *pull request* con CI en verde y la revisión de otra persona del equipo.
- Commits en español con *Conventional Commits*: `feat(publicacion): ...`, `fix(usuario): ...`, `docs: ...`, `test: ...`, `refactor: ...`, `ci: ...`, `chore: ...`.
- **CI** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)): en cada *push* y *pull request* a `main` y `develop` ejecuta `./mvnw -B clean verify` contra un contenedor MariaDB 11.8. Necesita los *repository secrets* `DB_PASSWORD` y `JWT_SECRET` (en *Settings → Secrets and variables → Actions*). El informe JaCoCo se publica como el artefacto `informe-jacoco`.

## 📚 Documentación

| Carpeta | Contenido |
|---|---|
| [`docs/01-negocio/`](docs/01-negocio/) | Lean Canvas |
| [`docs/02-requisitos/`](docs/02-requisitos/) | [SRS](docs/02-requisitos/SRS-AgroConecta.md), [casos de uso](docs/02-requisitos/casos-de-uso/casos-de-uso.md) y [matriz de trazabilidad](docs/02-requisitos/matriz-trazabilidad.md) |
| [`docs/03-procesos/`](docs/03-procesos/) | Proceso de negocio TO-BE (draw.io) |
| [`docs/04-diseno/`](docs/04-diseno/) | [Arquitectura](docs/04-diseno/arquitectura/), [modelo de base de datos](docs/04-diseno/base-datos/modelo-bd.sql) (v4.1), [guía de estilos](docs/04-diseno/ux-ui/guia-estilos.md) y [pantallas](docs/04-diseno/ux-ui/pantallas.md) |
| [`docs/05-api/`](docs/05-api/) | [Contrato de la API REST](docs/05-api/endpoints.md) |
| [`docs/06-decisiones/`](docs/06-decisiones/) | Decisiones de arquitectura (ADR-001 a ADR-005) |
| [`docs/07-calidad/`](docs/07-calidad/) | [Seguridad](docs/07-calidad/seguridad.md) y [evidencia de pruebas](docs/07-calidad/evidencia-pruebas.md) |
