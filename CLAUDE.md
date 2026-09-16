# CLAUDE.md — AgroConecta

## Proyecto

AgroConecta es una plataforma web que conecta a pequeños agricultores de Piura (Perú) directamente con compradores (mercados mayoristas, restaurantes, hoteles, procesadoras) para reducir la dependencia de intermediarios. Proyecto académico del curso **Integrador I: Sistemas – Software (100000I58N), 2026-2**. Equipo de 6 personas (4 programadores, 2 documentadores).

Contexto completo en `docs/academico/` (sílabo, Lean Canvas, resumen ejecutivo APF1). Diseño en `docs/diseno/`. **Léelos antes de tomar decisiones de alcance.**

## Stack (obligatorio — no sustituir sin preguntar)

- **Backend:** Java 21 LTS · Spring Boot 4.x · Maven (usar wrapper `./mvnw`)
- Spring Web (REST) · Spring Data JPA / Hibernate · Spring Security + JWT · Bean Validation · Spring Boot Actuator
- **Base de datos:** MySQL 8 (charset `utf8mb4`) · migraciones con **Flyway**
- **Frontend:** HTML5 + CSS3 + JavaScript vanilla (ES6+) + Bootstrap 5, servido desde `src/main/resources/static/`. **Sin React/Vue/Angular ni build de Node.**
- **Librerías Java del sílabo:** Google Guava · Apache Commons Lang3 · Apache POI (import/export Excel) · Logback (vía Spring Boot)
- **Pruebas:** JUnit 5 · Mockito · Spring Boot Test · H2 (modo MySQL) para integración · JaCoCo
- **Documentación API:** springdoc-openapi (Swagger UI)
- **Empaquetado:** WAR (desplegable en Apache Tomcat externo y ejecutable con Tomcat embebido)
- **Control de versiones:** Git + GitHub

**Meta académica: ≥ 65 % del código en Java.** La lógica de negocio y las validaciones viven en el backend; el JavaScript solo consume la API y pinta la interfaz.

## Arquitectura

MVC en capas + patrón DAO + API REST, aplicando SOLID. Flujo obligatorio:

```
static (HTML/JS) → controller → service → dao → MySQL
```

- Los controllers nunca acceden a los DAO directamente.
- Las entidades JPA nunca se exponen en la API: siempre DTOs.
- Los services se definen como interfaz + implementación.

```
src/main/java/pe/agroconecta/
├── AgroConectaApplication.java
├── config/          # beans, CORS, OpenAPI, ServletInitializer
├── security/        # JWT, filtros, UserDetailsService
├── controller/      # @RestController → /api/v1/**
├── service/         # interfaces
│   └── impl/        # implementaciones
├── dao/             # interfaces JpaRepository (patrón DAO)
├── model/
│   ├── entity/      # entidades JPA
│   └── enums/       # Rol, EstadoPublicacion, UnidadMedida...
├── dto/
│   ├── request/
│   └── response/
├── mapper/          # conversión Entity ↔ DTO (manual)
├── exception/       # excepciones de negocio + @RestControllerAdvice
└── util/
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
├── logback-spring.xml
├── db/migration/    # V1__esquema_inicial.sql, V2__...
└── static/
    ├── index.html, login.html, registro.html, ...
    ├── css/
    ├── img/
    └── js/          # api.js (fetch + token), auth.js, un .js por página
src/test/java/pe/agroconecta/   # espejo de main
```

## Dominio

**Roles:** `AGRICULTOR`, `COMPRADOR`, `ADMINISTRADOR`.

**Entidades principales** (referenciales — si existe `docs/diseno/modelo-bd.md`, ese documento manda): Usuario, PerfilAgricultor, PerfilComprador, Cultivo, Ubicacion (departamento/provincia/distrito), Publicacion, PrecioMercado, Conversacion, Mensaje, Calificacion, Transaccion, Notificacion.

### Requisitos funcionales → módulo

| RF | Descripción | Módulo / notas |
|---|---|---|
| RF-01 | Publicación de cosecha en máx. 3 pasos (cultivo, cantidad, precio, ubicación) | publicacion |
| RF-02 | Precios de mercado con históricos por cultivo y zona | precio — carga por admin, importación Excel con POI |
| RF-03 | Búsqueda y filtros multi-criterio (categoría, provincia/distrito, fecha) | publicacion — JPA Specifications, paginado |
| RF-04 | Mensajería directa agricultor–comprador | mensajeria — REST + polling en el MVP |
| RF-05 | Calificación 1–5 estrellas con comentario | calificacion |
| RF-06 | Historial de transacciones por usuario | transaccion |
| RF-07 | Alertas de vencimiento de productos perecibles | notificacion — tarea `@Scheduled` |
| RF-08 | Registro y gestión de roles | auth / usuario |
| RF-09 | Panel administrativo (usuarios, moderación, reportes) | admin — exportación Excel con POI |

