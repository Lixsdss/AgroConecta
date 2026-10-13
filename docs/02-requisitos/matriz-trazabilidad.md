# Matriz de trazabilidad — AgroConecta

Relaciona cada requisito funcional con su caso de uso, módulo, tablas, endpoints, pantallas y pruebas. Se actualiza al cerrar cada slice (CLAUDE.md §12.2).

**Fuentes:** SRS v1.3, casos de uso v1.4, `modelo-bd.sql` v4.1, `docs/05-api/endpoints.md`, `docs/04-diseno/ux-ui/pantallas.md`.

## Resumen

| RF | Nombre | UC | Módulo | Slice | Estado |
|---|---|---|---|---|---|
| **RF-01** | **Publicación de cosecha** | **UC-01** | **publicacion (+ catalogo)** | **2** | **Implementado** (referencia de precio: slice 4; marcas PRECIO_FUERA_RANGO y CANTIDAD_INUSUAL: slice 3) |
| **RF-02** | **Precios de mercado** | **UC-02** | **precio** | **4** | **Implementado (slice 4)**; también activa la marca PRECIO_FUERA_RANGO de RF-01 |
| **RF-03** | **Búsqueda multi-criterio** | **UC-03** | **publicacion** | **5** | **Implementado (slice 5)**; "Escribir" y "WhatsApp" del resultado llegan con RF-04 (slice 6) |
| **RF-04** | **Mensajería directa** | **UC-04** | **mensajeria** | **6** | **Implementado (slice 6)**; "Registrar acuerdo" del chat llega con RF-06 (slice 7) |
| **RF-05** | **Calificación** | **UC-07** | **venta** | **8** | **Implementado (slice 8)**; UC-07 A3 (comentario inapropiado) se atiende con la suspensión de RF-09 (slice 11) |
| **RF-06** | **Registro e historial de transacciones** | **UC-05, UC-06 A1, UC-08** | **venta** | **7** | **Implementado (slice 7)**; calificaciones en el detalle con RF-05 (slice 8); reclamos con RF-09 (slice 11); PROPUESTA con RF-10 (slice 10) |
| **RF-07** | **Alertas de vencimiento** | **UC-09** | **notificacion, publicacion** | **9** | **Implementado (slice 9)** |
| **RF-08** | **Registro y gestión de usuarios** | **UC-10** | **usuario (+ catalogo)** | **1** | **Implementado** (gestión por el administrador: slice 11) |
| **RF-09** | **Panel administrativo** | **UC-11** | **administracion** | **3 y 11** | **Moderación implementada (slice 3)**; reclamos, suspensiones, usuarios y panel: slice 11; corrección de una publicación bloqueada (CORREGIDA): implementada en el slice 9 |
| **RF-10** | **Venta agrupada** | **UC-12** | **ventaagrupada** | **10** | **Implementado (slice 10)** |
| **RF-11** | **Seguimiento de entrega** | **UC-06** | **venta** | **7** | **Implementado (slice 7)** |

## Detalle por requisito

