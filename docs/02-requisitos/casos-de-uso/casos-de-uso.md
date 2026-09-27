# Casos de Uso — AgroConecta

**Documento:** Casos de uso del sistema  
**Versión:** 1.3  
**Fecha:** 27 de septiembre de 2026  
**Fuente:** BPMN TO-BE (`docs/03-procesos/AgroConecta_TO-BE.drawio`), SRS v1.2 y `docs/04-diseno/base-datos/modelo-bd.sql`  
**Alcance:** RF-01 a RF-11 — 12 casos de uso (UC-01 a UC-12)

**Actores:** Agricultor, Comprador y Administrador. El **visitante sin sesión** no es actor (solo consulta publicaciones, precios y catálogo) y el **transportista** no es usuario ni se registra en la base de datos. "Sistema" indica la respuesta de la plataforma.

**Cambios v1.1:** moderación posterior; transacción que inicia como PROPUESTA; transportista como dato; historial exportable a Excel; inicio de sesión con teléfono; nuevo UC-12 Venta agrupada; estados alineados con `modelo-bd.sql`.

**Cambios v1.2:** módulos alineados con la arquitectura modular (SRS v1.2 §5); UC-05, UC-06 A1 y UC-08 trazados a RF-06 "Registro e historial de transacciones"; notificaciones en la aplicación creadas por el módulo `notificaciones`; cambio de contraseña propio (UC-10); verificación del estado del usuario en cada petición (UC-11); UC-06 A4 corregido (los eventos de entrega no se corrigen con eventos nuevos); rutas de `docs/` actualizadas.

**Cambios v1.3:** referencias a tablas, vistas, estados y triggers alineadas con el modelo de datos v4 (`docs/04-diseno/base-datos/cambios-v4.md`). Los puntos marcados con ⚠ describen comportamiento de v3 que el esquema v4 ya no admite y quedan pendientes de revisión.

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

**Notificaciones:** todas se muestran dentro de la aplicación (sin correo, SMS ni push). Las de tipo POR_VENCER las crea el evento `ev_tareas_diarias` y las de revisión de publicaciones (PUBLICACION_APROBADA, PUBLICACION_RECHAZADA) el trigger `trg_revisiones_ai`; las demás las crea el módulo que ejecuta la acción, a través de la `api` del módulo `notificaciones` y en la misma transacción.

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
   - Paso 1: cultivo y variedad (por defecto "Común"), cantidad y unidad de medida (con el peso de cada jaba o saco).
   - Paso 2: precio por unidad (`precio_unitario`, con el precio de referencia de UC-02 como ayuda) y distrito.
   - Paso 3: fecha de cosecha y fecha límite de venta (opcional).
3. El agricultor completa los pasos y confirma.
4. El sistema valida: cantidad > 0, precio > 0, fecha límite ≥ hoy y ≥ fecha de cosecha, usuario ACTIVO, variedad/unidad/distrito existentes.
5. El sistema registra la publicación con estado EN_REVISION. `trg_publicaciones_bi` toma el peso del catálogo para kilo, quintal y tonelada y, si no se indicó fecha límite, la calcula como `fecha_cosecha + vida_util_dias` del cultivo.
6. La publicación aparece en la búsqueda (UC-03) cuando un administrador la aprueba y pasa a PUBLICADA (UC-11, Parte 2).
7. El sistema muestra "Publicación creada" con la fecha límite resultante.

**Flujos alternos:**

**A1 — Validación falla:**
- En el paso 4 el sistema detecta un error (p. ej. cantidad inválida, usuario SUSPENDIDO).
- El sistema muestra el mensaje junto al campo (400/422) y el agricultor corrige desde el paso correspondiente.

**A2 — Cultivo sin vida útil registrada:**
- ⚠ En v4 `cultivos.vida_util_dias` es obligatorio, por lo que este flujo ya no ocurre. Con jaba o saco sin peso, `trg_publicaciones_bi` responde "Indique cuantos kilos pesa cada jaba o saco".

**A3 — Agricultor cancela:**
- El agricultor abandona el formulario; no se guarda nada.

**A4 — Revisión del administrador:**
- La publicación queda EN_REVISION hasta que un administrador la aprueba o la rechaza (UC-11, Parte 2); `trg_revisiones_ai` crea la notificación PUBLICACION_APROBADA o PUBLICACION_RECHAZADA.

