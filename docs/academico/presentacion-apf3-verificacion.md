# Verificación del guion APF3 contra el repositorio

**Guion revisado:** `docs/academico/presentacion-apf3-contenido.md`
**Repositorio:** rama `chore/apf3-cierre`, HEAD `c094fce` (25/10/2026). Revisión del 26/10/2026.
**Método:** lectura del código, `git`, `./mvnw -o dependency:list` / `help:effective-pom` (sin modificar el proyecto), informes de `target/` y consultas de solo lectura (`SELECT COUNT`) a la base local `agroconecta` (perfil dev).
**Regla:** lo que no se pudo comprobar dice **No verificado**.

---

## 1. Correcciones

### 1.1 Cifras y datos incorrectos o desactualizados

| Diap. | Dice el guion | Valor correcto | Evidencia |
|---|---|---|---|
| 2 | 238 pruebas en verde | **249** pruebas en verde | `target/surefire-reports/` (43 clases, 0 fallas, 0 errores, 0 omitidas); `docs/07-calidad/evidencia-pruebas.md:19` |
| 2 | 96,2 % de cobertura | **96,1 %** de líneas (2814/2928) | `target/site/jacoco/jacoco.csv`; `docs/07-calidad/evidencia-pruebas.md:24` |
| 7 | Unitarias con Mockito: 133 (junto con ArchUnit) | **135 unitarias** (26 clases) **+ 5 de ArchUnit** (1 clase) = 140 | `docs/07-calidad/evidencia-pruebas.md:21,23`; recuento de `target/surefire-reports/` |
| 7 | Integración contra MariaDB real: 105 | **109** (16 clases con `@SpringBootTest`) | `docs/07-calidad/evidencia-pruebas.md:22`; `src/main/resources/application-test.properties:2` |
| 7 | 238/238 · 0 fallas · 96,2 % de líneas | **249/249 · 0 fallas · 0 errores · 96,1 % de líneas** (ramas: 83,7 %) | `target/site/jacoco/jacoco.csv` |
| 7 | **[VERIFICAR]** hashes de los commits | `test:` **`fd95723`** · `feat:` **`28e0576`** (ver §2, diap. 7) | `git log` |
| 10 | Calidad: **JUnit 5** | **JUnit Jupiter 6.0.3** (llega por los `spring-boot-starter-*-test` de Spring Boot 4.1.1). El artefacto de ArchUnit se llama `archunit-junit5`, pero JUnit es 6 | `./mvnw dependency:list` → `org.junit.jupiter:junit-jupiter-api:6.0.3` |
| 10 | MariaDB 11 | CI: **MariaDB 11.8**; base local: **MariaDB 13.0.2**. Producción: **MySQL** (driver MySQL Connector/J 9.7.0) | `.github/workflows/ci.yml:27`; `SELECT VERSION()`; `pom.xml:100-102` |
| 13 | Esquema `main` ← `develop` ← `feature/rf-xx` con *pull requests* fusionados | En el repositorio local **no hay ningún merge**: `develop`, `main` y `origin/develop` siguen en el commit inicial `bdb0857` (27/09). Las ramas `feature/rf-*` están **apiladas** (cada una contiene a la anterior) y `chore/apf3-cierre` está 11 commits por delante de la última. Los *pull requests* en GitHub: **No verificado** (sin `gh` ni acceso SSH al remoto) | `git log -1 develop main origin/develop`; `git log --merges` = 0; `git merge-base --is-ancestor` |
| 13 | Etiqueta / release `apf3` | **No existe** ninguna etiqueta en el repositorio local. En GitHub: **No verificado** | `git tag -l` vacío |
| 13 | (implícito) el trabajo está en GitHub | Los 17 commits posteriores a `feature/rf-10-venta-agrupada` (incluidos `fd95723`, `28e0576` y el Javadoc) **no están en ninguna rama remota** conocida localmente (última sincronización: 24/10) | `git branch -r --contains c094fce` vacío |
| Preguntas | "Commits `test:` antes de `feat:`" como prueba general de TDD | En todo el historial (`git log --all`, 80 commits) hay **un solo** commit `test:` (`fd95723`). Sirve como ejemplo, no como práctica sistemática | `git log --all --format=%s` |
| Diseño | Plus Jakarta Sans, Inter y JetBrains Mono; paleta #0B2E16, #15803D, #4ADE80, #D97706, #0F172A | La guía de estilos del proyecto usa **Work Sans** (texto) y **Cormorant Garamond** (títulos); colores `--ac-verde` **#306D35**, `--ac-verde-oscuro` **#22572A**, `--ac-ambar` **#F9A825**, `--ac-fondo` **#F9F6F0**, `--ac-texto` **#2C3C46**. Si las capturas de la app deben verse coherentes con las diapositivas, conviene decidir cuál se usa | `docs/04-diseno/ux-ui/guia-estilos.md:18,21,33-34`; `src/main/resources/static/css` |

