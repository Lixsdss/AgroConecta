# SRS — AgroConecta
## Especificación de Requisitos del Sistema

**Proyecto:** Integrador I: Sistemas – Software (100000I58N), Ciclo VI, 2026-2  
**Institución:** Universidad Tecnológica del Perú (Piura, Perú)  
**Equipo:** 6 personas (4 programadores, 2 documentadores)  
**Versión:** 1.3  
**Fecha:** 27 de septiembre de 2026  
**Fuente de verdad:** Este documento + `docs/04-diseno/base-datos/modelo-bd.sql` v4.1 (el script manda sobre cualquier nombre de tabla, columna, estado o trigger)

**Cambios v1.1:** venta agrupada iniciada por el comprador (RF-10); transportista como registro de datos; moderación posterior; historial exportable a Excel (sin PDF); inicio de sesión con teléfono; estados, triggers y evento alineados con `modelo-bd.sql`.

**Cambios v1.2:** RF-06 ampliado a "Registro e historial de transacciones" (proponer, aceptar, rechazar, cancelar y confirmar recepción); arquitectura de monolito modular con módulo `catalogo`; RF-11 dentro del módulo `transacciones`; JWT con OAuth2 Resource Server; cambio de contraseña por el propio usuario; restricción de costo cero; formato de error unificado; orden de implementación corregido según dependencias; rutas de `docs/` actualizadas; Ley N.° 29733 en la normativa; notificaciones en la aplicación generadas por los módulos (§2.1); 12 casos de uso (UC-01 a UC-12).

**Cambios v1.3:** alineado con el modelo de datos v4.1 (`docs/04-diseno/base-datos/cambios-v4.md`). Unidad de venta elegida por el agricultor y comparación en kilos (RF-01); precios por kilo y por zona de precio (RF-02); ofertas estructuradas en el chat y conversaciones que no se cierran (RF-04); el acuerdo del chat se registra directamente como ACORDADA, recojo en chacra y sin cancelación tras el recojo (RF-06, RF-11); moderación posterior con CONFORME, ADVERTENCIA, BLOQUEO y DESBLOQUEO, reclamos y suspensiones justificadas (RF-09); venta agrupada por distrito en kilos con reparto proporcional (RF-10); inicio de sesión con celular y sin email (RF-08); el transportista ya no se registra; módulos en singular (`usuario`, `publicacion`, `precio`, `venta`, `notificacion`…) con capas `api`, `aplicacion`, `dominio` e `infraestructura` verificadas con ArchUnit; 20 tablas, 21 triggers y 10 vistas.

---

## 1. Contexto y propósito

### 1.1 Descripción del problema
Pequeños agricultores de Piura (Perú) dependen de intermediarios tradicionales para comercializar sus cosechas, lo que reduce márgenes de ganancia, limita el acceso a compradores e incrementa la pérdida por perecibilidad. La plataforma **AgroConecta** busca conectar directamente a agricultores con compradores (mercados mayoristas, restaurantes, hoteles, procesadoras, molinos) para reducir esa dependencia, ampliar mercado y mejorar competitividad.

### 1.2 Alcance del producto
**Plataforma web de dos lados (two-sided marketplace):**
- Actores: Agricultor, Comprador, Administrador.
- Funcionalidad principal: publicar cosechas, consultar precios, buscar productos, negociar con ofertas, registrar acuerdos, seguir entregas, calificar, recibir alertas de vencimiento, venta agrupada, reclamos y moderación.
- **Sin pasarela de pagos** — las transacciones solo registran el acuerdo comercial; el pago ocurre fuera de la plataforma.
- **Sin modelo de suscripción** en el MVP — documentado como extensión futura (RF-12).
- **Sin publicación delegada** — el agricultor es siempre el único publicador de su cosecha.
- **Sin envío de correos, SMS ni push** — las notificaciones se muestran dentro de la aplicación.
- **Sin comprobantes PDF** — los listados se exportan a Excel (Apache POI).
- **Costo cero** — el proyecto no genera gastos (ver §4.1).

### 1.3 Actores del sistema

| Actor | Descripción | Responsabilidades | Volumen esperado |
|---|---|---|---|
| **Agricultor** | Pequeño productor agrícola | Registrarse, publicar cosechas, responder mensajes y ofertas, registrar acuerdos, aceptar o rechazar propuestas conjuntas, marcar el avance de la entrega, calificar, reclamar | ~250 activos en piloto |
| **Comprador** | Empresa compradora (mercado mayorista, restaurante, hotel, procesadora, molino) | Registrarse, buscar productos, contactar, ofertar, registrar acuerdos, crear propuestas conjuntas, confirmar la recepción, calificar, reclamar | ~50 activos en piloto |
| **Administrador** | Personal de plataforma | Gestionar usuarios, reiniciar contraseñas, moderar publicaciones, resolver reclamos, suspender usuarios, cargar precios de referencia, analizar métricas | 2–3 personas |

**No son actores:**
- **Visitante sin sesión:** solo consulta publicaciones, precios y catálogo; para cualquier otra acción debe registrarse.
- **Transportista:** no tiene cuenta y no se registra en la base de datos; la entrega se sigue con los estados de la venta (RF-11).

---

## 2. Requisitos funcionales (RF)

Alcance confirmado: **RF-01 a RF-11** (11 requisitos). RF-12 documentado como extensión futura sin código.