**Postcondición:**
- Flujo básico: publicación EN_REVISION; visible en la búsqueda cuando pasa a PUBLICADA.
- A3: la publicación no se crea.

**Requisito:** RF-01  
**Módulo:** `publicaciones` (catálogos: `catalogo`)  
**Tablas:** publicaciones, variedades, cultivos, unidades_medida, distritos

---

## UC-02: Consultar precios de referencia

**Actores:**
- Agricultor (principal, al publicar)
- Comprador (principal, al buscar)
- Administrador (principal en A2, carga datos)
- También accesible para el visitante sin sesión

**Precondición:**
- Existen precios cargados o transacciones COMPLETADA.

**Flujo básico:**
1. El usuario entra a "Precios" (o los ve dentro de UC-01).
2. Elige cultivo (obligatorio) y, opcionalmente, zona de precio (distrito) y período (por defecto, últimos 30 días).
3. El sistema consulta `v_precios_vigentes` (precio del día) y `precios_mercado` (historial) con esos filtros.
4. El sistema muestra el precio por kilo del mercado real y su equivalente en la unidad local, con su fuente (MERCADO_MAYORISTA_PIURA, MERCADO_SULLANA o ENCUESTA_ACOPIADORES), y el rango de la plataforma: mínimo, máximo y promedio por kilo de las ventas COMPLETADA de los últimos 30 días (`v_rango_precio_plataforma`).
5. El sistema muestra la tendencia frente a la carga anterior (`v_precios_vigentes.tendencia`: SUBE, BAJA, IGUAL o SIN_DATO).

**Flujos alternos:**

**A1 — Sin datos de precio:**
- El cultivo no tiene precios en el período: el sistema muestra "No hay precios de referencia para este cultivo".

**A2 — Carga de precios por el administrador:**
- El administrador entra a "Cargar precios" y elige:
  - Formulario: cultivo, zona de precio (distrito), fecha, precio por kilo y fuente.
  - Archivo Excel/CSV con cabecera `cultivo, zona, fecha, precio_kg`, más la fuente.
- El sistema valida: cultivo y distrito existen y el distrito es zona de precio; precio por kilo > 0; no existe ya un precio para ese cultivo, zona y fecha.
- El sistema registra las filas válidas en `precios_mercado` con `metodo` FORMULARIO o ARCHIVO y el administrador que carga. `trg_precios_mercado_bi` confirma que el usuario es ADMINISTRADOR y que el distrito es zona de precio.
- Con archivo, las filas inválidas se listan con su motivo y el administrador puede reintentar solo esas.

**A3 — Filtro sin resultados:**
- El sistema muestra "Sin resultados" y sugiere quitar la zona o ampliar el período.

**Postcondición:**
- El usuario ve precios históricos y tendencia.
- A2: carga registrada con administrador, fuente y fecha.

**Requisito:** RF-02  
**Módulo:** `precios` (catálogos: `catalogo`)  
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
2. El sistema presenta filtros opcionales: cultivo, variedad, provincia/distrito, unidad y fecha de cosecha desde/hasta.
3. El usuario elige criterios y pulsa "Buscar".
4. El sistema consulta `v_publicaciones_disponibles` con:
   - `estado = PUBLICADA`
   - `disponible_kg > 0`
   - `fecha_limite >= hoy`
   - los filtros elegidos
5. El sistema devuelve resultados paginados (20 por página), ordenados por fecha límite más próxima.
6. Cada resultado muestra: cultivo y variedad, cantidad disponible y unidad, precio por unidad y por kilo, provincia y distrito, fecha límite con los días restantes (`dias_restantes`, `por_vencer`) y la reputación del agricultor (`calificacion_agricultor`, de `v_reputacion_usuario`).
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
**Módulo:** `publicaciones` (filtros: `catalogo`; reputación: `calificaciones`)  
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
4. El comprador escribe su mensaje o envía una oferta (`tipo` = OFERTA, con cantidad y precio por unidad de la publicación).
5. El sistema registra el mensaje con remitente y fecha; `trg_mensajes_bi` confirma que el remitente participa.
6. El sistema crea una notificación MENSAJE (u OFERTA, si es una oferta) para el agricultor.
7. El agricultor ve la notificación (el frontend consulta periódicamente) y abre el chat.
8. El agricultor responde; la conversación sigue hasta acordar cantidad y precio.
9. Cuando hay acuerdo, cualquiera de las dos partes pulsa "Registrar acuerdo" → UC-05.

