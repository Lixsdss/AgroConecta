# Seguridad de AgroConecta

Medidas de seguridad implementadas en el código (estado al 24/10/2026, rama `chore/apf3-cierre`), cómo se verificaron y los riesgos que quedan con su plan de mitigación. Las rutas de archivo son relativas a `src/main/java/pe/agroconecta/` salvo que se indique otra cosa.

## 1. Medidas implementadas

### 1.1 Autenticación y sesión

| Medida | Implementación | Dónde | Verificación |
|---|---|---|---|
| Inicio de sesión con celular y contraseña | Contraseñas con **BCrypt** (`BCryptPasswordEncoder`); el mensaje de error no dice qué dato falló; se compara un hash de relleno aunque el celular no exista, para no revelarlo por el tiempo de respuesta | `compartido/seguridad/JwtConfig.java`, `usuario/aplicacion/SesionService.java` | `SesionServiceTest`, `AuthIntegracionTest` |
| Token JWT HS256 | OAuth2 Resource Server de Spring Security con `NimbusJwtEncoder`/`NimbusJwtDecoder` (sin filtro propio ni JJWT). Se validan la firma, la vigencia (12 h) y el emisor `agroconecta`. Claims: `sub` = id del usuario, `roles` | `compartido/seguridad/JwtConfig.java`, `compartido/seguridad/SeguridadConfig.java`, `usuario/aplicacion/EmisorToken.java` | `EmisorTokenTest`, `AuthIntegracionTest`; payload comprobado en ejecución |
| Clave JWT fuera del código | `agroconecta.jwt.secreto=${JWT_SECRET}` sin valor por defecto; al arrancar se exige que no esté vacía y que mida ≥ 32 bytes | `src/main/resources/application.properties`, `JwtConfig.java` | La aplicación no arranca sin la clave |
| Usuario ACTIVO en cada petición | Validador JWT `ValidadorUsuarioActivo` → interfaz `VerificadorEstadoUsuario` (en `compartido`) → `VerificadorEstadoUsuarioBd` (en `usuario`). Un usuario SUSPENDIDO o INACTIVO recibe 401 aunque su token siga vigente | `compartido/seguridad/ValidadorUsuarioActivo.java`, `usuario/aplicacion/VerificadorEstadoUsuarioBd.java` | `AuthIntegracionTest.tokenDeUnUsuarioQueDejoDeEstarActivoDevuelve401` |
| **Límite de intentos de inicio de sesión** | Tras **5 intentos fallidos** con el mismo celular (normalizado), los siguientes responden **429** con `ErrorResponse` ("Demasiados intentos fallidos. Espere 15 minutos e intente de nuevo.") hasta que pasan **15 minutos desde el primer fallo**; un inicio de sesión correcto reinicia el contador. Guava Cache en memoria (`expireAfterWrite`, tope de 10 000 celulares) con un `Ticker` basado en el `Clock` inyectable. El bloqueo se registra con el celular enmascarado | `usuario/aplicacion/LimiteIntentosLogin.java`, `compartido/error/DemasiadosIntentosException.java`, `ManejadorGlobalExcepciones.java` | Desarrollado con TDD: `LimiteIntentosLoginIntegracionTest` (en rojo en `fd95723`, en verde en `28e0576`) y `LimiteIntentosLoginTest`. Comprobado en ejecución con el perfil `prod`: intentos 1–5 → 401 y el 6.º → 429 |
| API sin estado | `SessionCreationPolicy.STATELESS`; CSRF, `httpBasic`, `formLogin` y `logout` desactivados (el token viaja en la cabecera `Authorization`, no en una cookie) | `SeguridadConfig.java` | — |
| Token en el navegador | `sessionStorage` (no `localStorage`); al recibir 401 se cierra la sesión | `src/main/resources/static/js/comun/sesion.js`, `static/js/api.js` | Revisión de código |

### 1.2 Autorización

