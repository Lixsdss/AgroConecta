# Casos de Uso — AgroConecta

**Documento:** Casos de uso del sistema  
**Versión:** 1.4  
**Fecha:** 27 de septiembre de 2026  
**Fuente:** BPMN TO-BE (`docs/03-procesos/AgroConecta_TO-BE.drawio`), SRS v1.3 y `docs/04-diseno/base-datos/modelo-bd.sql` v4.1  
**Alcance:** RF-01 a RF-11 — 12 casos de uso (UC-01 a UC-12)

**Actores:** Agricultor, Comprador y Administrador. El **visitante sin sesión** no es actor (solo consulta publicaciones, precios y catálogo) y el **transportista** no es usuario ni se registra en la base de datos. "Sistema" indica la respuesta de la plataforma.

**Cambios v1.1:** moderación posterior; transacción que inicia como PROPUESTA; transportista como dato; historial exportable a Excel; inicio de sesión con teléfono; nuevo UC-12 Venta agrupada; estados alineados con `modelo-bd.sql`.

**Cambios v1.2:** módulos alineados con la arquitectura modular (SRS v1.2 §5); UC-05, UC-06 A1 y UC-08 trazados a RF-06 "Registro e historial de transacciones"; notificaciones en la aplicación creadas por el módulo `notificaciones`; cambio de contraseña propio (UC-10); verificación del estado del usuario en cada petición (UC-11); UC-06 A4 corregido (los eventos de entrega no se corrigen con eventos nuevos); rutas de `docs/` actualizadas.

**Cambios v1.3:** referencias a tablas, vistas, estados y triggers alineadas con el modelo de datos v4 (`docs/04-diseno/base-datos/cambios-v4.md`).

**Cambios v1.4:** alineado con el modelo de datos v4.1 y el SRS v1.3. La publicación nace PUBLICADA y la moderación es posterior (UC-01, UC-11: conforme, advertir, bloquear y desbloquear, con marca CORREGIDA); el acuerdo del chat se registra directamente como ACORDADA después de las ofertas (UC-05); recojo en chacra (UC-06 A1) y sin cancelación después del recojo; venta agrupada propuesta por el comprador y aceptada o rechazada por cada agricultor, con reparto proporcional (UC-12); reclamos (UC-11 Parte 3); se eliminan UC-01 A2 (la vida útil del cultivo es obligatoria) y UC-04 A3 (las conversaciones no se cierran); módulos en singular (`usuario`, `publicacion`, `precio`, `venta`, `notificacion`…).

---

## Estructura estándar de cada caso de uso

```
UC-NN: [Nombre]
Actores: [Principales y secundarios]
Precondición: [Estado del sistema antes de iniciar]
Flujo básico: [Pasos normales, numerados]
Flujos alternos: [Desvíos y excepciones]
Postcondición: [Estado del sistema después de terminar]
Requisito: [RF-XX]
Módulo: [módulo de pe.agroconecta]
```

**Notificaciones:** todas se muestran dentro de la aplicación (sin correo, SMS ni push). Las de tipo POR_VENCER las crea el evento `ev_tareas_diarias` y las de tipo MODERACION el trigger `trg_revisiones_ai`; las demás (MENSAJE, OFERTA, VENTA, PROPUESTA_CONJUNTA, CALIFICACION, RECLAMO, CUENTA) las crea el módulo que ejecuta la acción, a través de la capa `aplicacion` del módulo `notificacion` y en la misma transacción.

---

## UC-01: Publicar cosecha

**Actores:**
- Agricultor (principal)

**Precondición:**
- El agricultor tiene sesión iniciada y su usuario está ACTIVO.
- El agricultor tiene cosecha lista para vender.

**Flujo básico:**
1. El agricultor entra a "Publicar cosecha".
2. El sistema presenta un formulario de 3 pasos (los catálogos provienen del módulo `catalogo`):
   - Paso 1: cultivo y variedad (por defecto "Común"), unidad de venta (kilo, jaba, saco, quintal o tonelada) y cantidad; para jaba y saco, cuántos kilos pesa cada unidad (se sugiere el peso típico del cultivo).
   - Paso 2: precio por unidad y distrito donde está la cosecha. El sistema muestra el precio por kilo calculado, el precio del mercado de hoy y el rango de la plataforma (UC-02) como ayuda.
   - Paso 3: fecha de cosecha y fecha límite de venta (opcional).
3. El agricultor completa los pasos y confirma.
4. El sistema valida: cantidad > 0, precio > 0, peso por unidad > 0 si es jaba o saco, fecha límite ≥ hoy y ≥ fecha de cosecha, usuario ACTIVO, variedad/unidad/distrito existentes.
5. El sistema registra la publicación con estado **PUBLICADA** (moderación posterior). `trg_publicaciones_bi` toma el peso del catálogo para kilo, quintal y tonelada y, si no se indicó fecha límite, la calcula como `fecha_cosecha + vida_util_dias` del cultivo. Si es la primera publicación del agricultor, la marca como PRIMERA_PUBLICACION; el service marca PRECIO_FUERA_RANGO o CANTIDAD_INUSUAL cuando corresponde.
6. La publicación aparece de inmediato en la búsqueda (UC-03).
7. El sistema muestra "Publicación creada" con los kilos totales y la fecha límite resultante.

**Flujos alternos:**

**A1 — Validación falla:**
- En el paso 4 el sistema detecta un error (p. ej. cantidad inválida, jaba sin peso, usuario SUSPENDIDO).
- El sistema muestra el mensaje junto al campo (400/422) y el agricultor corrige desde el paso correspondiente.

**A2 — Agricultor cancela:**
- El agricultor abandona el formulario; no se guarda nada.

