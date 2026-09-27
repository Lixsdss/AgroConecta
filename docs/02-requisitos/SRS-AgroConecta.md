# SRS — AgroConecta
## Especificación de Requisitos del Sistema

**Proyecto:** Integrador I: Sistemas – Software (100000I58N), Ciclo VI, 2026-2  
**Institución:** Universidad Tecnológica del Perú (Piura, Perú)  
**Equipo:** 6 personas (4 programadores, 2 documentadores)  
**Versión:** 1.2  
**Fecha:** 26 de septiembre de 2026  
**Fuente de verdad:** Este documento + `docs/04-diseno/base-datos/modelo-bd.sql` (el script manda sobre cualquier nombre de tabla, columna, estado o trigger)

**Cambios v1.1:** venta agrupada iniciada por el comprador (RF-10); transportista como registro de datos; moderación posterior; historial exportable a Excel (sin PDF); inicio de sesión con teléfono; estados, triggers y evento alineados con `modelo-bd.sql`.

**Cambios v1.2:** RF-06 ampliado a "Registro e historial de transacciones" (proponer, aceptar, rechazar, cancelar y confirmar recepción); arquitectura de monolito modular con módulo `catalogo`; RF-11 dentro del módulo `transacciones`; JWT con OAuth2 Resource Server; cambio de contraseña por el propio usuario; restricción de costo cero; formato de error unificado; orden de implementación corregido según dependencias; rutas de `docs/` actualizadas; Ley N.° 29733 en la normativa; notificaciones en la aplicación generadas por los módulos (§2.1); 12 casos de uso (UC-01 a UC-12).

---

## 1. Contexto y propósito

### 1.1 Descripción del problema
Pequeños agricultores de Piura (Perú) dependen de intermediarios tradicionales para comercializar sus cosechas, lo que reduce márgenes de ganancia, limita el acceso a compradores e incrementa la pérdida por perecibilidad. La plataforma **AgroConecta** busca conectar directamente a agricultores con compradores (mercados mayoristas, restaurantes, hoteles, procesadoras) para reducir esa dependencia, ampliar mercado y mejorar competitividad.

### 1.2 Alcance del producto
**Plataforma web de dos lados (two-sided marketplace):**
- Actores: Agricultor, Comprador, Administrador.
- Funcionalidad principal: publicar cosechas, consultar precios, buscar productos, negociar, registrar acuerdos, seguir entregas, calificar, recibir alertas de vencimiento, venta agrupada.
- **Sin pasarela de pagos** — las transacciones solo registran el acuerdo comercial; el pago ocurre fuera de la plataforma.
- **Sin modelo de suscripción** en el MVP — documentado como extensión futura (RF-12).
- **Sin publicación delegada** — el agricultor es siempre el único publicador de su cosecha.
- **Sin envío de correos, SMS ni push** — las notificaciones se muestran dentro de la aplicación.
- **Sin comprobantes PDF** — los listados se exportan a Excel (Apache POI).
- **Costo cero** — el proyecto no genera gastos (ver §4.1).

### 1.3 Actores del sistema

| Actor | Descripción | Responsabilidades | Volumen esperado |
|---|---|---|---|
| **Agricultor** | Pequeño productor agrícola | Registrarse, publicar cosechas, responder consultas, negociar, registrar y aceptar propuestas, aportar a propuestas grupales, registrar eventos de entrega, calificar | ~250 activos en piloto |
| **Comprador** | Empresa compradora (mayorista, restaurante, hotel, procesadora) | Registrarse, buscar productos, contactar, registrar y aceptar propuestas, crear propuestas grupales, registrar eventos de entrega, confirmar recepción, calificar | ~50 activos en piloto |
| **Administrador** | Personal de plataforma | Gestionar usuarios, reiniciar contraseñas, moderar, cargar precios de referencia, analizar reportes | 2–3 personas |

**No son actores:**
- **Visitante sin sesión:** solo consulta publicaciones, precios y catálogo; para cualquier otra acción debe registrarse.
- **Transportista:** es un registro de datos (`transportistas`) que crean el agricultor o el comprador; no tiene cuenta ni interactúa con el sistema.

---

## 2. Requisitos funcionales (RF)

Alcance confirmado: **RF-01 a RF-11** (11 requisitos). RF-12 documentado como extensión futura sin código.

