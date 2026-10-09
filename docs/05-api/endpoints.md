# Contrato de la API REST — AgroConecta

Base: `/api/v1`. Formato JSON (UTF-8). Autenticación con JWT en la cabecera `Authorization: Bearer <token>` salvo en los endpoints públicos.

## Convenciones

- **Errores:** todos usan el formato de SRS §9.2, `compartido.error.ErrorResponse`:

  ```json
  {
    "timestamp": "2026-09-27T14:30:00-05:00",
    "estado": 400,
    "error": "Bad Request",
    "mensaje": "Los datos enviados no son válidos.",
    "ruta": "/api/v1/auth/registro",
    "errores": [ { "campo": "celular", "mensaje": "Escriba los 9 números de su celular, empezando por 9." } ]
  }
  ```

  `errores` solo tiene elementos en 400; en los demás casos es `[]`.
- **Códigos:** 400 validación · 401 sin sesión, credenciales incorrectas o usuario no ACTIVO · 403 rol incorrecto · 404 no existe · 409 dato duplicado · 422 regla de negocio o de la base de datos · 500 error interno.
- **Listas paginadas:** parámetros `page` (desde 0) y `size`. Respuesta:

  ```json
  { "content": [ ... ], "page": { "size": 20, "number": 0, "totalElements": 3, "totalPages": 1 } }
  ```

## Índice

| Método | Ruta | Rol | RF | Módulo |
|---|---|---|---|---|
| GET | `/api/v1/catalogo/provincias` | Público | RF-08 (soporte) | catalogo |
| GET | `/api/v1/catalogo/distritos` | Público | RF-08 (soporte) | catalogo |
| GET | `/api/v1/catalogo/cultivos` | Público | RF-01 (soporte) | catalogo |
| GET | `/api/v1/catalogo/variedades` | Público | RF-01 (soporte) | catalogo |
| GET | `/api/v1/catalogo/unidades` | Público | RF-01 (soporte) | catalogo |
| POST | `/api/v1/auth/registro` | Público | RF-08 | usuario |
| POST | `/api/v1/auth/login` | Público | RF-08 | usuario |
| GET | `/api/v1/usuarios/me` | Autenticado | RF-08 | usuario |
| PUT | `/api/v1/usuarios/me` | Autenticado | RF-08 | usuario |
| PUT | `/api/v1/usuarios/me/contrasena` | Autenticado | RF-08 | usuario |
| POST | `/api/v1/publicaciones` | AGRICULTOR | RF-01 | publicacion |
| GET | `/api/v1/publicaciones` | Público | RF-03 | publicacion |
| GET | `/api/v1/publicaciones/{id}` | Público | RF-01, RF-03 | publicacion |
| GET | `/api/v1/publicaciones/mias` | AGRICULTOR | RF-07 | publicacion |
| POST | `/api/v1/publicaciones/{id}/precio` | AGRICULTOR (dueño) | RF-07 | publicacion |
| POST | `/api/v1/publicaciones/{id}/extender` | AGRICULTOR (dueño) | RF-07 | publicacion |
| POST | `/api/v1/publicaciones/{id}/retirar` | AGRICULTOR (dueño) | RF-07 | publicacion |
| PUT | `/api/v1/publicaciones/{id}/correccion` | AGRICULTOR (dueño) | RF-09 | publicacion |
| GET | `/api/v1/notificaciones` | Autenticado | RF-07 | notificacion |
| GET | `/api/v1/notificaciones/no-leidas` | Autenticado | RF-07 | notificacion |
| POST | `/api/v1/notificaciones/{id}/leida` | Autenticado (dueño) | RF-07 | notificacion |
| POST | `/api/v1/notificaciones/leidas` | Autenticado | RF-07 | notificacion |
| GET | `/api/v1/precios/vigentes` | Público | RF-02 | precio |
| GET | `/api/v1/precios/historial` | Público | RF-02 | precio |
| GET | `/api/v1/precios/referencia` | Público | RF-02, RF-01 | precio |
| POST | `/api/v1/precios` | ADMINISTRADOR | RF-02 | precio |
| PUT | `/api/v1/precios/{id}` | ADMINISTRADOR | RF-02 | precio |
| POST | `/api/v1/precios/archivo` | ADMINISTRADOR | RF-02 | precio |
| POST | `/api/v1/conversaciones` | COMPRADOR | RF-04 | mensajeria |
| POST | `/api/v1/conversaciones/whatsapp` | COMPRADOR | RF-04 | mensajeria |
| GET | `/api/v1/conversaciones` | AGRICULTOR, COMPRADOR | RF-04 | mensajeria |
| GET | `/api/v1/conversaciones/no-leidos` | AGRICULTOR, COMPRADOR | RF-04 | mensajeria |
| GET | `/api/v1/conversaciones/{id}` | Participante | RF-04 | mensajeria |
| GET | `/api/v1/conversaciones/{id}/mensajes` | Participante | RF-04 | mensajeria |
| POST | `/api/v1/conversaciones/{id}/mensajes` | Participante | RF-04 | mensajeria |
| POST | `/api/v1/ventas` | Participante | RF-06 | venta |
| POST | `/api/v1/ventas/{id}/estado` | Parte de la venta | RF-06, RF-11 | venta |
| GET | `/api/v1/ventas` | AGRICULTOR, COMPRADOR | RF-06 | venta |
| GET | `/api/v1/ventas/exportar` | AGRICULTOR, COMPRADOR | RF-06 | venta |
| GET | `/api/v1/ventas/{id}` | Parte de la venta | RF-06, RF-11, RF-05 | venta |
| POST | `/api/v1/ventas/{id}/calificacion` | Parte de la venta | RF-05 | venta |
| GET | `/api/v1/reputaciones/{idUsuario}` | Público | RF-05 | venta |
| GET | `/api/v1/moderacion/publicaciones` | ADMINISTRADOR | RF-09 | administracion |
| GET | `/api/v1/moderacion/decisiones` | ADMINISTRADOR | RF-09 | administracion |
| POST | `/api/v1/moderacion/publicaciones/{id}/decisiones` | ADMINISTRADOR | RF-09 | administracion |

---

## Catálogo (`catalogo`)

### GET `/api/v1/catalogo/provincias`

Público. Las 8 provincias de Piura, ordenadas por nombre.

**200**
```json
[ { "id": 2, "nombre": "Ayabaca" }, { "id": 1, "nombre": "Piura" } ]
```

### GET `/api/v1/catalogo/distritos?idProvincia={id}`

Público. Distritos ordenados por provincia y nombre. `idProvincia` es opcional; sin él devuelve los 65.

**200**
```json
[ { "id": 1, "nombre": "Piura", "idProvincia": 1, "provincia": "Piura", "esZonaPrecio": true } ]
```

Si `idProvincia` no existe, devuelve `[]`.

### GET `/api/v1/catalogo/cultivos`

Público. Cultivos activos ordenados por nombre, con los datos que usa el formulario de publicación (RF-01).

**200**
```json
[
  { "id": 1, "nombre": "Mango", "vidaUtilDias": 14, "idUnidadLocal": 2, "unidadLocal": "Jaba", "pesoSugeridoKg": 20.000 }
]
```