**A3 — Moderación posterior:**
- Si la publicación quedó marcada, entra a la cola "Por revisar" del administrador (UC-11, Parte 2), sin dejar de estar visible.
- Si el administrador la advierte o la bloquea, el agricultor recibe una notificación MODERACION con el motivo y el mensaje. Si la corrige estando bloqueada, vuelve a la cola con la marca CORREGIDA.

**Postcondición:**
- Flujo básico: publicación PUBLICADA y visible en la búsqueda.
- A2: la publicación no se crea.

**Requisito:** RF-01  
**Módulo:** `publicacion` (catálogos: `catalogo`)  
**Tablas:** publicaciones, variedades, cultivos, unidades_medida, distritos

---

## UC-02: Consultar precios de referencia

**Actores:**
- Agricultor (principal, al publicar)
- Comprador (principal, al buscar)
- Administrador (principal en A2, carga datos)
- También accesible para el visitante sin sesión

**Precondición:**
- Existen precios cargados o ventas COMPLETADA.

**Flujo básico:**
1. El usuario entra a "Precios" (o los ve dentro de UC-01).
2. Elige cultivo (obligatorio) y, opcionalmente, zona de precio (Piura, Tambogrande, Chulucanas o Sullana).
3. El sistema consulta `v_precios_vigentes`.
4. El sistema muestra, por cultivo y zona, el precio por kilo del mercado real y su equivalente en la unidad local del cultivo (jaba, saco…), la fuente (MERCADO_MAYORISTA_PIURA, MERCADO_SULLANA o ENCUESTA_ACOPIADORES), la fecha de actualización y la tendencia frente a la carga anterior (SUBE, BAJA, IGUAL o SIN_DATO).
5. Al publicar (UC-01), el sistema muestra además el rango de la plataforma: mínimo, máximo y promedio por kilo de las ventas COMPLETADA de los últimos 30 días en ese cultivo y distrito (`v_rango_precio_plataforma`).
6. Opcionalmente, el usuario consulta el histórico de un cultivo y zona (`precios_mercado` por fecha).

**Flujos alternos:**

**A1 — Sin datos de precio:**
- El cultivo no tiene precios cargados: el sistema muestra "No hay precios de referencia para este cultivo". Sin ventas completadas, no muestra el rango de la plataforma.

**A2 — Carga de precios por el administrador:**
- El administrador entra a "Precios de mercado" y elige:
  - Formulario: cultivo, zona de precio, fecha, precio por kilo y fuente.
  - Archivo Excel/CSV con cabecera `cultivo, zona, fecha, precio_kg`, más la fuente.
- El sistema valida: cultivo y distrito existen y el distrito es zona de precio; precio por kilo > 0; no existe ya un precio para ese cultivo, zona y fecha.
- El sistema registra las filas válidas en `precios_mercado` con `metodo` FORMULARIO o ARCHIVO y el administrador que carga. `trg_precios_mercado_bi` confirma que el usuario es ADMINISTRADOR y que el distrito es zona de precio.
- Con archivo, las filas inválidas se listan con su motivo y el administrador puede reintentar solo esas.

**A3 — Filtro sin resultados:**
- El sistema muestra "Sin resultados" y sugiere quitar la zona.

**Postcondición:**
- El usuario ve los precios del día, su tendencia y el rango de la plataforma.
- A2: precios registrados con administrador, fuente, método y fecha.

**Requisito:** RF-02  
**Módulo:** `precio` (catálogos: `catalogo`)  
**Tablas:** precios_mercado, cultivos, distritos, unidades_medida, `v_precios_vigentes`, `v_rango_precio_plataforma`

---

## UC-03: Buscar productos disponibles

**Actores:**
- Comprador (principal)
- También accesible para el visitante sin sesión

**Precondición:**
- Ninguna (la búsqueda puede no tener resultados).

**Flujo básico:**
1. El usuario entra a "Buscar" o a la página de inicio.
2. El sistema presenta filtros opcionales: cultivo (el filtro "Categoría" de la interfaz), variedad, provincia/distrito, unidad y fecha de cosecha desde/hasta.
3. El usuario elige criterios, el orden (precio por kilo, fecha de cosecha o calificación del agricultor) y pulsa "Buscar".
4. El sistema consulta `v_publicaciones_disponibles` con:
   - `estado = PUBLICADA`
   - `disponible_kg > 0`
   - los filtros elegidos
5. El sistema devuelve resultados paginados (20 por página) en el orden elegido.
6. Cada resultado muestra: cultivo y variedad, cantidad disponible y unidad, precio por unidad y por kilo, provincia y distrito, días restantes hasta la fecha límite (`dias_restantes`, `por_vencer`) y la reputación del agricultor (`calificacion_agricultor`).
7. El usuario abre un resultado para ver el detalle y, con sesión de comprador, contactar (UC-04).

**Flujos alternos:**

**A1 — Sin resultados:**
- El sistema muestra "No encontramos productos con esos filtros" y sugiere quitar filtros.

**A2 — Visitante sin sesión:**
- Puede buscar y ver el detalle, pero "Contactar" y "WhatsApp" lo llevan a iniciar sesión o registrarse (UC-10).

**A3 — Paginación:**
- El usuario cambia de página; el sistema carga los siguientes 20 resultados.

**Postcondición:**
- El usuario ve la lista filtrada y puede abrir el detalle o contactar.

**Requisito:** RF-03  
**Módulo:** `publicacion` (filtros: `catalogo`; reputación: `venta`)  
**Tablas:** `v_publicaciones_disponibles`, `v_reputacion_usuario`, cultivos, variedades, provincias, distritos, unidades_medida

---

## UC-04: Contactar y negociar con el agricultor

**Actores:**
- Comprador (principal)
- Agricultor (secundario)

