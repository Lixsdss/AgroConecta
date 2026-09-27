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
- **Listas paginadas:** `page`, `size`, `sort` (a partir de los listados de RF-03).

## Índice

| Método | Ruta | Rol | RF | Módulo |
|---|---|---|---|---|
| GET | `/api/v1/catalogo/provincias` | Público | RF-08 (soporte) | catalogo |
| GET | `/api/v1/catalogo/distritos` | Público | RF-08 (soporte) | catalogo |
| POST | `/api/v1/auth/registro` | Público | RF-08 | usuario |
| POST | `/api/v1/auth/login` | Público | RF-08 | usuario |
| GET | `/api/v1/usuarios/me` | Autenticado | RF-08 | usuario |
| PUT | `/api/v1/usuarios/me` | Autenticado | RF-08 | usuario |
| PUT | `/api/v1/usuarios/me/contrasena` | Autenticado | RF-08 | usuario |

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

## Errores de seguridad

Las respuestas 401 y 403 que genera Spring Security (sin token, token vencido o inválido, rol insuficiente) usan el mismo formato `ErrorResponse`:

| Código | `mensaje` |
|---|---|
| 401 | "Inicie sesión para continuar." |
| 403 | "No tiene permiso para realizar esta acción." |