**Flujos alternos:**

**A1 — Contacto por WhatsApp:**
- El comprador pulsa "Contactar por WhatsApp".
- El sistema registra el interés en la conversación (la crea con `canal_inicio` = WHATSAPP si no existía e incrementa `clics_whatsapp`) y abre el enlace `wa.me` con el celular del agricultor y un texto sugerido.
- La negociación continúa fuera de la plataforma; el acuerdo puede registrarse igualmente en UC-05.

**A2 — Sin acuerdo:**
- Las partes no llegan a un acuerdo; la conversación queda sin acuerdo registrado.

**A3 — Cierre de la conversación:**
- ⚠ En v4 `conversaciones` no tiene estado (ya no existen ABIERTA ni CERRADA). Pendiente de revisión (ver `notas-integridad-bd.md` §9).

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

## UC-05: Proponer y acordar una transacción

**Actores:**
- Agricultor o Comprador (principal: quien propone)
- La otra parte (principal: quien acepta o rechaza)

**Precondición:**
- Existe una conversación entre las partes (UC-04) o un contacto por WhatsApp.
- La publicación está PUBLICADA y ambos usuarios están ACTIVO.

**Flujo básico:**
1. Una de las partes pulsa "Registrar acuerdo" e ingresa unidad, cantidad, precio por unidad y día de recojo (y el peso por unidad si es jaba o saco).
2. El sistema valida: cantidad > 0 y ≤ cantidad disponible (en kg), precio > 0, publicación PUBLICADA.
3. El sistema bloquea la publicación (`PESSIMISTIC_WRITE`) y crea la transacción en estado **ACORDADA** con `id_actualizado_por` = quien registra; `trg_transacciones_bi` repite las validaciones y `trg_transacciones_ai` registra el estado en `transacciones_historial`.
4. ⚠ En v4 el acuerdo del chat nace ACORDADA: ya no hay paso de "Aceptar" por la otra parte (PROPUESTA es solo para la venta agrupada, UC-12).
5. Si con este acuerdo se agota la cantidad, `sp_sincronizar_publicacion` (llamado por `trg_transacciones_ai`) pasa la publicación a VENDIDA.
6. El sistema notifica a la otra parte (VENTA) con los datos de contacto de la contraparte.
7. Continúa en UC-06.

**Flujos alternos:**

**A1 — Rechazo:**
- Solo aplica a las propuestas de venta agrupada (UC-12): el agricultor pasa la PROPUESTA a RECHAZADA.

**A2 — Cancelación:**
- Cualquiera de las partes cancela una transacción ACORDADA: estado CANCELADA (`id_actualizado_por` = quien cancela). La contraparte recibe una notificación VENTA.
- Si la publicación estaba VENDIDA, `sp_sincronizar_publicacion` (llamado por `trg_transacciones_au`) la devuelve a PUBLICADA (o VENCIDA si ya pasó su fecha límite).

**A3 — Cantidad no disponible o publicación no PUBLICADA:**
- El sistema responde 422 con el mensaje del trigger (p. ej. "La cantidad supera lo disponible en la publicacion").

**A4 — Cambio de términos:**
- Los términos de una transacción (unidad, cantidad, peso y precio) no se modifican: `trg_transacciones_bu` responde "Los terminos no se modifican; cancele y registre un nuevo acuerdo".

**Postcondición:**
- Transacción ACORDADA o CANCELADA.
- Publicación PUBLICADA o VENDIDA según la cantidad restante.

**Requisito:** RF-06 (registro de transacciones), a partir de la negociación de RF-04  
**Módulo:** `transacciones`  
**Tablas:** transacciones, transacciones_historial, conversaciones, publicaciones, notificaciones

---

## UC-06: Registrar entrega y completar la transacción

**Actores:**
- Agricultor (principal)
- Comprador (principal: confirma la recepción)

**Precondición:**
- La transacción está ACORDADA.