| RF | Nombre | Descripción | Módulo | Tablas / vistas | Prioridad |
|---|---|---|---|---|---|
| **RF-01** | Publicación de cosecha | El agricultor publica en máx. 3 pasos: variedad (y con ella el cultivo), cantidad y unidad; precio referencial y distrito; fecha de disponibilidad y fecha límite. Si no indica fecha límite, el trigger la calcula como `fecha_disponible + cultivos.vida_util_dias`. La publicación nace ACTIVA (moderación posterior). Solo un agricultor con usuario ACTIVO puede publicar (validado en el service). Los catálogos (cultivos, variedades, unidades, ubicaciones) los provee el módulo `catalogo`. | `publicaciones` | publicaciones (+ catálogos) | MUST |
| **RF-02** | Precios de mercado | Panel público de precios por cultivo, provincia y unidad, con histórico por fecha. Combina referencias externas (cargadas por el admin) y precios de la plataforma (transacciones COMPLETADA). El admin carga por formulario o archivo Excel/CSV (Apache POI). Cada carga queda registrada con su administrador, fuente, método y fecha. | `precios` | cargas_precio, precios_mercado, `v_tablero_precios`, `v_precio_plataforma` | MUST |
| **RF-03** | Búsqueda multi-criterio | Cualquier persona (con o sin sesión) busca publicaciones ACTIVA con cantidad disponible, filtrando por categoría, cultivo, variedad, provincia/distrito y rango de fecha de disponibilidad (todos opcionales). Resultado paginado (20 ítems/página). | `publicaciones` | `v_publicacion_disponible`, `v_reputacion_usuario` | MUST |
| **RF-04** | Mensajería directa | Chat agricultor ↔ comprador dentro de la plataforma, una conversación por publicación y comprador, con consulta periódica. Incluye el botón "Contactar por WhatsApp", que registra un lead. Solo los dos participantes pueden escribir y solo en conversaciones ABIERTA. | `mensajeria` | conversaciones, mensajes, leads_whatsapp | MUST |
| **RF-05** | Calificación de transacción | Agricultor y comprador se califican 1–5 estrellas con comentario opcional (máx. 500 caracteres), una vez por transacción y solo si está COMPLETADA. La reputación es el promedio de las calificaciones recibidas. | `calificaciones` | calificaciones, `v_reputacion_usuario` | SHOULD |
| **RF-06** | Registro e historial de transacciones | Una de las partes (agricultor o comprador) registra una propuesta sobre una publicación ACTIVA (cantidad y precio) → PROPUESTA. La otra parte la acepta (ACORDADA) o la rechaza (RECHAZADA). Cualquiera de las partes puede cancelar una PROPUESTA o ACORDADA (CANCELADA). Sin transporte, el comprador confirma la recepción de una ACORDADA → COMPLETADA. El usuario ve sus transacciones filtradas por estado, fecha y contraparte, paginadas, y las exporta a Excel (Apache POI). | `transacciones` | transacciones, `v_historial_transaccion` | MUST |
| **RF-07** | Alertas de vencimiento | El evento `ev_tareas_diarias` crea notificaciones en la app para el agricultor a 3 días y a 1 día de la fecha límite, y pasa a VENCIDA las publicaciones vencidas. El módulo `notificaciones` las lista y las marca como leídas. Acciones rápidas del agricultor (en `publicaciones`): bajar precio, extender la fecha límite (una VENCIDA vuelve a ACTIVA), marcar como retirada. | `notificaciones`, `publicaciones` | notificaciones, evento `ev_tareas_diarias` | MUST |
| **RF-08** | Registro y gestión de usuarios | Registro por rol (Agricultor o Comprador) con teléfono (único, obligatorio) y contraseña BCrypt; email opcional sin verificación. El inicio de sesión es con teléfono + contraseña. Perfil editable y cambio de la propia contraseña (requiere la contraseña actual). El admin crea administradores, cambia estados y reinicia contraseñas. | `usuarios` | usuarios, agricultores, compradores, roles | MUST |
| **RF-09** | Panel administrativo | El admin lista y busca usuarios (por rol, estado, nombre, teléfono), los suspende/reactiva, bloquea/desbloquea publicaciones o advierte (moderación posterior registrada en `moderaciones`), ve métricas (transacciones por mes, usuarios activos, cultivos más publicados, embudo de conversión) y exporta a Excel. | `administracion` | usuarios, publicaciones, transacciones, moderaciones, `v_conversion_publicacion` | MUST |
| **RF-10** | Venta agrupada | Un **comprador** crea una propuesta grupal (variedad, provincia, unidad, cantidad requerida, precio unitario ofrecido, condiciones, fecha límite de respuesta). Varios **agricultores** con publicaciones ACTIVA que coinciden en variedad, provincia y unidad aportan: cada aporte es una transacción PROPUESTA vinculada a la propuesta grupal (creada a través del módulo `transacciones`). El comprador acepta o rechaza cada aporte. Al vencer la fecha límite, el evento cancela los aportes pendientes y cierra la propuesta. `v_oferta_consolidada` ayuda al comprador a ver dónde hay volumen. | `ventaagrupada` | propuestas_grupales, transacciones, `v_oferta_consolidada` | SHOULD |
| **RF-11** | Seguimiento de entrega | Para una transacción ACORDADA con transporte, el agricultor o el comprador registra la entrega (transportista y placa) y los eventos RECOGIDO → EN_CAMINO → ENTREGADO. Tras ENTREGADO, el comprador confirma la recepción y la transacción pasa a COMPLETADA. Sin transporte no se registra entrega y la transacción se completa según RF-06. | `transacciones` | transportistas, entregas, eventos_entrega | SHOULD |

### 2.1 Detalles de RF no obviamente lineales

**RF-02 — Carga de precios:**
- Formulario: cultivo, provincia, unidad, fecha, precio mínimo, precio máximo, precio promedio y fuente → una fila en `cargas_precio` (método FORMULARIO) y una en `precios_mercado`.
- Archivo Excel/CSV con cabecera `cultivo, provincia, unidad, fecha, precio_minimo, precio_maximo, precio_promedio` → una fila en `cargas_precio` (método ARCHIVO, con `nombre_archivo`) y N filas en `precios_mercado`. Las filas válidas se importan y las inválidas se reportan.
- No se admiten dos precios para el mismo cultivo, provincia, unidad y fecha (UK); una fila repetida se rechaza como inválida.
- El histórico es la serie de `precios_mercado` por fecha; no se guardan valores anteriores de una misma fila.
- `v_precio_plataforma` agrega por día los precios de transacciones COMPLETADA; `v_tablero_precios` une ambas fuentes. El service filtra el período consultado (p. ej. últimos 30 días).

**RF-04 — Mensajería y WhatsApp:**
- Chat interno: agricultor responde al comprador y viceversa, dentro de la conversación de esa publicación.
- Lead de WhatsApp: el comprador pulsa "Contactar por WhatsApp"; el sistema registra en `leads_whatsapp` la publicación, el comprador y la fecha, y abre el enlace con el teléfono del agricultor (`usuarios.telefono`).
- `trg_mensaje_bi` valida que el remitente sea el comprador de la conversación o el agricultor de la publicación.

