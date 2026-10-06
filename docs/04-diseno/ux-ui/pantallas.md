# Pantallas ↔ endpoints — AgroConecta

## Fuente del diseño

El prototipo de Claude Design **no se guarda en el repositorio** (decisión del equipo, 27/09/2026): es una exportación comprimida de 1,3 MB que Git no puede comparar entre versiones. Se consulta en su ubicación original:

| Dato | Valor |
|---|---|
| Archivo exportado | `AgroConecta.html` (ignorado por `.gitignore` si se deja en esta carpeta) |
| Ubicación compartida | Pendiente de registrar: enlace de Claude Design o carpeta compartida del equipo |
| Versión usada en RF-08 | Exportación del 27/09/2026; todavía muestra moderación previa ("En revisión", "fue aprobado"), que debe cambiarse a moderación posterior (`cambios-v4.md`) |

Cada vez que el diseño cambie, actualice esta tabla con la fecha de la exportación usada.

## Pantallas

Relaciona cada pantalla del prototipo (numeración del diseño) con su archivo en `src/main/resources/static/` y los endpoints que usa (`docs/05-api/endpoints.md`). Las pantallas solo llaman endpoints documentados.

## Público y acceso (RF-08, slice 1)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| 01 | Inicio público con precios (RF-02, slice 4) | `index.html` | `js/portada.js` | `GET /api/v1/catalogo/cultivos`, `GET /api/v1/catalogo/distritos`, `GET /api/v1/precios/vigentes`, `GET /api/v1/precios/historial` | Ingresar → 02 · Publicar mi cosecha gratis → 04 (agricultor) · Soy comprador → 04 (comprador) · Ver productos disponibles → Buscar (slice 5) |
| 02 | Ingresar | `pages/usuario/ingresar.html` | `js/usuario/ingresar.js` | `POST /api/v1/auth/login` | Éxito → inicio · "Crear cuenta nueva" → 03 · "¿Olvidó su contraseña?" muestra el aviso de UC-10 A4 |
| 03 | Crear cuenta · ¿Quién es usted? | `pages/usuario/rol.html` | `js/usuario/rol.js` | — | Continuar → 04 con `?rol=AGRICULTOR` o `COMPRADOR` |
| 04 | Crear cuenta · datos | `pages/usuario/registro.html` | `js/usuario/registro.js` | `GET /api/v1/catalogo/distritos`, `POST /api/v1/auth/registro` | Agricultor → 05 · Comprador → inicio |
| 05 | Bienvenida (3 tarjetas) | `pages/usuario/bienvenida.html` | `js/usuario/bienvenida.js` | — | "Saltar" o "Empezar" → inicio |
| 06 / 14 | Inicio del agricultor / del comprador (versión sencilla) | `pages/usuario/inicio.html` | `js/usuario/inicio.js` | `GET /api/v1/usuarios/me`, `GET /api/v1/precios/vigentes` (slice 4) | Vender mi cosecha → 07 (agricultor) · Buscar productos (slice 5) · Ver todos los precios → 01 · Mi perfil · Salir → 02 |
| — | Mi perfil (datos y contraseña) | `pages/usuario/perfil.html` | `js/usuario/perfil.js` | `GET /api/v1/catalogo/distritos`, `GET` y `PUT /api/v1/usuarios/me`, `PUT /api/v1/usuarios/me/contrasena` | Atrás → inicio |

### Diferencias con el diseño (aprobadas en el plan del slice 1)

- **Contraseña:** el diseño dice "Use 6 letras o números, como mínimo"; se aplica la regla de UC-10: **al menos 8 caracteres, con letras y números**.
- **Comprador:** el formulario del diseño es igual para ambos roles; se agregan, solo para el comprador, **nombre del negocio**, **tipo de negocio** y **RUC (opcional)**, porque la base de datos los exige (`compradores`).
- **Agricultor:** se agrega el campo opcional **nombre de su chacra** (`agricultores.nombre_chacra`).
- **Distrito:** como en el diseño, chips con las 4 zonas de precio (Piura, Tambogrande, Chulucanas, Sullana) y "Otro", que abre la lista de los 65 distritos agrupados por provincia.