**Flujo básico:**
1. Al recoger la carga, el agricultor o el comprador marca **RECOGIDO**.
2. Al salir hacia el destino, el agricultor o el comprador marca **EN_CAMINO**.
3. El comprador revisa la carga y pulsa "Confirmar que recibí".
4. El sistema, en una sola transacción (`@Transactional`), pasa la transacción a **ENTREGADO** y luego a **COMPLETADA**, con `id_actualizado_por` = comprador.
5. En cada cambio, `trg_transacciones_bu` valida la secuencia y quién lo hace, y `trg_transacciones_au` registra el paso en `transacciones_historial` (línea de tiempo con fecha y hora). El sistema notifica a la otra parte (VENTA).
6. El sistema notifica a ambos que pueden calificar → UC-07.

**Flujos alternos:**

**A1 — Sin transporte (RF-06):**
- Las partes coordinan directamente (retiro en finca o entrega del propio agricultor).
- ⚠ En v4 `trg_transacciones_bu` no permite pasar de ACORDADA a COMPLETADA: toda venta pasa por RECOGIDO, EN_CAMINO y ENTREGADO. Pendiente de revisión.

**A2 — Discrepancia en la entrega:**
- El comprador no confirma la recepción y lo indica por la mensajería (UC-04).
- La transacción sigue en su estado hasta que se resuelva: el comprador confirma o, si sigue ACORDADA, una de las partes cancela (UC-05 A2). Desde RECOGIDO ya no se puede cancelar (`trg_transacciones_bu`); la parte afectada puede registrar un reclamo (`reclamos`).

**A3 — Cambio fuera de secuencia:**
- Se intenta marcar EN_CAMINO sin RECOGIDO (o ENTREGADO sin EN_CAMINO), o el agricultor intenta confirmar la recepción.
- El sistema responde 422: "Cambio de estado no permitido para este usuario".

**A4 — Estado registrado por error:**
- Los estados no retroceden y `transacciones_historial` no se edita ni se elimina; no existen registros de corrección.
- Las partes aclaran el error por la mensajería (UC-04); el registro se conserva para auditoría. Si el error impide continuar, una de las partes cancela la transacción (UC-05 A2).

**Postcondición:**
- Transacción COMPLETADA con su línea de tiempo en `transacciones_historial`.
- Ambas partes pueden calificar.

**Requisito:** RF-11 (A1: RF-06)  
**Módulo:** `transacciones`  
**Tablas:** transacciones, transacciones_historial, notificaciones

---

## UC-07: Calificar transacción

**Actores:**
- Agricultor (califica al comprador)
- Comprador (califica al agricultor)

**Precondición:**
- La transacción está COMPLETADA.
- El usuario es el comprador o el agricultor de la transacción y aún no la calificó.

**Flujo básico:**
1. El usuario abre la transacción en su historial (UC-08) y pulsa "Calificar".
2. El sistema muestra el formulario: puntuación de 1 a 5 estrellas y comentario opcional (máx. 500 caracteres).
3. El usuario completa y pulsa "Enviar calificación".
4. El sistema valida: puntuación 1–5, usuario participante, transacción COMPLETADA, sin calificación previa del mismo usuario.
5. El sistema registra la calificación; `trg_calificaciones_bi` repite las validaciones de estado y participante.
6. El sistema crea una notificación CALIFICACION para la contraparte.
7. La reputación se refleja al consultar `v_reputacion_usuario` (no se almacena).

**Flujos alternos:**

**A1 — Transacción no completada:**
- El sistema responde 422: "Solo se califican transacciones COMPLETADAS".

**A2 — Ya calificada:**
- El sistema responde 409 (clave única transacción–evaluador): "Ya calificaste esta transacción". Las calificaciones no se editan.

**A3 — Comentario inapropiado:**
- Si el administrador detecta un comentario ofensivo, puede suspender al autor (`suspensiones`, UC-11). ⚠ En v4 no existe la advertencia.

**Postcondición:**
- Calificación registrada; la reputación de la contraparte se actualiza en la vista.

**Requisito:** RF-05  
**Módulo:** `calificaciones`  
**Tablas:** calificaciones, transacciones, `v_reputacion_usuario`, notificaciones

---

## UC-08: Ver historial de transacciones

**Actores:**
- Agricultor (principal)
- Comprador (principal)

**Precondición:**
- El usuario tiene sesión iniciada.