### RF-08 — Registro y gestión de usuarios (slice 1)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-10: flujo básico (registro), inicio de sesión, cambio de contraseña propio; A1 celular o RUC duplicado, A2 contraseña débil, A3 credenciales incorrectas o cuenta no ACTIVA, A4 olvidó su contraseña, A6 contraseña actual incorrecta. A5 (alta de administrador desde el panel) queda para el slice 11; el primer administrador lo crea `InicializadorAdministrador` al arrancar. |
| Módulos | `usuario` (dueño); `catalogo` (provincias y distritos); `compartido.seguridad` (JWT, validador de usuario ACTIVO) |
| Tablas | usuarios, agricultores, compradores (escritura); provincias, distritos (lectura) |
| Triggers y restricciones | `trg_agricultores_bi`, `trg_compradores_bi`; `chk_usuario_celular`, `chk_usuario_rol`, `chk_usuario_estado`, `chk_comprador_tipo`, `chk_comprador_ruc`; UK de `celular` y `ruc` |
| Endpoints | `GET /api/v1/catalogo/provincias`, `GET /api/v1/catalogo/distritos`, `POST /api/v1/auth/registro`, `POST /api/v1/auth/login`, `GET` y `PUT /api/v1/usuarios/me`, `PUT /api/v1/usuarios/me/contrasena` |
| Pantallas | 02 Ingresar (`ingresar.html`), 03 ¿Quién es usted? (`rol.html`), 04 Cree su cuenta (`registro.html`), 05 Bienvenida (`bienvenida.html`); de apoyo: `inicio.html`, `perfil.html` |
| Pruebas | Unitarias: `RegistroServiceTest`, `SesionServiceTest`, `PerfilServiceTest`, `EmisorTokenTest`, `InicializadorAdministradorTest`, `ReglasUsuarioTest`, `CatalogoServiceTest`, `SeguridadUnitariaTest`, `ManejadorGlobalExcepcionesTest`. Integración (MariaDB): `AuthIntegracionTest`, `CatalogoIntegracionTest`, `TriggersUsuarioIntegracionTest`. Arquitectura: `ArquitecturaTest` |
| RNF | RNF-02 (API stateless, verificación de estado por clave primaria), RNF-03 (botones ≥ 48 px), RNF-04 (Javadoc), RNF-05 (triggers y CHECK como última barrera) |

### RF-01 — Publicación de cosecha (slice 2)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-01: flujo básico (3 pasos, confirmación, publicada); A1 validación falla (400/422); A2 el agricultor cancela (el borrador queda en el navegador y no se guarda nada en la base de datos); A3 moderación posterior (marca PRIMERA_PUBLICACION por trigger; advertir y bloquear en el slice 3) |
| Módulos | `publicacion` (dueño); `catalogo` (cultivos, variedades, unidades, distritos) |
| Tablas | publicaciones (escritura); cultivos, variedades, unidades_medida, distritos (lectura) |
| Triggers y restricciones | `trg_publicaciones_bi` (nace PUBLICADA, peso de unidades fijas, fecha límite calculada, fecha límite ≥ hoy, PRIMERA_PUBLICACION); columnas generadas `cantidad_kg` y `precio_kg`; `chk_publicacion_cantidad`, `_peso`, `_precio`, `_fechas`, `_estado`, `_marca` |
| Endpoints | `GET /api/v1/catalogo/cultivos`, `GET /api/v1/catalogo/variedades`, `GET /api/v1/catalogo/unidades`, `POST /api/v1/publicaciones`, `GET /api/v1/publicaciones/{id}` |
| Pantallas | 07 Paso 1 (`paso1.html`), 08 Paso 2 (`paso2.html`), 09 Paso 3 (`paso3.html`), 10 Confirmar (`confirmar.html`), 11 Publicada (`publicada.html`) |
| Pruebas | Unitarias: `PublicacionServiceTest`, `CatalogoServiceTest`. Integración (MariaDB): `PublicacionIntegracionTest` (jaba, quintal con 46 kg, fecha límite calculada, PRIMERA_PUBLICACION solo en la primera, 400/401/403/404/422, catálogo). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-01 (páginas livianas, borrador en `sessionStorage`), RNF-03 (una pregunta por bloque, botones ≥ 56 px), RNF-05 (trigger y CHECK como última barrera) |