### 1.2 Afirmaciones con matiz

| Diap. | Dice el guion | Matiz | Evidencia |
|---|---|---|---|
| 9 | POI: "protección contra *zip bombs*" | **No está configurada en el código** (ni `ZipSecureFile` ni ajustes de inflado). Solo existe la protección que POI trae por defecto; no se comprobó en este repositorio. Sí están verificados: 1 MB, solo .xlsx/.csv y 500 filas | `grep -i zip src/main` vacío; `application.properties:27-28`; `LectorArchivoPrecios.java:41,65-67` |
| 9 | Guava: `Preconditions` en servicios | Correcto, pero sobre todo `checkNotNull` (17 usos), `checkArgument` (3) y `checkState` (2) | `grep -rhoE "Preconditions\.[a-z]+" src/main/java` |
| 4 | 0 lógica de negocio en los controladores | **No verificado** como regla automática: ArchUnit solo prohíbe que `api` dependa de `infraestructura` (regla 3), no mide lógica | `ArquitecturaTest.java:53-58` |
| 4 | Vista `precios.html` + `precios.js` | Rutas exactas: `static/pages/administracion/precios.html` y `static/js/administracion/precios.js` | `precios.html:9` |
| 8 | Relación con OWASP A01, A02, A03, A07, A09 | **No verificado** (no hay documento en el código que haga ese mapeo) | — |
| 8 | (no aparece) | Dato adicional que se puede citar: Swagger UI y `/v3/api-docs` están desactivados en `prod` (commit `16b38c0`) | `application-prod.properties:7-8` |
| 1 | Docente: Richard Doan Asencios Vilchez | **No verificado**: el nombre solo aparece en el propio guion | `grep -r Asencios docs README.md` |
| 2 / 14 | 11 de 11 requisitos | Coherente con el código (endpoints, pantallas y pruebas de RF-01 a RF-11); no se repitió una prueba manual de extremo a extremo en esta revisión | — |

### 1.3 Confirmado sin cambios