| RF | Nombre | Descripción | Módulo | Tablas / vistas | Prioridad |
|---|---|---|---|---|---|
| **RF-01** | Publicación de cosecha | El agricultor publica en máx. 3 pasos: (1) variedad (y con ella el cultivo), unidad de venta (kilo, jaba, saco, quintal o tonelada) y cantidad; para jaba y saco indica cuántos kilos pesa cada unidad; (2) precio por unidad y distrito donde está la cosecha; el sistema muestra el precio por kilo calculado; (3) fecha de cosecha y, opcionalmente, fecha límite. Si no indica fecha límite, el trigger la calcula como `fecha_cosecha + cultivos.vida_util_dias`. La publicación nace **PUBLICADA** (moderación posterior). Solo un agricultor ACTIVO puede publicar (validado en el service). Los catálogos los provee el módulo `catalogo`. | `publicacion` | publicaciones (+ catálogos) | MUST |
| **RF-02** | Precios de mercado | Panel público con el precio de referencia del mercado real **por kilo**, por cultivo y **zona de precio** (4 distritos: Piura, Tambogrande, Chulucanas y Sullana), con su fuente y el administrador que lo cargó. El panel lo muestra también en la unidad local del cultivo (jaba, saco…) y con su tendencia frente a la carga anterior. Al publicar, el agricultor ve además el rango de la plataforma: mínimo, máximo y promedio por kilo de las ventas COMPLETADA de los últimos 30 días. El admin carga por formulario o archivo Excel/CSV (Apache POI). | `precio` | precios_mercado, `v_precios_vigentes`, `v_rango_precio_plataforma` | MUST |
| **RF-03** | Búsqueda multi-criterio | Cualquier persona (con o sin sesión) busca publicaciones **PUBLICADA** con kilos disponibles, filtrando por cultivo (el filtro "Categoría" de la interfaz es el cultivo), variedad, provincia/distrito, unidad y rango de fecha de cosecha (todos opcionales). Ordena por precio por kilo, fecha de cosecha o calificación del agricultor. Resultado paginado (20 ítems/página). | `publicacion` | `v_publicaciones_disponibles`, `v_reputacion_usuario` | MUST |
| **RF-04** | Mensajería directa | Chat agricultor ↔ comprador, una conversación por publicación y comprador, con consulta periódica. Además de texto, admite **ofertas estructuradas** (cantidad y precio por unidad), permitidas solo mientras la publicación está PUBLICADA. Las conversaciones **nunca se cierran**: siguen sirviendo para coordinar la entrega. El botón "Contactar por WhatsApp" suma un clic en el contador de la conversación (lead). Solo los dos participantes pueden escribir. | `mensajeria` | conversaciones, mensajes | MUST |
| **RF-05** | Calificación de transacción | Agricultor y comprador se califican 1–5 estrellas con comentario opcional (máx. 500 caracteres), una vez por venta y solo si está COMPLETADA. La reputación es el promedio de las calificaciones recibidas. | `venta` | calificaciones, `v_reputacion_usuario` | SHOULD |
| **RF-06** | Registro e historial de transacciones | Las partes negocian en el chat con mensajes de oferta. Cuando hay acuerdo, **cualquiera de las dos** registra la venta directamente como **ACORDADA** (unidad, cantidad, precio por unidad y fecha de recojo); la unidad acordada puede diferir de la publicada. Mientras está ACORDADA, cualquiera de las partes puede cancelarla (CANCELADA). PROPUESTA existe solo en la venta agrupada (RF-10). **Recojo en chacra sin transporte:** el comprador pasa la venta de ACORDADA a ENTREGADO y luego a COMPLETADA. **Después del recojo la venta no se cancela;** los problemas se resuelven con un reclamo (RF-09). El usuario ve sus ventas filtradas por estado, fecha y contraparte, paginadas, y las exporta a Excel (Apache POI). | `venta` | transacciones, transacciones_historial, `v_historial_transacciones` | MUST |
| **RF-07** | Alertas de vencimiento | El evento `ev_tareas_diarias` crea notificaciones **POR_VENCER** para el agricultor a 3 días y a 1 día de la fecha límite, y pasa a VENCIDA las publicaciones vencidas. El módulo `notificacion` las lista y las marca como leídas. Acciones rápidas del agricultor (en `publicacion`): bajar el precio, extender la fecha límite (una VENCIDA vuelve a PUBLICADA) y retirar. | `notificacion`, `publicacion` | notificaciones, publicaciones, evento `ev_tareas_diarias` | MUST |
| **RF-08** | Registro y gestión de usuarios | Registro por rol (Agricultor o Comprador) con nombre completo, **celular** (9 dígitos que empiezan en 9; único y obligatorio), **distrito obligatorio** y contraseña BCrypt. **No hay email.** El agricultor puede indicar el nombre de su chacra; el comprador registra el **nombre de su negocio** y su **tipo** (MERCADO_MAYORISTA, RESTAURANTE, HOTEL, PROCESADORA, MOLINO u OTRO) y, opcionalmente, su RUC. El inicio de sesión es con celular + contraseña. Perfil editable y cambio de la propia contraseña (requiere la actual). El admin crea administradores, cambia estados y reinicia contraseñas. | `usuario` | usuarios, agricultores, compradores | MUST |
| **RF-09** | Panel administrativo | **Moderación posterior:** la cola "Por revisar" contiene las publicaciones marcadas por el sistema (primera publicación del agricultor, precio fuera de rango, cantidad inusual o corregida tras un bloqueo). El admin registra CONFORME, ADVERTENCIA, BLOQUEO o DESBLOQUEO; advertir y bloquear exigen un motivo de catálogo y un mensaje para el agricultor. Si el agricultor corrige una publicación bloqueada, vuelve a la cola con la marca CORREGIDA. **Reclamos:** una parte reclama sobre una venta que llegó a ACORDADA; el admin lo resuelve como FUNDADO o INFUNDADO. **Suspensiones justificadas** con motivo de catálogo y detalle; el motivo RECLAMOS_FUNDADOS exige al menos 3 reclamos fundados contra el usuario. Gestión de usuarios y **métricas desde vistas**, con exportación a Excel. | `administracion` | revisiones_publicacion, reclamos, suspensiones, `v_panel_admin`, `v_resumen_usuario`, `v_ventas_mensuales_cultivo`, `v_conversion_publicacion` | MUST |
| **RF-10** | Venta agrupada | El sistema agrupa las publicaciones PUBLICADA por cultivo, variedad y distrito cuando hay **2 o más agricultores** (lote). El **comprador** envía a un lote una **propuesta conjunta** en kilos, con precio por kilo y fecha de recojo. Cada agricultor la recibe como una venta PROPUESTA **en su propia unidad** y la acepta (ACORDADA) o la rechaza (RECHAZADA). La cantidad se reparte **en proporción a los kilos disponibles** de cada agricultor, redondeando hacia abajo a unidades enteras de su publicación. Cuando pasa la fecha de recojo, las propuestas pendientes se cancelan. | `ventaagrupada` (ventas vía `venta`) | propuestas_grupales, transacciones, `v_lotes_agrupados` | SHOULD |
| **RF-11** | Seguimiento de entrega | Sin transportista ni placa: la entrega se sigue con los estados de la venta, ACORDADA → RECOGIDO → EN_CAMINO → ENTREGADO → COMPLETADA (o ACORDADA → ENTREGADO → COMPLETADA en el recojo en chacra). Solo el comprador confirma la recepción. La **línea de tiempo** es el historial de estados de la venta (`transacciones_historial`), que la base de datos registra automáticamente con fecha, hora y autor. | `venta` | transacciones, transacciones_historial | SHOULD |