**Flujo básico:**
1. El usuario entra a "Mis transacciones".
2. El sistema consulta `v_historial_transacciones` donde el usuario es `id_comprador` o `id_agricultor`.
3. El usuario puede filtrar por estado (PROPUESTA, ACORDADA, RECOGIDO, EN_CAMINO, ENTREGADO, COMPLETADA, RECHAZADA, CANCELADA), rango de fechas y contraparte.
4. El sistema devuelve la lista paginada (20 por página).
5. Cada fila muestra: cultivo y variedad, cantidad y unidad, kilos, precio unitario y por kilo, monto total, contraparte, estado, fecha de recojo y fechas de creación y última actualización.
6. Al abrir una transacción, el sistema muestra:
   - La línea de tiempo de `transacciones_historial` (cada estado con fecha, hora y autor).
   - Las calificaciones emitidas y recibidas.
   - Un enlace a la conversación de la publicación.
7. Opción "Exportar a Excel": el sistema genera un `.xlsx` (Apache POI) con las transacciones del filtro actual.

**Flujos alternos:**

**A1 — Sin transacciones:**
- El sistema muestra "Aún no tienes transacciones" y un enlace a "Buscar productos" (UC-03) o "Publicar cosecha" (UC-01).

**A2 — Filtro sin resultados:**
- El sistema muestra "Sin resultados para este filtro".

**A3 — Exportación a Excel:**
- El archivo se descarga como `AgroConecta_Historial_{fecha}.xlsx`, con una fila por transacción y las mismas columnas del listado.

**Postcondición:**
- El usuario consulta su historial y puede exportarlo.

**Requisito:** RF-06 (historial)  
**Módulo:** `transacciones` (calificaciones: `calificaciones`)  
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
   - **Bajar el precio:** actualiza `precio_unitario`.
   - **Extender la fecha:** elige +7, +14 o +30 días; el sistema valida que la nueva fecha sea ≥ hoy y actualiza `fecha_limite`.
   - **Retirar:** estado RETIRADA (no se venderá).
   - **No hacer nada:** la publicación sigue PUBLICADA.
4. **Vencimiento automático:** al día siguiente de la fecha límite, el evento pasa la publicación a VENCIDA; deja de aparecer en la búsqueda.
5. **Reactivación:** el agricultor extiende la fecha de una publicación VENCIDA (actualizando solo `fecha_limite`); `trg_publicaciones_bu` la devuelve a PUBLICADA.

**Flujos alternos:**

**A1 — Publicación no PUBLICADA:**
- Si está EN_REVISION, RECHAZADA, VENDIDA, VENCIDA o RETIRADA, el evento no genera alertas.

**A2 — Varias alertas el mismo día:**
- El centro de notificaciones las muestra juntas, ordenadas por fecha; cada una lleva a su publicación.

**Postcondición:**
- Publicación con precio ajustado, fecha extendida, RETIRADA, o VENCIDA según el caso.

**Requisito:** RF-07  
**Módulo:** `notificaciones` (centro de notificaciones) y `publicaciones` (acciones rápidas)  
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
   - **Común:** nombre completo, celular (obligatorio, 9 dígitos que empiezan en 9, será el usuario de acceso), distrito (catálogo) y contraseña (mín. 8 caracteres, con letras y números).
   - **Agricultor:** nombre de la chacra (opcional).
   - **Comprador:** tipo de comprador (MERCADO_MAYORISTA, RESTAURANTE, HOTEL, PROCESADORA, MOLINO, OTRO), nombre del negocio y RUC (opcional, 11 dígitos).
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
**Módulo:** `usuarios` (distritos: `catalogo`)  
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
   - Ver el detalle: rol, fecha de registro, último acceso, reputación, operaciones completadas, reclamos fundados y suspensiones.
   - Editar datos básicos.
   - **Reiniciar la contraseña:** asigna una contraseña temporal (BCrypt) y la comunica al usuario por teléfono.
   - Crear otro administrador (UC-10 A5).
   - Desactivar la cuenta (INACTIVO).
4. Ante una conducta inapropiada, pulsa "Suspender", elige el motivo (RECLAMOS_FUNDADOS, DATOS_FALSOS, PUBLICACIONES_ENGANOSAS, USO_INDEBIDO), escribe el detalle y confirma:
   - El sistema registra la suspensión en `suspensiones`; `trg_suspensiones_bi` verifica que quien suspende es ADMINISTRADOR, que el afectado no lo es, que no tenga otra suspensión abierta y, con RECLAMOS_FUNDADOS, que tenga al menos 3 reclamos fundados.
   - `trg_suspensiones_ai` pasa el usuario a SUSPENDIDO; pierde el acceso en su siguiente petición (el validador del token verifica el estado del usuario en cada petición).
   - El usuario recibe una notificación CUENTA.