**Precondición:**
- El comprador tiene sesión iniciada y está ACTIVO.
- La publicación está PUBLICADA (UC-03).

**Flujo básico:**
1. El comprador abre el detalle de la publicación y pulsa "Contactar al agricultor".
2. El sistema busca la conversación de ese comprador para esa publicación (una por par publicación–comprador).
3. Si no existe, la crea (`canal_inicio` = CHAT) y abre el chat; `trg_conversaciones_bi` exige que la publicación esté PUBLICADA.
4. El comprador escribe un mensaje o envía una **oferta** (`tipo` = OFERTA, con cantidad y precio por unidad de la publicación).
5. El sistema registra el mensaje con remitente y fecha; `trg_mensajes_bi` confirma que el remitente participa y que las ofertas solo se envían mientras la publicación está PUBLICADA.
6. El sistema crea una notificación MENSAJE (u OFERTA) para la otra parte.
7. El agricultor ve la notificación (el frontend consulta periódicamente), abre el chat y responde con mensajes o contraofertas.
8. Cuando hay acuerdo, cualquiera de las dos partes pulsa "Registrar acuerdo" → UC-05.
9. La conversación **no se cierra**: después del acuerdo sigue sirviendo para coordinar la entrega (UC-06).

**Flujos alternos:**

**A1 — Contacto por WhatsApp:**
- El comprador pulsa "Contactar por WhatsApp".
- El sistema crea la conversación si no existía (`canal_inicio` = WHATSAPP), suma 1 a `clics_whatsapp` y abre el enlace `wa.me` con el celular del agricultor y un texto sugerido.
- La negociación puede continuar fuera de la plataforma; el acuerdo se registra igualmente en UC-05.

**A2 — Sin acuerdo:**
- Las partes no llegan a un acuerdo; la conversación queda sin venta registrada.

**A3 — Oferta sobre una publicación no disponible:**
- La publicación ya está VENDIDA, VENCIDA, BLOQUEADA o RETIRADA: el sistema rechaza la oferta (422, "Solo se hacen ofertas sobre una publicacion PUBLICADA"). Los mensajes de texto se siguen aceptando.

**A4 — Remitente no autorizado:**
- Un usuario que no es el comprador ni el agricultor intenta escribir.
- El service lo rechaza (403); si llegara a la BD, `trg_mensajes_bi` lo rechaza ("El remitente no participa en la conversacion").

**Postcondición:**
- Conversación con historial visible para ambas partes.
- A1: clic de WhatsApp registrado en la conversación.

**Requisito:** RF-04  
**Módulo:** `mensajeria`  
**Tablas:** conversaciones, mensajes, notificaciones

---

## UC-05: Registrar el acuerdo de venta

**Actores:**
- Agricultor o Comprador (principal: quien registra el acuerdo)
- La otra parte (secundario: puede cancelarlo mientras esté ACORDADA)

**Precondición:**
- Existe una conversación entre las partes (UC-04), normalmente con ofertas intercambiadas.
- La publicación está PUBLICADA y ambos usuarios están ACTIVO.

**Flujo básico:**
1. Tras las ofertas del chat, una de las partes pulsa "Registrar acuerdo". El sistema propone los datos de la última oferta.
2. La parte confirma o ajusta la unidad (puede diferir de la publicada), la cantidad, el precio por unidad, el peso por unidad si es jaba o saco y el día de recojo.
3. El sistema valida: cantidad > 0, precio > 0, kilos acordados ≤ kilos disponibles, publicación PUBLICADA.
4. El sistema bloquea la publicación (`PESSIMISTIC_WRITE`) y registra la venta directamente como **ACORDADA**, con `id_actualizado_por` = quien registra; `trg_transacciones_bi` repite las validaciones y `trg_transacciones_ai` guarda el estado en `transacciones_historial`.
5. Si con este acuerdo se agotan los kilos, `sp_sincronizar_publicacion` pasa la publicación a VENDIDA.
6. El sistema muestra el monto total y notifica a la otra parte (VENTA) con los datos de contacto de la contraparte.
7. Continúa en UC-06.

**Flujos alternos:**

**A1 — Cancelación mientras está ACORDADA:**
- Cualquiera de las partes cancela la venta: estado CANCELADA (`id_actualizado_por` = quien cancela). La contraparte recibe una notificación VENTA.
- Si la publicación estaba VENDIDA y vuelve a haber saldo, `sp_sincronizar_publicacion` la devuelve a PUBLICADA (o VENCIDA si ya pasó su fecha límite).

**A2 — Cantidad no disponible o publicación no PUBLICADA:**
- El sistema responde 422 con el mensaje del trigger (p. ej. "La cantidad supera lo disponible en la publicacion").

**A3 — Cambio de términos:**
- Los términos de una venta (unidad, cantidad, peso y precio) no se modifican: `trg_transacciones_bu` responde "Los terminos no se modifican; cancele y registre un nuevo acuerdo". Se cancela (A1) y se registra otro acuerdo.

**A4 — Cancelación después del recojo:**
- La venta ya está RECOGIDO, EN_CAMINO o ENTREGADO: no se puede cancelar (422). El problema se resuelve con un reclamo (UC-11, Parte 3).

**Postcondición:**
- Venta ACORDADA o CANCELADA.
- Publicación PUBLICADA o VENDIDA según los kilos restantes.

**Requisito:** RF-06 (registro de transacciones), a partir de la negociación de RF-04  
**Módulo:** `venta`  
**Tablas:** transacciones, transacciones_historial, conversaciones, publicaciones, notificaciones

---

## UC-06: Registrar entrega y completar la venta

**Actores:**
- Agricultor (principal: marca el recojo y el traslado)
- Comprador (principal: confirma la recepción)