`pesoSugeridoKg` es el peso típico de la **unidad local** del cultivo cuando es de peso variable (jaba, saco); puede ser `null`.

### GET `/api/v1/catalogo/variedades?idCultivo={id}`

Público. Variedades ordenadas por nombre; `idCultivo` es opcional (sin él devuelve todas).

**200**
```json
[ { "id": 1, "nombre": "Kent", "idCultivo": 1, "cultivo": "Mango", "vidaUtilDias": 14 } ]
```

### GET `/api/v1/catalogo/unidades`

Público. Las unidades de venta del catálogo (V2). `factorKg` es `null` en las de peso variable (jaba, saco): el agricultor indica cuántos kilos pesa cada una.

**200**
```json
[
  { "id": 1, "nombre": "Kilo", "nombrePlural": "kilos", "factorKg": 1.000 },
  { "id": 2, "nombre": "Jaba", "nombrePlural": "jabas", "factorKg": null }
]
```

---

## Autenticación (`usuario`)

### POST `/api/v1/auth/registro`

Público. Registra un AGRICULTOR o un COMPRADOR (UC-10) y devuelve la sesión iniciada.

**Request**

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `rol` | string | Sí | `AGRICULTOR` o `COMPRADOR` |
| `nombreCompleto` | string | Sí | 3 a 120 caracteres |
| `celular` | string | Sí | 9 dígitos que empiezan en 9 (se ignoran espacios) |
| `idDistrito` | integer | Sí | Distrito existente |
| `contrasena` | string | Sí | 8 a 72 caracteres, con al menos una letra y un número |
| `nombreChacra` | string | No | Solo agricultor; máx. 80 |
| `nombreNegocio` | string | Comprador | Obligatorio si `rol` = COMPRADOR; máx. 120 |
| `tipoComprador` | string | Comprador | Obligatorio si `rol` = COMPRADOR: `MERCADO_MAYORISTA`, `RESTAURANTE`, `HOTEL`, `PROCESADORA`, `MOLINO`, `OTRO` |
| `ruc` | string | No | Solo comprador; 11 dígitos que empiezan en 1 o 2 |

Los campos que no corresponden al rol se ignoran.

```json
{
  "rol": "COMPRADOR",
  "nombreCompleto": "Rosa Quispe",
  "celular": "987654321",
  "idDistrito": 1,
  "contrasena": "clave2026",
  "nombreNegocio": "Restaurante El Chalán",
  "tipoComprador": "RESTAURANTE",
  "ruc": "20123456789"
}
```