**RF-06 — Registro de transacciones:**
- Proponer: cualquiera de las partes registra cantidad y precio sobre una publicación ACTIVA; `id_proponente` identifica a quien propone. `trg_transaccion_bi` valida estado inicial, publicación ACTIVA, proponente y cantidad disponible.
- Aceptar o rechazar: solo la contraparte del proponente. Al aceptar, el service bloquea la publicación (`PESSIMISTIC_WRITE`) para evitar sobreventa; `trg_transaccion_bu` valida la transición y fija `fecha_acuerdo`; `trg_transaccion_au` pasa la publicación a VENDIDA si se agota la cantidad.
- Cancelar: cualquiera de las partes, desde PROPUESTA o ACORDADA; cancelar un acuerdo reactiva la publicación (`trg_transaccion_au`).
- Confirmar recepción sin transporte: el comprador pasa una ACORDADA sin entrega registrada a COMPLETADA. Con entrega, la confirmación sigue RF-11.
- Historial: `v_historial_transaccion` con filtros por estado, fecha y contraparte; exportación a Excel.

**RF-07 — Alertas y evento diario:**
- `ev_tareas_diarias` se ejecuta cada día a las 06:00 (hora del servidor, America/Lima).
- Publicaciones ACTIVA con `fecha_limite = hoy + 3` → notificación VENCIMIENTO "Tu cosecha vence en 3 dias".
- Publicaciones ACTIVA con `fecha_limite = hoy + 1` → notificación VENCIMIENTO "Tu cosecha vence en 24 horas".
- No se repite la alerta de una publicación el mismo día.
- Publicaciones ACTIVA con `fecha_limite < hoy` → estado VENCIDA.
- Propuestas grupales ABIERTA con `fecha_limite_respuesta < hoy` → sus transacciones PROPUESTA pasan a CANCELADA y la propuesta a CERRADA.
- **No usar `@Scheduled` ni eventos de aplicación para esto** — el evento de base de datos es la fuente de verdad.

**Notificaciones en la aplicación (transversal):**
- VENCIMIENTO: las crea `ev_tareas_diarias` (RF-07).
- MENSAJE (RF-04), PROPUESTA (RF-06, RF-10), TRANSACCION (RF-06), ENTREGA (RF-11), CALIFICACION (RF-05) y MODERACION (RF-09): las crea el módulo que ejecuta la acción, llamando a la `api` del módulo `notificaciones` dentro de la misma transacción.
- Se usan solo los tipos definidos en `modelo-bd.sql`. El frontend las consulta periódicamente; no hay correo, SMS ni push.

**RF-08 — Consistencia usuario/subtipo:**
- El service crea `usuarios` y la fila del subtipo (`agricultores` o `compradores`) en la misma transacción.
- `trg_agricultor_bi` (BEFORE INSERT en `agricultores`) rechaza la fila si el usuario no tiene rol AGRICULTOR: "El usuario no tiene rol AGRICULTOR".
- `trg_comprador_bi` (BEFORE INSERT en `compradores`) hace lo mismo con el rol COMPRADOR.
- El rol no se cambia tras el registro. El service **también** valida todo esto (defensa en profundidad).
- Cambio de contraseña propio: requiere la contraseña actual; el reinicio por el administrador no la requiere.

**RF-10 — Venta agrupada:**
- El comprador crea la propuesta grupal (estado ABIERTA).
- Un agricultor con una publicación ACTIVA de la misma variedad, provincia y unidad aporta: se crea una transacción PROPUESTA con `id_propuesta_grupal`, `id_comprador` = comprador de la propuesta, `id_proponente` = agricultor, la cantidad aportada y el precio ofrecido.
- `trg_transaccion_bi` rechaza el aporte si la propuesta no está ABIERTA, si no coinciden comprador, variedad, provincia o unidad, o si la cantidad supera lo disponible.
- El comprador acepta (ACORDADA) o rechaza (RECHAZADA) cada aporte; cada transacción sigue luego su ciclo normal (RF-06, RF-11, RF-05).
- El comprador puede cerrar (CERRADA) o cancelar (CANCELADA) la propuesta; el service cancela los aportes PROPUESTA pendientes.

**RF-11 — Entrega:**
- `trg_entrega_bi`: solo transacciones ACORDADA tienen entrega (máx. una por transacción); la placa se guarda en mayúsculas.
- `trg_evento_entrega_bi`: solo el comprador o el agricultor registran eventos; EN_CAMINO requiere RECOGIDO y ENTREGADO requiere EN_CAMINO.
- `trg_entrega_bu`: la recepción solo se confirma si existe ENTREGADO; una entrega confirmada ya no se modifica.
- `trg_entrega_au`: al confirmar la recepción, la transacción pasa a COMPLETADA.

---

## 3. Requisitos no funcionales (RNF)

| RNF | Atributo | Medida | Justificación |
|---|---|---|---|
| **RNF-01** | Rendimiento | < 3 segundos en la primera carga (4G) | Usuarios rurales, conectividad limitada |
| **RNF-02** | Capacidad | 500+ usuarios concurrentes sin degradación | Fase piloto; escalabilidad posterior |
| **RNF-03** | Usabilidad | Mobile-first, botones ≥ 48 px, alto contraste, navegación lineal, textos simples | Adopción en zonas rurales, baja alfabetización digital |
| **RNF-04** | Mantenibilidad | Principios SOLID + Javadoc en toda clase/método público | Contexto académico y práctica profesional |
| **RNF-05** | Integridad | 3NF, FK con integridad referencial, CHECK y triggers | Confiabilidad de datos |
| **RNF-06** | Disponibilidad | 99 % en horario comercial (8:00–18:00, lunes a viernes) | MVP académico; sin infraestructura de alta disponibilidad |