**Precondición:**
- La venta está ACORDADA.

**Flujo básico (con transporte):**
1. Al recoger la carga, el agricultor o el comprador marca **RECOGIDO**.
2. Al salir hacia el destino, el agricultor o el comprador marca **EN_CAMINO**.
3. El comprador revisa la carga y pulsa "Confirmar que recibí".
4. El sistema, en una sola transacción (`@Transactional`), pasa la venta a **ENTREGADO** y luego a **COMPLETADA**, con `id_actualizado_por` = comprador.
5. En cada cambio, `trg_transacciones_bu` valida la secuencia y quién lo hace, y `trg_transacciones_au` registra el paso en `transacciones_historial` (línea de tiempo con fecha, hora y autor). El sistema notifica a la otra parte (VENTA).
6. El sistema notifica a ambos que pueden calificar → UC-07.

**Flujos alternos:**

**A1 — Recojo en chacra (sin transporte):**
- El comprador recoge la cosecha en la chacra del agricultor.
- Al recibirla, el comprador pulsa "Confirmar que recibí" sobre la venta ACORDADA; el sistema la pasa a ENTREGADO y luego a COMPLETADA en una sola transacción. No se registran RECOGIDO ni EN_CAMINO.

**A2 — Discrepancia en la entrega:**
- El comprador no confirma la recepción y lo indica por el chat (UC-04), que sigue abierto.
- Si la venta sigue ACORDADA, una de las partes puede cancelarla (UC-05 A1). **Desde RECOGIDO ya no se puede cancelar:** la parte afectada registra un reclamo (UC-11, Parte 3).

**A3 — Cambio fuera de secuencia:**
- Se intenta marcar EN_CAMINO sin RECOGIDO, o el agricultor intenta confirmar la recepción.
- El sistema responde 422: "Cambio de estado no permitido para este usuario".

**A4 — Estado registrado por error:**
- Los estados no retroceden y `transacciones_historial` no se edita ni se elimina; no existen registros de corrección. Si el error impide continuar, la parte afectada registra un reclamo.

**Postcondición:**
- Venta COMPLETADA con su línea de tiempo en `transacciones_historial`.
- Ambas partes pueden calificar.

**Requisito:** RF-11 (A1: RF-06)  
**Módulo:** `venta`  
**Tablas:** transacciones, transacciones_historial, notificaciones

---

## UC-07: Calificar la venta

**Actores:**
- Agricultor (califica al comprador)
- Comprador (califica al agricultor)

**Precondición:**
- La venta está COMPLETADA.
- El usuario es el comprador o el agricultor de la venta y aún no la calificó.

**Flujo básico:**
1. El usuario abre la venta en su historial (UC-08) y pulsa "Calificar".
2. El sistema muestra el formulario: puntuación de 1 a 5 estrellas y comentario opcional (máx. 500 caracteres).
3. El usuario completa y pulsa "Enviar calificación".
4. El sistema valida: puntuación 1–5, usuario participante, venta COMPLETADA, sin calificación previa del mismo usuario.
5. El sistema registra la calificación; `trg_calificaciones_bi` repite las validaciones de estado y participante.
6. El sistema crea una notificación CALIFICACION para la contraparte.
7. La reputación se refleja al consultar `v_reputacion_usuario` (no se almacena).

**Flujos alternos:**

**A1 — Venta no completada:**
- El sistema responde 422: "Solo se califican ventas COMPLETADAS".

**A2 — Ya calificada:**
- El sistema responde 409 (clave única venta–evaluador): "Ya calificaste esta venta". Las calificaciones no se editan.

**A3 — Comentario inapropiado:**
- El afectado lo informa a la plataforma; si el administrador confirma una conducta indebida, puede suspender al autor con el motivo USO_INDEBIDO y un detalle (UC-11, Parte 1). La advertencia y el bloqueo de la moderación posterior se aplican a publicaciones, no a comentarios; el comentario no se edita ni se elimina.

**Postcondición:**
- Calificación registrada; la reputación de la contraparte se actualiza en la vista.

**Requisito:** RF-05  
**Módulo:** `venta`  
**Tablas:** calificaciones, transacciones, `v_reputacion_usuario`, notificaciones

---

## UC-08: Ver historial de ventas

**Actores:**
- Agricultor (principal)
- Comprador (principal)

**Precondición:**
- El usuario tiene sesión iniciada.

**Flujo básico:**
1. El usuario entra a "Mis ventas" (agricultor) o "Mis compras" (comprador).
2. El sistema consulta `v_historial_transacciones` donde el usuario es `id_comprador` o `id_agricultor`.
3. El usuario puede filtrar por estado (en curso: PROPUESTA, ACORDADA, RECOGIDO, EN_CAMINO, ENTREGADO; terminadas: COMPLETADA, RECHAZADA, CANCELADA), rango de fechas y contraparte.
4. El sistema devuelve la lista paginada (20 por página).
5. Cada fila muestra: cultivo y variedad, cantidad y unidad, kilos, precio por unidad y por kilo, monto total, contraparte, estado, fecha de recojo y fechas de creación y última actualización.
6. Al abrir una venta, el sistema muestra:
   - La línea de tiempo de `transacciones_historial` (cada estado con fecha, hora y autor).
   - Las calificaciones emitidas y recibidas.
   - Un enlace a la conversación de la publicación.
7. Opción "Exportar a Excel": el sistema genera un `.xlsx` (Apache POI) con las ventas del filtro actual.

**Flujos alternos:**

**A1 — Sin ventas:**
- El sistema muestra "Aún no tienes ventas" y un enlace a "Buscar productos" (UC-03) o "Publicar cosecha" (UC-01).

**A2 — Filtro sin resultados:**
- El sistema muestra "Sin resultados para este filtro".