### 2.1 Detalles de RF no obviamente lineales

**RF-01 — Unidades y kilos:**
- `unidades_medida` define la unidad y su factor en kilos: kilo (1), quintal (46), tonelada (1000); jaba y saco son de peso variable (`factor_kg` NULL).
- Con unidad de peso fijo, `trg_publicaciones_bi` toma el peso del catálogo; con jaba o saco, el agricultor debe indicar `peso_unidad_kg` ("Indique cuantos kilos pesa cada jaba o saco").
- `cantidad_kg` y `precio_kg` son columnas generadas por la base de datos; todas las comparaciones (búsqueda, disponibilidad, lotes) se hacen en kilos.
- Si el agricultor no indica fecha límite, se calcula con la vida útil del cultivo (obligatoria en `cultivos`). La fecha límite no puede ser anterior a hoy.
- La primera publicación de un agricultor entra a la cola de moderación con la marca PRIMERA_PUBLICACION (trigger). Las marcas PRECIO_FUERA_RANGO y CANTIDAD_INUSUAL las pone el service; sus umbrales se definen en el slice de RF-09.

**RF-02 — Carga de precios:**
- Formulario: cultivo, zona de precio, fecha, precio por kilo y fuente (MERCADO_MAYORISTA_PIURA, MERCADO_SULLANA o ENCUESTA_ACOPIADORES) → una fila en `precios_mercado` con `metodo` = FORMULARIO y el administrador.
- Archivo Excel/CSV con cabecera `cultivo, zona, fecha, precio_kg`, más la fuente → N filas con `metodo` = ARCHIVO. Las filas válidas se importan y las inválidas se reportan con su motivo.
- No se admiten dos precios para el mismo cultivo, zona y fecha (UK). `trg_precios_mercado_bi` exige que quien carga sea ADMINISTRADOR y que el distrito sea zona de precio.
- `v_precios_vigentes` devuelve el último precio por cultivo y zona, su equivalente en la unidad local del cultivo y la tendencia (SUBE, BAJA, IGUAL o SIN_DATO).
- `v_rango_precio_plataforma` calcula el rango de las ventas COMPLETADA de los últimos 30 días, por cultivo y distrito.

**RF-04 — Mensajería, ofertas y WhatsApp:**
- `trg_conversaciones_bi`: una conversación solo se abre sobre una publicación PUBLICADA.
- `trg_mensajes_bi`: el remitente debe ser el comprador de la conversación o el agricultor de la publicación; los mensajes de tipo OFERTA solo se aceptan mientras la publicación está PUBLICADA. Los de texto se aceptan siempre (coordinación de la entrega).
- Lead de WhatsApp: el comprador pulsa "Contactar por WhatsApp"; el sistema crea la conversación si no existía (`canal_inicio` = WHATSAPP), suma 1 a `clics_whatsapp` y abre `wa.me` con el celular del agricultor.

**RF-06 — Registro de transacciones:**
- Registrar el acuerdo: cualquiera de las partes, sobre una publicación PUBLICADA, con unidad, cantidad, precio por unidad y fecha de recojo. El service bloquea la publicación (`PESSIMISTIC_WRITE`) y fija `id_actualizado_por`. `trg_transacciones_bi` valida estado inicial, publicación PUBLICADA, que quien registra sea parte, el peso de la unidad acordada y los kilos disponibles.
- Si con el acuerdo se agotan los kilos, `sp_sincronizar_publicacion` pasa la publicación a VENDIDA; si se cancela un acuerdo y vuelve a haber saldo, la devuelve a PUBLICADA (o VENCIDA).
- Los términos (unidad, cantidad, peso y precio) no se modifican: para cambiarlos se cancela y se registra un nuevo acuerdo.
- Cancelar: cualquiera de las partes, solo desde ACORDADA.
- Historial: `v_historial_transacciones` con filtros por estado, fecha y contraparte; exportación a Excel.

**RF-07 — Alertas y evento diario:**
- `ev_tareas_diarias` se ejecuta cada día a las 06:00 (hora del servidor, America/Lima).
- Publicaciones PUBLICADA con `fecha_limite = hoy + 3` → notificación POR_VENCER "Su [cultivo] vence en 3 días. ¿Qué desea hacer?".
- Publicaciones PUBLICADA con `fecha_limite = hoy + 1` → notificación POR_VENCER "Su [cultivo] vence mañana. ¿Qué desea hacer?".
- No se repite la alerta de una publicación el mismo día.
- Publicaciones PUBLICADA con `fecha_limite < hoy` → estado VENCIDA.
- Ventas PROPUESTA de propuestas conjuntas ABIERTA cuya `fecha_recojo` ya pasó → CANCELADA (sin usuario); la propuesta pasa a CERRADA.
- Extender: si una publicación VENCIDA recibe una `fecha_limite` ≥ hoy, `trg_publicaciones_bu` la devuelve a PUBLICADA.
- **No usar `@Scheduled` ni eventos de aplicación para esto** — el evento de base de datos es la fuente de verdad.

**Notificaciones en la aplicación (transversal):**
- POR_VENCER: las crea `ev_tareas_diarias` (RF-07).
- MODERACION: las crea `trg_revisiones_ai` al advertir, bloquear o desbloquear una publicación (RF-09).
- MENSAJE y OFERTA (RF-04), VENTA (RF-06, RF-11), PROPUESTA_CONJUNTA (RF-10), CALIFICACION (RF-05), RECLAMO y CUENTA (RF-09): las crea el módulo que ejecuta la acción, llamando al servicio de `notificacion.aplicacion` dentro de la misma transacción.
- Se usan solo los tipos del `CHECK` de `notificaciones.tipo`. El frontend las consulta periódicamente; no hay correo, SMS ni push.

**RF-08 — Consistencia usuario/subtipo:**
- El service crea `usuarios` y la fila del subtipo (`agricultores` o `compradores`) en la misma transacción.
- `trg_agricultores_bi` rechaza la fila si el usuario no tiene rol AGRICULTOR: "El usuario no tiene rol AGRICULTOR".
- `trg_compradores_bi` hace lo mismo con el rol COMPRADOR.
- El rol no se cambia tras el registro. El service **también** valida todo esto (defensa en profundidad).
- Cambio de contraseña propio: requiere la contraseña actual; el reinicio por el administrador no la requiere.