### RF-02 — Precios de referencia (slice 4)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-02: flujo básico (precios vigentes por cultivo y zona con unidad local, fuente, fecha y tendencia; rango de la plataforma en el paso 3; historial de 30 días); A1 sin datos ("No hay precios de referencia para este cultivo", 204 en la referencia); A2 carga por formulario (409 si ya existe, con "Editar") o por archivo Excel/CSV (filas válidas guardadas, inválidas informadas); A3 filtro sin resultados. UC-01: referencia ±15 % y veredicto en el paso 3; marca PRECIO_FUERA_RANGO |
| Módulos | `precio` (dueño); `catalogo` (cultivos, unidades, zonas); `publicacion` (usa `precio.aplicacion.ReferenciaPrecioService` para la marca) |
| Tablas | precios_mercado (escritura); `v_precios_vigentes`, `v_rango_precio_plataforma` (lectura); cultivos, unidades_medida, distritos (lectura) |
| Triggers y restricciones | `trg_precios_mercado_bi` (solo ADMINISTRADOR, distrito zona de precio), `trg_precios_mercado_bu` (solo ADMINISTRADOR); `chk_precio_valor`, `chk_precio_fuente`, `chk_precio_metodo`; UK cultivo + distrito + fecha |
| Endpoints | `GET /api/v1/precios/vigentes`, `GET /api/v1/precios/historial`, `GET /api/v1/precios/referencia`, `POST /api/v1/precios`, `PUT /api/v1/precios/{id}`, `POST /api/v1/precios/archivo` |
| Pantallas | 01 Inicio público (`index.html`, con gráfico SVG de 30 días), 06/14 Inicio del agricultor y del comprador (`inicio.html`, "Precios de hoy"), 09 Paso 3 (`paso3.html`, referencia y veredicto), 11b Precios de mercado (`pages/administracion/precios.html`) |
| Pruebas | Unitarias: `ReferenciaPrecioServiceTest` (zona, promedio de zonas, 7 días, bajo/dentro/alto), `PublicacionServiceTest` (prioridad PRECIO_FUERA_RANGO). Integración (MariaDB): `PrecioIntegracionTest` (tendencia tras editar, duplicado 409, zona 422, fecha futura 400, 403/404, referencia de zona/promedio/204, CSV con filas rechazadas, Excel, formato inválido 400, marca PRECIO_FUERA_RANGO). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-01 (gráfico SVG propio sin librerías, página pública liviana), RNF-03 (chips ≥ 48 px, veredicto con ícono y texto, no solo color), RNF-05 (triggers y CHECK como última barrera) |

### RF-03 — Búsqueda multicriterio (slice 5)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-03: flujo básico (filtros opcionales de cultivo, variedad, provincia/distrito, unidad y fecha de cosecha; orden por precio por kilo, fecha de cosecha o calificación; 20 por página; resultado con cantidad disponible, precios, ubicación, días restantes y calificación; detalle); A1 sin resultados ("No encontramos productos" y "Quitar filtros"); A2 visitante sin sesión (búsqueda y detalle públicos; contactar llega con RF-04); A3 paginación ("Ver más") |
| Módulos | `publicacion` (dueño de `v_publicaciones_disponibles`); `catalogo` (filtros y nombre de la unidad) |
| Tablas | `v_publicaciones_disponibles` (lectura; ya incluye `v_reputacion_usuario`); publicaciones, cultivos, variedades, provincias, distritos, unidades_medida (lectura) |
| Triggers y restricciones | Ninguno (solo lectura). Índices `idx_publicacion_busqueda` e `idx_publicacion_precio_kg` |
| Endpoints | `GET /api/v1/publicaciones` (búsqueda), `GET /api/v1/publicaciones/{id}` (detalle, con agricultor, calificación, disponibilidad y días restantes) |
| Pantallas | Buscar productos (`pages/publicacion/buscar.html`, con hoja de filtros) y Detalle de la cosecha (`pages/publicacion/detalle.html`); entradas desde la portada (01) y el inicio (06/14) |
| Pruebas | Unitarias: `BusquedaServiceTest` (orden por defecto, 20 por página, orden por calificación, unidades disponibles redondeadas hacia abajo, rango de fechas y página inválidos), `PublicacionServiceTest`. Integración (MariaDB): `BusquedaIntegracionTest` (datos de cada resultado, filtros combinados y orden por precio, provincia y fecha de cosecha, excluye BLOQUEADA y VENCIDA, paginación de 20, página vacía, errores 400, detalle). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-01 (20 resultados por página y "Ver más", sin librerías), RNF-03 (chips ≥ 48 px, "Vence en N días" con ícono y texto) |

Reglas aprobadas en el plan del slice 5: solo publicaciones con fecha límite hoy o después; desempates (precio por kilo → la más reciente; fecha de cosecha y calificación → menor precio por kilo; al final el id); los agricultores sin calificación van al final en el orden por calificación.