**Fuera del MVP inicial** (aparecen como pantallas pero no tienen RF): venta agrupada, seguimiento de transporte, pasarela de pagos. No implementarlos sin confirmación.

**Orden de implementación sugerido:** RF-08 → RF-01 + RF-03 → RF-02 → RF-04 → RF-05 + RF-06 → RF-07 → RF-09.

### Requisitos no funcionales → reglas de implementación

- **RNF-01 (< 3 s en 4G):** páginas ligeras, listados paginados (máx. 20 ítems), imágenes comprimidas, sin librerías JS pesadas.
- **RNF-02 (500+ usuarios concurrentes):** API stateless, pool HikariCP, índices en columnas usadas en filtros.
- **RNF-03 (usabilidad rural):** mobile-first, botones grandes (≥ 48 px), alto contraste, navegación lineal, textos simples en español.
- **RNF-04 (mantenibilidad):** SOLID y **Javadoc en toda clase y método público**.
- **RNF-05 (integridad):** 3NF, FK con integridad referencial. Todo cambio de esquema va en una **nueva** migración Flyway; nunca editar una migración ya aplicada.
- **RNF-06 (disponibilidad):** endpoint `/actuator/health` habilitado.

### Reglas generales de negocio

- Moneda: soles (PEN), siempre `BigDecimal`.
- Zona horaria: `America/Lima`.
- Un usuario solo puede editar o eliminar sus propios recursos (publicaciones, mensajes, calificaciones).
- El uso de la plataforma es gratuito para el agricultor.

## Convenciones de código

- **Idioma:** nombres de dominio en español (`Publicacion`, `crearPublicacion`), sufijos técnicos en inglés (`Controller`, `Service`, `Dao`, `Dto`). Sin tildes ni ñ en identificadores. Javadoc, comentarios y mensajes al usuario en español.
- **Base de datos:** tablas y columnas en `snake_case`; tablas en plural (`publicaciones`).
- **API:** `/api/v1/{recurso-en-plural}`; verbos HTTP correctos; errores con formato JSON uniforme `{timestamp, status, error, mensaje, ruta}`.
- Inyección por constructor (no `@Autowired` en campos). Sin Lombok.
- Validación con Bean Validation en los DTOs de request.
- Logs con SLF4J; nunca registrar contraseñas, tokens ni datos personales.

## Seguridad (se evalúa en semana 14)

- Contraseñas con BCrypt. JWT con expiración; el secreto se lee de variable de entorno.
- Credenciales fuera del repositorio: usar `${DB_PASSWORD}`, `${JWT_SECRET}`; `.env` en `.gitignore`.
- Autorización por rol con `@PreAuthorize`.
- Nunca concatenar SQL; solo JPA o consultas parametrizadas.
- En el frontend, insertar datos del usuario con `textContent`, no con `innerHTML`. Token en `sessionStorage`.

## Pruebas

- TDD en la capa service: escribir la prueba antes de la implementación.
- Unitarias con Mockito; controllers con `@WebMvcTest`; integración con `@SpringBootTest` + H2.
- Cobertura objetivo ≥ 70 % en service (JaCoCo).
- Toda funcionalidad nueva incluye sus pruebas.

## Comandos

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # ejecutar en local
./mvnw test                                              # pruebas
./mvnw verify                                            # pruebas + reporte JaCoCo
./mvnw clean package                                     # genera target/agroconecta.war
```

- App: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Git

- Ramas: `main` (entregas), `develop` (integración), `feature/RF-XX-descripcion`, `fix/descripcion`.
- Commits en español con prefijo: `feat(RF-01): ...`, `fix: ...`, `test: ...`, `docs: ...`, `refactor: ...`.
- Pull request hacia `develop` con al menos una revisión.

## Hitos del curso (Unidades 3 y 4)

| Semana | Entrega |
|---|---|
| 10 | Avance 30 % usando librerías Java |
| 11 | Avance 40 % integrado en GitHub |
| 12 | APF3 |
| 13 | Avance 60 % con pruebas de software |
| 14 | Avance 70 % + reporte de pruebas de seguridad |
| 15 | Avance 80 % con la aplicación desplegada |
| 16 | Avance 90 % + plan de monitoreo |
| 17 | 100 % + plan de mantenimiento |
| 18 | Proyecto final (PROY) |

## Instrucciones para Claude

- Revisa la estructura existente antes de crear archivos y respétala.
- Trabaja por RF: un requisito por rama/sesión.
- Si algo es ambiguo o no está en `docs/`, pregunta antes de inventar alcance.
- No agregues dependencias fuera del stack sin preguntar.
- Después de cada cambio, ejecuta `./mvnw test` e informa el resultado.
- Responde en español y de forma concisa.