**RF-09 — Moderación, reclamos y suspensiones:**
- Cola "Por revisar": publicaciones con `marca_revision` no nula (PRIMERA_PUBLICACION, PRECIO_FUERA_RANGO, CANTIDAD_INUSUAL o CORREGIDA).
- `trg_revisiones_bi`: solo un ADMINISTRADOR registra decisiones; CONFORME, ADVERTENCIA y BLOQUEO solo sobre una publicación PUBLICADA; DESBLOQUEO solo sobre una BLOQUEADA.
- `trg_revisiones_ai`: BLOQUEO pasa la publicación a BLOQUEADA; DESBLOQUEO la devuelve a PUBLICADA (o VENCIDA si ya pasó su fecha límite); toda decisión borra la marca; ADVERTENCIA, BLOQUEO y DESBLOQUEO notifican al agricultor (MODERACION).
- Motivos de advertencia y bloqueo: PRECIO_NO_COINCIDE, CANTIDAD_INCORRECTA, PESO_UNIDAD_INCORRECTO, DATOS_INCORRECTOS u OTRO, siempre con mensaje.
- Corrección: si el agricultor cambia los datos de una publicación BLOQUEADA, `trg_publicaciones_bu` le pone la marca CORREGIDA y vuelve a la cola.
- Reclamos: motivo de catálogo (NO_SE_PRESENTO, PRODUCTO_NO_COINCIDE, CANTIDAD_INCOMPLETA, INCUMPLIO_PAGO, MAL_TRATO) y descripción; uno por venta y reclamante; solo si la venta llegó a ACORDADA (según su historial); el admin lo resuelve una sola vez con respuesta.
- Suspensiones: motivo (RECLAMOS_FUNDADOS, DATOS_FALSOS, PUBLICACIONES_ENGANOSAS, USO_INDEBIDO) y detalle; nunca a un administrador; una suspensión abierta por usuario. Suspender pasa el usuario a SUSPENDIDO; registrar `fecha_fin` lo reactiva.
- Métricas: `v_panel_admin`, `v_ventas_mensuales_cultivo`, `v_resumen_usuario` y `v_conversion_publicacion`.

**RF-10 — Venta agrupada:**
- `v_lotes_agrupados` agrupa publicaciones PUBLICADA con kilos disponibles por cultivo, variedad y distrito, con 2 o más agricultores; muestra total de kilos y precio promedio por kilo.
- El comprador crea la propuesta conjunta (estado ABIERTA): variedad, distrito, cantidad en kilos, precio por kilo y fecha de recojo (también límite para responder).
- El service reparte la cantidad en proporción a los kilos disponibles de cada agricultor, redondeando hacia abajo a unidades enteras de su publicación, y crea para cada uno una venta PROPUESTA en su unidad, a través del módulo `venta`, en la conversación de ese comprador con esa publicación (`canal_inicio` = PROPUESTA_CONJUNTA si no existía).
- `trg_transacciones_bi` rechaza la venta si la propuesta no está ABIERTA, si no coinciden comprador, variedad o distrito, o si la cantidad supera lo disponible.
- Solo el agricultor acepta (ACORDADA) o rechaza (RECHAZADA) su propuesta; el comprador puede cancelarla. Cada venta aceptada sigue su ciclo normal (RF-06, RF-11, RF-05).
- El comprador puede cerrar (CERRADA) o cancelar (CANCELADA) la propuesta; el service cancela las ventas PROPUESTA pendientes.

**RF-11 — Entrega:**
- `trg_transacciones_bu` controla las transiciones y quién las hace: ACORDADA → RECOGIDO y RECOGIDO → EN_CAMINO (cualquiera de las partes); EN_CAMINO → ENTREGADO, ACORDADA → ENTREGADO (recojo en chacra) y ENTREGADO → COMPLETADA (solo el comprador).
- "Confirmar que recibí" hace dos cambios en una misma transacción: ENTREGADO y COMPLETADA.
- `trg_transacciones_ai` y `trg_transacciones_au` registran cada estado en `transacciones_historial`. La aplicación nunca escribe en esa tabla y sus filas no se corrigen.

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
- Antes de que una venta quede ACORDADA (acuerdo del chat o propuesta conjunta aceptada), el service bloquea la publicación (`PESSIMISTIC_WRITE`) para evitar sobreventa.
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
- BD: compatible con **MySQL 8.0+ y MariaDB 10.6+** (el desarrollo local usa la versión de MariaDB de Arch Linux, hoy 13.0), `utf8mb4` / `utf8mb4_unicode_ci`, Flyway (`flyway-mysql`), driver MySQL Connector/J.
- Frontend: HTML5 + CSS3 + JavaScript vanilla (ES6+), Bootstrap 5, **sin React/Vue/Angular**.
- Librerías obligatorias: Google Guava, Apache Commons Lang3, Apache POI, Logback (vía Spring Boot).
- Arquitectura verificada con **ArchUnit** (`ArquitecturaTest`).
- Tests: JUnit 5, Mockito, Spring Boot Test, ArchUnit; integración contra la base real `agroconecta_test` (sin H2 ni Testcontainers).

**Versionado y estructura:**
- Git: ramas `main` (entregas), `develop` (integración), `feature/rf-XX-nombre-corto`.
- Monolito modular bajo `pe.agroconecta`: un módulo por dominio (`catalogo`, `usuario`, `publicacion`, `precio`, `mensajeria`, `venta`, `ventaagrupada`, `notificacion`, `administracion`) más `compartido` (`config`, `seguridad`, `error`). Cada módulo tiene las capas `api`, `aplicacion`, `dominio` e `infraestructura`; otro módulo solo usa su capa `aplicacion`. Detalle en `CLAUDE.md` §5.
- BD: `V1__esquema_inicial.sql` (secciones 2–11 de `modelo-bd.sql`) y `V2__datos_iniciales.sql` (sección 12); `ddl-auto=validate` en todos los perfiles. Una migración aplicada nunca se edita; los cambios van en una nueva `Vn` y se reflejan en `modelo-bd.sql`.
- Servidor de BD: `event_scheduler=ON` y privilegios TRIGGER, EVENT, CREATE ROUTINE y ALTER ROUTINE para el usuario de la aplicación (configurado fuera de las migraciones).