- **Diap. 2:** 67 endpoints REST (67 anotaciones `@Get/Post/Put/Patch/DeleteMapping` en `src/main/java`).
- **Diap. 3:** 9 módulos de negocio + `compartido`; 4 capas; **5 reglas ArchUnit** en verde (`ArquitecturaTest.java:39-73`: comunicación solo por `aplicacion`, infraestructura solo desde su `aplicacion`, `api` sin `infraestructura`, `compartido` sin módulos, sin ciclos).
- **Diap. 4:** 17 controladores `@RestController`; 28 páginas + portada (29 HTML); cadena `PrecioController` → `CargaPrecioService` (`PrecioController.java:47,115`) → `PrecioMercado`; `api.js` envía `Authorization: Bearer` (`api.js:38`).
- **Diap. 5:** `PrecioMercadoRepository extends JpaRepository` (`PrecioMercadoRepository.java:14`); trigger `trg_precios_mercado_bi` (`V1__esquema_inicial.sql:666`).
- **Diap. 6:** las cinco clases existen con el papel descrito (`ValidadorUsuarioActivo.java:14`, `VerificadorEstadoUsuario.java:7,15`, `VerificadorEstadoUsuarioBd.java:15`; inyección por constructor en `:24`).
- **Diap. 7:** todos los paquetes `aplicacion` ≥ 95 % de líneas (mínimo: `precio` 95,4 %; máximo: `catalogo` y `notificacion` 100 %).
- **Diap. 8:** JWT con vigencia 12 h (`application.properties:18`, `PT12H`); BCrypt; usuario no activo → `invalid_token` (`ValidadorUsuarioActivo.java:39-40`); **5 fallos en 15 min → 429** (`LimiteIntentosLogin.java:31,34`); celular enmascarado `98*****81` (`ReglasUsuario.java:56`, `StringUtils.overlay(celular, "*****", 2, 7)`); `.env` en `.gitignore:39` y ningún `.env` en el historial (`git log --all --name-only`).
- **Diap. 9:** versiones Guava 33.7.1-jre, Commons Lang3 3.20.0, POI 5.5.1, Logback 1.5.38; **`Cache` de Guava sí se usa** (`LimiteIntentosLogin.java:51-54`); **`logback-spring.xml` existe** con consola en dev/test y archivo rotativo en prod (10 MB, 30 días, 1 GB) (`logback-spring.xml:30-56`).
- **Diap. 10:** Flyway con migraciones V1–V3 (`src/main/resources/db/migration/`); Bootstrap 5 (5.3.8, servido desde `static/vendor/bootstrap`).
- **Diap. 11:** el orden de las ramas es correcto (ver §4); el umbral de 70 % por paquete `aplicacion` está en el `jacoco:check` (`pom.xml:186-197`).
- **Diap. 12:** la sesión vive en `sessionStorage` (`static/js/comun/sesion.js:2,14,27`).
- **Diap. 13:** 78 commits en la rama actual, los 78 con formato Conventional Commits; CI en cada *push* y *pull request* a `develop`/`main` (`ci.yml:6-10`).

---

## 2. Fragmentos de código para las diapositivas

### Diapositiva 4 — `PrecioController.registrar`
`src/main/java/pe/agroconecta/precio/api/PrecioController.java:111-118`
```java
@PostMapping
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<PrecioRegistradoDto> registrar(@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RegistrarPrecioRequest request) {
    PrecioRegistradoDto guardado = carga.registrar(Long.parseLong(jwt.getSubject()), request.idCultivo(),
            request.idDistrito(), request.fecha(), request.precioKg(), request.fuente());
    return ResponseEntity.created(URI.create("/api/v1/precios/" + guardado.id())).body(guardado);
}
```

### Diapositiva 5 — `PrecioMercadoRepository`
`src/main/java/pe/agroconecta/precio/infraestructura/PrecioMercadoRepository.java:14-44` (sin Javadoc)
```java
public interface PrecioMercadoRepository extends JpaRepository<PrecioMercado, Long> {

    boolean existsByIdCultivoAndIdDistritoAndFecha(Integer idCultivo, Integer idDistrito, LocalDate fecha);

    List<PrecioMercado> findByIdCultivoAndIdDistritoAndFechaGreaterThanEqualOrderByFechaAsc(Integer idCultivo,
            Integer idDistrito, LocalDate desde);
    // ...
}
```

### Diapositiva 6 — interfaz e implementación
`src/main/java/pe/agroconecta/compartido/seguridad/VerificadorEstadoUsuario.java:7-16` y `src/main/java/pe/agroconecta/usuario/aplicacion/VerificadorEstadoUsuarioBd.java:14-15,24,31`
```java
// compartido.seguridad
public interface VerificadorEstadoUsuario {
    boolean estaActivo(long idUsuario);
}

// usuario.aplicacion
@Service
public class VerificadorEstadoUsuarioBd implements VerificadorEstadoUsuario {
    public VerificadorEstadoUsuarioBd(UsuarioRepository usuarios) { /* ... */ }
    public boolean estaActivo(long idUsuario) { /* ... */ }
}
```
(Son 10 líneas con los dos comentarios de paquete; sin ellos quedan 8.)