**A3 — Exportación a Excel:**
- El archivo se descarga como `AgroConecta_Historial_{fecha}.xlsx`, con una fila por venta y las mismas columnas del listado.

**Postcondición:**
- El usuario consulta su historial y puede exportarlo.

**Requisito:** RF-06 (historial)  
**Módulo:** `venta`  
**Tablas:** `v_historial_transacciones`, transacciones_historial, calificaciones

---

## UC-09: Recibir alertas de vencimiento

**Actores:**
- Agricultor (principal)
- Evento programado `ev_tareas_diarias` (disparador temporal, no es actor)

**Precondición:**
- La publicación está PUBLICADA.

**Flujo básico:**
1. **Evento diario (06:00, hora del servidor):**
   - Si `fecha_limite = hoy + 3`, crea una notificación POR_VENCER "Su [cultivo] vence en 3 días. ¿Qué desea hacer?".
   - Si `fecha_limite = hoy + 1`, crea una notificación POR_VENCER "Su [cultivo] vence mañana. ¿Qué desea hacer?".
   - No repite la alerta de una publicación el mismo día.
2. **El agricultor ve la alerta** en su centro de notificaciones (en la app) y la abre; el sistema la marca como leída y muestra la publicación.
3. **El agricultor elige una acción rápida:**
   - **Bajar el precio:** actualiza `precio_unitario` (el precio por kilo se recalcula).
   - **Extender la fecha:** elige +7, +14 o +30 días; el sistema valida que la nueva fecha sea ≥ hoy y actualiza `fecha_limite`.
   - **Retirar:** estado RETIRADA (no se venderá).
   - **No hacer nada:** la publicación sigue PUBLICADA.
4. **Vencimiento automático:** al día siguiente de la fecha límite, el evento pasa la publicación a VENCIDA; deja de aparecer en la búsqueda.
5. **Reactivación:** el agricultor extiende la fecha de una publicación VENCIDA (actualizando solo `fecha_limite`); `trg_publicaciones_bu` la devuelve a PUBLICADA.

**Flujos alternos:**

**A1 — Publicación no PUBLICADA:**
- Si está BLOQUEADA, VENDIDA, VENCIDA o RETIRADA, el evento no genera alertas.

**A2 — Varias alertas el mismo día:**
- El centro de notificaciones las muestra juntas, ordenadas por fecha; cada una lleva a su publicación.

**Postcondición:**
- Publicación con precio ajustado, fecha extendida, RETIRADA, o VENCIDA según el caso.

**Requisito:** RF-07  
**Módulo:** `notificacion` (centro de notificaciones) y `publicacion` (acciones rápidas)  
**Tablas:** notificaciones, publicaciones (evento `ev_tareas_diarias`)

---

## UC-10: Registrar usuario e iniciar sesión

**Actores:**
- Agricultor o Comprador (principal, se registra desde un visitante sin sesión)
- Administrador (A5: crea administradores)

**Precondición:**
- El visitante está en la página de registro.

**Flujo básico:**
1. El visitante elige "Soy agricultor" o "Soy comprador".
2. El sistema muestra el formulario:
   - **Común:** nombre completo, celular (obligatorio, 9 dígitos que empiezan en 9, será el usuario de acceso), distrito (obligatorio, catálogo) y contraseña (mín. 8 caracteres, con letras y números). No se pide email.
   - **Agricultor:** nombre de la chacra (opcional).
   - **Comprador:** nombre del negocio (obligatorio), tipo de comprador (MERCADO_MAYORISTA, RESTAURANTE, HOTEL, PROCESADORA, MOLINO u OTRO) y RUC (opcional, 11 dígitos).
3. El visitante pulsa "Crear cuenta".
4. El sistema valida: campos obligatorios, formato del celular y del RUC; celular y RUC no registrados; contraseña segura.
5. El sistema, en una sola transacción:
   - Crea la fila en `usuarios` (rol AGRICULTOR o COMPRADOR, estado ACTIVO, contraseña con BCrypt).
   - Crea la fila en `agricultores` o `compradores`; `trg_agricultores_bi` / `trg_compradores_bi` verifican que el rol coincida.
6. El sistema emite un JWT, el frontend lo guarda en `sessionStorage` y muestra "Bienvenido, [nombre]".
7. El agricultor puede publicar (UC-01); el comprador, buscar (UC-03).

**Inicio de sesión:**
1. El usuario ingresa celular y contraseña.
2. El sistema verifica la contraseña y que el usuario esté ACTIVO, actualiza `ultimo_acceso` y emite el JWT.

**Cambio de contraseña propio:**
1. El usuario con sesión entra a "Mi perfil" → "Cambiar contraseña".
2. Ingresa la contraseña actual y la nueva (mín. 8 caracteres, con letras y números).
3. El sistema verifica la contraseña actual y guarda la nueva con BCrypt.

**Flujos alternos:**

**A1 — Celular o RUC ya registrado:**
- El sistema responde 409: "Este celular ya está registrado".

**A2 — Contraseña débil:**
- El sistema responde 400: "Usa al menos 8 caracteres con letras y números".

**A3 — Credenciales incorrectas o usuario no ACTIVO:**
- El sistema responde 401 sin indicar cuál dato falló (o que la cuenta está suspendida, si corresponde).

**A4 — Olvidó su contraseña:**
- El sistema muestra "Comunícate con el administrador para reiniciar tu contraseña" (UC-11).

**A5 — Alta de administrador:**
- Un administrador crea otra cuenta con rol ADMINISTRADOR desde el panel (sin fila de subtipo).

**A6 — Contraseña actual incorrecta (cambio de contraseña):**
- El sistema responde 422: "La contraseña actual no es correcta" y no cambia nada.