### 4.2 Supuestos y decisiones de diseño

| Supuesto | Razón | Impacto |
|---|---|---|
| Costo cero | Proyecto académico sin presupuesto | Solo herramientas gratuitas; despliegue en opción gratuita |
| Monolito modular | Equipo pequeño, un despliegue, bajo acoplamiento (ADR-001) | Módulos por dominio verificados con ArchUnit |
| Sin pasarela de pagos | Alcance MVP + requisito del curso | La transacción solo registra el acuerdo; el pago es externo |
| Sin suscripciones (RF-12) | Extensión futura | Sin comisiones ni modelo freemium en la fase 1 |
| Sin publicación delegada | Simplifica reglas | El agricultor es el único dueño y publicador |
| Transportista no registrado | No es actor y la interfaz solo muestra la línea de tiempo | La entrega se sigue con los estados de la venta |
| Moderación posterior | Simplicidad y rapidez de publicación | La publicación nace PUBLICADA; el admin revisa las marcadas y puede bloquearlas |
| Login con celular | Muchos agricultores no usan email | Sin email; el admin reinicia contraseñas |
| Unidad elegida por el agricultor | Así se comercializa en Piura (jaba, saco…) | Todo se compara en kilos con columnas generadas |
| Triggers + validación en service | Defensa en profundidad | Errores del trigger se devuelven como 422 (ver §9.2) |
| Base de datos como fuente de verdad | Auditoría y consistencia | El esquema no se genera desde Hibernate |
| Evento MySQL para alertas (no `@Scheduled`) | Consistencia temporal única | `ev_tareas_diarias` corre una vez al día sin carreras entre instancias |

---

## 5. Matriz de trazabilidad RF/RNF a módulos y tablas

Cada módulo expone su capa `aplicacion` (servicios y DTO) a los demás; `api` contiene los controladores, `dominio` las entidades y `infraestructura` los repositorios. Los nombres de clases son orientativos: se definen en cada slice.

| Módulo | RF | RNF | Tablas / vistas | Controllers (`api`) | Services (`aplicacion`) | Repositorios (`infraestructura`) |
|---|---|---|---|---|---|---|
| `catalogo` | Soporte de RF-01, RF-02, RF-03, RF-10 | RNF-01, RNF-05 | provincias, distritos, unidades_medida, cultivos, variedades | CatalogoController | CatalogoService | ProvinciaRepository, DistritoRepository, UnidadMedidaRepository, CultivoRepository, VariedadRepository |
| `usuario` | RF-08 | RNF-03, RNF-04, RNF-05 | usuarios, agricultores, compradores | AuthController, UsuarioController | AuthService, UsuarioService | UsuarioRepository, AgricultorRepository, CompradorRepository |
| `publicacion` | RF-01, RF-03, RF-07 (acciones rápidas) | RNF-01, RNF-02, RNF-05 | publicaciones, `v_publicaciones_disponibles` | PublicacionController | PublicacionService | PublicacionRepository, PublicacionDisponibleRepository |
| `precio` | RF-02 | RNF-01, RNF-05 | precios_mercado, `v_precios_vigentes`, `v_rango_precio_plataforma` | PrecioController | PrecioService, CargaPrecioService | PrecioMercadoRepository, PrecioVigenteRepository, RangoPrecioRepository |
| `mensajeria` | RF-04 | RNF-03, RNF-05 | conversaciones, mensajes | ConversacionController | ConversacionService | ConversacionRepository, MensajeRepository |
| `venta` | RF-05, RF-06, RF-11 | RNF-01, RNF-02, RNF-05 | transacciones, transacciones_historial, calificaciones, `v_historial_transacciones`, `v_reputacion_usuario` | VentaController, CalificacionController | VentaService, CalificacionService | TransaccionRepository, HistorialRepository, CalificacionRepository, HistorialTransaccionRepository, ReputacionRepository |
| `ventaagrupada` | RF-10 | RNF-01, RNF-02, RNF-05 | propuestas_grupales, `v_lotes_agrupados` (ventas vía `venta`) | PropuestaConjuntaController | PropuestaConjuntaService | PropuestaGrupalRepository, LoteAgrupadoRepository |
| `notificacion` | RF-07 (y avisos de RF-04, RF-05, RF-06, RF-09, RF-10, RF-11) | RNF-02, RNF-05 | notificaciones (evento `ev_tareas_diarias`) | NotificacionController | NotificacionService | NotificacionRepository |
| `administracion` | RF-09 | RNF-03, RNF-04, RNF-05, RNF-06 | revisiones_publicacion, reclamos, suspensiones, `v_panel_admin`, `v_resumen_usuario`, `v_ventas_mensuales_cultivo`, `v_conversion_publicacion` | ModeracionController, ReclamoController, UsuarioAdminController, PanelController | ModeracionService, ReclamoService, SuspensionService, PanelService | RevisionRepository, ReclamoRepository, SuspensionRepository (+ repositorios de solo lectura de sus vistas) |

---

## 6. Vistas, función, procedimiento, triggers y evento

### 6.1 Vistas (solo lectura desde JPA, `@Immutable`; se recalculan en cada consulta)

```
v_reputacion_usuario         — RF-05: total y promedio de calificaciones recibidas por usuario
v_publicaciones_disponibles  — RF-03/RF-07: publicación con cultivo, variedad, ubicación, unidad, precio por kilo,
                               kilos disponibles, días restantes y si está por vencer
v_lotes_agrupados            — RF-10: kilos disponibles y precio promedio por cultivo, variedad y distrito (≥ 2 agricultores)
v_rango_precio_plataforma    — RF-02: mínimo, máximo y promedio por kilo de ventas COMPLETADA en 30 días
v_precios_vigentes           — RF-02: último precio del mercado por cultivo y zona, en kilo y unidad local, con tendencia
v_historial_transacciones    — RF-06/RF-11: ventas con partes, cultivo, unidad, kilos, precio por kilo y monto total
v_resumen_usuario            — RF-09: usuario con subtipo, estado, suspensión abierta, operaciones, reputación y reclamos
v_conversion_publicacion     — RF-04/RF-09: clics de WhatsApp, contactos, ofertas, acuerdos y completadas por publicación
v_ventas_mensuales_cultivo   — RF-09: ventas completadas, kilos y monto por mes y cultivo
v_panel_admin                — RF-09: tarjetas del panel (activas, por revisar, decisiones de hoy, precios, reclamos)
```