### RF-04 — Mensajería, ofertas y WhatsApp (slice 6)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-04: flujo básico (Escribir abre o reutiliza la conversación; mensajes y ofertas; aviso a la otra parte; consulta periódica; la conversación no se cierra); A1 WhatsApp (crea la conversación, suma el clic y abre `wa.me`); A2 sin acuerdo (la conversación queda); A3 oferta sobre publicación no disponible (422; el texto sigue); A4 remitente no autorizado (403). Paso 8 "Registrar acuerdo" → UC-05 (slice 7). UC-03 A2: el visitante va a "Ingresar" y vuelve a la cosecha |
| Módulos | `mensajeria` (dueño); `publicacion` (`PublicacionConsultaService`: publicación en cualquier estado, ids del agricultor); `usuario` (`UsuarioConsultaService`: nombre, negocio y celular); `notificacion` (`NotificacionService.crear`, avisos MENSAJE y OFERTA) |
| Tablas | conversaciones, mensajes, notificaciones (escritura); `v_publicaciones_disponibles`, usuarios, compradores (lectura) |
| Triggers y restricciones | `trg_conversaciones_bi` (solo PUBLICADA), `trg_mensajes_bi` (remitente participa; OFERTA solo con PUBLICADA); `uk_conversacion_pub_comprador`, `chk_conversacion_canal`, `chk_conversacion_clics`, `chk_mensaje_tipo`, `chk_notificacion_tipo` |
| Endpoints | `POST /api/v1/conversaciones`, `POST /api/v1/conversaciones/whatsapp`, `GET /api/v1/conversaciones`, `GET /api/v1/conversaciones/no-leidos`, `GET /api/v1/conversaciones/{id}`, `GET` y `POST /api/v1/conversaciones/{id}/mensajes` |
| Pantallas | Mensajes (`pages/mensajeria/conversaciones.html`), Chat con hoja "Hacer oferta" (`pages/mensajeria/chat.html`); botones Escribir y WhatsApp en la búsqueda y el detalle; "Mensajes" con no leídos en el inicio |
| Pruebas | Unitarias: `ConversacionServiceTest` (enlace wa.me codificado, publicación no disponible, 404, 403, oferta mayor que lo disponible, agricultor sin publicaciones, página negativa), `NotificacionServiceTest` (recorte del texto, avisos sin repetir), `UsuarioConsultaServiceTest`. Integración (MariaDB): `MensajeriaIntegracionTest` (una conversación por par, agricultor 403, publicación no disponible 422, texto y oferta con avisos MENSAJE sin repetir y OFERTA, no leídos y leídos, lista, consulta con `despuesDe`, oferta mayor que lo disponible y sobre publicación vencida 422 con texto aceptado, tercero 403, validación por campo, WhatsApp con canal y clics, sin sesión 401). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-01 (consulta periódica cada 10 s / 30 s, sin WebSockets ni librerías), RNF-02 (API sin estado), RNF-03 (botones ≥ 48 px, ofertas con ícono y texto), RNF-05 (triggers como última barrera) |

Reglas aprobadas en el plan del slice 6: ambas partes pueden ofertar; la oferta no supera las unidades disponibles (422); aviso OFERTA siempre y MENSAJE solo si no hay uno sin leer de la misma conversación, sin copiar el texto; el celular del agricultor solo viaja dentro del enlace `wa.me`.