## Agricultor · Vender una cosecha (RF-01, slice 2)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| 07 | Vender · Paso 1 | `pages/publicacion/paso1.html` | `js/publicacion/paso1.js` | `GET /api/v1/catalogo/cultivos`, `GET /api/v1/catalogo/variedades?idCultivo=` | Siguiente → 08 |
| 08 | Vender · Paso 2 | `pages/publicacion/paso2.html` | `js/publicacion/paso2.js` | `GET /api/v1/catalogo/unidades` | Siguiente → 09 |
| 09 | Vender · Paso 3 | `pages/publicacion/paso3.html` | `js/publicacion/paso3.js` | `GET /api/v1/usuarios/me`, `GET /api/v1/catalogo/distritos`, `GET /api/v1/precios/referencia` (slice 4) | Revisar y publicar → 10 |
| 10 | Confirmar publicación | `pages/publicacion/confirmar.html` | `js/publicacion/confirmar.js` | `POST /api/v1/publicaciones` | Sí, publicar → 11 · Corregir → 07 |
| 11 | Cosecha publicada | `pages/publicacion/publicada.html` | `js/publicacion/publicada.js` | — | Volver al inicio |

El borrador de los 3 pasos vive en `sessionStorage` (`js/publicacion/borrador.js`) y la pantalla muestra "Guardado" después de cada cambio. Se entra desde el botón "Vender mi cosecha" del inicio, visible solo para agricultores.

### Diferencias con el diseño (aprobadas en el plan del slice 2)

- **Unidades:** se muestran las 5 del catálogo para todos los cultivos; el diseño limita la lista por cultivo, pero el esquema no guarda esa relación.
- **Quintal:** no se pregunta "¿Cuántos kilos tiene su quintal?"; vale siempre 46 kg (catálogo y trigger). Solo jaba y saco piden el peso.
- **Peso sugerido:** solo cuando la unidad elegida es la unidad local del cultivo (`cultivos.peso_sugerido_kg`).
- **Fecha límite:** no se muestra; la calcula la base de datos con la vida útil del cultivo y aparece en la pantalla 11.
- **Fecha de cosecha:** hoy por defecto; no se aceptan fechas futuras.
- **Referencia de precio y veredicto** (paso 3, desde el slice 4): "Hoy en {zona} pagan entre … por {unidad} (… por kilo)" con la referencia ±15 %; veredicto "Buen precio" / "Precio bajo, podría ganar más" / "Precio muy alto, puede tardar en vender" con ícono y texto. Si no hay referencia de 7 días o menos, no se muestra nada (la publicación sigue). Si hay ventas completadas, se agrega el rango de AgroConecta.

## Administrador · Moderación (RF-09, slice 3) — formato escritorio

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| 11c | Moderación | `pages/administracion/moderacion.html` | `js/administracion/moderacion.js` | `GET /api/v1/moderacion/publicaciones?vista=POR_REVISAR` y `OCULTAS`, `GET /api/v1/moderacion/decisiones` | Pestañas Por revisar / Ocultas / Decisiones de hoy; lista y detalle |
| 11d | Advertir u ocultar (en el diseño: "Rechazar con motivo") | ventana `<dialog>` de la misma página | — | `POST /api/v1/moderacion/publicaciones/{id}/decisiones` | Cancelar · Advertir y avisar / Ocultar y avisar |

El administrador entra por la misma pantalla "Ingresar"; el inicio lo lleva directo a Moderación. La barra lateral muestra Moderación y Precios de mercado (slice 4); Panel y Usuarios se agregan en sus slices.

### Diferencias con el diseño (moderación posterior de v4.1, aprobadas en el plan del slice 3)

- **Acciones:** "Aprobar y publicar" / "Rechazar" pasan a **Conforme**, **Advertir**, **Ocultar** y **Volver a mostrar** (`revisiones_publicacion.decision`).
- **Pestañas:** "En revisión / Rechazadas hoy / Aprobadas hoy" pasan a **Por revisar / Ocultas / Decisiones de hoy**.
- **Marcas:** la marca del diseño "Peso por unidad inusual" no existe en el esquema; se muestran PRIMERA_PUBLICACION, CANTIDAD_INUSUAL, PRECIO_FUERA_RANGO (desde el slice 4) y CORREGIDA.
- **Detalle:** la línea "Hoy en … pagan entre …" todavía no se muestra en moderación (el endpoint `GET /api/v1/precios/referencia` ya existe desde el slice 4); la calificación y las ventas del agricultor, con RF-05 y RF-06.
- **Motivos y mensajes sugeridos:** son los 5 del diseño, que coinciden con `chk_revision_motivo`.