### 6.2 Función, procedimiento y triggers (21)

- `fn_rol_usuario(id_usuario)` devuelve el rol del usuario; la usan los triggers de actores y de administración.
- `sp_sincronizar_publicacion(id_publicacion)` pasa la publicación a VENDIDA cuando lo acordado cubre todos sus kilos, o la devuelve a PUBLICADA/VENCIDA si se cancela un acuerdo y vuelve a haber saldo.

| Trigger | Momento / tabla | Regla | Efecto |
|---|---|---|---|
| `trg_agricultores_bi` | BEFORE INSERT `agricultores` | El usuario tiene rol AGRICULTOR | 45000 si no |
| `trg_compradores_bi` | BEFORE INSERT `compradores` | El usuario tiene rol COMPRADOR | 45000 si no |
| `trg_publicaciones_bi` | BEFORE INSERT `publicaciones` | Estado inicial PUBLICADA; peso de la unidad; `fecha_limite` ≥ hoy | Completa `peso_unidad_kg`, `fecha_limite` y la marca PRIMERA_PUBLICACION |
| `trg_publicaciones_bu` | BEFORE UPDATE `publicaciones` | No se cambia el agricultor; transiciones válidas; no se baja la cantidad por debajo de lo acordado | VENCIDA → PUBLICADA al extender; marca CORREGIDA al corregir una BLOQUEADA |
| `trg_revisiones_bi` | BEFORE INSERT `revisiones_publicacion` | Solo ADMINISTRADOR; decisión coherente con el estado de la publicación | 45000 si falla |
| `trg_revisiones_ai` | AFTER INSERT `revisiones_publicacion` | Aplica la decisión | BLOQUEADA / PUBLICADA / VENCIDA, borra la marca y notifica MODERACION |
| `trg_precios_mercado_bi` | BEFORE INSERT `precios_mercado` | Solo ADMINISTRADOR; distrito zona de precio | 45000 si falla |
| `trg_precios_mercado_bu` | BEFORE UPDATE `precios_mercado` | Solo ADMINISTRADOR | 45000 si no |
| `trg_conversaciones_bi` | BEFORE INSERT `conversaciones` | La publicación está PUBLICADA | 45000 si no |
| `trg_mensajes_bi` | BEFORE INSERT `mensajes` | El remitente participa; OFERTA solo con la publicación PUBLICADA | 45000 si falla |
| `trg_transacciones_bi` | BEFORE INSERT `transacciones` | Inicia ACORDADA (chat) o PROPUESTA (solo venta agrupada); publicación PUBLICADA; quien registra es parte; peso de la unidad; kilos disponibles; coherencia con la propuesta conjunta ABIERTA | Completa `peso_unidad_kg` |
| `trg_transacciones_bu` | BEFORE UPDATE `transacciones` | Finalizadas y términos inmutables; transiciones válidas por usuario; disponibilidad al aceptar una PROPUESTA | 45000 si falla |
| `trg_transacciones_ai` | AFTER INSERT `transacciones` | — | Historial + `sp_sincronizar_publicacion` si nace ACORDADA |
| `trg_transacciones_au` | AFTER UPDATE `transacciones` | — | Historial + `sp_sincronizar_publicacion` en cada cambio de estado |
| `trg_calificaciones_bi` | BEFORE INSERT `calificaciones` | Venta COMPLETADA; evaluador es parte | 45000 si falla |
| `trg_reclamos_bi` | BEFORE INSERT `reclamos` | La venta llegó a ACORDADA; el reclamante es parte; inicia PENDIENTE | 45000 si falla |
| `trg_reclamos_bu` | BEFORE UPDATE `reclamos` | Solo se resuelve una vez, por un ADMINISTRADOR; datos del reclamo inmutables | Completa `fecha_resolucion` |
| `trg_suspensiones_bi` | BEFORE INSERT `suspensiones` | Solo ADMINISTRADOR; nunca a un administrador; una abierta por usuario; RECLAMOS_FUNDADOS exige ≥ 3 fundados | 45000 si falla |
| `trg_suspensiones_ai` | AFTER INSERT `suspensiones` | — | `usuarios.estado` = SUSPENDIDO |
| `trg_suspensiones_bu` | BEFORE UPDATE `suspensiones` | Solo se registra la fecha de reactivación, una vez | 45000 si falla |
| `trg_suspensiones_au` | AFTER UPDATE `suspensiones` | Al registrar `fecha_fin` | `usuarios.estado` = ACTIVO |

Como varios cambios de estado ocurren en triggers y en el evento diario (no en Java), la aplicación no publica eventos de dominio para ellos: los módulos consultan el estado actual a través de la capa `aplicacion` del módulo dueño.

### 6.3 Evento programado

```
ev_tareas_diarias
  Ejecuta: cada día a las 06:00 (hora del servidor)
  Requiere: event_scheduler=ON en el servidor
  Acciones:
    1. Notificaciones POR_VENCER para publicaciones PUBLICADA que vencen en 3 días o en 1 día (una por día)
    2. Publicaciones PUBLICADA con fecha_limite < hoy → VENCIDA
    3. Ventas PROPUESTA de propuestas conjuntas ABIERTA con fecha_recojo < hoy → CANCELADA
    4. Propuestas conjuntas ABIERTA con fecha_recojo < hoy → CERRADA
```

---

## 7. Reglas de negocio críticas

### 7.1 Ciclo de la transacción (RF-06, RF-10, RF-11)

```
ESTADOS:
  Acuerdo del chat:   ACORDADA → RECOGIDO → EN_CAMINO → ENTREGADO → COMPLETADA
  Recojo en chacra:   ACORDADA → ENTREGADO → COMPLETADA
  Venta agrupada:     PROPUESTA → ACORDADA (sigue igual) | RECHAZADA | CANCELADA
  Cancelación:        solo ACORDADA → CANCELADA

1. El agricultor publica (RF-01) → publicación PUBLICADA.
2. Comprador y agricultor conversan y se envían ofertas (RF-04).
3. Una de las partes registra el acuerdo → venta ACORDADA. Si se agotan los kilos, la publicación pasa a VENDIDA.
4a. Con transporte (RF-11): RECOGIDO → EN_CAMINO (cualquiera de las partes); el comprador confirma que recibió
    → ENTREGADO y COMPLETADA en una misma transacción.
4b. Recojo en chacra: el comprador confirma que recibió → ENTREGADO y COMPLETADA.
5. Ambas partes pueden calificar (RF-05).
```