**Postcondición:**
- Usuario ACTIVO con su subtipo y sesión iniciada con JWT.
- Cambio de contraseña: nueva contraseña vigente desde el siguiente inicio de sesión.

**Requisito:** RF-08  
**Módulo:** `usuario` (distritos: `catalogo`)  
**Tablas:** usuarios, agricultores, compradores, distritos

---

## UC-11: Gestionar la plataforma (Administrador)

**Actores:**
- Administrador (principal)

**Precondición:**
- El administrador tiene sesión iniciada con rol ADMINISTRADOR.

**Flujo básico:**

**Parte 1 — Gestión de usuarios:**
1. El administrador entra a "Usuarios".
2. El sistema lista los usuarios (`v_resumen_usuario`), filtrables por rol y estado, con búsqueda por nombre o celular.
3. El administrador puede:
   - Ver el detalle: rol, distrito, negocio o chacra, fecha de registro, último acceso, reputación, operaciones completadas, reclamos fundados y suspensión vigente.
   - Editar datos básicos.
   - **Reiniciar la contraseña:** asigna una contraseña temporal (BCrypt) y la comunica al usuario por teléfono.
   - Crear otro administrador (UC-10 A5).
   - Desactivar la cuenta (INACTIVO).
4. Ante una conducta indebida, pulsa "Suspender", elige el motivo (RECLAMOS_FUNDADOS, DATOS_FALSOS, PUBLICACIONES_ENGANOSAS, USO_INDEBIDO), escribe el detalle y confirma:
   - El sistema registra la suspensión en `suspensiones`; `trg_suspensiones_bi` verifica que quien suspende es ADMINISTRADOR, que el afectado no lo es, que no tenga otra suspensión abierta y, con RECLAMOS_FUNDADOS, que tenga al menos 3 reclamos fundados.
   - `trg_suspensiones_ai` pasa el usuario a SUSPENDIDO; pierde el acceso en su siguiente petición (el validador del token verifica el estado del usuario en cada petición).
   - El usuario recibe una notificación CUENTA.
5. "Reactivar" registra `fecha_fin` en la suspensión y `trg_suspensiones_au` devuelve el usuario a ACTIVO.

**Parte 2 — Moderación posterior de publicaciones:**
1. El administrador abre la cola "Por revisar": publicaciones con marca del sistema (`marca_revision`: PRIMERA_PUBLICACION, PRECIO_FUERA_RANGO, CANTIDAD_INUSUAL o CORREGIDA), que siguen visibles mientras esperan.
2. Revisa los datos de la publicación, los del agricultor y sus revisiones anteriores (`revisiones_publicacion`).
3. Registra una decisión:
   - **Conforme:** la publicación sigue PUBLICADA y sale de la cola.
   - **Advertir:** con motivo de catálogo (PRECIO_NO_COINCIDE, CANTIDAD_INCORRECTA, PESO_UNIDAD_INCORRECTO, DATOS_INCORRECTOS u OTRO) y mensaje; sigue PUBLICADA y el agricultor recibe una notificación MODERACION.
   - **Bloquear (Ocultar):** con motivo y mensaje; la publicación pasa a BLOQUEADA, deja de aparecer en la búsqueda y el agricultor recibe una notificación MODERACION.
   - `trg_revisiones_bi` verifica que quien revisa es ADMINISTRADOR y que la publicación está PUBLICADA; `trg_revisiones_ai` aplica la decisión y borra la marca.
4. Si el agricultor corrige una publicación BLOQUEADA, `trg_publicaciones_bu` le pone la marca CORREGIDA y vuelve a la cola.
5. **Desbloquear (Volver a mostrar):** sobre una publicación BLOQUEADA; `trg_revisiones_ai` la devuelve a PUBLICADA (o VENCIDA si ya pasó su fecha límite) y notifica al agricultor (MODERACION).

**Parte 3 — Reclamos:**
1. Una de las partes de una venta que llegó a ACORDADA registra un reclamo con motivo (NO_SE_PRESENTO, PRODUCTO_NO_COINCIDE, CANTIDAD_INCOMPLETA, INCUMPLIO_PAGO, MAL_TRATO) y descripción; `trg_reclamos_bi` verifica que la venta llegó a ACORDADA (según su historial) y que el reclamante es parte. El reclamo queda PENDIENTE.
2. El administrador revisa los reclamos pendientes, la venta y su línea de tiempo.
3. Resuelve el reclamo como FUNDADO o INFUNDADO con una respuesta; `trg_reclamos_bu` exige que sea ADMINISTRADOR y fija la fecha de resolución. Las partes reciben una notificación RECLAMO.
4. Con 3 o más reclamos fundados contra un usuario, el administrador puede suspenderlo con el motivo RECLAMOS_FUNDADOS (Parte 1).

**Parte 4 — Panel y métricas:**
1. El administrador entra al "Panel".
2. El sistema muestra desde vistas:
   - Tarjetas (`v_panel_admin`): publicaciones activas, publicadas en los últimos 7 días, por revisar y la más antigua, conformes y bloqueadas hoy, precios cargados frente a esperados y reclamos pendientes.
   - Ventas completadas, kilos y monto por mes y cultivo (`v_ventas_mensuales_cultivo`).
   - Embudo por publicación: clics de WhatsApp, contactos, ofertas, acuerdos y completadas (`v_conversion_publicacion`).
   - Usuarios con mejor y peor reputación (`v_resumen_usuario`).
3. "Exportar a Excel" descarga el reporte (Apache POI).

**Parte 5 — Carga de precios de referencia:**
- Ver UC-02 A2.

**Flujos alternos:**