### Diapositiva 7 — prueba que falla primero (TDD)
**La tarea está hecha.** Commits:
- `fd95723` — `test(usuario): especificar el límite de intentos de inicio de sesión` (24/10/2026 00:31) — solo añade la prueba de integración.
- `28e0576` — `feat(usuario): límite de intentos de inicio de sesión con respuesta 429` (24/10/2026 00:34) — `LimiteIntentosLogin` con Guava Cache, `DemasiadosIntentosException`, manejador 429.

La prueba no cambió desde el commit `test:` (`git show fd95723:… | diff` sin diferencias).
`src/test/java/pe/agroconecta/usuario/LimiteIntentosLoginIntegracionTest.java:49-72` (recortado)
```java
@Test
void cincoFallosBloqueanConCodigo429HastaQueVenceLaVentana() throws Exception {
    registrar("977000001");
    for (int i = 0; i < 5; i++) {
        login("977000001", ERRONEA).andExpect(status().isUnauthorized());
    }
    login("977000001", ERRONEA).andExpect(status().isTooManyRequests());
    // ...
}
```
Nota: que la prueba fallara en `fd95723` antes de `28e0576` se deduce del historial (en `fd95723` no existen `LimiteIntentosLogin` ni el manejo de 429); la ejecución en rojo **no se repitió** en esta revisión.

### Diapositiva 9 — una línea real por librería

| Librería | Línea | Ubicación |
|---|---|---|
| Guava | `this.fallos = CacheBuilder.newBuilder().expireAfterWrite(VENTANA)` *(dos líneas unidas)* | `usuario/aplicacion/LimiteIntentosLogin.java:51-52` |
| Guava (alternativa) | `List<String> lineas = Splitter.onPattern("\r?\n").splitToList(texto);` | `precio/infraestructura/LectorArchivoPrecios.java:132` |
| Commons Lang3 | `return StringUtils.overlay(celular, "*****", 2, 7);` | `usuario/aplicacion/ReglasUsuario.java:56` |
| Apache POI | `try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {` | `precio/infraestructura/LectorArchivoPrecios.java:99` |
| Logback (vía SLF4J) | `LOG.warn("Inicio de sesión bloqueado por {} intentos fallidos: celular={}", MAXIMO_FALLOS, ReglasUsuario.enmascararCelular(celular));` | `usuario/aplicacion/LimiteIntentosLogin.java:81-82` |
| Logback (configuración) | `<appender name="ARCHIVO" class="ch.qos.logback.core.rolling.RollingFileAppender">` | `src/main/resources/logback-spring.xml:37` |

Rutas relativas a `src/main/java/pe/agroconecta/`. En total, 21 archivos importan Guava y 21 importan Commons Lang3; POI se usa en 3 clases (`LectorArchivoPrecios`, `ExportadorExcelVentas`, `ExportadorExcelPanel`).

---

## 3. Diapositiva 10 — dependencias del `pom.xml` con versión resuelta

Padre: `spring-boot-starter-parent` **4.1.1** · Java **21** · empaquetado WAR. Versiones obtenidas con `./mvnw -o dependency:list`.

**Seguridad**
| Dependencia (pom) | Versión | Incluye (transitivas relevantes) |
|---|---|---|
| spring-boot-starter-security | 4.1.1 | Spring Security 7.1.1 |
| spring-boot-starter-security-oauth2-resource-server | 4.1.1 | Nimbus JOSE + JWT 10.9.1 |
| spring-boot-starter-validation | 4.1.1 | Jakarta Validation 3.1.1, Hibernate Validator 9.1.3.Final |

**Base de datos**
| Dependencia | Versión | Notas |
|---|---|---|
| spring-boot-starter-data-jpa | 4.1.1 | Hibernate ORM 7.4.5.Final |
| spring-boot-starter-flyway | 4.1.1 | Flyway Core 12.4.0 |
| flyway-mysql | 12.4.0 | |
| mysql-connector-j (runtime) | 9.7.0 | Driver para MySQL y MariaDB |

