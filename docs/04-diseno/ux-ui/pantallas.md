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
| 06 / 14 | Inicio del agricultor / del comprador (versión sencilla) | `pages/usuario/inicio.html` | `js/usuario/inicio.js` | `GET /api/v1/usuarios/me`, `GET /api/v1/precios/vigentes` (slice 4), `GET /api/v1/conversaciones/no-leidos` (slice 6) | Vender mi cosecha → 07 (agricultor) · Mensajes (slice 6) · Buscar productos (slice 5) · Ver todos los precios → 01 · Mi perfil · Salir → 02 |
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

El administrador entra por la misma pantalla "Ingresar"; desde el slice 11 el inicio lo lleva al **Panel**. La barra lateral muestra Panel · Moderación · Precios de mercado · Usuarios · Reclamos (`js/administracion/comun.js` exige el rol y conecta "Salir").

### Diferencias con el diseño (moderación posterior de v4.1, aprobadas en el plan del slice 3)

- **Acciones:** "Aprobar y publicar" / "Rechazar" pasan a **Conforme**, **Advertir**, **Ocultar** y **Volver a mostrar** (`revisiones_publicacion.decision`).
- **Pestañas:** "En revisión / Rechazadas hoy / Aprobadas hoy" pasan a **Por revisar / Ocultas / Decisiones de hoy**.
- **Marcas:** la marca del diseño "Peso por unidad inusual" no existe en el esquema; se muestran PRIMERA_PUBLICACION, CANTIDAD_INUSUAL, PRECIO_FUERA_RANGO (desde el slice 4) y CORREGIDA.
- **Detalle:** la línea "Hoy en … pagan entre …" todavía no se muestra en moderación (el endpoint `GET /api/v1/precios/referencia` ya existe desde el slice 4); la calificación y las ventas del agricultor, con RF-05 y RF-06.
- **Motivos y mensajes sugeridos:** son los 5 del diseño, que coinciden con `chk_revision_motivo`.

## Venta agrupada (RF-10, slice 10)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Lotes agrupados (diseño "agrupados") | `pages/ventaagrupada/lotes.html` | `js/ventaagrupada/lotes.js` | `GET /api/v1/lotes`, `GET /api/v1/lotes/agricultores` | Enviar propuesta conjunta → Propuesta · Mis propuestas |
| — | Propuesta conjunta (diseño "propuesta") | `pages/ventaagrupada/propuesta.html?idVariedad=&idDistrito=` | `js/ventaagrupada/propuesta.js` | `GET /api/v1/lotes`, `POST /api/v1/propuestas` | Al enviar muestra el reparto · Ver mis propuestas |
| — | Mis propuestas (no está en el diseño) | `pages/ventaagrupada/propuestas.html` | `js/ventaagrupada/propuestas.js` | `GET /api/v1/propuestas`, `POST /{id}/cerrar`, `/cancelar` | Cada parte → Entrega |

- **Entrega de una parte:** `pages/venta/venta.html` muestra "Propuesta conjunta" como primer paso de la línea de tiempo; el agricultor tiene **Aceptar mi parte** y **Rechazar** (con confirmación) y el comprador **Cancelar esta parte**.
- **Aviso PROPUESTA_CONJUNTA:** "Ver propuesta" lleva a la entrega de esa parte.
- **Barra inferior del comprador:** Inicio · Buscar · **Agrupados** · Mensajes, como en el diseño; "Mis compras" sigue en el inicio.

### Diferencias con el diseño (aprobadas en el plan del slice 10)

- **Resultado del envío:** en lugar de volver a la lista, la pantalla muestra cómo se repartió (partes por agricultor) y, si el redondeo deja kilos sin repartir, lo dice.
- **Mis propuestas** y la respuesta del agricultor en la entrega no están en el diseño.

## Avisos y Mis ventas del agricultor (RF-07, slice 9)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Avisos (diseño "notif") | `pages/notificacion/avisos.html` | `js/notificacion/avisos.js` | `GET /api/v1/notificaciones`, `GET /no-leidas`, `POST /{id}/leida`, `POST /leidas` | Cada aviso lleva a su cosecha, chat o venta · Volver → inicio |
| — | Mis ventas del agricultor (diseño "ventas") | `pages/publicacion/mis-cosechas.html` | `js/publicacion/mis-cosechas.js` | `GET /api/v1/publicaciones/mias`, `POST /{id}/precio`, `/extender`, `/retirar`, `PUT /{id}/correccion`, `GET /api/v1/catalogo/unidades` | Ver historial de ventas · Volver → inicio |