### RF-06 y RF-11 — Acuerdo, entrega e historial (slice 7)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-05: registrar acuerdo desde el chat, precargado con la última oferta (1), unidad acordada distinta de la publicada con su peso (2), validaciones (3), bloqueo de la publicación y venta ACORDADA (4), VENDIDA al agotar los kilos (5), aviso VENTA con el contacto (6); A1 cancelar desde ACORDADA (vuelve a PUBLICADA); A2 kilos o publicación no disponible (422); A3 términos fijos (sin edición); A4 no se cancela tras el recojo (422). UC-06: RECOGIDO y EN_CAMINO por cualquiera de las partes, "Confirmar que recibí" (ENTREGADO y COMPLETADA en una transacción) solo por el comprador, aviso a la otra parte; A1 recojo en chacra desde ACORDADA; A3 fuera de secuencia (422); A4 la línea de tiempo no se corrige. UC-08: Mis ventas / Mis compras con grupo En curso / Terminadas, fechas y contraparte, 20 por página, detalle con línea de tiempo y enlace al chat, Excel; A1 sin ventas; A2 filtro sin resultados; A3 `AgroConecta_Historial_{fecha}.xlsx`. Paso 6 de UC-08 (calificaciones) → slice 8 |
| Módulos | `venta` (dueño); `mensajeria` (`ConversacionService.participantes`); `publicacion` (`PublicacionConsultaService.bloquearParaVenta` con `PESSIMISTIC_WRITE`); `usuario` (nombres, negocio y celular); `catalogo` (unidades); `notificacion` (avisos VENTA) |
| Tablas | transacciones (escritura); transacciones_historial y `v_historial_transacciones` (lectura); publicaciones (bloqueo; estado por `sp_sincronizar_publicacion`); notificaciones (escritura) |
| Triggers y restricciones | `trg_transacciones_bi` (nace ACORDADA, publicación PUBLICADA, quien registra es parte, peso de la unidad, kilos disponibles), `trg_transacciones_bu` (finales no cambian, términos fijos, transiciones por usuario), `trg_transacciones_ai/au` (línea de tiempo y `sp_sincronizar_publicacion`); `chk_transaccion_*` |
| Endpoints | `POST /api/v1/ventas`, `POST /api/v1/ventas/{id}/estado`, `GET /api/v1/ventas/{id}`, `GET /api/v1/ventas`, `GET /api/v1/ventas/exportar` |
| Pantallas | Registrar acuerdo (`pages/venta/acuerdo.html`), Entrega (`pages/venta/venta.html`), Mis ventas / Mis compras (`pages/venta/historial.html`); botón "Registrar acuerdo" y franja de ventas en el chat; botón en el inicio |
| Pruebas | Unitarias: `VentaServiceTest` (acciones por estado y parte, tercero 403, peso de jaba o saco, publicación no disponible y kilos, mensajes de cambio no permitido, 404), `HistorialVentasServiceTest` (grupo, fechas y página inválidos, tope de 5000 en Excel). Integración (MariaDB): `VentaIntegracionTest` (acuerdo que vende la publicación y cancelación que la devuelve, aviso con celular, camino con transporte y línea de tiempo con autor, recojo en chacra, unidad distinta y validaciones, tercero 403 y acción inválida 400, historial por grupo, contraparte y conversación, Excel leído con Apache POI). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-01 (Excel generado en el servidor, sin librerías en el navegador), RNF-02 (bloqueo `PESSIMISTIC_WRITE` de la publicación y de la venta), RNF-03 (solo los botones permitidos, estados con ícono y texto), RNF-05 (triggers como última barrera) |

Reglas aprobadas en el plan del slice 7: día de recojo hoy o después; las 5 unidades del catálogo (jaba o saco distinta de la publicada pide el peso); el aviso del acuerdo lleva nombre y celular de quien registra; filtro de contraparte por texto y fechas por fecha del acuerdo; Excel con hasta 5000 ventas; cancelar con confirmación y sin motivo.

