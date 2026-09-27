# Notas de integridad — reglas que el diagrama ER no expresa

Complementan `modelo-bd.sql` v4 (fuente de verdad) y `modelo-logico.md`. Indican
qué garantiza la base de datos y qué debe validar además la capa `service`
de Spring antes de escribir los repositorios y servicios de cada área.

## 1. Consistencia usuario ↔ subtipo (agricultor/comprador)

El modelo usa herencia por tabla de subtipo: `usuarios.rol` (CHECK) indica el
rol y los datos propios viven en `agricultores` o `compradores`.

**Lo que garantiza la BD:** `trg_agricultores_bi` y `trg_compradores_bi`
(BEFORE INSERT en cada subtabla, con `fn_rol_usuario`) impiden crear una fila
de subtipo para un usuario con otro rol. En consecuencia, un usuario no puede
tener filas en ambas subtablas.

**Lo que NO garantiza:** que un usuario AGRICULTOR o COMPRADOR tenga su fila
de subtipo, ni que `usuarios.rol` no cambie después del registro.

**Regla en el service:**
- Crear `usuarios` y la fila del subtipo en la misma transacción (`@Transactional`).
- El rol no se cambia tras el registro (ningún endpoint lo permite).
- Un ADMINISTRADOR no tiene fila de subtipo.

## 2. Secuencia de la entrega

En v4 la entrega no tiene tablas propias: RECOGIDO, EN_CAMINO y ENTREGADO son
estados de `transacciones`, y la línea de tiempo es `transacciones_historial`.

**Lo que garantiza la BD:**
- `trg_transacciones_bu` solo permite ACORDADA → RECOGIDO → EN_CAMINO →
  ENTREGADO → COMPLETADA (sin saltos ni retrocesos), valida quién hace cada
  cambio con `id_actualizado_por` y rechaza cualquier cambio en una venta
  COMPLETADA, RECHAZADA o CANCELADA.
- `trg_transacciones_ai` y `trg_transacciones_au` insertan una fila en
  `transacciones_historial` por cada estado, con su fecha y autor.

**Lo que NO garantiza:** que `transacciones_historial` no se edite o se borre
directamente (no tiene triggers propios).

**Regla en el service:** la aplicación nunca inserta, actualiza ni borra en
`transacciones_historial`. Cada update a `transacciones` fija
`id_actualizado_por`; "Confirmar que recibí" hace los dos updates
(ENTREGADO y COMPLETADA) en un mismo método `@Transactional`.

## 3. `usuarios.password_hash` e inicio de sesión

`password_hash` es NOT NULL: todo usuario tiene contraseña desde el registro
(RF-08). El inicio de sesión es con `usuarios.celular` (NOT NULL, UNIQUE,
9 dígitos que empiezan en 9) + contraseña. En v4 no hay columna de email.
La recuperación de contraseña la hace el administrador asignando una nueva
(hash BCrypt).

Si en el futuro se agrega verificación por celular, hará falta una nueva
migración con un estado adicional en `usuarios.estado` (p. ej.
`PENDIENTE_VERIFICACION`).

## 4. Índices (RNF-01 y RNF-02)

InnoDB ya indexa cada FK. Además, `modelo-bd.sql` sección 8 define:

| Índice | Uso |
|---|---|
| `publicaciones (estado, id_variedad, id_distrito, fecha_cosecha)` | Búsqueda (RF-03) |
| `publicaciones (estado, precio_kg)` | Orden por precio por kilo (RF-03) |
| `publicaciones (estado, fecha_limite)` | Evento de alertas y vencimiento (RF-07) |
| `publicaciones (id_agricultor, estado)` | Mis ventas |
| `revisiones_publicacion (decision, fecha)` | Moderación (RF-09) |
| `precios_mercado (fecha)` | Precios de hoy (RF-09) |
| `mensajes (id_conversacion, fecha_envio)` | Orden cronológico del chat (RF-04) |
| `transacciones (id_conversacion, estado)` | Cantidad reservada / disponibilidad |
| `transacciones_historial (id_transaccion, fecha)` | Línea de tiempo (RF-11) |
| `transacciones_historial (estado, fecha)` | Métricas (RF-09) |
| `reclamos (estado, fecha_registro)` | Reclamos pendientes (RF-09) |
| `notificaciones (id_usuario, leida, fecha_creacion)` | Centro de notificaciones (RF-07) |
| `propuestas_grupales (estado, fecha_recojo)` | Cierre de propuestas (RF-10) |

El historial del agricultor (RF-06) se resuelve por `publicaciones.id_agricultor`
→ `conversaciones.id_publicacion` → `transacciones (id_conversacion, estado)`;
el del comprador, por `conversaciones.id_comprador` (índices de FK). Cualquier
índice nuevo va en una migración Vn y se refleja en `modelo-bd.sql`.

## 5. Tablas de catálogo sin auditoría — PENDIENTE

`cultivos`, `variedades`, `unidades_medida`, `provincias` y `distritos` no
tienen `fecha_creacion` / `fecha_actualizacion`. Es aceptable mientras los
catálogos se carguen solo por migración (V2 y posteriores).

Si el panel administrativo (RF-09) llega a editarlos en producción, agregar
esas columnas en una nueva migración. Decisión pendiente.

## 6. `transacciones` sin porcentaje de comisión

No existe columna de comisión (dependía de las tablas de planes y
suscripciones, descartadas junto con RF-12). Si se activa esa extensión, la
columna y sus tablas se agregan en una nueva migración — no antes.

## 7. Concurrencia en acuerdos (RNF-02)

`trg_transacciones_bi` y `trg_transacciones_bu` suman la cantidad reservada en kg
(ACORDADA, RECOGIDO, EN_CAMINO, ENTREGADO y COMPLETADA) sin bloquear la
publicación. Dos acuerdos simultáneos podrían superar la cantidad publicada.

**Regla en el service:** antes de que una transacción quede ACORDADA
(acuerdo del chat o PROPUESTA aceptada), leer la publicación con
`@Lock(LockModeType.PESSIMISTIC_WRITE)` dentro de la misma transacción.

## 8. Comparaciones con NULL en triggers

Si un ID referenciado no existe, las variables del trigger quedan en NULL y
comparaciones como `v_estado_pub <> 'PUBLICADA'` o
`fn_rol_usuario(...) <> 'ADMINISTRADOR'` no disparan el error; la FK lo
rechaza después con un error 1452 (409). **Regla en el service:** validar la
existencia de las entidades referenciadas (404) antes de insertar.

## 9. Mensajes y estado de la conversación

En v4 `conversaciones` no tiene estado (ya no existen ABIERTA/CERRADA).
`trg_conversaciones_bi` exige que la publicación esté PUBLICADA al crear la
conversación y `trg_mensajes_bi` valida al remitente, pero ninguno impide
enviar mensajes cuando la publicación ya no está PUBLICADA (VENDIDA, VENCIDA,
RETIRADA). **Pendiente de decidir** si el service lo restringe.

## 10. Requisitos del servidor

Las migraciones no ejecutan `SET GLOBAL`. El servidor debe tener
`event_scheduler=ON` y el usuario de la aplicación los privilegios TRIGGER,
EVENT, CREATE ROUTINE y ALTER ROUTINE (ver CLAUDE.md §6). Con el binlog activo
puede requerirse `log_bin_trust_function_creators=1` para crear
`fn_rol_usuario`, `sp_sincronizar_publicacion` y los triggers — **pendiente de
verificar en el servidor de despliegue**.