- **Campana** en el inicio con los avisos sin leer (`data-avisos-no-leidos`, cada 30 s junto con los mensajes, `js/comun/barra.js`).
- **Destino de cada aviso:** POR_VENCER y MODERACION → `mis-cosechas.html#cosecha-{id}` (la resalta); OFERTA y MENSAJE → chat; VENTA y CALIFICACION → entrega. PROPUESTA_CONJUNTA, RECLAMO y CUENTA no llevan botón hasta sus slices.
- **Barra inferior:** "Mis ventas" del agricultor abre esta pantalla (antes, el historial). El inicio también.

### Diferencias con el diseño (aprobadas en el plan del slice 9)

- **"Bajar precio" del aviso:** abre una hoja con el nuevo precio (solo menor) y su equivalente por kilo; en el diseño llevaba al paso 3 de "Vender".
- **"Extender":** hoja con 7, 14 o 30 días y la nueva fecha; "Publicar otra vez" en una vencida es la misma acción.
- **"N ofertas"** de cada cosecha: no se muestra (las ofertas están en Mensajes).
- **Motivo de la moderación:** el diseño lo muestra en la tarjeta; aquí la tarjeta dice "Oculta por la moderación" y enlaza al aviso, que tiene el motivo.
- **Corregir** (cosecha oculta): hoja con cantidad, peso por unidad (jaba o saco) y precio; no está en el diseño.

## Calificaciones (RF-05, slice 8)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Hoja "¿Cómo le fue con…?" (diseño "calificar") | `<dialog>` de `pages/venta/venta.html` (se abre sola con `?calificar=1`) | `js/venta/venta.js` | `POST /api/v1/ventas/{id}/calificacion` | Enviar calificación · Ahora no |
| — | Reputación (en "Mi perfil" del diseño) | bloque en `pages/usuario/perfil.html` y `pages/publicacion/detalle.html` | `js/venta/reputacion.js` | `GET /api/v1/reputaciones/{idUsuario}` | Ver más comentarios |

- **Entrega:** sección "Calificaciones" con "Usted calificó", "… le calificó" y el botón "Calificar a …" (solo con la venta terminada y sin calificar).
- **Mis ventas / Mis compras → Terminadas:** "Usted calificó ★★★★" o "Calificar a …" (abre la entrega con la hoja).
- Estrellas compartidas en `js/venta/estrellas.js` (texto "★★★★☆" con `aria-label`).

### Diferencias con el diseño (aprobadas en el plan del slice 8)

- **Confirmación:** en lugar de la pantalla "¡Gracias por calificar!", el mensaje aparece en la misma entrega, que ya muestra la calificación.
- **Búsqueda:** solo el promedio "★ 4.8" (el total va en el detalle de la cosecha).
- **Chat:** la reputación de la otra parte ("★ 4.7 · 32 compras") no se muestra todavía.

## Ventas (RF-06 y RF-11, slice 7)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Registrar acuerdo (diseño "acuerdo") | `pages/venta/acuerdo.html?conversacion={id}` | `js/venta/acuerdo.js` | `GET /api/v1/conversaciones/{id}` y `/mensajes` (última oferta), `GET /api/v1/catalogo/unidades`, `POST /api/v1/ventas` | Registrar → Entrega · Volver → Chat |
| — | Entrega (diseño "entrega") | `pages/venta/venta.html?id={id}` | `js/venta/venta.js` | `GET /api/v1/ventas/{id}`, `POST /api/v1/ventas/{id}/estado` | Ir al chat · Volver → Mis ventas |
| — | Mis ventas / Mis compras (en el diseño, pestañas de "Mi perfil") | `pages/venta/historial.html` | `js/venta/historial.js` | `GET /api/v1/ventas`, `GET /api/v1/ventas/exportar` | Venta → Entrega · Volver → inicio |

- **Chat:** botón **Registrar acuerdo** junto a "Hacer oferta" (solo si la cosecha está disponible) y una franja con las ventas de la conversación ("En camino · 10 jabas · Total S/ 280.00 · Ver entrega").
- **Inicio:** botón "Mis ventas" (agricultor) o "Mis compras" (comprador).
- Textos de estados y acciones en `js/venta/estados.js`; descarga del Excel con `descargar()` de `js/api.js` (lleva el token).

### Diferencias con el diseño (aprobadas en el plan del slice 7)

