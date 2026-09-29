# Matriz de trazabilidad — AgroConecta

Relaciona cada requisito funcional con su caso de uso, módulo, tablas, endpoints, pantallas y pruebas. Se actualiza al cerrar cada slice (CLAUDE.md §12.2).

**Fuentes:** SRS v1.3, casos de uso v1.4, `modelo-bd.sql` v4.1, `docs/05-api/endpoints.md`, `docs/04-diseno/ux-ui/pantallas.md`.

## Resumen

| RF | Nombre | UC | Módulo | Slice | Estado |
|---|---|---|---|---|---|
| **RF-01** | **Publicación de cosecha** | **UC-01** | **publicacion (+ catalogo)** | **2** | **Implementado** (referencia de precio: slice 4; marcas PRECIO_FUERA_RANGO y CANTIDAD_INUSUAL: slice 3) |
| **RF-02** | **Precios de mercado** | **UC-02** | **precio** | **4** | **Implementado (slice 4)**; también activa la marca PRECIO_FUERA_RANGO de RF-01 |
| RF-03 | Búsqueda multi-criterio | UC-03 | publicacion | 5 | Pendiente |
| RF-04 | Mensajería directa | UC-04 | mensajeria | 6 | Pendiente |
| RF-05 | Calificación | UC-07 | venta | 8 | Pendiente |
| RF-06 | Registro e historial de transacciones | UC-05, UC-06 A1, UC-08 | venta | 7 | Pendiente |
| RF-07 | Alertas de vencimiento | UC-09 | notificacion, publicacion | 9 | Pendiente |
| **RF-08** | **Registro y gestión de usuarios** | **UC-10** | **usuario (+ catalogo)** | **1** | **Implementado** (gestión por el administrador: slice 11) |
| **RF-09** | **Panel administrativo** | **UC-11** | **administracion** | **3 y 11** | **Moderación implementada (slice 3)**; reclamos, suspensiones, usuarios y panel: slice 11; corrección de una publicación bloqueada (CORREGIDA): slice 9 |
| RF-10 | Venta agrupada | UC-12 | ventaagrupada | 10 | Pendiente |
| RF-11 | Seguimiento de entrega | UC-06 | venta | 7 | Pendiente |

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