### 3.1 Aclaraciones importantes

**RNF-06 — Disponibilidad 99 %:**
- **Objetivo de negocio:** el sistema debe estar disponible el 99 % del tiempo en horario comercial.
- **Mecanismo técnico:** el endpoint `/actuator/health` (único endpoint de Actuator expuesto) debe responder `UP`.
- **Esto no reemplaza el requisito:** el endpoint es la herramienta de monitoreo, no la especificación.
- La medición con un monitor externo gratuito se define en el plan de monitoreo (semana 16).

**RNF-01 — Rendimiento < 3 s:**
- Listados paginados (20 ítems), sin cargar todo.
- Sin librerías JS pesadas (no jQuery, no MomentJS, no jQuery UI); Bootstrap 5 como única librería.
- Consultas apoyadas en los índices de `modelo-bd.sql` sección 8.

**RNF-02 — Concurrencia:**
- API stateless (JWT), pool HikariCP.
- Al acordar una transacción, el service bloquea la publicación (`PESSIMISTIC_WRITE`) para evitar sobreventa.
- La verificación del estado del usuario en cada petición (§9.1) es una consulta por clave primaria; si las pruebas de carga muestran impacto, se agrega una caché en memoria (sin servicios externos).

---

## 4. Restricciones técnicas y supuestos

### 4.1 Restricciones de implementación

