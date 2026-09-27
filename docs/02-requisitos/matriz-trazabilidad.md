# Matriz de trazabilidad — AgroConecta

Relaciona cada requisito funcional con su caso de uso, módulo, tablas, endpoints, pantallas y pruebas. Se actualiza al cerrar cada slice (CLAUDE.md §12.2).

**Fuentes:** SRS v1.3, casos de uso v1.4, `modelo-bd.sql` v4.1, `docs/05-api/endpoints.md`, `docs/04-diseno/ux-ui/pantallas.md`.

## Resumen

| RF | Nombre | UC | Módulo | Slice | Estado |
|---|---|---|---|---|---|
| **RF-01** | **Publicación de cosecha** | **UC-01** | **publicacion (+ catalogo)** | **2** | **Implementado** (referencia de precio: slice 4; marcas PRECIO_FUERA_RANGO y CANTIDAD_INUSUAL: slice 3) |
| RF-02 | Precios de mercado | UC-02 | precio | 4 | Pendiente |
| RF-03 | Búsqueda multi-criterio | UC-03 | publicacion | 5 | Pendiente |
| RF-04 | Mensajería directa | UC-04 | mensajeria | 6 | Pendiente |
| RF-05 | Calificación | UC-07 | venta | 8 | Pendiente |
| RF-06 | Registro e historial de transacciones | UC-05, UC-06 A1, UC-08 | venta | 7 | Pendiente |
| RF-07 | Alertas de vencimiento | UC-09 | notificacion, publicacion | 9 | Pendiente |
| **RF-08** | **Registro y gestión de usuarios** | **UC-10** | **usuario (+ catalogo)** | **1** | **Implementado** (gestión por el administrador: slice 11) |
| RF-09 | Panel administrativo | UC-11 | administracion | 3 y 11 | Pendiente |
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