**Reglas:** una venta solo avanza, nunca retrocede; una finalizada (COMPLETADA, RECHAZADA, CANCELADA) no se modifica; los términos no cambian (se cancela y se registra otra); desde RECOGIDO no se cancela y los problemas van por reclamo; cada cambio de estado fija `id_actualizado_por` y queda en `transacciones_historial`.

### 7.2 Reputación y calificación

```
Agricultor y comprador se califican mutuamente, 1–5 estrellas + comentario opcional.
Solo si la venta está COMPLETADA; una calificación por evaluador y venta.
Reputación = promedio de las calificaciones recibidas (v_reputacion_usuario).
Visible en el perfil y en los resultados de búsqueda.
```

### 7.3 Perecibilidad y vencimiento

```
fecha_limite = fecha_cosecha + cultivos.vida_util_dias si el agricultor no la indica.
Evento diario:
  - 3 días antes: notificación POR_VENCER "Su [cultivo] vence en 3 días."
  - 1 día antes:  notificación POR_VENCER "Su [cultivo] vence mañana."
  - Pasada la fecha límite: estado → VENCIDA

Estados de la publicación: PUBLICADA, BLOQUEADA, VENDIDA, VENCIDA, RETIRADA.
Transiciones: PUBLICADA → BLOQUEADA | VENDIDA | VENCIDA | RETIRADA
              BLOQUEADA → PUBLICADA | VENCIDA | RETIRADA
              VENDIDA   → PUBLICADA | VENCIDA
              VENCIDA   → PUBLICADA | RETIRADA

El agricultor puede:
  - Bajar el precio por unidad
  - Extender la fecha límite (una VENCIDA vuelve a PUBLICADA)
  - Retirar la publicación (RETIRADA)
BLOQUEADA solo la asigna el administrador (moderación posterior).
```

### 7.4 Precios de referencia

```
El admin carga (RF-02) un precio por kilo por cultivo, zona de precio y fecha, con fuente, método y administrador.
v_precios_vigentes muestra el último precio en kilo y en la unidad local del cultivo, con tendencia.
v_rango_precio_plataforma muestra el rango de las ventas completadas en 30 días.
El agricultor ve ambos al publicar (no es obligatorio que coincidan). Si su precio se aleja del rango,
el service marca la publicación PRECIO_FUERA_RANGO (umbral a definir en el slice de RF-09).
```

---

## 8. Restricciones de datos

### 8.1 Integridad referencial

- Cada publicación → agricultor + variedad (y por ella el cultivo) + unidad + distrito.
- Cada conversación → publicación + comprador (única por par).
- Cada venta → conversación (de ella salen la publicación, el agricultor y el comprador) + unidad acordada (+ propuesta conjunta opcional) + usuario que hizo el último cambio.
- Cada fila del historial → venta + usuario (NULL = proceso automático).
- Cada calificación → venta + evaluador; el evaluado se deriva (contraparte) en `v_reputacion_usuario`.
- Cada mensaje → conversación + remitente.
- Cada revisión → publicación + administrador.
- Cada reclamo → venta + reclamante (+ administrador que lo resuelve).
- Cada suspensión → usuario suspendido + administrador.
- Cada notificación → usuario (+ `id_referencia` sin FK, según su tipo).

En Java, las entidades de un módulo referencian a las de otro módulo solo por ID; las claves foráneas se mantienen en la base de datos.

### 8.2 Validaciones de dominio

| Columna | Restricción | Por qué |
|---|---|---|
| `usuarios.celular` | NOT NULL, UNIQUE, `^9[0-9]{8}$` | Identificador de inicio de sesión |
| `usuarios.rol` | AGRICULTOR, COMPRADOR, ADMINISTRADOR | Roles fijos |
| `usuarios.id_distrito` | NOT NULL | Ubicación de todo usuario |
| `usuarios.password_hash` | NOT NULL, hash BCrypt | Seguridad obligatoria |
| `usuarios.estado` | ACTIVO, SUSPENDIDO, INACTIVO | Control de acceso |
| `compradores.tipo_comprador` | MERCADO_MAYORISTA, RESTAURANTE, HOTEL, PROCESADORA, MOLINO, OTRO | Segmento del comprador |
| `compradores.ruc` | NULL o `^[12][0-9]{10}$`, UNIQUE | RUC opcional |
| `publicaciones.cantidad`, `peso_unidad_kg`, `precio_unitario` | > 0 | Sin cantidades ni precios negativos |
| `publicaciones.fecha_limite` | ≥ `fecha_cosecha` | Coherencia de fechas |
| `publicaciones.estado` | PUBLICADA, BLOQUEADA, VENDIDA, VENCIDA, RETIRADA | Máquina de estados |
| `publicaciones.marca_revision` | NULL o PRIMERA_PUBLICACION, PRECIO_FUERA_RANGO, CANTIDAD_INUSUAL, CORREGIDA | Cola de moderación |
| `revisiones_publicacion.decision` | CONFORME, ADVERTENCIA, BLOQUEO, DESBLOQUEO; advertencia y bloqueo con motivo y mensaje | Moderación justificada |
| `precios_mercado.precio_kg` | > 0; UK cultivo + zona + fecha | Un precio por día |
| `mensajes.tipo` | TEXTO (con contenido, máx. 1000) u OFERTA (cantidad y precio > 0) | Mensaje u oferta estructurada |
| `transacciones.estado` | PROPUESTA, ACORDADA, RECOGIDO, EN_CAMINO, ENTREGADO, COMPLETADA, RECHAZADA, CANCELADA | Máquina de estados |
| `calificaciones.puntuacion` | BETWEEN 1 AND 5; UK venta + evaluador | Rating 1–5 |
| `calificaciones.comentario` | máx. 500 caracteres, opcional | UX liviana |
| `reclamos.motivo` / `estado` | Motivos de catálogo; PENDIENTE, FUNDADO, INFUNDADO | Reclamos justificados |
| `suspensiones.motivo` | RECLAMOS_FUNDADOS, DATOS_FALSOS, PUBLICACIONES_ENGANOSAS, USO_INDEBIDO + detalle | Suspensión justificada |

### 8.3 Política de eliminación

- **Usuarios:** borrado lógico con `estado = INACTIVO`.
- **Publicaciones:** borrado lógico con `estado = RETIRADA`.
- **Ventas, historial, mensajes, revisiones, reclamos y suspensiones:** nunca se eliminan (auditoría).