### RF-05 — Calificaciones y reputación (slice 8)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-07: "Calificar a …" desde la entrega o el historial (1), 1–5 estrellas y comentario opcional de hasta 500 caracteres (2–3), validaciones (4), registro (5), aviso CALIFICACION (6), reputación calculada por la vista (7); A1 venta no terminada (422); A2 ya calificada (409, no se edita); A3 comentario inapropiado → suspensión del administrador (slice 11). UC-08 paso 6: calificaciones dada y recibida en el detalle. SRS §7.2: reputación visible en el perfil y en la búsqueda |
| Módulos | `venta` (dueño de calificaciones y `v_reputacion_usuario`); `usuario` (existencia y rol); `notificacion` (aviso CALIFICACION) |
| Tablas | calificaciones (escritura); `v_reputacion_usuario`, `v_historial_transacciones` (lectura; partes de la venta); `v_publicaciones_disponibles` ya muestra el promedio en la búsqueda |
| Triggers y restricciones | `trg_calificaciones_bi` (solo COMPLETADA, solo una parte), `uk_calificacion_trans_evaluador`, `chk_calificacion_puntuacion` |
| Endpoints | `POST /api/v1/ventas/{id}/calificacion`, `GET /api/v1/reputaciones/{idUsuario}` (público); `GET /api/v1/ventas/{id}` y `GET /api/v1/ventas` agregan las calificaciones |
| Pantallas | Entrega con "Calificaciones" y la hoja "¿Cómo le fue con…?"; Mis ventas / Mis compras (Terminadas) con "Calificar a …" o "Usted calificó"; detalle de la cosecha con "Lo que dicen del agricultor"; Mi perfil con "Mi reputación" |
| Pruebas | Unitarias: `CalificacionServiceTest` (no terminada, tercero, ya calificada, 404, reputación sin calificaciones, administrador e inexistente, página negativa), `VentaServiceTest`, `HistorialVentasServiceTest`. Integración (MariaDB): `CalificacionIntegracionTest` (ambas partes califican una vez, comentario recortado o nulo, 409, aviso, `miPuntuacion`, reputación pública con autor, promedio en la publicación, 404, validación de puntuación y comentario, 403 y 401). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-03 (estrellas grandes con texto "Muy mal … Muy bien" y descripción para lectores de pantalla), RNF-05 (trigger y clave única como última barrera) |

Reglas aprobadas en el plan del slice 8: la reputación es pública; el autor de un comentario es el negocio del comprador o el nombre del agricultor (sin celulares); en la búsqueda solo el promedio; sin reputación en el chat por ahora; sin plazo para calificar.

### RF-07 — Avisos y alertas de vencimiento (slice 9)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-09: el evento diario crea los avisos POR_VENCER a 3 días y a 1 día sin repetir y pasa a VENCIDA las atrasadas (1, 4, base de datos); el agricultor ve el aviso en "Avisos", lo abre y se marca leído (2); acciones rápidas bajar precio, extender +7/+14/+30 y retirar (3); extender una VENCIDA la devuelve a PUBLICADA (5); A1 sin alertas fuera de PUBLICADA (evento); A2 varios avisos juntos, ordenados por fecha. UC-11 parte 2 paso 4: corregir una publicación oculta → CORREGIDA y vuelve a la cola |
| Módulos | `notificacion` (centro de avisos); `publicacion` (Mis cosechas y acciones rápidas, `MisCosechasService`); `catalogo` (unidades) |
| Tablas | notificaciones (lectura y marca de leída); publicaciones (precio, fecha límite, estado RETIRADA, corrección); `v_publicaciones_disponibles` (lectura); evento `ev_tareas_diarias` |
| Triggers y restricciones | `ev_tareas_diarias` (POR_VENCER y VENCIDA); `trg_publicaciones_bu` (VENCIDA → PUBLICADA al extender, fecha límite ≥ hoy, transiciones de estado, CORREGIDA al corregir una BLOQUEADA, cantidad ≥ lo acordado); índice `idx_notificacion_usuario` |
| Endpoints | `GET /api/v1/notificaciones`, `GET /api/v1/notificaciones/no-leidas`, `POST /api/v1/notificaciones/{id}/leida`, `POST /api/v1/notificaciones/leidas`; `GET /api/v1/publicaciones/mias`, `POST /api/v1/publicaciones/{id}/precio`, `/extender`, `/retirar`, `PUT /api/v1/publicaciones/{id}/correccion` |
| Pantallas | Avisos (`pages/notificacion/avisos.html`) con la campana del inicio; Mis ventas del agricultor (`pages/publicacion/mis-cosechas.html`) con el aviso de vencimiento y las hojas de cada acción; la barra inferior lleva "Mis ventas" a esta pantalla |
| Pruebas | Unitarias: `MisCosechasServiceTest` (acciones por estado, extender desde hoy si la fecha pasó, días, estados no permitidos, dueño, 404, corrección sin cambios en unidad fija, grupo y página), `NotificacionServiceTest`. Integración (MariaDB): `MisCosechasIntegracionTest` (lista por grupo con acciones, bajar solo a menor y precio por kilo recalculado, VENCIDA → PUBLICADA al extender, retirar y no retirar una oculta, corrección → CORREGIDA, otro agricultor 403), `AvisosIntegracionTest` (lista ordenada, no leídos, marcar uno y todos, solo los propios, 404 ajeno, 400, 401). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-01 (consulta de la campana cada 30 s, sin push), RNF-03 (acciones con ícono y texto, confirmación al retirar), RNF-05 (evento y triggers como fuente de verdad) |