5. "Reactivar" registra `fecha_fin` en la suspensión y `trg_suspensiones_au` devuelve el usuario a ACTIVO. ⚠ En v4 no existe la advertencia (ADVERTENCIA).

**Parte 2 — Moderación de publicaciones (cola EN_REVISION):**
1. El administrador revisa la cola de publicaciones EN_REVISION, con la marca del sistema (`marca_revision`: PRIMERA_PUBLICACION, PRECIO_FUERA_RANGO, CANTIDAD_INUSUAL).
2. Revisa los datos de la publicación, los del agricultor y sus revisiones anteriores (`revisiones_publicacion`).
3. Pulsa "Aprobar" o "Rechazar" (con motivo y mensaje para el agricultor):
   - El sistema registra la decisión en `revisiones_publicacion`; `trg_revisiones_bi` verifica que quien revisa es ADMINISTRADOR y que la publicación está EN_REVISION.
   - `trg_revisiones_ai` pasa la publicación a PUBLICADA o RECHAZADA y crea la notificación PUBLICACION_APROBADA o PUBLICACION_RECHAZADA.
4. Una publicación RECHAZADA vuelve a EN_REVISION cuando el agricultor la corrige. ⚠ En v4 no existen BLOQUEADA ni el desbloqueo.

**Parte 3 — Reportes y métricas:**
1. El administrador entra a "Reportes".
2. El sistema muestra:
   - Usuarios activos por rol (con `ultimo_acceso` en los últimos 30 días).
   - Transacciones COMPLETADA por mes y cultivo: cantidad, kilos y monto (`v_ventas_mensuales_cultivo`); tarjetas del panel (`v_panel_admin`).
   - Cultivos más publicados (top 5, últimos 30 días).
   - Precio por cultivo y zona (`v_precios_vigentes`).
   - Embudo de conversión por publicación (`v_conversion_publicacion`).
   - Usuarios con mejor y peor reputación (`v_reputacion_usuario`).
3. Puede filtrar por fecha, provincia y cultivo.
4. "Exportar a Excel" descarga el reporte (Apache POI).

**Parte 4 — Carga de precios de referencia:**
- Ver UC-02 A2.

**Flujos alternos:**

**A1 — Suspensión con transacciones en curso:**
- Si el usuario tiene transacciones PROPUESTA o ACORDADA, el sistema lo advierte.
- El administrador confirma; las transacciones no cambian de estado automáticamente. Las contrapartes reciben una notificación y pueden cancelarlas (UC-05 A2).

**A2 — Reconsideración:**
- El usuario suspendido se comunica con la plataforma por teléfono o WhatsApp; el administrador decide si lo reactiva (registra `fecha_fin`).

**A3 — Motivo vacío:**
- El sistema no registra la suspensión sin motivo y detalle, ni el rechazo de una publicación sin motivo y mensaje (400).

**Postcondición:**
- Usuarios y publicaciones moderados con su registro en `suspensiones` y `revisiones_publicacion`.
- Reportes consultados o exportados.

**Requisito:** RF-09 (panel administrativo), RF-08 (gestión de usuarios), RF-02 (carga de precios)  
**Módulo:** `administracion` (usa la `api` de `usuarios`, `publicaciones`, `transacciones`, `calificaciones`, `precios` y `notificaciones`)  
**Tablas:** usuarios, publicaciones, revisiones_publicacion, suspensiones, reclamos, transacciones, notificaciones, `v_resumen_usuario`, `v_panel_admin`, `v_ventas_mensuales_cultivo`, `v_conversion_publicacion`, `v_reputacion_usuario`, `v_precios_vigentes`

---

## UC-12: Venta agrupada

**Actores:**
- Comprador (principal: crea la propuesta y acepta aportes)
- Agricultores (principales: aportan)

**Precondición:**
- El comprador y los agricultores tienen sesión iniciada y están ACTIVO.