**Pruebas y calidad** (scope `test`, salvo el plugin)
| Dependencia | Versión | Notas |
|---|---|---|
| spring-boot-starter-actuator-test | 4.1.1 | |
| spring-boot-starter-data-jpa-test | 4.1.1 | |
| spring-boot-starter-flyway-test | 4.1.1 | |
| spring-boot-starter-security-oauth2-resource-server-test | 4.1.1 | |
| spring-boot-starter-security-test | 4.1.1 | |
| spring-boot-starter-validation-test | 4.1.1 | |
| spring-boot-starter-webmvc-test | 4.1.1 | JUnit Jupiter **6.0.3**, Mockito 5.23.0, AssertJ 3.27.7 |
| archunit-junit5 | 1.5.1 | |
| jacoco-maven-plugin (plugin) | 0.8.15 | `check` con 70 % de líneas por paquete `aplicacion` |
| maven-surefire-plugin (plugin, heredado) | 3.5.6 | |

**Documentación**
| Dependencia | Versión | Notas |
|---|---|---|
| springdoc-openapi-starter-webmvc-ui | 3.1.1 | Swagger UI 5.32.14; desactivado en `prod` |
| maven-javadoc-plugin (plugin, heredado) | 3.12.0 | No está declarado en el `pom.xml`; versión del padre |

**Otras**
| Dependencia | Versión | Notas |
|---|---|---|
| spring-boot-starter-webmvc | 4.1.1 | Spring Framework 7.0.9 |
| spring-boot-starter-actuator | 4.1.1 | |
| spring-boot-starter-tomcat (provided) | 4.1.1 | Tomcat 11.0.24 |
| spring-boot-devtools (runtime, opcional) | 4.1.1 | |
| guava | 33.7.1-jre | Librería del curso |
| commons-lang3 | 3.20.0 | Librería del curso; versión del BOM de Spring Boot |
| poi-ooxml | 5.5.1 | Librería del curso |
| logback-classic (transitiva de los starters) | 1.5.38 | Librería del curso; no se declara en el pom |
| spring-boot-maven-plugin (plugin) | 4.1.1 | |

**Fuera del pom (interfaz):** Bootstrap **5.3.8** e íconos Lucide, servidos desde `src/main/resources/static/vendor/`.

---

## 4. Diapositiva 11 — último commit de cada rama `feature/rf-*` (orden cronológico)

| # | Rama | Último commit | Hash |
|---|---|---|---|
| 1 | feature/rf-08-registro-login | 27/09/2026 15:19 | `88e8882` |
| 2 | feature/rf-01-publicar-cosecha | 27/09/2026 16:39 | `9e86625` |
| 3 | feature/rf-09-moderacion | 27/09/2026 18:54 | `e041ebb` |
| 4 | feature/rf-02-precios | 28/09/2026 20:29 | `ab833c1` |
| 5 | feature/rf-03-busqueda | 06/10/2026 16:15 | `7adc1a6` |
| 6 | feature/rf-04-mensajeria | 06/10/2026 16:41 | `e483dfe` |
| 7 | feature/rf-06-ventas | 06/10/2026 19:13 | `2b414b2` |
| 8 | feature/rf-05-calificaciones | 06/10/2026 19:34 | `42d1c61` |
| 9 | feature/rf-07-avisos | 09/10/2026 10:23 | `3249f23` |
| 10 | feature/rf-10-venta-agrupada | 13/10/2026 14:04 | `7e79458` |
| 11 | feature/rf-09-administracion | 14/10/2026 12:56 | `84f0691` |

El orden del guion es **correcto**. Las ramas locales coinciden con `origin/feature/*` (mismo hash). Cada rama contiene a la anterior (ramas apiladas) y ninguna se fusionó en `develop` (ver §1.1, diap. 13). RF-11 no tiene rama propia: va dentro de `feature/rf-06-ventas` (su último commit dice "pantallas de RF-06 y RF-11").

---

## 5. Diapositivas 2 y 7 — resultado del último `./mvnw -B clean verify`

