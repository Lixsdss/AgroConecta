# Pantallas ↔ endpoints — AgroConecta

Relaciona cada pantalla del prototipo de Claude Design (`AgroConecta.html`, numeración del diseño) con su archivo en `src/main/resources/static/` y los endpoints que usa (`docs/05-api/endpoints.md`). Las pantallas solo llaman endpoints documentados.

## Público y acceso (RF-08, slice 1)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Portada (mínima; el inicio público con precios, pantalla 01, llega con RF-02) | `index.html` | — | — | Ingresar · Crear cuenta |
| 02 | Ingresar | `pages/usuario/ingresar.html` | `js/usuario/ingresar.js` | `POST /api/v1/auth/login` | Éxito → inicio · "Crear cuenta nueva" → 03 · "¿Olvidó su contraseña?" muestra el aviso de UC-10 A4 |
| 03 | Crear cuenta · ¿Quién es usted? | `pages/usuario/rol.html` | `js/usuario/rol.js` | — | Continuar → 04 con `?rol=AGRICULTOR` o `COMPRADOR` |
| 04 | Crear cuenta · datos | `pages/usuario/registro.html` | `js/usuario/registro.js` | `GET /api/v1/catalogo/distritos`, `POST /api/v1/auth/registro` | Agricultor → 05 · Comprador → inicio |
| 05 | Bienvenida (3 tarjetas) | `pages/usuario/bienvenida.html` | `js/usuario/bienvenida.js` | — | "Saltar" o "Empezar" → inicio |
| — | Inicio mínimo (hasta que existan 06 y 14) | `pages/usuario/inicio.html` | `js/usuario/inicio.js` | `GET /api/v1/usuarios/me` | Mi perfil · Salir → 02 |
| — | Mi perfil (datos y contraseña) | `pages/usuario/perfil.html` | `js/usuario/perfil.js` | `GET /api/v1/catalogo/distritos`, `GET` y `PUT /api/v1/usuarios/me`, `PUT /api/v1/usuarios/me/contrasena` | Atrás → inicio |

### Diferencias con el diseño (aprobadas en el plan del slice 1)

- **Contraseña:** el diseño dice "Use 6 letras o números, como mínimo"; se aplica la regla de UC-10: **al menos 8 caracteres, con letras y números**.
- **Comprador:** el formulario del diseño es igual para ambos roles; se agregan, solo para el comprador, **nombre del negocio**, **tipo de negocio** y **RUC (opcional)**, porque la base de datos los exige (`compradores`).
- **Agricultor:** se agrega el campo opcional **nombre de su chacra** (`agricultores.nombre_chacra`).
- **Distrito:** como en el diseño, chips con las 4 zonas de precio (Piura, Tambogrande, Chulucanas, Sullana) y "Otro", que abre la lista de los 65 distritos agrupados por provincia.

## Sesión en el navegador

- `js/api.js` agrega `Authorization: Bearer <token>` y convierte las respuestas de error en `ErrorApi` (`estado`, `message`, `errores`).
- `js/comun/sesion.js` guarda la sesión en `sessionStorage` y la borra si vence o si la API responde 401.
- `js/comun/formulario.js` muestra cada error de la API junto a su campo (`data-error-de`) y el resto como mensaje general.

## Pendiente

La pantalla 01 y las demás pantallas del diseño se conectan en los slices siguientes; esta tabla se amplía en cada uno.