- **Unidades del acuerdo:** las 5 del catálogo (el diseño muestra 3); al cambiar de unidad se convierten la cantidad y el precio por los kilos. Jaba o saco distinta de la publicada pide su peso.
- **Cantidad propuesta:** la de la última oferta, sin pasar de lo disponible.
- **Día de recojo:** editable, hoy o después (en el diseño era fijo).
- **Entrega:** muestra solo los botones que el usuario puede usar ("Confirmar que recibí", "Ya recogieron la carga", "La carga va en camino", "Cancelar venta" con confirmación); en el recojo en chacra la línea de tiempo es Venta acordada → Ya entregado → Venta terminada. "Chacra Las Lomas" del diseño no existe en el esquema: se muestra el distrito.
- **Historial:** página propia (en el diseño son pestañas de "Mi perfil"); pestañas En curso / Terminadas, filtros de fecha y persona, Excel. Calificar llegó con RF-05 (slice 8).

## Mensajería (RF-04, slice 6)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Mensajes (diseño "chats") | `pages/mensajeria/conversaciones.html` | `js/mensajeria/conversaciones.js` | `GET /api/v1/conversaciones` (cada 30 s) | Conversación → Chat · Volver → inicio |
| — | Chat (diseño "chat") | `pages/mensajeria/chat.html` | `js/mensajeria/chat.js` | `GET /api/v1/conversaciones/{id}`, `GET` (cada 10 s, con `despuesDe`) y `POST /api/v1/conversaciones/{id}/mensajes` | Volver → Mensajes |
| — | Hoja "Hacer oferta" (no está en el diseño) | `<dialog>` del chat | — | `POST /api/v1/conversaciones/{id}/mensajes` (OFERTA) | Cerrar · Enviar oferta |

- **Escribir y WhatsApp** (`js/mensajeria/contacto.js`): en las tarjetas de la búsqueda y en el detalle de la cosecha, para el comprador y el visitante (el agricultor y el administrador no los ven). El visitante va a "Ingresar" y vuelve a la misma página (`?volver=`, solo rutas de la aplicación).
- **Inicio:** botón "Mensajes" con el número de no leídos (`GET /api/v1/conversaciones/no-leidos`, cada 30 s).

### Diferencias con el diseño (aprobadas en el plan del slice 6)

- **Hacer oferta:** el diseño muestra la tarjeta "Oferta" pero no cómo se crea; se agrega el botón y la hoja con cantidad y precio por unidad, que muestra los kilos y el total. Ambas partes pueden ofertar.
- **Registrar acuerdo:** el botón del chat llegó con RF-06 (slice 7).
- **Calificación y compras** junto al nombre ("★ 4.7 · 32 compras"): llegan con el módulo `venta`.
- **Barra inferior:** se agregó después del slice 7 (ver "Barra inferior del celular").
- **Cosecha no disponible:** el chat sigue abierto para coordinar; se oculta "Hacer oferta" y se muestra un aviso.

## Administrador · Panel, usuarios y reclamos (RF-09, slice 11) — formato escritorio

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| 11a | Panel | `pages/administracion/panel.html` | `js/administracion/panel.js` | `GET /api/v1/admin/panel`, `GET /api/v1/admin/panel/exportar` | Ir a moderación · Cargar precios · Descargar Excel |
| 11e | Usuarios | `pages/administracion/usuarios.html` | `js/administracion/usuarios.js` | `GET /api/v1/admin/usuarios`, `GET /api/v1/admin/usuarios/{id}`, `POST …/suspension`, `/reactivacion`, `/contrasena-temporal`, `/desactivacion`, `/activacion`, `POST /api/v1/admin/administradores`, `GET /api/v1/catalogo/distritos` | Filtros (rol, estado, texto) · nombre → detalle; ventanas Suspender, Contraseña temporal y Nuevo administrador |
| — | Reclamos (no está en el diseño) | `pages/administracion/reclamos.html` | `js/administracion/reclamos.js` | `GET /api/v1/admin/reclamos?estado=`, `GET /api/v1/admin/reclamos/{id}`, `POST /api/v1/admin/reclamos/{id}/resolucion` | Pestañas Pendientes / Fundados / Infundados; lista y detalle con la línea de tiempo |
| 11c | Moderación: "Mantener oculta" | `pages/administracion/moderacion.html` | `js/administracion/moderacion.js` | `POST /api/v1/moderacion/publicaciones/{id}/decisiones` (BLOQUEO) | En una publicación oculta y corregida: Volver a mostrar · Mantener oculta |
| — | Entrega: "Registrar reclamo" (celular) | `pages/venta/venta.html` | `js/venta/reclamo.js` | `GET /api/v1/reclamos/mios?idVenta=`, `POST /api/v1/reclamos` | Aparece si la venta llegó a acordarse; muestra el estado y la respuesta del reclamo |