**Costo cero (obligatorio):**
- Solo software libre o gratuito; ninguna dependencia, API o servicio de pago, ni que requiera plan pagado o clave de API comercial.
- Sin servicios externos de caché, colas, correo o almacenamiento (no Redis, no S3, no SendGrid).
- CI en GitHub Actions dentro de la cuota gratuita de la cuenta.
- Despliegue (semana 15) en una opción gratuita: capa gratuita de un proveedor cloud, créditos académicos o servidor de la universidad. Se decide y documenta en un ADR antes de la semana 15.
- HTTPS con certificados gratuitos (Let's Encrypt) y monitoreo externo con herramientas gratuitas.

**Stack obligatorio — no sustituir sin consentimiento explícito:**
- Backend: Java 21 LTS, Spring Boot **4.1.1**, Maven (con wrapper), empaquetado WAR para Tomcat.
- Security: Spring Security + **OAuth2 Resource Server** con JWT firmado HS256 (Nimbus, incluido en Spring Security; sin JJWT), BCrypt para contraseñas.
- BD: compatible con **MySQL 8.0+ y MariaDB 10.6+** (desarrollo en MariaDB 12.3), `utf8mb4` / `utf8mb4_unicode_ci`, Flyway (`flyway-mysql`).
- Frontend: HTML5 + CSS3 + JavaScript vanilla (ES6+), Bootstrap 5, **sin React/Vue/Angular**.
- Librerías obligatorias: Google Guava, Apache Commons Lang3, Apache POI, Logback (vía Spring Boot).
- Arquitectura verificada con Spring Modulith.
- Tests: JUnit 5, Mockito, Spring Boot Test; integración contra la base real `agroconecta_test` (sin H2 ni Testcontainers).

**Versionado y estructura:**
- Git: ramas `main` (entregas), `develop` (integración), `feature/rf-XX-nombre-corto`.
- Monolito modular bajo `pe.agroconecta`: un módulo por dominio (`catalogo`, `usuarios`, `precios`, `publicaciones`, `mensajeria`, `transacciones`, `calificaciones`, `ventaagrupada`, `notificaciones`, `administracion`) más `compartido`. Cada módulo tiene capas internas `api`, `web`, `service`, `dao`, `model`, `mapper`. Detalle en `docs/04-diseno/arquitectura-modular.md` y ADR-001.
- BD: `V1__esquema_inicial.sql` (secciones 2–11 de `modelo-bd.sql`) y `V2__datos_iniciales.sql` (sección 12); `ddl-auto=validate` en todos los perfiles. Una migración aplicada nunca se edita; los cambios van en una nueva `Vn` y se reflejan en `modelo-bd.sql`.
- Servidor de BD: `event_scheduler=ON` y privilegios TRIGGER, EVENT, CREATE ROUTINE y ALTER ROUTINE para el usuario de la aplicación (configurado fuera de las migraciones).

### 4.2 Supuestos y decisiones de diseño

| Supuesto | Razón | Impacto |
|---|---|---|
| Costo cero | Proyecto académico sin presupuesto | Solo herramientas gratuitas; despliegue en opción gratuita |
| Monolito modular | Equipo pequeño, un despliegue, bajo acoplamiento (ADR-001) | Módulos por dominio verificados por prueba de arquitectura |
| Sin pasarela de pagos | Alcance MVP + requisito del curso | La transacción solo registra el acuerdo; el pago es externo |
| Sin suscripciones (RF-12) | Extensión futura | Sin comisiones ni modelo freemium en la fase 1 |
| Sin publicación delegada | Simplifica reglas | El agricultor es el único dueño y publicador |
| Transportista como dato | No requiere cuenta | Agricultor o comprador registran entrega y eventos |
| Moderación posterior | Simplicidad y rapidez de publicación | La publicación nace ACTIVA; el admin puede bloquearla |
| Login con teléfono | Muchos agricultores no usan email | Email opcional; el admin reinicia contraseñas |
| Triggers + validación en service | Defensa en profundidad | Errores del trigger se devuelven como 422 (ver §9.2) |
| Base de datos como fuente de verdad | Auditoría y consistencia | El esquema no se genera desde Hibernate |
| Evento MySQL para alertas (no `@Scheduled`) | Consistencia temporal única | `ev_tareas_diarias` corre una vez al día sin carreras entre instancias |

---

## 5. Matriz de trazabilidad RF/RNF a módulos y tablas

Los servicios listados son las interfaces públicas del paquete `api` de cada módulo; su implementación (`XxxServiceImpl`) está en `service/`.

| Módulo | RF | RNF | Tablas / vistas | Controllers | Services | DAOs |
|---|---|---|---|---|---|---|
| `catalogo` | Soporte de RF-01, RF-02, RF-03, RF-10 | RNF-01, RNF-05 | categorias, cultivos, variedades, unidades_medida, provincias, distritos | CatalogoController | CatalogoService | CategoriaDao, CultivoDao, VariedadDao, UnidadMedidaDao, ProvinciaDao, DistritoDao |
| `usuarios` | RF-08 | RNF-03, RNF-04, RNF-05 | usuarios, agricultores, compradores, roles | AuthController, UsuarioController | AuthService, UsuarioService | UsuarioDao, AgricultorDao, CompradorDao, RolDao |
| `publicaciones` | RF-01, RF-03, RF-07 (acciones rápidas) | RNF-01, RNF-02, RNF-05 | publicaciones, `v_publicacion_disponible` | PublicacionController | PublicacionService | PublicacionDao, PublicacionDisponibleDao |
| `precios` | RF-02 | RNF-01, RNF-05 | cargas_precio, precios_mercado, `v_tablero_precios`, `v_precio_plataforma` | PrecioController | PrecioService, CargaPrecioService | PrecioMercadoDao, CargaPrecioDao, TableroPrecioDao |
| `mensajeria` | RF-04 | RNF-03, RNF-05 | conversaciones, mensajes, leads_whatsapp | ConversacionController | ConversacionService | ConversacionDao, MensajeDao, LeadWhatsappDao |
| `transacciones` | RF-06, RF-11 | RNF-01, RNF-02, RNF-05 | transacciones, `v_historial_transaccion`, transportistas, entregas, eventos_entrega | TransaccionController, EntregaController | TransaccionService, EntregaService | TransaccionDao, HistorialTransaccionDao, TransportistaDao, EntregaDao, EventoEntregaDao |
| `calificaciones` | RF-05 | RNF-03, RNF-05 | calificaciones, `v_reputacion_usuario` | CalificacionController | CalificacionService | CalificacionDao, ReputacionDao |
| `ventaagrupada` | RF-10 | RNF-01, RNF-02, RNF-05 | propuestas_grupales, `v_oferta_consolidada` (aportes vía `transacciones`) | PropuestaGrupalController | PropuestaGrupalService | PropuestaGrupalDao, OfertaConsolidadaDao |
| `notificaciones` | RF-07 (y notificaciones de RF-04, RF-05, RF-06, RF-09, RF-10, RF-11) | RNF-02, RNF-05 | notificaciones (evento `ev_tareas_diarias`) | NotificacionController | NotificacionService | NotificacionDao |
| `administracion` | RF-09 | RNF-03, RNF-04, RNF-05, RNF-06 | moderaciones, `v_conversion_publicacion` (+ lectura de otros módulos vía su `api`) | AdminController | AdminService, ReporteService | ModeracionDao, ConversionPublicacionDao |

---

## 6. Vistas, función, triggers y evento

### 6.1 Vistas (solo lectura desde JPA; se recalculan en cada consulta)

```
v_publicacion_disponible  — RF-03/RF-07: publicaciones con cultivo, variedad, categoría, ubicación,
                            cantidad disponible, días restantes y nivel de perecibilidad
v_oferta_consolidada      — RF-10: volumen disponible por cultivo, variedad, provincia y unidad (≥ 2 publicaciones)
v_precio_plataforma       — RF-02: precios diarios de transacciones COMPLETADA por cultivo, provincia y unidad
v_tablero_precios         — RF-02: referencias externas + precios de la plataforma
v_reputacion_usuario      — RF-05: total y promedio de calificaciones recibidas por usuario
v_historial_transaccion   — RF-06/RF-11: transacciones con monto total y estado de entrega
v_conversion_publicacion  — RF-09/RF-04: leads, conversaciones, propuestas y completadas por publicación
```

### 6.2 Función y triggers (15)

`fn_rol_usuario(id_usuario)` devuelve el nombre del rol del usuario; la usan los triggers de actores y de administración.

| Trigger | Momento / tabla | Regla | Efecto |
|---|---|---|---|
| `trg_agricultor_bi` | BEFORE INSERT `agricultores` | El usuario tiene rol AGRICULTOR | 45000 si no |
| `trg_comprador_bi` | BEFORE INSERT `compradores` | El usuario tiene rol COMPRADOR | 45000 si no |
| `trg_publicacion_bi` | BEFORE INSERT `publicaciones` | Estado inicial ACTIVA; calcula `fecha_limite` si es NULL (error si el cultivo no tiene vida útil); `fecha_limite` ≥ hoy | Completa `fecha_limite` |
| `trg_publicacion_bu` | BEFORE UPDATE `publicaciones` | No se cambia el agricultor; si una VENCIDA recibe `fecha_limite` ≥ hoy (sin cambiar el estado), vuelve a ACTIVA | Cambia `estado` |
| `trg_mensaje_bi` | BEFORE INSERT `mensajes` | El remitente participa en la conversación | 45000 si no |
| `trg_transaccion_bi` | BEFORE INSERT `transacciones` | Estado inicial PROPUESTA; publicación ACTIVA; proponente es comprador o agricultor; cantidad ≤ disponible; coherencia con la propuesta grupal ABIERTA | 45000 si falla |
| `trg_transaccion_bu` | BEFORE UPDATE `transacciones` | Finalizadas no se modifican; partes inmutables; términos ACORDADA inmutables; transiciones válidas; disponibilidad al acordar; recepción confirmada antes de COMPLETADA si hay entrega | Completa `fecha_acuerdo` / `fecha_cierre` |
| `trg_transaccion_au` | AFTER UPDATE `transacciones` | Agotada la cantidad → publicación VENDIDA; cancelar un acuerdo reactiva la publicación (ACTIVA o VENCIDA) | Actualiza `publicaciones.estado` |
| `trg_calificacion_bi` | BEFORE INSERT `calificaciones` | Transacción COMPLETADA; evaluador es comprador o agricultor | 45000 si falla |
| `trg_entrega_bi` | BEFORE INSERT `entregas` | Transacción ACORDADA; sin confirmación inicial | Placa en mayúsculas |
| `trg_entrega_bu` | BEFORE UPDATE `entregas` | Transacción inmutable; entrega confirmada no se modifica; confirmar requiere evento ENTREGADO | Placa en mayúsculas |
| `trg_entrega_au` | AFTER UPDATE `entregas` | Al registrar `fecha_confirmacion_recepcion` | Transacción → COMPLETADA |
| `trg_evento_entrega_bi` | BEFORE INSERT `eventos_entrega` | Transacción ACORDADA; registra comprador o agricultor; EN_CAMINO requiere RECOGIDO; ENTREGADO requiere EN_CAMINO | 45000 si falla |
| `trg_carga_precio_bi` | BEFORE INSERT `cargas_precio` | El usuario es ADMINISTRADOR | 45000 si no |
| `trg_moderacion_bi` | BEFORE INSERT `moderaciones` | El usuario es ADMINISTRADOR | 45000 si no |

Como varios cambios de estado ocurren en triggers y en el evento diario (no en Java), la aplicación no publica eventos de dominio para ellos: los módulos consultan el estado actual a través de la `api` del módulo dueño.

### 6.3 Evento programado

```
ev_tareas_diarias
  Ejecuta: cada día a las 06:00 (hora del servidor)
  Requiere: event_scheduler=ON en el servidor
  Acciones:
    1. Notificaciones VENCIMIENTO para publicaciones ACTIVA que vencen en 3 días o en 1 día (una por día)
    2. Publicaciones ACTIVA con fecha_limite < hoy → VENCIDA
    3. Transacciones PROPUESTA de propuestas grupales vencidas → CANCELADA
    4. Propuestas grupales ABIERTA vencidas → CERRADA
```

---

## 7. Reglas de negocio críticas

### 7.1 Ciclo de la transacción (RF-06)

```
ESTADOS: PROPUESTA → ACORDADA → COMPLETADA
         PROPUESTA → RECHAZADA
         PROPUESTA / ACORDADA → CANCELADA

1. El agricultor publica (RF-01) → publicación ACTIVA.
2. Comprador y agricultor conversan (RF-04).
3. Una de las partes registra la propuesta → transacción PROPUESTA (fecha_propuesta).
4. La otra parte la acepta → ACORDADA (el trigger fija fecha_acuerdo) o la rechaza → RECHAZADA.
   Si se agota la cantidad, la publicación pasa a VENDIDA.
5a. Con transporte (RF-11): entrega + eventos RECOGIDO → EN_CAMINO → ENTREGADO;
    el comprador confirma la recepción → COMPLETADA (trg_entrega_au).
5b. Sin transporte (RF-06): el comprador confirma la recepción → COMPLETADA directamente.
6. Ambas partes pueden calificar (RF-05).
```

**Reglas:** una transacción solo avanza, nunca retrocede; una finalizada (COMPLETADA, RECHAZADA, CANCELADA) no se modifica; para corregir se crea una nueva. Los eventos de entrega no son estados de la transacción.

### 7.2 Reputación y calificación

```
Agricultor y comprador se califican mutuamente, 1–5 estrellas + comentario opcional.
Solo si la transacción está COMPLETADA; una calificación por evaluador y transacción.
Reputación = promedio de las calificaciones recibidas (v_reputacion_usuario).
Visible en el perfil y en los resultados de búsqueda.
```

### 7.3 Perecibilidad y vencimiento

```
fecha_limite = fecha_disponible + cultivos.vida_util_dias si el agricultor no la indica.
Si el cultivo no tiene vida útil registrada, la fecha límite es obligatoria.
Evento diario:
  - 3 días antes: notificación "Tu cosecha vence en 3 dias"
  - 1 día antes:  notificación "Tu cosecha vence en 24 horas"
  - Pasada la fecha límite: estado → VENCIDA

Estados de la publicación: ACTIVA, VENDIDA, VENCIDA, RETIRADA, BLOQUEADA.

El agricultor puede:
  - Bajar el precio referencial
  - Extender la fecha límite (una VENCIDA vuelve a ACTIVA)
  - Retirar la publicación (RETIRADA)
BLOQUEADA solo la asigna el administrador (moderación).
```

### 7.4 Precios de referencia

```
El admin carga (RF-02) precios mínimo, máximo y promedio por cultivo, provincia, unidad y fecha.
Cada carga registra administrador, método, fuente, archivo y fecha (cargas_precio).
v_tablero_precios combina referencias externas y precios de la plataforma.
El agricultor ve los precios al publicar (no es obligatorio que coincidan).
```

---

## 8. Restricciones de datos

### 8.1 Integridad referencial

- Cada publicación → agricultor + variedad (y por ella el cultivo) + unidad de medida + distrito.
- Cada transacción → publicación + comprador + proponente (+ propuesta grupal opcional); el agricultor se obtiene de la publicación.
- Cada calificación → transacción + evaluador; el evaluado se deriva (contraparte) en `v_reputacion_usuario`.
- Cada mensaje → conversación + remitente.
- Cada lead de WhatsApp → publicación + comprador.
- Cada notificación → usuario (+ publicación opcional).
- Cada entrega → transacción (única) + transportista; cada evento → entrega + usuario que lo registra.
- Cada moderación → administrador + (publicación **o** usuario afectado, exactamente uno).

En Java, las entidades de un módulo referencian a las de otro módulo solo por ID; las claves foráneas se mantienen en la base de datos.

### 8.2 Validaciones de dominio

| Columna | Restricción | Por qué |
|---|---|---|
| `usuarios.telefono` | NOT NULL, UNIQUE, `^[+]?[0-9]{9,14}$` | Identificador de inicio de sesión |
| `usuarios.email` | NULL permitido, UNIQUE, formato válido; el service lo guarda en minúsculas | Contacto opcional |
| `usuarios.password_hash` | NOT NULL, hash BCrypt | Seguridad obligatoria |
| `usuarios.estado` | ACTIVO, SUSPENDIDO, INACTIVO | Control de acceso |
| `publicaciones.cantidad` | > 0, DECIMAL(12,2) | Sin cantidades negativas |
| `publicaciones.precio_referencial` | > 0, DECIMAL(10,2) | Precio en soles por unidad |
| `publicaciones.estado` | ACTIVA, VENDIDA, VENCIDA, RETIRADA, BLOQUEADA | Máquina de estados |
| `transacciones.estado` | PROPUESTA, ACORDADA, COMPLETADA, RECHAZADA, CANCELADA | Máquina de estados |
| `eventos_entrega.estado` | RECOGIDO, EN_CAMINO, ENTREGADO | Línea de tiempo de entrega |
| `entregas.placa_vehiculo` | `^[A-Z0-9]{3}-?[A-Z0-9]{3}$`, mayúsculas | Formato de placa |
| `calificaciones.puntuacion` | BETWEEN 1 AND 5 | Rating 1–5 |
| `calificaciones.comentario` | máx. 500 caracteres, opcional | UX liviana |
| `mensajes.contenido` | NOT NULL, máx. 1000 caracteres | Límite de mensaje |

### 8.3 Política de eliminación

- **Usuarios:** borrado lógico con `estado = INACTIVO`.
- **Publicaciones:** borrado lógico con `estado = RETIRADA`.
- **Transacciones, mensajes, moderaciones, eventos de entrega:** nunca se eliminan (auditoría).

---

## 9. Restricciones de seguridad

### 9.1 Autenticación y autorización

- El login devuelve un JWT firmado HS256 con el ID del usuario como `sub` y el claim `roles`. La clave se lee de la variable de entorno `JWT_SECRET`.
- En cada petición autenticada se verifica que el usuario siga ACTIVO: `compartido` define la interfaz del verificador y `usuarios` la implementa. Un usuario SUSPENDIDO o INACTIVO pierde el acceso de inmediato (401).

| Recurso | Rol permitido | Método |
|---|---|---|
| POST /api/v1/auth/registro | Público | Teléfono, contraseña, rol (AGRICULTOR o COMPRADOR) |
| POST /api/v1/auth/login | Público | Teléfono + contraseña → JWT |
| GET /api/v1/catalogo/** | Público | Sin autenticación |
| GET /api/v1/publicaciones/** | Público | Sin autenticación |
| POST /api/v1/publicaciones | AGRICULTOR | `@PreAuthorize("hasRole('AGRICULTOR')")` |
| GET /api/v1/precios/** | Público | Sin autenticación |
| POST /api/v1/precios (carga) | ADMINISTRADOR | `@PreAuthorize("hasRole('ADMINISTRADOR')")` |
| POST /api/v1/transacciones | AGRICULTOR, COMPRADOR | Parte de la publicación; lógica en el service |
| POST /api/v1/propuestas-grupales | COMPRADOR | `@PreAuthorize("hasRole('COMPRADOR')")` |
| GET /api/v1/usuarios/{id} | El propio usuario o ADMINISTRADOR | Lógica en el service |
| PUT /api/v1/usuarios/{id} | El propio usuario o ADMINISTRADOR | Lógica en el service |
| PUT /api/v1/usuarios/me/contrasena | Usuario autenticado | Requiere contraseña actual |
| PUT /api/v1/usuarios/{id}/contrasena | ADMINISTRADOR (reinicio) | `@PreAuthorize("hasRole('ADMINISTRADOR')")` |
| DELETE /api/v1/usuarios/{id} | ADMINISTRADOR (pasa a INACTIVO) | `@PreAuthorize("hasRole('ADMINISTRADOR')")` |

El contrato completo de endpoints se mantiene en `docs/05-api/endpoints.md`.

### 9.2 Manejo de errores

```json
{
  "timestamp": "2026-09-16T14:30:00-05:00",
  "estado": 422,
  "error": "Unprocessable Entity",
  "mensaje": "[mensaje del trigger en español]",
  "ruta": "/api/v1/transacciones",
  "errores": []
}
```

`errores` contiene el detalle por campo solo en errores de validación (400); en los demás casos es una lista vacía.

**Mapeo (se evalúa el código de error del motor antes que el SQLSTATE):**
- Bean Validation → 400.
- Trigger (SQLSTATE 45000), CHECK (3819 MySQL / 4025 MariaDB) o regla de negocio del service → **422**.
- Clave duplicada o FK (1062, 1451, 1452) → **409**.
- 401 sin token, token inválido o usuario no ACTIVO; 403 rol o dueño incorrecto; 404 recurso inexistente.

### 9.3 Protecciones frontend

- Token JWT en `sessionStorage` (no `localStorage`).
- Datos de usuario con `textContent`, no `innerHTML`.
- No registrar contraseñas, tokens ni contenido de mensajes en logs.
- HTTPS obligatorio en producción (certificado gratuito).

---

## 10. Requisitos de documentación y entrega

### 10.1 Documentación técnica

- `CLAUDE.md` — instrucciones para Claude Code y reglas del proyecto.
- `docs/02-requisitos/` — este SRS, casos de uso UC-01 a UC-12 (un archivo por UC) y matriz de trazabilidad.
- `docs/03-procesos/` — BPMN TO-BE (`.drawio`, imagen y descripción textual).
- `docs/04-diseno/base-datos/` — `modelo-bd.sql` (24 tablas, 1 función, 15 triggers, 7 vistas, 1 evento), `modelo-logico.md` (ER en Mermaid) y `notas-integridad-bd.md`.
- `docs/04-diseno/arquitectura-modular.md` — módulos, capas y reglas de desacoplamiento.
- `docs/05-api/endpoints.md` — contrato REST.
- `docs/06-decisiones/` — registros de decisiones (ADR).
- Javadoc — toda clase y método públicos.
- README.md — instalación, despliegue y entorno de desarrollo.

### 10.2 Documentación de usuario

- Guía de registro (agricultor vs comprador).
- Manual de publicación de cosechas.
- Manual de búsqueda, contacto, propuestas y venta agrupada.
- FAQ de perecibilidad y alertas.
- Términos de servicio y aviso de privacidad (documento informativo conforme a la Ley N.° 29733).

### 10.3 Código y pruebas

- Cobertura ≥ 70 % en la capa service (JaCoCo).
- TDD en service.
- Pruebas de integración contra MariaDB/MySQL real (`agroconecta_test`).
- Prueba de arquitectura modular (`ArquitecturaModularTest`) en verde.
- Sin lógica de negocio en controllers.
- Validación con Bean Validation en DTOs de request.

---

## 11. Orden de implementación recomendado

Ordenado por dependencias: cada RF solo necesita lo implementado antes.

1. **RF-08** (Usuarios/Auth) — bloquea todos los demás; esqueleto andante desplegado
2. **RF-01 + RF-03** (Publicación + Búsqueda, con endpoints de lectura de `catalogo`) — flujo principal
3. **RF-02** (Precios) — datos de contexto
4. **RF-04** (Mensajería) — negociación
5. **RF-06** (Registro e historial de transacciones) — acuerdos
6. **RF-11** (Entrega) — requiere transacciones ACORDADA
7. **RF-05** (Calificación) — requiere transacciones COMPLETADA
8. **RF-07** (Alertas) — lectura de notificaciones y acciones rápidas
9. **RF-10** (Venta agrupada) — usa transacciones
10. **RF-09** (Panel admin) — lee de todos los módulos

---

## 12. Hitos del curso

| Hito | Semana | Fecha aprox. | Entregable |
|---|---|---|---|
| APF1 | 4 | 3 sept 2026 | ✓ Entregado (Lean Canvas, requisitos, prototipos) |
| APF2 | 8 | ~1 oct 2026 | BPMN + diseño de BD + prototipos UI |
| APF3 | 12 | ~29 oct 2026 | Desarrollo: backend + frontend funcionando |
| PROY | 18 | 27 nov 2026 | Sistema completo, pruebas, despliegue |

**Progreso esperado:** 30 % (sem 10) → 40 % (sem 11) → 60 % (sem 13) → 70 % (sem 14) → 80 % (sem 15) → 90 % (sem 16) → 100 % (sem 17)

---

## 13. Fuera de alcance (documentado, no implementado)

| RF | Nombre | Razón | Referencia |
|---|---|---|---|
| **RF-12** | Suscripciones y comisiones | Modelo de sostenibilidad futuro; requiere pasarela de pagos | Lean Canvas sección 6 |
| (N/A) | Publicación delegada por asociación | Simplifica la fase 1 | Decisión de alcance |
| (N/A) | Pasarela de pagos | Las transacciones solo registran el acuerdo | Requisito del curso / ADR-002 |
| (N/A) | Comprobante PDF | Sin librería PDF en el stack; se exporta a Excel | Decisión de alcance |
| (N/A) | Correos, SMS, push, verificación de email | Sin infraestructura de mensajería y con costo cero | Decisión de alcance |
| (N/A) | Aprobación previa, reportes de usuarios, fotos | Moderación posterior; el esquema no los contempla | Decisión de alcance |
| (N/A) | Transportista con cuenta propia | Es un registro de datos | Decisión de alcance |
| (N/A) | App móvil nativa / PWA offline | Stack web; acceso por URL/4G | Decisión técnica / RNF-03 |
| (N/A) | Servicios o dependencias de pago | Restricción de costo cero | §4.1 |

---

## 14. Referencias y normativa

- **IEEE 830** — Software Requirements Specification
- **ISO/IEC 25010** — Modelo de calidad de sistemas y software (RNF)
- **SOLID** — Código mantenible
- **Ley N.° 29733** — Ley de Protección de Datos Personales (Perú) y su reglamento
- **Proceso:** desarrollo iterativo e incremental por rebanadas verticales (sprints de 1 semana); casos de uso con la estructura de RUP
- **APA 7.ª edición** — Citas en documentación académica

---

**Documento preparado para Claude Code y el equipo de desarrollo.**  
**Última actualización:** 26 de septiembre de 2026  
**Próxima revisión:** después de APF2 (diseño) si hay cambios en alcance o BPMN.