---

## 9. Restricciones de seguridad

### 9.1 Autenticación y autorización

- El login devuelve un JWT firmado HS256 con el ID del usuario como `sub` y el claim `roles`. La clave se lee de la variable de entorno `JWT_SECRET`.
- En cada petición autenticada se verifica que el usuario siga ACTIVO: `compartido.seguridad` define la interfaz del verificador y `usuario` la implementa. Un usuario SUSPENDIDO o INACTIVO pierde el acceso de inmediato (401).

| Recurso | Rol permitido | Método |
|---|---|---|
| POST /api/v1/auth/registro | Público | Celular, contraseña, distrito, rol (AGRICULTOR o COMPRADOR) |
| POST /api/v1/auth/login | Público | Celular + contraseña → JWT |
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
  "error": "Unprocessable Content",
  "mensaje": "[mensaje del trigger en español]",
  "ruta": "/api/v1/transacciones",
  "errores": []
}
```

`errores` contiene el detalle por campo (`campo`, `mensaje`) solo en errores de validación (400); en los demás casos es una lista vacía. `error` es la frase estándar del código HTTP. Nunca se exponen detalles de SQL.

**Mapeo (se evalúa el código de error del motor antes que el SQLSTATE; `compartido.error.ManejadorGlobalExcepciones`):**
- Bean Validation → 400.
- Trigger (SQLSTATE 45000, con su mensaje), CHECK (3819 MySQL / 4025 MariaDB, mensaje genérico) o regla de negocio del service (`ReglaNegocioException`) → **422**.
- Clave duplicada o FK (1062, 1451, 1452) → **409**, mensaje genérico.
- Cualquier otro error → 500, mensaje genérico; el detalle va al log.
- 401 sin token, token inválido o usuario no ACTIVO; 403 rol o dueño incorrecto; 404 recurso inexistente.

### 9.3 Protecciones frontend

- Token JWT en `sessionStorage` (no `localStorage`).
- Datos de usuario con `textContent`, no `innerHTML`.
- No registrar contraseñas, tokens ni contenido de mensajes en logs.
- HTTPS obligatorio en producción (certificado gratuito).

---

## 10. Requisitos de documentación y entrega

### 10.1 Documentación técnica

- `CLAUDE.md` — instrucciones para Claude Code y reglas del proyecto (incluye la arquitectura modular).
- `docs/02-requisitos/` — este SRS, casos de uso UC-01 a UC-12 y matriz de trazabilidad.
- `docs/03-procesos/` — BPMN TO-BE (`.drawio`, imagen y descripción textual).
- `docs/04-diseno/base-datos/` — `modelo-bd.sql` v4.1 (20 tablas, 36 FK, 1 función, 1 procedimiento, 21 triggers, 10 vistas, 1 evento), `cambios-v4.md`, `modelo-logico.md` (un diagrama ER en Mermaid por módulo) y `notas-integridad-bd.md`.
- `docs/05-api/endpoints.md` — contrato REST.
- `docs/06-decisiones/` — registros de decisiones (ADR).
- Javadoc — toda clase y método públicos.
- README.md — instalación, despliegue y entorno de desarrollo.

### 10.2 Documentación de usuario

- Guía de registro (agricultor vs comprador).
- Manual de publicación de cosechas.
- Manual de búsqueda, contacto, ofertas, acuerdos y venta agrupada.
- FAQ de perecibilidad, alertas y reclamos.
- Términos de servicio y aviso de privacidad (documento informativo conforme a la Ley N.° 29733).

### 10.3 Código y pruebas

- Cobertura ≥ 70 % en la capa `aplicacion` (JaCoCo).
- TDD en los servicios.
- Pruebas de integración contra MariaDB/MySQL real (`agroconecta_test`).
- Prueba de arquitectura (`ArquitecturaTest`, ArchUnit) en verde.
- Sin lógica de negocio en controllers.
- Validación con Bean Validation en los `Request` de `api`.

---

## 11. Orden de implementación recomendado

Ordenado por dependencias: cada slice solo necesita lo implementado antes (detalle en `CLAUDE.md` §12.5).

1. **RF-08** (`catalogo` + `usuario`) — registro, login con celular, JWT y roles; esqueleto andante desplegado
2. **RF-01** (`publicacion`) — publicar cosecha
3. **RF-09, solo moderación** (`administracion`) — cola "Por revisar"
4. **RF-02** (`precio`) — precios de referencia
5. **RF-03** (`publicacion`) — búsqueda y filtros
6. **RF-04** (`mensajeria`) — chat, ofertas y WhatsApp
7. **RF-06 + RF-11** (`venta`) — acuerdo y seguimiento de la entrega
8. **RF-05** (`venta`) — calificaciones
9. **RF-07** (`notificacion` + acciones rápidas en `publicacion`) — alertas de vencimiento
10. **RF-10** (`ventaagrupada`) — venta agrupada
11. **RF-09, resto** (`administracion`) — reclamos, suspensiones y panel

---

## 12. Hitos del curso

| Hito | Semana | Fecha aprox. | Entregable |
|---|---|---|---|
| APF1 | 4 | 3 sept 2026 | ✓ Entregado (Lean Canvas, requisitos, prototipos) |
| APF2 | 8 | ~1 oct 2026 | BPMN + diseño de BD + prototipos UI |
| Avance 30 % | 10 | — | Slices 1 a 3; mostrar librerías Java: Guava, Apache Commons, Apache POI, Logback |
| Avance 40 % | 11 | — | Slices 4 y 5; integrado con Git y GitHub |
| APF3 | 12 | ~29 oct 2026 | Slices 6 y 7: backend + frontend funcionando |
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
| (N/A) | Correos, SMS, push, email de usuario | Sin infraestructura de mensajería y con costo cero | Decisión de alcance |
| (N/A) | Aprobación previa de publicaciones, fotos | Moderación posterior; el esquema no los contempla | Decisión de alcance |
| (N/A) | Transportista, vehículo o placa | La entrega se sigue con los estados de la venta | Decisión de alcance |
| (N/A) | Venta por área (hectárea, poza) | Solo unidades de peso o conteo | Decisión de alcance |
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
**Última actualización:** 27 de septiembre de 2026  
**Próxima revisión:** después de APF2 (diseño) si hay cambios en alcance o BPMN.