**A1 — Suspensión con ventas en curso:**
- Si el usuario tiene ventas PROPUESTA o ACORDADA, el sistema lo advierte.
- El administrador confirma; las ventas no cambian de estado automáticamente. Las contrapartes reciben una notificación y pueden cancelar las ACORDADA (UC-05 A1).

**A2 — Reconsideración:**
- El usuario suspendido se comunica con la plataforma por teléfono o WhatsApp; el administrador decide si lo reactiva (registra `fecha_fin`).

**A3 — Motivo o mensaje vacío:**
- El sistema no registra la suspensión sin motivo y detalle, ni la advertencia o el bloqueo sin motivo y mensaje (400).

**A4 — Decisión no permitida:**
- Se intenta advertir, bloquear o dejar conforme una publicación que no está PUBLICADA, o desbloquear una que no está BLOQUEADA: 422 con el mensaje del trigger.

**A5 — Reclamo no permitido:**
- La venta nunca llegó a ACORDADA, el reclamante no es parte o ya reclamó esa venta: 422 o 409.

**Postcondición:**
- Usuarios, publicaciones y reclamos gestionados con su registro en `suspensiones`, `revisiones_publicacion` y `reclamos`.
- Métricas consultadas o exportadas.

**Requisito:** RF-09 (panel administrativo), RF-08 (gestión de usuarios), RF-02 (carga de precios)  
**Módulo:** `administracion` (usa la capa `aplicacion` de `usuario`, `publicacion`, `venta`, `precio` y `notificacion`)  
**Tablas:** usuarios, publicaciones, revisiones_publicacion, reclamos, suspensiones, transacciones, notificaciones, `v_resumen_usuario`, `v_panel_admin`, `v_ventas_mensuales_cultivo`, `v_conversion_publicacion`

---

## UC-12: Venta agrupada

**Actores:**
- Comprador (principal: crea la propuesta conjunta)
- Agricultores (principales: aceptan o rechazan su parte)

**Precondición:**
- El comprador y los agricultores tienen sesión iniciada y están ACTIVO.
- Existe al menos un lote: publicaciones PUBLICADA del mismo cultivo, variedad y distrito, de 2 o más agricultores.

**Flujo básico:**
1. El comprador entra a "Venta agrupada" y consulta `v_lotes_agrupados`: total de kilos disponibles, precio promedio por kilo y número de agricultores por cultivo, variedad y distrito.
2. Elige un lote y pulsa "Enviar propuesta conjunta": cantidad total en kilos, precio por kilo y fecha de recojo (también es el límite para responder).
3. El sistema valida (cantidad > 0, cantidad ≤ kilos del lote, precio > 0, fecha ≥ hoy) y registra la propuesta en `propuestas_grupales` con estado ABIERTA.
4. El sistema **reparte la cantidad** entre los agricultores del lote en proporción a los kilos disponibles de cada uno, redondeando hacia abajo a unidades enteras de su publicación.
5. Para cada agricultor, el sistema crea, a través del módulo `venta`, una venta **PROPUESTA** vinculada a la propuesta conjunta, en la unidad de su publicación y con el precio por unidad equivalente al precio por kilo ofrecido, en la conversación de ese comprador con esa publicación (se crea con `canal_inicio` = PROPUESTA_CONJUNTA si no existía). `trg_transacciones_bi` verifica que la propuesta esté ABIERTA, que coincidan comprador, variedad y distrito, y que la cantidad no supere lo disponible.
6. El sistema notifica a cada agricultor (PROPUESTA_CONJUNTA).
7. Cada agricultor revisa su parte y la **acepta** (ACORDADA, con bloqueo de la publicación como en UC-05) o la **rechaza** (RECHAZADA); solo el agricultor puede hacerlo (`trg_transacciones_bu`).
8. El comprador ve el avance: kilos acordados frente a la cantidad pedida y el estado de cada parte.
9. Cuando lo decide, el comprador cierra la propuesta (CERRADA); el sistema cancela las partes que sigan en PROPUESTA.
10. Cada venta ACORDADA sigue su ciclo normal: entrega (UC-06), calificación (UC-07) e historial (UC-08).

**Flujos alternos:**

**A1 — Publicación incompatible:**
- El comprador, la variedad o el distrito no coincide: el sistema responde 422 ("La publicacion no pertenece al lote de la propuesta (comprador, variedad o zona)").

**A2 — Vencimiento sin respuesta:**
- Pasada la fecha de recojo, `ev_tareas_diarias` cancela las partes que sigan en PROPUESTA y cierra la propuesta (CERRADA). Las ya ACORDADA continúan.

**A3 — Cancelación por el comprador:**
- El comprador cancela la propuesta (CANCELADA); el sistema cancela las partes en PROPUESTA y notifica a los agricultores. Las ACORDADA se cancelan individualmente si corresponde (UC-05 A1).

**A4 — Propuesta no ABIERTA:**
- Se intenta crear una parte para una propuesta CERRADA o CANCELADA: 422 ("La propuesta conjunta no esta ABIERTA").

**A5 — Parte mayor a lo disponible:**
- Al aceptar, los kilos ya no están disponibles (otra venta los tomó): 422 ("La cantidad supera lo disponible en la publicacion").

**A6 — Reparto menor a una unidad:**
- Si a un agricultor le corresponde menos de una unidad entera, no recibe propuesta.

**Postcondición:**
- Propuesta conjunta CERRADA o CANCELADA, con N ventas vinculadas (una por agricultor) en su estado final o en curso.

**Requisito:** RF-10  
**Módulo:** `ventaagrupada` (ventas: `venta`)  
**Tablas:** propuestas_grupales, conversaciones, transacciones, publicaciones, `v_lotes_agrupados`, notificaciones

---

## Matriz de trazabilidad: Casos de uso → RF → módulo → BPMN