**Flujo básico:**
1. El comprador entra a "Venta agrupada" y consulta `v_lotes_agrupados` para ver los lotes con volumen disponible (mismo cultivo, variedad y zona; 2 o más agricultores), en kg y con precio promedio por kilo.
2. Pulsa "Crear propuesta conjunta" e ingresa: variedad (y con ella el cultivo), distrito, cantidad total en kg, precio por kilo y fecha de recojo (también es el límite para responder).
3. El sistema valida (cantidad > 0, precio > 0, fecha ≥ hoy) y registra la propuesta en `propuestas_grupales` con estado ABIERTA.
4. El sistema crea, a través del módulo `transacciones`, una transacción **PROPUESTA** para cada agricultor del lote, vinculada a la propuesta y expresada en la unidad de su publicación (en una conversación con `canal_inicio` = PROPUESTA_CONJUNTA). `trg_transacciones_bi` verifica que la propuesta esté ABIERTA, que coincidan comprador, variedad y zona, y que la cantidad no supere lo disponible. ⚠ El reparto de la cantidad entre agricultores está pendiente de definir.
5. El sistema notifica (PROPUESTA_CONJUNTA) a cada agricultor.
6. Cada agricultor revisa su propuesta y la acepta (ACORDADA, con bloqueo de la publicación como en UC-05) o la rechaza (RECHAZADA); solo el agricultor puede hacerlo (`trg_transacciones_bu`).
7. El comprador ve el avance: kilos acordados frente a la cantidad requerida y el estado de cada propuesta.
8. ⚠ En v4 el aporte ya no lo inicia el agricultor ni lo acepta el comprador: la propuesta la inicia el comprador y la acepta cada agricultor.
9. Cuando reúne la cantidad deseada, el comprador cierra la propuesta (CERRADA); el sistema cancela las transacciones que sigan en PROPUESTA.
10. Cada transacción ACORDADA sigue su ciclo normal: entrega (UC-06), recepción, calificación (UC-07) e historial (UC-08).

**Flujos alternos:**

**A1 — Publicación incompatible:**
- El comprador, la variedad o la zona no coincide: el sistema responde 422 ("La publicacion no pertenece al lote de la propuesta (comprador, variedad o zona)").

**A2 — Vencimiento sin cierre:**
- Pasada la fecha de recojo, `ev_tareas_diarias` cancela las transacciones en PROPUESTA y cierra la propuesta (CERRADA). Las ya ACORDADA continúan.

**A3 — Cancelación por el comprador:**
- El comprador cancela la propuesta (CANCELADA); el sistema cancela las transacciones en PROPUESTA y notifica a los agricultores. Las ACORDADA se cancelan individualmente si corresponde (UC-05 A2).

**A4 — Propuesta no ABIERTA:**
- Se intenta crear una transacción para una propuesta CERRADA o CANCELADA: 422 ("La propuesta conjunta no esta ABIERTA").

**A5 — Aporte mayor a lo disponible:**
- 422 ("La cantidad supera lo disponible en la publicacion").

**Postcondición:**
- Propuesta conjunta CERRADA o CANCELADA, con N transacciones vinculadas (una por agricultor) en su estado final o en curso.

**Requisito:** RF-10  
**Módulo:** `ventaagrupada` (aportes: `transacciones`)  
**Tablas:** propuestas_grupales, conversaciones, transacciones, publicaciones, `v_lotes_agrupados`, notificaciones

---

## Matriz de trazabilidad: Casos de uso → RF → módulo → BPMN

| UC | Nombre | RF | Módulo | Actores | Actividades BPMN (TO-BE) |
|---|---|---|---|---|---|
| UC-01 | Publicar cosecha | RF-01 | publicaciones | Agricultor | A1 (publica), S1 (valida), gwVal (decisión) |
| UC-02 | Consultar precios | RF-02 | precios | Agricultor, Comprador, Administrador | S2 (consulta precios), nInt1 |
| UC-03 | Buscar productos | RF-03 | publicaciones | Comprador | C1 (busca/filtra), S3 (publica en catálogo) |
| UC-04 | Contactar y negociar | RF-04 | mensajeria | Comprador, Agricultor | C2 (negocia), S4 (registra oferta), nInt2 |
| UC-05 | Proponer y acordar | RF-06 | transacciones | Agricultor, Comprador | A2/gwOf (revisa y acepta), A3 (confirma), S6 (registra transacción), nInt3 |
| UC-06 | Entrega y recepción | RF-11 (A1: RF-06) | transacciones | Agricultor, Comprador | A4 (entrega), C3 (confirma conformidad) |
| UC-07 | Calificar | RF-05 | calificaciones | Agricultor, Comprador | A5 / C4 (califican), S7 (actualiza reputación) |
| UC-08 | Ver historial | RF-06 | transacciones | Agricultor, Comprador | nInt3 (historial) |
| UC-09 | Alertas de vencimiento | RF-07 | notificaciones, publicaciones | Agricultor | evTimer (evento), S5 (alerta), nInt5 |
| UC-10 | Registro, sesión y contraseña | RF-08 | usuarios | Agricultor, Comprador, Administrador | — (previo al proceso) |
| UC-11 | Gestionar plataforma | RF-09, RF-08, RF-02 | administracion | Administrador | D1 (modera), D2 (gestiona), evStartAdm (reportes) |
| UC-12 | Venta agrupada | RF-10 | ventaagrupada | Comprador, Agricultores | — (no modelado en el BPMN) |