Última ejecución disponible: **24/10/2026, 00:40**, sobre el código de `3a1dfa7` (`docs/07-calidad/evidencia-pruebas.md:3`; marcas de tiempo de `target/`). Entre `3a1dfa7` y HEAD **no cambió nada en `src/` ni en `pom.xml`** (`git diff --quiet 3a1dfa7 HEAD -- src pom.xml`), así que las cifras valen para HEAD. No se volvió a ejecutar en esta revisión.

| Indicador | Valor |
|---|---|
| Pruebas ejecutadas | **249** (43 clases) |
| Unitarias | 135 (26 clases) |
| Integración (MariaDB real, perfil `test`) | 109 (16 clases) |
| Arquitectura (ArchUnit) | 5 (1 clase) |
| Fallas / errores / omitidas | **0 / 0 / 0** |
| Cobertura de líneas (proyecto) | **96,1 %** (2814/2928) |
| Cobertura de ramas (proyecto) | 83,7 % |
| Cobertura de líneas por paquete `aplicacion` | administracion 99,5 · catalogo 100 · mensajeria 97,6 · notificacion 100 · precio 95,4 · publicacion 99,3 · usuario 99,2 · venta 98,7 · ventaagrupada 98,1 |
| `jacoco:check` (≥ 70 % por paquete `aplicacion`) | Superado (BUILD SUCCESS) |

Si se quiere la cifra "en vivo" o desde CI: el último *push* a GitHub conocido localmente es anterior a estos commits, por lo que **una ejecución de GitHub Actions con 249 pruebas: No verificado**.

---

## 6. Diapositiva 12 — viabilidad de la demo

### 6.1 Paso a paso

| Paso | Acción del guion | ¿Se puede? | Evidencia / observación |
|---|---|---|---|
| 1 | Portada con precios públicos | **Sí** | `index.html` + `js/portada.js:39` → `GET /precios/vigentes`, público en `SeguridadConfig.java:56-57` |
| 2 | Agricultor publica una cosecha en 3 pasos | **Sí** | `pages/publicacion/paso1.html`, `paso2.html`, `paso3.html` + `confirmar.html` (`js/publicacion/confirmar.js:45`) y `publicada.html` |
| 3 | Comprador busca con filtros y abre el detalle | **Sí** | `pages/publicacion/buscar.html` (`buscar.js:113`), `detalle.html` (`detalle.js:29`) |
| 4 | Comprador envía mensaje y "propone el acuerdo" | **Sí, con matiz** | Chat: `chat.js:165`; acuerdo: `pages/venta/acuerdo.html` (`acuerdo.js:130`, `POST /ventas`). La venta **nace ACORDADA**, no como propuesta (`Transaccion.java:80,100`). Cualquiera de las dos partes puede registrarla (`VentaService.java:128-129`) |
| 5 | Agricultor ve el aviso, **acepta** y marca "Recogido" | **Parcial** | El aviso "Venta acordada" sí llega (`VentaService.java:151`; `pages/notificacion/avisos.html`). **No hay botón "Aceptar" en una venta directa**: `ACEPTAR` solo existe en estado `PROPUESTA`, que es de la venta agrupada RF-10 (`VentaService.java:365-369`). "Ya recogieron la carga" sí existe (`estados.js:20`) |
| 6 | Comprador confirma recepción y califica | **Sí, con un clic extra** | Desde `RECOGIDO` la única acción es "La carga va en camino" (`VentaService.java:370`); "Confirmar que recibí" aparece en `EN_CAMINO`/`ENTREGADO` (`:371`) o directamente desde `ACORDADA` (`:365-366`). Calificar exige `COMPLETADA` (`CalificacionService.java:101`; `venta.js:195`) |
| 7 | Administrador: panel y exportar a Excel | **Sí** | `pages/administracion/panel.html`; `panel.js:143` y `panel.js:161` (`AgroConecta_Panel.xlsx`) |

**Ajuste sugerido del guion:** paso 5 = "Ver el aviso de venta acordada y marcar *Ya recogieron la carga*"; paso 6 = "Marcar *La carga va en camino*, confirmar recepción y calificar" (o saltarse "Recogido" y confirmar directamente desde `ACORDADA`).