| UC | Nombre | RF | Módulo | Actores | Actividades BPMN (TO-BE) |
|---|---|---|---|---|---|
| UC-01 | Publicar cosecha | RF-01 | publicacion | Agricultor | A1 (publica), S1 (valida), gwVal (decisión) |
| UC-02 | Consultar precios | RF-02 | precio | Agricultor, Comprador, Administrador | S2 (consulta precios), nInt1 |
| UC-03 | Buscar productos | RF-03 | publicacion | Comprador | C1 (busca/filtra), S3 (publica en catálogo) |
| UC-04 | Contactar y negociar | RF-04 | mensajeria | Comprador, Agricultor | C2 (negocia), S4 (registra oferta), nInt2 |
| UC-05 | Registrar el acuerdo | RF-06 | venta | Agricultor, Comprador | A2/gwOf (revisa la oferta), A3 (confirma), S6 (registra transacción), nInt3 |
| UC-06 | Entrega y recepción | RF-11 (A1: RF-06) | venta | Agricultor, Comprador | A4 (entrega), C3 (confirma conformidad) |
| UC-07 | Calificar | RF-05 | venta | Agricultor, Comprador | A5 / C4 (califican), S7 (actualiza reputación) |
| UC-08 | Ver historial | RF-06 | venta | Agricultor, Comprador | nInt3 (historial) |
| UC-09 | Alertas de vencimiento | RF-07 | notificacion, publicacion | Agricultor | evTimer (evento), S5 (alerta), nInt5 |
| UC-10 | Registro, sesión y contraseña | RF-08 | usuario | Agricultor, Comprador, Administrador | — (previo al proceso) |
| UC-11 | Gestionar plataforma | RF-09, RF-08, RF-02 | administracion | Administrador | D1 (modera), D2 (gestiona), evStartAdm (reportes) |
| UC-12 | Venta agrupada | RF-10 | ventaagrupada | Comprador, Agricultores | — (no modelado en el BPMN) |

**Cobertura:** cada RF tiene al menos un caso de uso. RF-06 se cubre con UC-05 (registro), UC-06 A1 (recojo en chacra) y UC-08 (historial).

**Pendiente en el BPMN TO-BE:** aún muestra la aprobación previa (gwMod, S9, "Publicación no aprobada") y una alerta a las 48 h, y no incluye la venta agrupada, los reclamos ni el recojo en chacra. Debe actualizarse según este documento.

---

## Notas de implementación para Claude Code

1. **UC-01** dispara `trg_publicaciones_bi`; refrescar la entidad tras guardar para obtener `fecha_limite`, `peso_unidad_kg`, `marca_revision` y las columnas generadas `cantidad_kg` y `precio_kg`. Las marcas PRECIO_FUERA_RANGO y CANTIDAD_INUSUAL las pone el service (umbrales del slice de RF-09). Los catálogos se consultan a través de `catalogo.aplicacion`.
2. **UC-02** consulta `v_precios_vigentes` y `v_rango_precio_plataforma` (vistas normales, se recalculan en cada consulta); la carga dispara `trg_precios_mercado_bi`.
3. **UC-03** usa `v_publicaciones_disponibles` con JPA Specifications y paginado de 20 (`@PageableDefault(size = 20)`).
4. **UC-04** dispara `trg_conversaciones_bi` y `trg_mensajes_bi`; el service valida participante y que las ofertas sean sobre una publicación PUBLICADA.
5. **UC-05** dispara `trg_transacciones_bi` y `trg_transacciones_ai` (y `trg_transacciones_bu`/`_au` al cancelar); bloqueo pesimista antes de ACORDADA; `id_actualizado_por` en cada insert y update; refrescar la entidad tras guardar (`peso_unidad_kg`, `cantidad_kg`, `monto_total`).
6. **UC-06** dispara `trg_transacciones_bu` y `trg_transacciones_au`; "Confirmar que recibí" hace dos updates (ENTREGADO y COMPLETADA) en un mismo método `@Transactional`, tanto desde EN_CAMINO como desde ACORDADA (recojo en chacra). La aplicación nunca inserta en `transacciones_historial`.
7. **UC-07** dispara `trg_calificaciones_bi`; la UK venta–evaluador produce 409.
8. **UC-08** lee `v_historial_transacciones` y la línea de tiempo de `transacciones_historial`; exportación con Apache POI.
9. **UC-09** alimentado por `ev_tareas_diarias` — no usar `@Scheduled`; la reactivación la hace `trg_publicaciones_bu` al cambiar solo `fecha_limite`.
10. **UC-10** dispara `trg_agricultores_bi` o `trg_compradores_bi`; usuario y subtipo en la misma transacción.
11. **UC-11** dispara `trg_revisiones_bi`/`trg_revisiones_ai`, `trg_reclamos_*` y `trg_suspensiones_*` (y `trg_precios_mercado_bi` en la carga de precios); los triggers actualizan el estado de la publicación y del usuario y crean la notificación MODERACION; el service no lo repite.
12. **UC-12** reparte la cantidad en el service (proporcional a los kilos disponibles, hacia abajo a unidades enteras) y crea las ventas PROPUESTA a través de `venta.aplicacion` (dispara `trg_transacciones_bi` con `id_propuesta_grupal`); el cierre por vencimiento lo hace `ev_tareas_diarias`.
13. **Notificaciones** (MENSAJE, OFERTA, VENTA, PROPUESTA_CONJUNTA, CALIFICACION, RECLAMO, CUENTA): el módulo que ejecuta la acción llama a `notificacion.aplicacion` dentro de la misma transacción. POR_VENCER y MODERACION las crea la base de datos.

---

**Documento preparado para el equipo de desarrollo y Claude Code.**  
**Próxima revisión:** después de actualizar el BPMN TO-BE (APF2).