| Medida | Implementación | Dónde |
|---|---|---|
| Endpoints públicos acotados | Solo `POST /api/v1/auth/registro` y `/login`; `GET` de `/catalogo/**`, `/publicaciones/**`, `/precios/**` y `/reputaciones/**`; archivos estáticos; `/actuator/health`. Todo lo demás exige JWT (`anyRequest().authenticated()`) | `SeguridadConfig.java` |
| Roles | `@EnableMethodSecurity` + `@PreAuthorize` en 14 de los 17 controladores. `AuthController` y `CatalogoController` son públicos por diseño; `NotificacionController` no tiene `@PreAuthorize`, pero exige token y cada usuario solo ve sus propios avisos | `*/api/*Controller.java` |
| Dueño del recurso | Los servicios comprueban que quien actúa sea parte de la conversación, la venta, la propuesta o la publicación (403/404) y registran `Acceso negado ...` | p. ej. `mensajeria/aplicacion/ConversacionService.java`, `venta/aplicacion/VentaService.java`, `publicacion/aplicacion/MisCosechasService.java` |
| Reglas en la base de datos | Los triggers comprueban el rol y la parte (solo un ADMINISTRADOR modera, carga precios o suspende; solo las partes registran reclamos o calificaciones) como última barrera | `docs/04-diseno/base-datos/modelo-bd.sql` |

Verificado en ejecución (24/10): sin token, `/api/v1/ventas` → 401; con token de ADMINISTRADOR, `/api/v1/ventas` → 403 y `/api/v1/admin/panel` → 200.

### 1.3 Validación de entradas y archivos

| Medida | Implementación | Dónde |
|---|---|---|
| Bean Validation | Los `record` `XxxRequest` validan formato y obligatoriedad (celular de 9 dígitos que empieza por 9, longitudes, rangos) con `@Valid`; errores 400 por campo | `*/api/*Request.java`, `mensajeria/api/ValidadorMensajeCoherente.java` |
| Reglas de negocio | Los servicios rechazan con `ReglaNegocioException` (422) y mensajes en español; la base de datos es la última barrera | `*/aplicacion/*Service.java` |
| Importación de precios con Apache POI | Máximo **1 MB** por archivo y por petición (`spring.servlet.multipart.*`); solo `.xlsx` o `.csv`; máximo **500 filas**; un Excel corrupto da un error controlado; el exceso de tamaño responde con `ErrorResponse`. POI mantiene su protección por defecto contra *zip bombs* (`ZipSecureFile`) | `src/main/resources/application.properties`, `precio/infraestructura/LectorArchivoPrecios.java` |
| Salida en el navegador | Los datos del usuario se escriben con `textContent`, nunca con `innerHTML` | `src/main/resources/static/js/**` |

### 1.4 Manejo de errores

- `compartido/error/ManejadorGlobalExcepciones.java` (extiende `ResponseEntityExceptionHandler`) responde siempre con el mismo formato `ErrorResponse { timestamp, estado, error, mensaje, ruta, errores[] }`: 400, 401, 403, 404, 409, 422, **429** y 500.
- Nunca se exponen detalles de SQL; los errores 500 llevan un mensaje genérico y el detalle solo va al log.
- 401 y 403 de Spring Security usan el mismo formato (`compartido/seguridad/ManejadorErroresSeguridad.java`).

### 1.5 Exposición de la aplicación

| Medida | Implementación | Verificación |
|---|---|---|
| Actuator mínimo | Solo `/actuator/health` (`management.endpoints.web.exposure.include=health`) | `/actuator/env` → 401 |
| **Swagger desactivado en producción** | `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false` en `application-prod.properties`; en `dev` sigue disponible | WAR con perfil `prod`: `/swagger-ui.html` y `/v3/api-docs` → 404. En `dev`: 200 |
| CORS | Solo en el perfil `dev`, con orígenes locales configurables | `compartido/config/CorsConfig.java` |

### 1.6 Secretos y registros (logs)