Reglas aprobadas en el plan del slice 9: bajar el precio solo a uno menor; extender desde la fecha límite actual u hoy, la mayor; retirar se permite con ventas acordadas (siguen su curso); una cosecha oculta solo se corrige (no se retira, no baja precio ni se extiende); el motivo de la moderación se ve en Avisos; sin el número de ofertas por cosecha; abrir un aviso lo marca leído y hay "Marcar todos".

### RF-10 — Venta agrupada (slice 10)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-12: lotes con total, promedio y agricultores (1); propuesta conjunta con kilos, precio por kilo y fecha de recojo (2–3); reparto proporcional en unidades enteras (4); una venta PROPUESTA por cosecha en su unidad, en la conversación del comprador (canal PROPUESTA_CONJUNTA si no existía) (5); aviso PROPUESTA_CONJUNTA (6); el agricultor acepta con bloqueo o rechaza (7); el comprador ve el avance (8); cerrar cancela lo pendiente (9); las aceptadas siguen su ciclo (10). A1 lote incompatible (trigger); A2 vencimiento por `ev_tareas_diarias`; A3 cancelar; A4 propuesta no ABIERTA (trigger); A5 al aceptar ya no hay kilos (422); A6 menos de una unidad sin parte |
| Módulos | `ventaagrupada` (dueño de propuestas_grupales y `v_lotes_agrupados`); `venta` (partes PROPUESTA, aceptar, rechazar, cancelar, partes de una propuesta); `mensajeria` (`abrirParaPropuesta`); `publicacion` (`cosechasDeLote`, `bloquearParaVenta`); `catalogo`, `usuario`, `notificacion` |
| Tablas | propuestas_grupales (escritura); transacciones, conversaciones, notificaciones (escritura vía sus módulos); `v_lotes_agrupados`, `v_publicaciones_disponibles`, `v_historial_transacciones` (lectura); evento `ev_tareas_diarias` |
| Triggers y restricciones | `trg_transacciones_bi` (PROPUESTA solo con propuesta ABIERTA, mismo comprador, variedad y distrito, kilos disponibles), `trg_transacciones_bu` (solo el agricultor acepta o rechaza, el comprador cancela, kilos al aceptar), `trg_transacciones_ai/au`; `chk_propuesta_cantidad`, `_precio`, `_fecha`, `_estado`, `uk_transaccion_propuesta_conv`, `idx_propuesta_estado` |
| Endpoints | `GET /api/v1/lotes`, `GET /api/v1/lotes/agricultores`, `POST /api/v1/propuestas`, `GET /api/v1/propuestas`, `GET /api/v1/propuestas/{id}`, `POST /api/v1/propuestas/{id}/cerrar`, `/cancelar`; `POST /api/v1/ventas/{id}/estado` con ACEPTAR, RECHAZAR y CANCELAR sobre una parte |
| Pantallas | Lotes agrupados (`pages/ventaagrupada/lotes.html`), Propuesta conjunta (`propuesta.html`), Mis propuestas (`propuestas.html`); la entrega (`pages/venta/venta.html`) muestra la parte con Aceptar mi parte / Rechazar o Cancelar esta parte; barra del comprador con "Agrupados" |
| Pruebas | Unitarias: `RepartoTest` (proporción en jabas, sacos y kilos; redondeo que no pasa lo pedido; menos de una unidad), `VentaServiceTest` (acciones de PROPUESTA). Integración (MariaDB): `VentaAgrupadaIntegracionTest` (lote y agricultores, 403 para el agricultor; reparto, partes, avisos y canal; aceptar y rechazar con avance y avisos al comprador; mensajes al comprador y al agricultor; cerrar con partes pendientes canceladas y acordadas que siguen; aceptar sin kilos 422; lote inexistente, cantidad mayor, muy poca, fecha pasada, 403 y 404). Arquitectura: `ArquitecturaTest` |
| RNF | RNF-02 (bloqueo `PESSIMISTIC_WRITE` al aceptar), RNF-03 (confirmación al rechazar, cerrar o cancelar), RNF-05 (triggers y evento como última barrera) |