**201** — cabecera `Location: /api/v1/usuarios/me`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEn": "2026-09-28T02:30:00Z",
  "usuario": { "id": 12, "nombreCompleto": "Rosa Quispe", "rol": "COMPRADOR" }
}
```

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Campo inválido o faltante (detalle en `errores`) | "Los datos enviados no son válidos." |
| 409 | Celular ya registrado | "Este celular ya está registrado." |
| 409 | RUC ya registrado | "Este RUC ya está registrado." |
| 422 | El distrito no existe | "El distrito elegido no existe." |

### POST `/api/v1/auth/login`

Público. Inicia sesión con celular y contraseña; actualiza `ultimo_acceso`.

**Request**
```json
{ "celular": "987654321", "contrasena": "clave2026" }
```

**200** — mismo cuerpo que el registro (`token`, `tipo`, `expiraEn`, `usuario`).

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Faltan datos | "Los datos enviados no son válidos." |
| 401 | Celular o contraseña incorrectos (no se indica cuál) | "Celular o contraseña incorrectos." |
| 401 | Usuario SUSPENDIDO | "Su cuenta está suspendida. Comuníquese con el administrador." |
| 401 | Usuario INACTIVO | "Su cuenta está desactivada. Comuníquese con el administrador." |

**Token:** JWT HS256, emisor `agroconecta`, `sub` = id del usuario, claim `roles` = `["AGRICULTOR"]` (o `COMPRADOR`, `ADMINISTRADOR`), vigencia de 12 horas (`agroconecta.jwt.expiracion`). En cada petición se verifica además que el usuario siga ACTIVO; si no, 401.

---

## Perfil (`usuario`)

### GET `/api/v1/usuarios/me`

Autenticado (cualquier rol). Perfil del usuario del token.

**200**
```json
{
  "id": 12,
  "rol": "COMPRADOR",
  "nombreCompleto": "Rosa Quispe",
  "celular": "987654321",
  "estado": "ACTIVO",
  "idDistrito": 1,
  "distrito": "Piura",
  "provincia": "Piura",
  "fechaRegistro": "2026-09-27T09:30:00",
  "nombreChacra": null,
  "nombreNegocio": "Restaurante El Chalán",
  "tipoComprador": "RESTAURANTE",
  "ruc": "20123456789"
}
```

Los campos del subtipo que no corresponden al rol van en `null`.

**Errores:** 401 sin token, token inválido o usuario no ACTIVO.

### PUT `/api/v1/usuarios/me`

Autenticado. Actualiza los datos editables del propio perfil. El rol y el celular no se cambian aquí.

**Request**

| Campo | Obligatorio | Regla |
|---|---|---|
| `nombreCompleto` | Sí | 3 a 120 caracteres |
| `idDistrito` | Sí | Distrito existente |
| `nombreChacra` | No | Solo agricultor; máx. 80 |
| `nombreNegocio` | Comprador | Obligatorio si el usuario es COMPRADOR; máx. 120 |
| `tipoComprador` | Comprador | Obligatorio si el usuario es COMPRADOR |
| `ruc` | No | Solo comprador; 11 dígitos que empiezan en 1 o 2 |

**200** — el perfil actualizado (mismo cuerpo que GET).

**Errores:** 400 validación · 401 · 409 "Este RUC ya está registrado." · 422 "El distrito elegido no existe." · 422 "Indique el nombre y el tipo de su negocio." (comprador sin esos datos).

### PUT `/api/v1/usuarios/me/contrasena`

Autenticado. Cambia la propia contraseña (UC-10, cambio de contraseña propio).

**Request**
```json
{ "contrasenaActual": "clave2026", "contrasenaNueva": "nueva2027" }
```

`contrasenaNueva`: 8 a 72 caracteres, con al menos una letra y un número.

**204** sin cuerpo. La nueva contraseña rige desde el siguiente inicio de sesión; el token actual sigue vigente.

**Errores:** 400 validación · 401 · 422 "La contraseña actual no es correcta."

---

## Publicaciones (`publicacion`)

### POST `/api/v1/publicaciones`

Rol **AGRICULTOR**. Publica una cosecha (RF-01, UC-01). La publicación nace **PUBLICADA** (moderación posterior).

**Request**

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `idVariedad` | integer | Sí | Variedad existente (de ella sale el cultivo) |
| `idUnidad` | integer | Sí | Unidad del catálogo |
| `cantidad` | number | Sí | > 0, hasta 2 decimales |
| `pesoUnidadKg` | number | Jaba y saco | > 0, hasta 3 decimales. Obligatorio si la unidad es de peso variable; con kilo, quintal y tonelada se ignora (se usa el del catálogo) |
| `precioUnitario` | number | Sí | Soles por unidad elegida, > 0, hasta 2 decimales |
| `idDistrito` | integer | Sí | Distrito donde está la cosecha |
| `fechaCosecha` | date | Sí | `AAAA-MM-DD`, no puede ser futura |
| `fechaLimite` | date | No | Si falta, se calcula: `fechaCosecha + vida útil del cultivo`. Si llega: ≥ hoy y ≥ `fechaCosecha` |

```json
{ "idVariedad": 1, "idUnidad": 2, "cantidad": 30, "pesoUnidadKg": 20, "precioUnitario": 28, "idDistrito": 9, "fechaCosecha": "2026-09-27" }
```

**201** — cabecera `Location: /api/v1/publicaciones/{id}`; cuerpo igual al del GET por id.

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Campo inválido o faltante (detalle en `errores`) | "Los datos enviados no son válidos." |
| 401 | Sin sesión | "Inicie sesión para continuar." |
| 403 | El usuario no es agricultor | "No tiene permiso para realizar esta acción." |
| 422 | Variedad, unidad o distrito inexistente | "La variedad elegida no existe." / "La unidad elegida no existe." / "El distrito elegido no existe." |
| 422 | Jaba o saco sin peso | "Indique cuántos kilos pesa cada jaba o saco." |
| 422 | Fecha límite anterior a hoy o a la cosecha | "La fecha límite no puede ser anterior a hoy." / "La fecha límite no puede ser anterior a la fecha de cosecha." |
| 422 | Sin fecha límite y la cosecha ya pasó la vida útil del cultivo | "Con esa fecha de cosecha, el producto ya pasó su tiempo de venta. Revise la fecha." |

### GET `/api/v1/publicaciones?idCultivo=&idVariedad=&idProvincia=&idDistrito=&idUnidad=&cosechaDesde=&cosechaHasta=&orden=PRECIO_KG&page=0`

Público (UC-03, también sin sesión). Búsqueda multicriterio en `v_publicaciones_disponibles`. Siempre devuelve solo publicaciones **PUBLICADA**, con **kilos disponibles > 0** y **fecha límite hoy o después**; las de la cola de moderación también aparecen (moderación posterior).

| Parámetro | Obligatorio | Regla |
|---|---|---|
| `idCultivo` | No | Cultivo (el filtro "Categoría") |
| `idVariedad` | No | Variedad |
| `idProvincia` | No | Todos los distritos de la provincia |
| `idDistrito` | No | Un distrito |
| `idUnidad` | No | Unidad en que vende el agricultor |
| `cosechaDesde`, `cosechaHasta` | No | `AAAA-MM-DD`; `cosechaDesde` no puede ser posterior a `cosechaHasta` |
| `orden` | No | `PRECIO_KG` (por defecto): menor precio por kilo primero, empate la más reciente · `FECHA_COSECHA`: cosecha más reciente primero, empate menor precio por kilo · `CALIFICACION`: mejor calificado primero, sin calificación al final, empate menor precio por kilo |
| `page` | No | Desde 0. Siempre **20 por página** (`size` y `sort` no se usan) |

Los filtros se combinan (todos deben cumplirse). Un id que no existe no es error: simplemente no hay resultados.

**200** — lista paginada:
```json
{
  "content": [
    {
      "id": 15, "idAgricultor": 3, "agricultor": "Juan Carlos Chunga", "calificacionAgricultor": 4.8,
      "idCultivo": 1, "cultivo": "Mango", "idVariedad": 1, "variedad": "Kent",
      "idUnidad": 2, "unidad": "Jaba", "unidadPlural": "jabas",
      "cantidad": 30.00, "pesoUnidadKg": 20.000, "cantidadKg": 600.00,
      "disponibleKg": 500.00, "unidadesDisponibles": 25.00,
      "precioUnitario": 28.00, "precioKg": 1.4000,
      "idDistrito": 9, "distrito": "Tambogrande", "idProvincia": 1, "provincia": "Piura",
      "fechaCosecha": "2026-09-27", "fechaLimite": "2026-10-11", "diasRestantes": 5, "porVencer": false,
      "fechaPublicacion": "2026-09-27T12:30:00"
    }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

- `calificacionAgricultor`: promedio de 1 a 5 con un decimal; `null` si aún no tiene calificaciones.
- `unidadesDisponibles`: `disponibleKg / pesoUnidadKg`, redondeado hacia abajo a 2 decimales.
- `porVencer`: `true` si faltan 3 días o menos (`diasRestantes ≤ 3`).

**Errores:** 400 si `cosechaDesde` es posterior a `cosechaHasta` ("La fecha «desde» no puede ser posterior a la fecha «hasta»."), si `page` es negativo, si `orden` no es válido o si una fecha no tiene el formato `AAAA-MM-DD`.

### GET `/api/v1/publicaciones/{id}`

Público. Devuelve la publicación si está **PUBLICADA**, o en cualquier estado si la pide su propio agricultor (con su token). En otro caso, 404. Es también el detalle que se abre desde la búsqueda (UC-03 paso 7).

**200**
```json
{
  "id": 15,
  "idAgricultor": 3,
  "idCultivo": 1,
  "cultivo": "Mango",
  "idVariedad": 1,
  "variedad": "Kent",
  "idUnidad": 2,
  "unidad": "Jaba",
  "unidadPlural": "jabas",
  "cantidad": 30.00,
  "pesoUnidadKg": 20.000,
  "cantidadKg": 600.00,
  "precioUnitario": 28.00,
  "precioKg": 1.4000,
  "idDistrito": 9,
  "distrito": "Tambogrande",
  "provincia": "Piura",
  "fechaCosecha": "2026-09-27",
  "fechaLimite": "2026-10-11",
  "estado": "PUBLICADA",
  "fechaPublicacion": "2026-09-27T12:30:00",
  "agricultor": "Juan Carlos Chunga",
  "calificacionAgricultor": 4.8,
  "disponibleKg": 600.00,
  "unidadesDisponibles": 30.00,
  "diasRestantes": 14,
  "porVencer": false
}
```

Desde el slice 5 (RF-03) agrega `agricultor`, `calificacionAgricultor`, `disponibleKg`, `unidadesDisponibles`, `diasRestantes` y `porVencer`, con las mismas reglas que la búsqueda. También los devuelve el `POST` (201).

`marca_revision` (cola de moderación) no se expone aquí: es interna de la moderación (RF-09).

**Marcas de moderación al publicar** (SRS RF-09; umbrales aprobados en el plan del slice 3). La columna guarda una sola marca, con prioridad PRECIO_FUERA_RANGO > CANTIDAD_INUSUAL > PRIMERA_PUBLICACION:

- `CANTIDAD_INUSUAL`: los kilos superan 3 veces el promedio de las publicaciones del mismo cultivo en el mismo distrito de los últimos 90 días (solo si hay al menos 3 para comparar).
- `PRECIO_FUERA_RANGO`: el precio por kilo se aleja más de ±15 % de la referencia de `GET /api/v1/precios/referencia` (ver abajo). Sin referencia no se marca.
- `PRIMERA_PUBLICACION`: la pone `trg_publicaciones_bi` si no hay otra marca.

**Errores:** 404 "La publicación no existe." (inexistente, o no PUBLICADA y quien la pide no es su dueño).

---

### Mis cosechas y acciones rápidas (RF-07, UC-09; corrección de RF-09)

Rol **AGRICULTOR**; solo el dueño de la publicación (otro agricultor recibe **403**, una publicación inexistente **404** "La publicación no existe."). Las acciones devuelven la cosecha actualizada (formato de abajo). Vencer y crear los avisos POR_VENCER lo hace `ev_tareas_diarias`, no Java.

**Cosecha** (cada elemento de `GET /mias` y respuesta de las acciones):
```json
{
  "id": 15, "cultivo": "Mango", "variedad": "Kent", "distrito": "Tambogrande",
  "unidad": "Jaba", "unidadPlural": "jabas", "cantidad": 30.00, "pesoUnidadKg": 20.000, "cantidadKg": 600.00,
  "disponibleKg": 400.00, "unidadesDisponibles": 20.00, "precioUnitario": 28.00, "precioKg": 1.4000,
  "fechaCosecha": "2026-09-27", "fechaLimite": "2026-10-11", "diasRestantes": 2, "porVencer": true,
  "estado": "PUBLICADA", "fechaPublicacion": "2026-09-27T12:30:00",
  "acciones": ["BAJAR_PRECIO", "EXTENDER", "RETIRAR"]
}
```

`acciones` según el estado: PUBLICADA → `BAJAR_PRECIO`, `EXTENDER`, `RETIRAR`; VENCIDA → `EXTENDER` ("Publicar otra vez"), `RETIRAR`; BLOQUEADA → `CORREGIR`; VENDIDA y RETIRADA → ninguna. `marca_revision` no se expone.

### GET `/api/v1/publicaciones/mias?grupo=ACTIVAS&page=0`

"Mis ventas" del agricultor (diseño "ventas"). `grupo`: `ACTIVAS` (PUBLICADA, BLOQUEADA, VENCIDA; por defecto) o `CERRADAS` (VENDIDA, RETIRADA). Orden: en ACTIVAS, la que vence antes primero; en CERRADAS, la más reciente primero. 20 por página.

**200** — lista paginada de cosechas. **Errores:** 400 si `grupo` no es válido o la página es negativa.

### POST `/api/v1/publicaciones/{id}/precio`

**Bajar el precio** (solo PUBLICADA). Cuerpo `{ "precioUnitario": 25.00 }` (> 0, hasta 2 decimales). El precio por kilo lo recalcula la base de datos.

**Errores:** 400 · 403 · 404 · 422 "El nuevo precio debe ser menor que el actual (S/ 28.00)." · 422 "Solo se baja el precio de una cosecha que está a la venta."

### POST `/api/v1/publicaciones/{id}/extender`

**Extender** (PUBLICADA o VENCIDA). Cuerpo `{ "dias": 7 }` con 7, 14 o 30. La nueva fecha límite es la mayor entre la actual y hoy, más esos días. En una VENCIDA, `trg_publicaciones_bu` la devuelve a PUBLICADA.

**Errores:** 400 si `dias` no es 7, 14 ni 30 · 403 · 404 · 422 "Solo se extiende una cosecha a la venta o vencida."

### POST `/api/v1/publicaciones/{id}/retirar`

**Retirar** (PUBLICADA o VENCIDA → RETIRADA). Las ventas ya acordadas siguen su curso; nadie más puede comprar.

**Errores:** 403 · 404 · 422 "Una cosecha oculta por la moderación no se retira; corríjala para que la revisen." · 422 "Esta cosecha ya no se puede retirar." (VENDIDA o RETIRADA)

### PUT `/api/v1/publicaciones/{id}/correccion`

**Corregir** una cosecha oculta por la moderación (solo BLOQUEADA; UC-11 parte 2, paso 4). Cuerpo `{ "cantidad": 25, "pesoUnidadKg": 20, "precioUnitario": 27 }`; `pesoUnidadKg` solo cuenta en jaba o saco (en las demás unidades lo fija el catálogo). `trg_publicaciones_bu` marca la publicación CORREGIDA y vuelve a la cola "Por revisar"; sigue oculta hasta que el administrador la vuelva a mostrar.

**Errores:** 400 · 403 · 404 · 422 "Solo se corrige una cosecha oculta por la moderación." · 422 "No cambió ningún dato." · 422 "La cantidad no puede ser menor a lo ya acordado" (trigger)

---

## Precios de referencia (`precio`)

Precios del mercado real, **por kilo**, por cultivo y **zona de precio** (Piura, Tambogrande, Chulucanas, Sullana). Fuentes: `MERCADO_MAYORISTA_PIURA`, `MERCADO_SULLANA`, `ENCUESTA_ACOPIADORES`.

### GET `/api/v1/precios/vigentes?idCultivo=&idDistrito=`

Público. Último precio de cada cultivo y zona (`v_precios_vigentes`); ambos filtros son opcionales.

**200**
```json
[
  {
    "idPrecio": 31, "idCultivo": 1, "cultivo": "Mango", "idDistrito": 9, "zona": "Tambogrande",
    "fecha": "2026-09-28", "precioKg": 1.40, "unidadLocal": "Jaba", "precioUnidadLocal": 28.00,
    "tendencia": "SUBE", "fuente": "MERCADO_MAYORISTA_PIURA", "actualizado": "2026-09-28T07:30:00"
  }
]
```

`tendencia`: `SUBE`, `BAJA`, `IGUAL` o `SIN_DATO` frente a la carga anterior. `precioUnidadLocal` usa el factor de la unidad local o el peso sugerido del cultivo (jaba, saco).

### GET `/api/v1/precios/historial?idCultivo=&idDistrito=&dias=30`

Público. Serie de precios por kilo de un cultivo en una zona, del más antiguo al más reciente. `dias` entre 1 y 365 (por defecto 30).

**200**
```json
[ { "fecha": "2026-08-30", "precioKg": 1.20 }, { "fecha": "2026-09-28", "precioKg": 1.40 } ]
```

**Errores:** 400 si falta `idCultivo` o `idDistrito`.

### GET `/api/v1/precios/referencia?idCultivo=&idDistrito=`

Público. Referencia que ve el agricultor al poner su precio (paso 3) y que usa la marca PRECIO_FUERA_RANGO:

- Si el distrito es una zona de precio y tiene un precio de **7 días o menos**, se usa ese precio.
- Si no, el **promedio** de los precios de 7 días o menos de las zonas que tengan ese cultivo.
- `minimoKg` y `maximoKg` son la referencia ±15 %: dentro de ese rango, "Buen precio".
- `rangoPlataforma` sale de las ventas COMPLETADA de los últimos 30 días en ese cultivo y distrito (`v_rango_precio_plataforma`); es `null` si no hay ventas.

**200**
```json
{
  "idCultivo": 1, "cultivo": "Mango", "zona": "Tambogrande", "esPromedioDeZonas": false, "fecha": "2026-09-28",
  "precioKg": 1.40, "minimoKg": 1.19, "maximoKg": 1.61, "unidadLocal": "Jaba", "precioUnidadLocal": 28.00,
  "rangoPlataforma": { "minimoKg": 1.30, "maximoKg": 1.50, "promedioKg": 1.42, "ventas": 6 }
}
```

**204** sin cuerpo si no hay referencia (ningún precio de 7 días o menos).

### POST `/api/v1/precios`

Rol **ADMINISTRADOR**. Carga el precio del día (UC-02 A2, formulario).

| Campo | Obligatorio | Regla |
|---|---|---|
| `idCultivo` | Sí | Cultivo activo |
| `idDistrito` | Sí | Una de las 4 zonas de precio |
| `fecha` | No | `AAAA-MM-DD`, hoy por defecto; no puede ser futura |
| `precioKg` | Sí | > 0, hasta 2 decimales |
| `fuente` | Sí | Una de las 3 fuentes |

**201** — cabecera `Location: /api/v1/precios/{id}`
```json
{ "id": 31, "idCultivo": 1, "cultivo": "Mango", "idDistrito": 9, "zona": "Tambogrande", "fecha": "2026-09-28",
  "precioKg": 1.40, "fuente": "MERCADO_MAYORISTA_PIURA", "metodo": "FORMULARIO" }
```

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Campo inválido o fecha futura | "Los datos enviados no son válidos." |
| 403 | No es administrador | "No tiene permiso para realizar esta acción." |
| 409 | Ya hay precio de ese cultivo, zona y fecha | "Ya hay un precio de {cultivo} en {zona} para esa fecha. Use Editar para cambiarlo." |
| 422 | El cultivo no existe o no está activo | "El cultivo elegido no existe." |
| 422 | El distrito no es zona de precio | "El distrito elegido no es una zona de precio." |

### PUT `/api/v1/precios/{id}`

Rol **ADMINISTRADOR**. "Editar" de la tabla: cambia `precioKg` y `fuente`. El administrador que edita queda como responsable (`id_administrador`) y la base de datos registra `fecha_actualizacion`.

**200** — igual que el POST. **Errores:** 400 · 403 · 404 "El precio no existe."

### POST `/api/v1/precios/archivo`

Rol **ADMINISTRADOR**. `multipart/form-data` con `archivo` (`.xlsx` o `.csv`, máx. 1 MB y 500 filas) y `fuente` (una para todo el archivo).

**Formato:** primera fila con las columnas `cultivo, zona, fecha, precio_kg` (en ese orden). Nombres del catálogo (`Mango`, `Tambogrande`; se ignoran mayúsculas y tildes); fecha `AAAA-MM-DD` o `dd/mm/aaaa` (o celda de fecha en Excel); precio con punto o coma decimal. En CSV el separador puede ser coma o punto y coma.

Se importan las filas válidas y se informan las inválidas con su motivo (UC-02 A2); el administrador puede corregir y volver a subir solo esas.

**200**
```json
{
  "importadas": 14,
  "rechazadas": [
    { "fila": 5, "motivo": "La zona \"Paita\" no es una zona de precio." },
    { "fila": 9, "motivo": "Ya hay un precio de Mango en Piura para el 28/09/2026." }
  ]
}
```

**Errores:** 400 archivo vacío, formato no soportado o columnas distintas · 403 · 413 "El archivo es demasiado grande (máximo 1 MB)."

---

## Mensajería (`mensajeria`)

Chat entre el comprador y el agricultor de una publicación (RF-04, UC-04). Una conversación por publicación y comprador; **nunca se cierra** (sirve después para coordinar la entrega). "Participante" = el comprador de la conversación o el agricultor de la publicación; cualquier otro usuario recibe **403**. El frontend consulta periódicamente (cada 10 s en el chat, cada 30 s en la lista y el inicio).

**Conversación** (respuesta de `POST /conversaciones`, `GET /conversaciones/{id}` y cada elemento de la lista):
```json
{
  "id": 7, "idPublicacion": 15, "canalInicio": "CHAT", "fechaInicio": "2026-10-06T09:58:00",
  "soyAgricultor": false,
  "otraParte": { "id": 3, "nombre": "Juan Carlos Chunga", "negocio": null },
  "publicacion": {
    "cultivo": "Mango", "variedad": "Kent", "distrito": "Tambogrande", "estado": "PUBLICADA",
    "unidad": "Jaba", "unidadPlural": "jabas", "pesoUnidadKg": 20.000,
    "precioUnitario": 28.00, "precioKg": 1.4000,
    "disponibleKg": 600.00, "unidadesDisponibles": 30.00, "diasRestantes": 5
  },
  "ultimoMensaje": { "tipo": "OFERTA", "contenido": null, "ofertaCantidad": 30.00, "ofertaPrecioUnitario": 27.00,
                     "mio": true, "fechaEnvio": "2026-10-06T10:02:00" },
  "noLeidos": 0
}
```

- `otraParte.negocio`: nombre del negocio cuando la otra parte es el comprador (por ejemplo, "Restaurante El Chalán"); `null` para el agricultor.
- `ultimoMensaje` es `null` si aún no hay mensajes. `noLeidos`: mensajes de la otra parte que no he leído.

### POST `/api/v1/conversaciones`

Rol **COMPRADOR**. "Escribir" (UC-04 pasos 1–3). Cuerpo `{ "idPublicacion": 15 }`. Si el comprador ya tiene conversación con esa publicación, devuelve la misma.

**201** (creada, `canalInicio` = CHAT, con `Location: /api/v1/conversaciones/{id}`) o **200** (ya existía) — una conversación.

**Errores:** 400 sin `idPublicacion` · 403 no es comprador · 404 "La publicación no existe." · 422 "Solo se puede contactar una publicación disponible." (no está PUBLICADA y no había conversación).

### POST `/api/v1/conversaciones/whatsapp`

Rol **COMPRADOR**. "WhatsApp" (UC-04 A1). Cuerpo `{ "idPublicacion": 15 }`. Crea la conversación si no existía (`canalInicio` = WHATSAPP), **suma 1 a `clics_whatsapp`** y devuelve el enlace para abrir WhatsApp con el celular del agricultor y un texto sugerido. El celular solo se entrega dentro de este enlace.

**200**
```json
{ "idConversacion": 7, "enlace": "https://wa.me/51987654321?text=Hola%2C%20Juan%20Carlos%20Chunga.%20Vi%20su%20Mango%20Kent%20en%20AgroConecta%20%2830%20jabas%20a%20S%2F%2028.00%20por%20jaba%29.%20%C2%BFSigue%20disponible%3F" }
```

Texto: "Hola, {agricultor}. Vi su {cultivo variedad} en AgroConecta ({unidades disponibles} {unidad} a S/ {precio} por {unidad}). ¿Sigue disponible?"

**Errores:** los mismos que `POST /conversaciones`.

### GET `/api/v1/conversaciones?page=0`

Roles **AGRICULTOR** y **COMPRADOR**. "Mensajes": las conversaciones del usuario (como comprador, o de sus publicaciones como agricultor), con actividad más reciente primero. 20 por página.

**200** — lista paginada de conversaciones (`content` y `page`, como la búsqueda).

### GET `/api/v1/conversaciones/no-leidos`

Roles **AGRICULTOR** y **COMPRADOR**. Total de mensajes no leídos en todas sus conversaciones (aviso del inicio).

**200** `{ "total": 3 }`

### GET `/api/v1/conversaciones/{id}`

**Participante**. Cabecera del chat. **200** — una conversación. **Errores:** 403 · 404 "La conversación no existe."

### GET `/api/v1/conversaciones/{id}/mensajes?despuesDe={idMensaje}`

**Participante**. Mensajes del más antiguo al más reciente; con `despuesDe`, solo los posteriores a ese id (consulta periódica). Marca como **leídos** los mensajes de la otra parte.

**200**
```json
[
  { "id": 40, "idRemitente": 12, "mio": true, "tipo": "TEXTO", "contenido": "Buenos días, me interesa su mango.",
    "ofertaCantidad": null, "ofertaPrecioUnitario": null, "ofertaKg": null, "ofertaTotal": null,
    "fechaEnvio": "2026-10-06T09:58:00" },
  { "id": 41, "idRemitente": 12, "mio": true, "tipo": "OFERTA", "contenido": null,
    "ofertaCantidad": 30.00, "ofertaPrecioUnitario": 27.00, "ofertaKg": 600.00, "ofertaTotal": 810.00,
    "fechaEnvio": "2026-10-06T10:02:00" }
]
```

`ofertaKg` = cantidad × peso por unidad de la publicación; `ofertaTotal` = cantidad × precio por unidad.

**Errores:** 403 · 404.

### POST `/api/v1/conversaciones/{id}/mensajes`

**Participante**. Envía un mensaje de texto o una oferta (UC-04 pasos 4–6). Ambas partes pueden ofertar (contraofertas).

| Campo | Obligatorio | Regla |
|---|---|---|
| `tipo` | Sí | `TEXTO` u `OFERTA` |
| `contenido` | Con TEXTO | 1 a 1000 caracteres, no vacío |
| `ofertaCantidad` | Con OFERTA | > 0, hasta 2 decimales, en la unidad de la publicación; no mayor que `unidadesDisponibles` |
| `ofertaPrecioUnitario` | Con OFERTA | > 0, hasta 2 decimales, soles por unidad |

**201** — el mensaje (mismo formato que en la lista).

Crea un aviso para la otra parte (`notificaciones`, `id_referencia` = id de la conversación): **OFERTA** siempre ("Nueva oferta": "{nombre} le ofrece S/ 27.00 por jaba por su Mango Kent."); **MENSAJE** solo si la otra parte no tiene ya un aviso MENSAJE sin leer de esa conversación ("Nuevo mensaje": "{nombre} le escribió sobre Mango Kent."). El aviso no copia el texto del mensaje.

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Falta el texto o los datos de la oferta | "Los datos enviados no son válidos." (con `errores` por campo) |
| 403 | No participa en la conversación (UC-04 A4) | "No tiene permiso para realizar esta acción." |
| 404 | La conversación no existe | "La conversación no existe." |
| 422 | Oferta sobre una publicación que ya no está PUBLICADA (UC-04 A3) | "Solo se hacen ofertas mientras la publicación está disponible." |
| 422 | La oferta supera lo disponible | "La oferta supera lo disponible (≈ 25 jabas)." |

---

## Ventas (`venta`)

Acuerdo de venta, seguimiento de la entrega e historial (RF-06, RF-11; UC-05, UC-06, UC-08). Roles **AGRICULTOR** y **COMPRADOR**; "parte de la venta" = el comprador de la conversación o el agricultor de la publicación; cualquier otro usuario recibe **403**. No hay pago en la plataforma: la venta solo registra el acuerdo.

Estados: `ACORDADA` → `RECOGIDO` → `EN_CAMINO` → `ENTREGADO` → `COMPLETADA`, o `ACORDADA` → `ENTREGADO` → `COMPLETADA` (recojo en chacra). Solo `ACORDADA` se cancela (`CANCELADA`). `PROPUESTA` y `RECHAZADA` son de la venta agrupada (RF-10). Los cambios los validan el servicio y `trg_transacciones_bu`; la línea de tiempo la escriben `trg_transacciones_ai/au` en `transacciones_historial` y `sp_sincronizar_publicacion` pasa la publicación a VENDIDA (o la devuelve a PUBLICADA/VENCIDA al cancelar).

**Venta** (respuesta del `POST`, de `POST /estado` y de `GET /ventas/{id}`):
```json
{
  "id": 21, "idConversacion": 7, "idPublicacion": 15, "soyComprador": false,
  "contraparte": { "id": 12, "nombre": "Rosa Quispe", "negocio": "Restaurante El Chalán", "celular": "912000001" },
  "cultivo": "Mango", "variedad": "Kent", "distrito": "Tambogrande",
  "idUnidad": 2, "unidad": "Jaba", "unidadPlural": "jabas",
  "cantidad": 30.00, "pesoUnidadKg": 20.000, "cantidadKg": 600.00,
  "precioUnitario": 28.00, "precioKg": 1.40, "montoTotal": 840.00,
  "fechaRecojo": "2026-10-10", "estado": "ACORDADA",
  "fechaCreacion": "2026-10-06T10:30:00", "fechaActualizacion": null,
  "lineaDeTiempo": [ { "estado": "ACORDADA", "fecha": "2026-10-06T10:30:00", "autor": "Rosa Quispe", "esUsted": false } ],
  "acciones": ["RECOGIDO", "CANCELAR"],
  "calificaciones": {
    "puedoCalificar": false,
    "mia": null,
    "recibida": null
  }
}
```

- `contraparte`: la otra parte; `negocio` solo si es el comprador. El celular se muestra porque ya hay acuerdo (UC-05 paso 6).
- `lineaDeTiempo`: `transacciones_historial` en orden, con el autor (`null` si fue un proceso automático).
- `calificaciones` (RF-05, desde el slice 8): `mia` es la que dio este usuario y `recibida` la que le dio la otra parte; cada una es `{ "puntuacion": 5, "comentario": "Pagó a tiempo.", "fecha": "2026-10-07T09:00:00", "autor": "Restaurante El Chalán" }` o `null`. `puedoCalificar` es `true` si la venta está COMPLETADA y este usuario aún no calificó.
- `acciones`: lo que **este** usuario puede hacer ahora: `RECOGIDO` y `CANCELAR` (cualquiera, desde ACORDADA), `EN_CAMINO` (cualquiera, desde RECOGIDO), `CONFIRMAR_RECEPCION` (solo el comprador, desde ACORDADA, EN_CAMINO o ENTREGADO).

### POST `/api/v1/ventas`

**Participante** de la conversación (UC-05). Registra el acuerdo directamente como **ACORDADA**; bloquea la publicación (`PESSIMISTIC_WRITE`) mientras lo registra.

| Campo | Obligatorio | Regla |
|---|---|---|
| `idConversacion` | Sí | Conversación de la que nace el acuerdo |
| `idUnidad` | Sí | Unidad acordada (puede ser distinta de la publicada) |
| `cantidad` | Sí | > 0, hasta 2 decimales |
| `pesoUnidadKg` | Con jaba o saco distinta de la publicada | > 0, hasta 3 decimales. Con la misma unidad se usa el peso de la publicación; las unidades de peso fijo usan el del catálogo |
| `precioUnitario` | Sí | > 0, hasta 2 decimales, soles por unidad acordada |
| `fechaRecojo` | Sí | `AAAA-MM-DD`, hoy o después |

**201** — `Location: /api/v1/ventas/{id}` y la venta. Avisa a la otra parte (VENTA, "Venta acordada") con el nombre y el celular de quien registró.

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Campo inválido o día de recojo pasado | "Los datos enviados no son válidos." (con `errores`) |
| 403 | No participa en la conversación | "No tiene permiso para realizar esta acción." |
| 404 | La conversación no existe | "La conversación no existe." |
| 422 | La publicación ya no está PUBLICADA | "La cosecha ya no está disponible." |
| 422 | La unidad no existe | "La unidad elegida no existe." |
| 422 | Falta el peso de una jaba o saco | "Indique cuántos kilos pesa cada jaba o saco." |
| 422 | Los kilos superan lo disponible | "La cantidad supera lo disponible (≈ 600 kg)." |

### POST `/api/v1/ventas/{id}/estado`

**Parte de la venta** (UC-05 A1, UC-06). Cuerpo `{ "accion": "RECOGIDO" | "EN_CAMINO" | "CONFIRMAR_RECEPCION" | "CANCELAR" }`. `CONFIRMAR_RECEPCION` hace **ENTREGADO y COMPLETADA en una sola transacción** (desde ENTREGADO, solo COMPLETADA). Cada cambio fija `id_actualizado_por` y avisa a la otra parte (VENTA).

**200** — la venta actualizada.

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | `accion` no válida | "Los datos enviados no son válidos." |
| 403 | No es parte de la venta | "No tiene permiso para realizar esta acción." |
| 404 | La venta no existe | "La venta no existe." |
| 422 | La venta ya terminó (COMPLETADA, RECHAZADA, CANCELADA) | "La venta ya terminó y no se modifica." |
| 422 | Cancelar después del recojo (UC-05 A4) | "Una venta recogida ya no se cancela. Si hay un problema, podrá registrar un reclamo." |
| 422 | El agricultor intenta confirmar la recepción | "Solo el comprador confirma que recibió." |
| 422 | Cambio fuera de secuencia (UC-06 A3) | "Ese cambio no corresponde al estado actual de la venta." |

### GET `/api/v1/ventas?grupo=&desde=&hasta=&contraparte=&idConversacion=&page=0`

Roles **AGRICULTOR** y **COMPRADOR** (UC-08). "Mis ventas" o "Mis compras" desde `v_historial_transacciones`, la más reciente primero, 20 por página.

| Parámetro | Regla |
|---|---|
| `grupo` | `EN_CURSO` (PROPUESTA, ACORDADA, RECOGIDO, EN_CAMINO, ENTREGADO) o `TERMINADAS` (COMPLETADA, RECHAZADA, CANCELADA); sin él, todas |
| `desde`, `hasta` | Fecha del acuerdo (`AAAA-MM-DD`); `desde` no puede ser posterior a `hasta` |
| `contraparte` | Texto que se busca en el nombre de la otra parte (el negocio, si es el comprador) |
| `idConversacion` | Solo las ventas de esa conversación (franja del chat) |

**200** — lista paginada de:
```json
{
  "id": 21, "soyComprador": false, "contraparte": "Restaurante El Chalán",
  "cultivo": "Mango", "variedad": "Kent", "cantidad": 30.00, "unidad": "Jaba", "unidadPlural": "jabas",
  "cantidadKg": 600.00, "precioUnitario": 28.00, "precioKg": 1.40, "montoTotal": 840.00,
  "fechaRecojo": "2026-10-10", "estado": "ACORDADA",
  "fechaCreacion": "2026-10-06T10:30:00", "fechaActualizacion": null, "miPuntuacion": null
}
```

`miPuntuacion` (RF-05): las estrellas que este usuario dio en esa venta, o `null` si aún no la calificó.

**Errores:** 400 si las fechas están invertidas, `grupo` no es válido o la página es negativa.

### GET `/api/v1/ventas/exportar?grupo=&desde=&hasta=&contraparte=`

Roles **AGRICULTOR** y **COMPRADOR** (UC-08 A3). El mismo filtro en un archivo Excel (Apache POI), hasta **5000** ventas.

**200** — `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="AgroConecta_Historial_2026-10-06.xlsx"`. Columnas: Fecha del acuerdo, Cultivo, Variedad, Cantidad, Unidad, Kilos, Precio por unidad (S/), Precio por kilo (S/), Total (S/), Contraparte, Estado, Recojo, Última actualización.

**Errores:** 400 como la lista · 422 "Hay más de 5000 ventas en ese filtro. Acorte el rango de fechas."

### POST `/api/v1/ventas/{id}/calificacion`

**Parte de la venta** (RF-05, UC-07). Califica a la otra parte una sola vez, solo si la venta está **COMPLETADA**. Las calificaciones no se editan ni se eliminan. `trg_calificaciones_bi` repite las validaciones y `uk_calificacion_trans_evaluador` impide la segunda.

| Campo | Obligatorio | Regla |
|---|---|---|
| `puntuacion` | Sí | Entero de 1 a 5 |
| `comentario` | No | Hasta 500 caracteres; vacío se guarda como `null` |

**201** — la venta (mismo formato que `GET /ventas/{id}`), con `calificaciones.mia`. Avisa a la otra parte (CALIFICACION, "Nueva calificación": "{nombre} le dio 5 estrellas por la venta de Mango Kent.").

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Puntuación fuera de 1–5 o comentario de más de 500 caracteres | "Los datos enviados no son válidos." (con `errores`) |
| 403 | No es parte de la venta | "No tiene permiso para realizar esta acción." |
| 404 | La venta no existe | "La venta no existe." |
| 409 | Ya calificó esa venta (UC-07 A2) | "Ya calificó esta venta. Las calificaciones no se cambian." |
| 422 | La venta no está COMPLETADA (UC-07 A1) | "Solo se califican ventas terminadas." |

### GET `/api/v1/reputaciones/{idUsuario}?page=0`

**Público** (SRS §7.2: la reputación se ve en el perfil y en la búsqueda). Promedio y total de `v_reputacion_usuario` y las calificaciones que recibió el usuario, la más reciente primero, 10 por página.

**200**
```json
{
  "idUsuario": 3, "promedio": 4.8, "total": 23,
  "calificaciones": {
    "content": [ { "puntuacion": 5, "comentario": "Mango de primera.", "fecha": "2026-10-07T09:00:00", "autor": "Restaurante El Chalán" } ],
    "page": { "size": 10, "number": 0, "totalElements": 23, "totalPages": 3 }
  }
}
```

- `promedio` es `null` y `total` 0 si aún no tiene calificaciones.
- `autor`: el nombre del negocio si calificó un comprador, o el nombre completo si calificó un agricultor. No se muestran celulares.

**Errores:** 400 si la página es negativa · 404 "El usuario no existe." (o es administrador).

### GET `/api/v1/ventas/{id}`

**Parte de la venta**. Detalle con la línea de tiempo y las acciones (UC-06, UC-08 paso 6). **Errores:** 403 · 404 "La venta no existe."

---

## Avisos (`notificacion`)

Centro de avisos (RF-07, UC-09 paso 2). Cualquier usuario autenticado ve **solo los suyos**. Los crean los triggers (`MODERACION`), el evento diario (`POR_VENCER`) y los módulos que ejecutan cada acción (`MENSAJE`, `OFERTA`, `VENTA`, `CALIFICACION`…). No se borran.

**Aviso:**
```json
{ "id": 40, "tipo": "POR_VENCER", "titulo": "Su mango vence en 3 días. ¿Qué desea hacer?",
  "contenido": "30 jabas · Tambogrande", "idReferencia": 15, "leida": false, "fechaCreacion": "2026-10-09T06:00:00" }
```

`idReferencia` según el tipo: POR_VENCER y MODERACION → publicación; MENSAJE y OFERTA → conversación; VENTA y CALIFICACION → venta.

### GET `/api/v1/notificaciones?page=0`

Mis avisos, el más reciente primero, 20 por página. **200** — lista paginada. **Errores:** 400 si la página es negativa.

### GET `/api/v1/notificaciones/no-leidas`

**200** `{ "total": 3 }` (campana del inicio).

### POST `/api/v1/notificaciones/{id}/leida`

Marca un aviso como leído. **204**. **Errores:** 404 "El aviso no existe." (también si es de otro usuario).

### POST `/api/v1/notificaciones/leidas`

"Marcar todos como leídos". **200** `{ "marcados": 5 }`.

---

## Moderación posterior (`administracion`)

Todos los endpoints exigen rol **ADMINISTRADOR** (403 para los demás roles).

### GET `/api/v1/moderacion/publicaciones?vista={vista}&page=0&size=20`

| `vista` | Qué devuelve | Orden |
|---|---|---|
| `POR_REVISAR` (por defecto) | Publicaciones PUBLICADA o BLOQUEADA con marca de revisión (la cola "Por revisar") | La más antigua primero |
| `OCULTAS` | Publicaciones BLOQUEADA (para "Volver a mostrar") | La más reciente primero |

**200** — lista paginada de:
```json
{
  "id": 15, "cultivo": "Mango", "variedad": "Kent", "unidad": "Jaba", "unidadPlural": "jabas",
  "cantidad": 30.00, "pesoUnidadKg": 20.000, "cantidadKg": 600.00, "precioUnitario": 28.00, "precioKg": 1.4000,
  "distrito": "Tambogrande", "provincia": "Piura", "fechaCosecha": "2026-09-27", "fechaLimite": "2026-10-11",
  "estado": "PUBLICADA", "marcaRevision": "PRIMERA_PUBLICACION", "fechaPublicacion": "2026-09-27T12:30:00",
  "agricultor": { "id": 3, "nombreCompleto": "Juan Carlos Chunga", "celular": "987654321", "distrito": "Tambogrande", "fechaRegistro": "2026-09-27T09:00:00", "rol": "AGRICULTOR", "nombreNegocio": null }
}
```

**Errores:** 400 si `vista` no es válida.

### GET `/api/v1/moderacion/decisiones?fecha=AAAA-MM-DD`

Decisiones registradas ese día (por defecto, hoy), de la más reciente a la más antigua.

**200**
```json
[
  {
    "id": 4, "idPublicacion": 15, "producto": "Mango Kent", "decision": "BLOQUEO",
    "motivo": "PRECIO_NO_COINCIDE", "mensaje": "El precio no coincide con el mercado de hoy. Revise el precio por jaba.",
    "fecha": "2026-09-27T13:05:00", "administrador": "Lucía Farfán"
  }
]
```

### POST `/api/v1/moderacion/publicaciones/{id}/decisiones`

Registra una decisión sobre la publicación (UC-11, parte 2). `trg_revisiones_ai` aplica el cambio, borra la marca y avisa al agricultor.

**Request**

| Campo | Obligatorio | Regla |
|---|---|---|
| `decision` | Sí | `CONFORME`, `ADVERTENCIA`, `BLOQUEO` (ocultar) o `DESBLOQUEO` (volver a mostrar) |
| `motivo` | ADVERTENCIA y BLOQUEO | `PRECIO_NO_COINCIDE`, `CANTIDAD_INCORRECTA`, `PESO_UNIDAD_INCORRECTO`, `DATOS_INCORRECTOS`, `OTRO`. Se ignora en CONFORME y DESBLOQUEO |
| `mensaje` | ADVERTENCIA y BLOQUEO | Texto que verá el agricultor, máx. 300 caracteres. Opcional en DESBLOQUEO; se ignora en CONFORME |

```json
{ "decision": "BLOQUEO", "motivo": "PRECIO_NO_COINCIDE", "mensaje": "El precio no coincide con el mercado de hoy. Revise el precio por jaba." }
```

**201** — cabecera `Location: /api/v1/moderacion/decisiones?fecha=AAAA-MM-DD`

```json
{ "idRevision": 4, "idPublicacion": 15, "decision": "BLOQUEO", "estadoPublicacion": "BLOQUEADA" }
```

| Decisión | Estado requerido | Resultado | Aviso al agricultor |
|---|---|---|---|
| CONFORME | PUBLICADA y en la cola | Sigue PUBLICADA, sale de la cola | — |
| ADVERTENCIA | PUBLICADA | Sigue PUBLICADA, sale de la cola | MODERACION |
| BLOQUEO | PUBLICADA | BLOQUEADA (deja de verse) | MODERACION |
| DESBLOQUEO | BLOQUEADA | PUBLICADA (o VENCIDA si ya pasó su fecha límite) | MODERACION |

**Errores**

| Código | Cuándo | `mensaje` |
|---|---|---|
| 400 | Falta la decisión, o motivo y mensaje en ADVERTENCIA/BLOQUEO (detalle en `errores`) | "Los datos enviados no son válidos." |
| 404 | La publicación no existe | "La publicación no existe." |
| 422 | CONFORME, ADVERTENCIA u OCULTAR sobre una publicación que no está visible | "Solo se revisa, advierte u oculta una publicación visible." |
| 422 | Volver a mostrar una publicación que no está oculta | "Solo se vuelve a mostrar una publicación oculta." |
| 422 | CONFORME sobre una publicación que no está en la cola | "La publicación no está en la cola de revisión." |

---

## Errores de seguridad

Las respuestas 401 y 403 que genera Spring Security (sin token, token vencido o inválido, rol insuficiente) usan el mismo formato `ErrorResponse`:

| Código | `mensaje` |
|---|---|
| 401 | "Inicie sesión para continuar." |
| 403 | "No tiene permiso para realizar esta acción." |