**Cobertura:** cada RF tiene al menos un caso de uso. RF-06 se cubre con UC-05 (registro), UC-06 A1 (recepción sin transporte) y UC-08 (historial).

**Pendiente en el BPMN TO-BE (APF2):** aún muestra la moderación previa (gwMod, S9, "Publicación no aprobada") y una alerta a las 48 h, y no incluye la venta agrupada ni el seguimiento de entrega detallado. Debe actualizarse según este documento.

---

## Notas de implementación para Claude Code

1. **UC-01** dispara `trg_publicaciones_bi`; refrescar la entidad tras guardar para obtener `fecha_limite`, `peso_unidad_kg`, `marca_revision` y las columnas generadas `cantidad_kg` y `precio_kg`. Los catálogos se consultan a través de `catalogo::api`.
2. **UC-02** consulta `v_precios_vigentes` y `v_rango_precio_plataforma` (vistas normales, se recalculan en cada consulta); la carga dispara `trg_precios_mercado_bi`.
3. **UC-03** usa `v_publicaciones_disponibles` con JPA Specifications y paginado de 20 (`@PageableDefault(size = 20)`).
4. **UC-04** dispara `trg_conversaciones_bi` y `trg_mensajes_bi`; el service valida participante.
5. **UC-05** dispara `trg_transacciones_bi`, `trg_transacciones_ai`, `trg_transacciones_bu` y `trg_transacciones_au`; bloqueo pesimista antes de ACORDADA; `id_actualizado_por` en cada insert y update; refrescar la entidad tras guardar (`peso_unidad_kg`, `cantidad_kg`, `monto_total`).
6. **UC-06** dispara `trg_transacciones_bu` y `trg_transacciones_au`; "Confirmar que recibí" hace dos updates (ENTREGADO y COMPLETADA) en un mismo método `@Transactional`. La aplicación nunca inserta en `transacciones_historial`.
7. **UC-07** dispara `trg_calificaciones_bi`; la UK transacción–evaluador produce 409.
8. **UC-08** lee `v_historial_transacciones` y la línea de tiempo de `transacciones_historial`; exportación con Apache POI.
9. **UC-09** alimentado por `ev_tareas_diarias` — no usar `@Scheduled`; la reactivación la hace `trg_publicaciones_bu` al cambiar solo `fecha_limite`.
10. **UC-10** dispara `trg_agricultores_bi` o `trg_compradores_bi`; usuario y subtipo en la misma transacción.
11. **UC-11** dispara `trg_revisiones_bi`/`trg_revisiones_ai` y `trg_suspensiones_*` (y `trg_precios_mercado_bi` en la carga de precios); los triggers actualizan el estado de la publicación y del usuario, el service no lo repite.
12. **UC-12** crea las transacciones PROPUESTA a través de `transacciones::api` (dispara `trg_transacciones_bi` con `id_propuesta_grupal`); el cierre por vencimiento lo hace `ev_tareas_diarias`.
13. **Notificaciones** (MENSAJE, OFERTA, VENTA, PROPUESTA_CONJUNTA, CALIFICACION, RECLAMO, CUENTA): el módulo que ejecuta la acción llama a `notificaciones::api` dentro de la misma transacción. Usar solo los tipos definidos en `modelo-bd.sql`.

---

**Documento preparado para el equipo de desarrollo y Claude Code.**  
**Próxima revisión:** después de actualizar el BPMN TO-BE (APF2).