## Búsqueda (RF-03, slice 5)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Buscar (diseño "buscar") | `pages/publicacion/buscar.html` | `js/publicacion/buscar.js` | `GET /api/v1/publicaciones`, `GET /api/v1/catalogo/cultivos`, `/variedades`, `/provincias`, `/distritos`, `/unidades` | Resultado → Detalle · Volver → inicio (con sesión) o portada |
| — | Hoja "Filtrar" | `<dialog>` de la misma página | — | — | Limpiar · Ver resultados · Cerrar |
| — | Detalle de la cosecha (no está en el diseño) | `pages/publicacion/detalle.html` | `js/publicacion/detalle.js` | `GET /api/v1/publicaciones/{id}` | Volver → la búsqueda con sus filtros |

Se entra desde "Ver productos disponibles" en la portada (01) y "Buscar productos" en el inicio (botón principal del comprador, secundario del agricultor). Los filtros quedan en la dirección de la página, así que "atrás" desde el detalle vuelve a la misma búsqueda. La tarjeta de resultado está en `js/publicacion/resultado.js`.

### Diferencias con el diseño (aprobadas en el plan del slice 5)

- **Caja de texto "Mango, limón, arroz…":** no se incluye; el SRS no pide búsqueda por texto. El cultivo se elige con los chips de "Categoría".
- **Filtros:** se agrega **Variedad** (aparece al elegir cultivo, SRS RF-03). "Provincia o distrito" es una lista con "Toda la provincia de …" y sus distritos. Fecha de cosecha: Cualquier fecha / Esta semana (últimos 7 días) / Este mes (últimos 30).
- **Calificación:** se muestra "★ 4.8" o "Sin calificaciones"; el número de ventas "(23 ventas)" llega con el módulo `venta` (slices 7 y 8).
- **Escribir y WhatsApp:** llegan con RF-04 (slice 6). Por ahora cada resultado abre el **detalle**.
- **Cantidad:** "≈ 25 jabas disponibles (≈ 500 kg)", con los kilos aún no comprometidos en ventas, redondeado hacia abajo.

## Precios de referencia (RF-02, slice 4)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| 11b | Precios de mercado (escritorio) | `pages/administracion/precios.html` | `js/administracion/precios.js` | `GET /api/v1/catalogo/cultivos`, `/distritos`, `/unidades`, `GET /api/v1/precios/vigentes`, `POST /api/v1/precios`, `PUT /api/v1/precios/{id}`, `POST /api/v1/precios/archivo` | Barra lateral: Moderación · Precios de mercado |

Piezas compartidas: `js/comun/precios.js` (tarjeta de precio, tendencia, fuentes, chips) y `js/comun/grafico.js` (línea SVG de una serie: 2 px, punto final con su valor, línea guía con detalle al pasar el puntero o con las flechas del teclado, y "Ver como tabla").

### Diferencias con el diseño (aprobadas en el plan del slice 4)

- **Alcance visual:** 01, 06, 14 y 11b son funcionales y sencillas, con la paleta y los componentes existentes; el pulido del diseño queda para después.
- **01:** cultivo y zona tienen la opción "Todos" / "Todas" (en UC-02 la zona es opcional); el gráfico de 30 días aparece solo con un cultivo y una zona elegidos. Sin resultados: "Sin resultados en esta zona. Pruebe con «Todas»." (UC-02 A3).
- **06 / 14:** solo el bloque "Precios de hoy en {distrito}"; si el distrito del usuario no es zona de precio, se muestran todas las zonas. El resto de esos inicios llega con sus RF.
- **11b:** se agregan el campo **Fecha** (hoy por defecto), el botón **Editar ese precio** cuando ya existe (409) y la sección **Cargar archivo** (Excel o CSV) con la lista de filas no guardadas. "X de N cargados hoy" cuenta cultivos activos × 4 zonas.

## Sesión en el navegador

- `js/api.js` agrega `Authorization: Bearer <token>` y convierte las respuestas de error en `ErrorApi` (`estado`, `message`, `errores`). Acepta `FormData` para subir archivos (slice 4).
- `js/comun/sesion.js` guarda la sesión en `sessionStorage` y la borra si vence o si la API responde 401.
- `js/comun/formulario.js` muestra cada error de la API junto a su campo (`data-error-de`) y el resto como mensaje general.

## Pendiente

Las demás pantallas del diseño se conectan en los slices siguientes; esta tabla se amplía en cada uno.
