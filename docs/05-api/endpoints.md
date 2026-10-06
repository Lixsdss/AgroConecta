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
| GET | `/api/v1/precios/vigentes` | Público | RF-02 | precio |
| GET | `/api/v1/precios/historial` | Público | RF-02 | precio |
| GET | `/api/v1/precios/referencia` | Público | RF-02, RF-01 | precio |
| POST | `/api/v1/precios` | ADMINISTRADOR | RF-02 | precio |
| PUT | `/api/v1/precios/{id}` | ADMINISTRADOR | RF-02 | precio |
| POST | `/api/v1/precios/archivo` | ADMINISTRADOR | RF-02 | precio |
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
  "agricultor": { "id": 3, "nombreCompleto": "Juan Carlos Chunga", "celular": "987654321", "distrito": "Tambogrande", "fechaRegistro": "2026-09-27T09:00:00" }
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