- `DB_PASSWORD` y `JWT_SECRET` vienen de variables de entorno sin valor por defecto; en CI, de *GitHub Secrets*. En local se guardan en `.env`, ignorado por Git (`*.env`).
- Revisión del historial completo (`git log --all -p`, 24/10/2026): los valores reales de `JWT_SECRET` y `ADMIN_PASSWORD` no aparecen en ningún commit, y nunca se versionó un `.env`, `*.pem`, `*.key`, `*.jks`, `*.p12` ni `application-local.properties`.
- Logback (`src/main/resources/logback-spring.xml`): solo consola en `dev` y `test`; en `prod`, consola y archivo con rotación por tamaño y por día (30 días, tope de 1 GB). La carpeta `logs/` está fuera de Git.
- Nunca se registran contraseñas, hashes, tokens ni el texto de los mensajes; el celular se registra enmascarado (`ReglasUsuario.enmascararCelular`, Ley N.° 29733). Comprobado en ejecución: tras inicios de sesión correctos, fallidos y bloqueados, el log tiene 0 apariciones de la contraseña, del celular completo y de tokens JWT.
- La contraseña temporal que genera el administrador (10 letras y dígitos con `SecureRandom`) se devuelve una sola vez y no se registra.

## 2. Riesgos pendientes y plan de mitigación

| # | Riesgo | Impacto | Mitigación propuesta | Prioridad |
|---|---|---|---|---|
| 1 | **Sin HTTPS todavía**: el destino de despliegue no está decidido (ADR-005 pendiente). Sin TLS, el token y la contraseña viajarían en claro | Alto | Desplegar detrás de un proxy (Nginx/Caddy) con certificado gratuito de Let's Encrypt y activar HSTS. Es requisito 7 de ADR-005 | Alta (Unidad 4) |
| 2 | **Límite de intentos en memoria**: los contadores se pierden al reiniciar y no se comparten si hay varias instancias; la ventana es fija (15 min desde el primer fallo) y, con peticiones simultáneas, el sexto intento podría colarse | Medio | Suficiente para una sola instancia (el escenario del curso). Si se escala, mover los contadores a una tabla (requiere una migración aprobada). Añadir un límite por IP en el proxy | Media |
| 3 | **Bloqueo intencional de una cuenta**: quien conozca un celular puede bloquearlo 15 minutos con 5 intentos | Bajo | El bloqueo es temporal y no revela si el celular existe. Si ocurre, agregar un límite por IP en el proxy | Baja |
| 4 | **Sin límite en el registro y en otros endpoints públicos** (se pueden crear cuentas o hacer búsquedas de forma masiva) | Medio | Límite de peticiones por IP en el proxy de despliegue (`limit_req` de Nginx), sin costo | Media |
| 5 | **Cabeceras de seguridad por defecto**: no hay una *Content-Security-Policy* explícita | Medio | Agregar en `SeguridadConfig` `headers().contentSecurityPolicy("default-src 'self'")` (los recursos ya son locales: Bootstrap, fuentes e íconos), `Referrer-Policy` y `Permissions-Policy`; HSTS al tener HTTPS | Media |
| 6 | **Token de 12 horas sin revocación**: si se roba un token, vale hasta que vence (salvo que el administrador suspenda o desactive la cuenta, que se verifica en cada petición) | Medio | Mantener la verificación de usuario ACTIVO; evaluar reducir la vigencia o agregar un token de refresco | Baja |
| 7 | **Contraseña local de la base de datos débil** (solo en el equipo de desarrollo) | Bajo | Cambiarla por una aleatoria larga y actualizar `.env`; en producción, generarla aleatoria y guardarla solo como variable de entorno | Baja |
| 8 | **Dependencias con vulnerabilidades conocidas**: no hay un análisis automático | Medio | Agregar a CI OWASP Dependency-Check o activar Dependabot de GitHub (ambos gratuitos), con la aprobación previa que exige CLAUDE.md §14 para cambiar `ci.yml` | Media |
| 9 | **Sin pruebas de seguridad dinámicas** (por ejemplo, escaneo del sitio desplegado) | Medio | Ejecutar OWASP ZAP (gratuito) en modo *baseline* contra el despliegue de la Unidad 4 y registrar el informe en esta carpeta | Media (Unidad 4) |