Reglas aprobadas en el plan del slice 10: una parte por publicación del lote; la barra del comprador sigue el diseño (Agrupados); los lotes solo para el comprador; se informa lo que no se reparte por el redondeo; la parte se acepta completa o se rechaza; cerrar y cancelar cancelan lo pendiente y las acordadas siguen.

### RF-09 — Moderación posterior (slice 3)

| Elemento | Trazas |
|---|---|
| Caso de uso | UC-11 parte 2: cola "Por revisar" (1), revisión con datos del agricultor (2), Conforme / Advertir / Ocultar (3), Volver a mostrar (5); A3 motivo o mensaje vacío (400); A4 decisión no permitida (422). Parte 2 paso 4 (corrección → CORREGIDA) queda para el slice 9, que agrega la edición de la publicación |
| Módulos | `administracion` (dueño de `revisiones_publicacion`); `publicacion` (cola y marca CANTIDAD_INUSUAL, vía `publicacion.aplicacion`); `usuario` (datos del agricultor, vía `usuario.aplicacion`) |
| Tablas | revisiones_publicacion (escritura); publicaciones, usuarios (lectura); notificaciones (la escribe el trigger) |
| Triggers y restricciones | `trg_revisiones_bi`, `trg_revisiones_ai` (estado, borra la marca, notificación MODERACION); `chk_revision_decision`, `chk_revision_motivo`, `chk_revision_coherencia` |
| Endpoints | `GET /api/v1/moderacion/publicaciones`, `GET /api/v1/moderacion/decisiones`, `POST /api/v1/moderacion/publicaciones/{id}/decisiones` |
| Pantallas | 11c Moderación y 11d Advertir u ocultar (`pages/administracion/moderacion.html`, formato escritorio) |
| Pruebas | Unitarias: `ModeracionServiceTest`, `PublicacionServiceTest` (CANTIDAD_INUSUAL y consultas), `UsuarioConsultaServiceTest`. Integración (MariaDB): `ModeracionIntegracionTest` (cola, conforme, advertir con aviso, ocultar y volver a mostrar, 400/403/404/422, CANTIDAD_INUSUAL). Arquitectura: `ArquitecturaTest` |

### Resto de requisitos

| RF | Tablas / vistas | Triggers principales |
|---|---|---|
| RF-01 | publicaciones (+ catálogos) | `trg_publicaciones_bi`, `trg_publicaciones_bu` |
| RF-02 | precios_mercado, `v_precios_vigentes`, `v_rango_precio_plataforma` | `trg_precios_mercado_bi`, `trg_precios_mercado_bu` |
| RF-03 | `v_publicaciones_disponibles`, `v_reputacion_usuario` | — |
| RF-04 | conversaciones, mensajes | `trg_conversaciones_bi`, `trg_mensajes_bi` |
| RF-05 | calificaciones, `v_reputacion_usuario` | `trg_calificaciones_bi` |
| RF-06 | transacciones, transacciones_historial, `v_historial_transacciones` | `trg_transacciones_bi`, `_bu`, `_ai`, `_au` |
| RF-07 | notificaciones, publicaciones | evento `ev_tareas_diarias`, `trg_publicaciones_bu` |
| RF-09 | revisiones_publicacion, reclamos, suspensiones, `v_panel_admin`, `v_resumen_usuario`, `v_ventas_mensuales_cultivo`, `v_conversion_publicacion` | `trg_revisiones_*`, `trg_reclamos_*`, `trg_suspensiones_*` |
| RF-10 | propuestas_grupales, transacciones, `v_lotes_agrupados` | `trg_transacciones_bi`, evento `ev_tareas_diarias` |
| RF-11 | transacciones, transacciones_historial | `trg_transacciones_bu`, `_au` |