### Diferencias con el diseño (aprobadas en el plan del slice 11)

- **Alcance visual:** funcional y sencillo, con la paleta y los componentes existentes; el pulido queda para después.
- **11a:** las tarjetas son publicaciones activas, por revisar, decisiones de hoy y reclamos pendientes; se agregan el embudo (10 publicaciones con más contactos), las mejores y peores reputaciones y "Descargar Excel" (hojas Resumen, Ventas por mes, Embudo y Usuarios). La cola muestra las 3 más antiguas.
- **11e:** la acción de cada fila se abre con el nombre; el detalle agrega último ingreso, suspensión vigente y ventas en curso. Suspender avisa antes cuántas ventas en curso tiene (UC-11 A1). La contraseña temporal se muestra una sola vez y se borra al cerrar la ventana. No hay "editar datos básicos".
- **Reclamos:** pantalla nueva del administrador; la resolución pide resultado y respuesta para las dos partes.
- **Avisos:** RECLAMO lleva a la venta ("Ver venta"); CUENTA no tiene botón.

## Búsqueda (RF-03, slice 5)

| # | Pantalla del diseño | Archivo | JS | Endpoints | Navegación |
|---|---|---|---|---|---|
| — | Buscar (diseño "buscar") | `pages/publicacion/buscar.html` | `js/publicacion/buscar.js` | `GET /api/v1/publicaciones`, `GET /api/v1/catalogo/cultivos`, `/variedades`, `/provincias`, `/distritos`, `/unidades` | Resultado → Detalle · Volver → inicio (con sesión) o portada |
| — | Hoja "Filtrar" | `<dialog>` de la misma página | — | — | Limpiar · Ver resultados · Cerrar |
| — | Detalle de la cosecha (no está en el diseño) | `pages/publicacion/detalle.html` | `js/publicacion/detalle.js` | `GET /api/v1/publicaciones/{id}`; Escribir y WhatsApp (slice 6) | Volver → la búsqueda con sus filtros · Escribir → Chat |

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

## Barra inferior del celular (después del slice 7)

`js/comun/barra.js` agrega la barra en las pantallas principales del agricultor y del comprador (no para el visitante ni el administrador). La opción actual se marca con `aria-current="page"`; "Mensajes" muestra los no leídos (`GET /api/v1/conversaciones/no-leidos`, cada 30 s; también el botón "Mensajes" del inicio, con `data-no-leidos`).

| Rol | Opciones | Pantallas que la muestran |
|---|---|---|
| Agricultor | Inicio · Vender (paso 1) · Mis ventas (sus cosechas, desde el slice 9) · Mensajes | inicio, Mis ventas, historial de ventas, Mensajes, Avisos; en Buscar aparece sin opción marcada |
| Comprador | Inicio · Buscar · Agrupados (desde el slice 10) · Mensajes | inicio, Buscar, Lotes agrupados, Mis propuestas, Mensajes, Avisos; en el historial de compras aparece sin opción marcada |

Los pasos de "Vender", el chat, la entrega, el acuerdo y el detalle de la cosecha no la muestran (usan "atrás"), como en el diseño.

### Diferencias con el diseño (aprobadas)

- **"Mis ventas" del agricultor:** desde el slice 9 abre la lista de sus cosechas con acciones rápidas, como en el diseño; el historial de ventas queda enlazado desde ahí.
- **"Agrupados" del comprador:** desde el slice 10, como en el diseño; "Mis compras" quedó como botón del inicio.

## Sesión en el navegador

- `js/api.js` agrega `Authorization: Bearer <token>` y convierte las respuestas de error en `ErrorApi` (`estado`, `message`, `errores`). Acepta `FormData` para subir archivos (slice 4).
- `js/comun/sesion.js` guarda la sesión en `sessionStorage` y la borra si vence o si la API responde 401.
- `js/comun/formulario.js` muestra cada error de la API junto a su campo (`data-error-de`) y el resto como mensaje general.

## Pendiente

Las demás pantallas del diseño se conectan en los slices siguientes; esta tabla se amplía en cada uno.