**Preparación — sesiones:** confirmado que la sesión está en `sessionStorage` (`sesion.js:14,27`). Abrir cada actor en una pestaña nueva escribiendo la URL; "Duplicar pestaña" copia el `sessionStorage` de la original.

### 6.2 Datos que necesita la demo (estado de la base local `agroconecta` al 26/10/2026)

| Dato | Estado actual | ¿Falta? |
|---|---|---|
| Usuarios por rol | 1 ADMINISTRADOR, 2 AGRICULTOR, 1 COMPRADOR (todos ACTIVO) | Alcanza para un usuario por rol. El administrador lo crea la app al arrancar con `ADMIN_*` (`InicializadorAdministrador.java:51-53`); no hay script de datos de demo en el repositorio (V2 solo carga catálogos) |
| Cosecha por vencer | 1 publicación (id 4) vence el **27/10/2026** | **Sí, falta para el día de la demo**: mañana pasará a vencida. Hace falta una con `fecha_limite` = día de la demo + 1 o + 3. El aviso `POR_VENCER` lo genera solo el evento `ev_tareas_diarias` a las 06:00 (`V1__esquema_inicial.sql:1037-1067`; `event_scheduler` = ON), así que debe existir desde el día anterior |
| Venta acordada | 1 ACORDADA, 1 COMPLETADA, 1 RECHAZADA | Existe una para respaldo; la demo crea la suya en el paso 4. Para el paso 4 hace falta una publicación PUBLICADA con kilos disponibles (hay 3) |
| Lote para RF-10 | `v_lotes_agrupados` = **0 filas** | **Sí, falta.** La vista exige ≥ 2 agricultores distintos con la misma variedad y distrito publicados (`V1__esquema_inicial.sql:1165-1167`). Las publicaciones 2 y 3 (variedad 10, distrito 9) son del mismo agricultor: el segundo agricultor debe publicar esa misma variedad en ese distrito |
| Precios de mercado | 37 registros, 8 vigentes; el más reciente es del **28/09/2026** | Conviene cargar precios recientes para que la portada (paso 1) no muestre datos de hace un mes |
| Panel con datos (paso 7) | 3 conversaciones, 2 calificaciones, 1 propuesta conjunta | Suficiente para que el panel no salga vacío; más ventas COMPLETADAS lo harían más vistoso |
| Fecha de la demo | — | **No verificado** (el guion dice semana 12; sin fecha concreta) |

---

## Resumen de correcciones

1. **Pruebas:** 249 (no 238): 135 unitarias + 109 de integración + 5 ArchUnit; 0 fallas.
2. **Cobertura:** 96,1 % de líneas (no 96,2 %).
3. **TDD:** commits `fd95723` (`test:`) y `28e0576` (`feat:`). Es el único par `test:` → `feat:` del historial, así que no conviene presentarlo como práctica sistemática.
4. **JUnit:** la versión es 6.0.3, no JUnit 5.
5. **MariaDB:** 11.8 en CI y 13.0.2 en local; producción usa MySQL.
6. **Diap. 13 (GitHub):** no hay merges ni *pull requests* fusionados en el repositorio local; `develop` y `main` siguen en el commit inicial. Las ramas están apiladas, no existe la etiqueta `apf3` y los últimos 17 commits no se han subido. Los PR en GitHub quedan como No verificado.
7. **POI:** la protección contra *zip bombs* no está configurada en el código.
8. **Demo:** la venta directa no tiene botón "Aceptar" (solo existe en RF-10). Después de "Recogido" hay que marcar "En camino" antes de confirmar la recepción.
9. **Datos de la demo:** faltan una cosecha que venza en la fecha de la demo, un lote para RF-10 (segundo agricultor con la misma variedad y distrito) y precios recientes.
10. **Diseño:** las fuentes y la paleta del guion no coinciden con la guía de estilos del proyecto (Work Sans y Cormorant Garamond; #306D35, #F9A825).
11. **No verificado:** el nombre del docente, el mapeo a OWASP y la regla de "0 lógica de negocio en los controladores".
