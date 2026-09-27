# Casos de Uso — AgroConecta

**Documento:** Casos de uso del sistema  
**Versión:** 1.2  
**Fecha:** 26 de septiembre de 2026  
**Fuente:** BPMN TO-BE (`docs/03-procesos/AgroConecta_TO-BE.drawio`), SRS v1.2 y `docs/04-diseno/base-datos/modelo-bd.sql`  
**Alcance:** RF-01 a RF-11 — 12 casos de uso (UC-01 a UC-12)

**Actores:** Agricultor, Comprador y Administrador. El **visitante sin sesión** no es actor (solo consulta publicaciones, precios y catálogo) y el **transportista** es un registro de datos, no un usuario. "Sistema" indica la respuesta de la plataforma.

**Cambios v1.1:** moderación posterior; transacción que inicia como PROPUESTA; transportista como dato; historial exportable a Excel; inicio de sesión con teléfono; nuevo UC-12 Venta agrupada; estados alineados con `modelo-bd.sql`.

**Cambios v1.2:** módulos alineados con la arquitectura modular (SRS v1.2 §5); UC-05, UC-06 A1 y UC-08 trazados a RF-06 "Registro e historial de transacciones"; notificaciones en la aplicación creadas por el módulo `notificaciones`; cambio de contraseña propio (UC-10); verificación del estado del usuario en cada petición (UC-11); UC-06 A4 corregido (los eventos de entrega no se corrigen con eventos nuevos); rutas de `docs/` actualizadas.

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

**Notificaciones:** todas se muestran dentro de la aplicación (sin correo, SMS ni push). Las de tipo VENCIMIENTO las crea el evento `ev_tareas_diarias`; las demás las crea el módulo que ejecuta la acción, a través de la `api` del módulo `notificaciones` y en la misma transacción.

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
   - Paso 1: cultivo y variedad (por defecto "Común"), cantidad y unidad de medida.
   - Paso 2: precio referencial por unidad (con el precio de referencia de UC-02 como ayuda) y distrito.
   - Paso 3: fecha de disponibilidad, fecha límite de venta (opcional) y descripción (opcional).
3. El agricultor completa los pasos y confirma.
4. El sistema valida: cantidad > 0, precio > 0, fecha límite ≥ hoy y ≥ fecha de disponibilidad, usuario ACTIVO, variedad/unidad/distrito existentes.
5. El sistema registra la publicación con estado ACTIVA. Si no se indicó fecha límite, `trg_publicacion_bi` la calcula como `fecha_disponible + vida_util_dias` del cultivo.
6. La publicación aparece de inmediato en la búsqueda (UC-03).
7. El sistema muestra "Publicación creada" con la fecha límite resultante.

**Flujos alternos:**

**A1 — Validación falla:**
- En el paso 4 el sistema detecta un error (p. ej. cantidad inválida, usuario SUSPENDIDO).
- El sistema muestra el mensaje junto al campo (400/422) y el agricultor corrige desde el paso correspondiente.

**A2 — Cultivo sin vida útil registrada:**
- El agricultor no indicó fecha límite y el cultivo no tiene `vida_util_dias`.
- El sistema pide la fecha límite ("Indique fecha_limite: el cultivo no tiene vida util registrada").

**A3 — Agricultor cancela:**
- El agricultor abandona el formulario; no se guarda nada.

**A4 — Moderación posterior:**
- Si luego un administrador detecta un problema, bloquea la publicación (UC-11); el agricultor recibe una notificación MODERACION.

**Postcondición:**
- Flujo básico: publicación ACTIVA visible en la búsqueda.
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
2. Elige cultivo (obligatorio) y, opcionalmente, provincia, unidad y período (por defecto, últimos 30 días).
3. El sistema consulta `v_tablero_precios` con esos filtros.
4. El sistema muestra, por fecha, precio mínimo, máximo y promedio, e indica la fuente (REFERENCIA_EXTERNA con su fuente, o PLATAFORMA).
5. El sistema calcula en el service el promedio del período y la tendencia (sube / baja / estable) comparando el inicio y el final del período.

**Flujos alternos:**

**A1 — Sin datos de precio:**
- El cultivo no tiene precios en el período: el sistema muestra "No hay precios de referencia para este cultivo".

**A2 — Carga de precios por el administrador:**
- El administrador entra a "Cargar precios" y elige:
  - Formulario: cultivo, provincia, unidad, fecha, precio mínimo, máximo, promedio y fuente.
  - Archivo Excel/CSV con cabecera `cultivo, provincia, unidad, fecha, precio_minimo, precio_maximo, precio_promedio`, más la fuente.
- El sistema valida: cultivo, provincia y unidad existen; 0 < mínimo ≤ promedio ≤ máximo; no existe ya un precio para ese cultivo, provincia, unidad y fecha.
- El sistema registra una fila en `cargas_precio` (método FORMULARIO o ARCHIVO, con `nombre_archivo`) y las filas válidas en `precios_mercado`. `trg_carga_precio_bi` confirma que el usuario es ADMINISTRADOR.
- Con archivo, las filas inválidas se listan con su motivo y el administrador puede reintentar solo esas.

**A3 — Filtro sin resultados:**
- El sistema muestra "Sin resultados" y sugiere quitar la provincia o ampliar el período.

**Postcondición:**
- El usuario ve precios históricos y tendencia.
- A2: carga registrada con administrador, fuente y fecha.

**Requisito:** RF-02  
**Módulo:** `precios` (catálogos: `catalogo`)  
**Tablas:** cargas_precio, precios_mercado, cultivos, provincias, unidades_medida, `v_tablero_precios`, `v_precio_plataforma`

---

## UC-03: Buscar productos disponibles

**Actores:**
- Comprador (principal)
- También accesible para el visitante sin sesión

**Precondición:**
- Ninguna (la búsqueda puede no tener resultados).

**Flujo básico:**
1. El usuario entra a "Buscar" o a la página de inicio.
2. El sistema presenta filtros opcionales: categoría, cultivo, variedad, provincia/distrito, fecha de disponibilidad desde/hasta.
3. El usuario elige criterios y pulsa "Buscar".
4. El sistema consulta `v_publicacion_disponible` con:
   - `estado = ACTIVA`
   - `cantidad_disponible > 0`
   - `fecha_limite >= hoy`
   - los filtros elegidos
5. El sistema devuelve resultados paginados (20 por página), ordenados por fecha límite más próxima.
6. Cada resultado muestra: cultivo y variedad, cantidad disponible y unidad, precio referencial, provincia y distrito, fecha límite con su nivel de perecibilidad y la reputación del agricultor (`v_reputacion_usuario`).
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
**Tablas:** `v_publicacion_disponible`, `v_reputacion_usuario`, categorias, cultivos, variedades, provincias, distritos

---

## UC-04: Contactar y negociar con el agricultor

**Actores:**
- Comprador (principal)
- Agricultor (secundario)

**Precondición:**
- El comprador tiene sesión iniciada y está ACTIVO.
- La publicación está ACTIVA (UC-03).

**Flujo básico:**
1. El comprador abre el detalle de la publicación y pulsa "Contactar al agricultor".
2. El sistema busca la conversación de ese comprador para esa publicación (una por par publicación–comprador).
3. Si no existe, la crea en estado ABIERTA y abre el chat.
4. El comprador escribe su mensaje (cantidad, precio, condiciones).
5. El sistema valida que la conversación esté ABIERTA y registra el mensaje con remitente y fecha; `trg_mensaje_bi` confirma que el remitente participa.
6. El sistema crea una notificación MENSAJE para el agricultor.
7. El agricultor ve la notificación (el frontend consulta periódicamente) y abre el chat.
8. El agricultor responde; la conversación sigue hasta acordar cantidad y precio.
9. Cuando hay acuerdo, cualquiera de las dos partes pulsa "Registrar propuesta" → UC-05.

**Flujos alternos:**

**A1 — Contacto por WhatsApp:**
- El comprador pulsa "Contactar por WhatsApp".
- El sistema registra una fila en `leads_whatsapp` (publicación, comprador, fecha) y abre el enlace `wa.me` con el teléfono del agricultor y un texto sugerido.
- La negociación continúa fuera de la plataforma; la propuesta puede registrarse igualmente en UC-05.

**A2 — Sin acuerdo:**
- Las partes no llegan a un acuerdo; la conversación queda ABIERTA sin propuesta.

**A3 — Cierre de la conversación:**
- Una de las partes cierra la conversación: estado CERRADA. El sistema rechaza nuevos mensajes en ella (422).

**A4 — Remitente no autorizado:**
- Un usuario que no es el comprador ni el agricultor intenta escribir.
- El service lo rechaza (403); si llegara a la BD, `trg_mensaje_bi` lo rechaza ("El remitente no participa en la conversacion").

**Postcondición:**
- Conversación ABIERTA con historial visible para ambas partes.
- A1: lead registrado.

**Requisito:** RF-04  
**Módulo:** `mensajeria`  
**Tablas:** conversaciones, mensajes, leads_whatsapp, notificaciones

---

## UC-05: Proponer y acordar una transacción

**Actores:**
- Agricultor o Comprador (principal: quien propone)
- La otra parte (principal: quien acepta o rechaza)

**Precondición:**
- Existe una conversación entre las partes (UC-04) o un contacto por WhatsApp.
- La publicación está ACTIVA y ambos usuarios están ACTIVO.

**Flujo básico:**
1. El proponente pulsa "Registrar propuesta" e ingresa cantidad y precio unitario acordado.
2. El sistema valida: cantidad > 0 y ≤ cantidad disponible, precio > 0, publicación ACTIVA.
3. El sistema crea la transacción en estado **PROPUESTA** (`id_proponente` = quien propone); `trg_transaccion_bi` repite las validaciones.
4. El sistema crea una notificación PROPUESTA para la otra parte.
5. La otra parte revisa cultivo, cantidad, precio unitario, monto total y la reputación de la contraparte, y pulsa "Aceptar".
6. El sistema bloquea la publicación (`PESSIMISTIC_WRITE`), vuelve a verificar la disponibilidad y pasa la transacción a **ACORDADA**; `trg_transaccion_bu` fija `fecha_acuerdo`.
7. Si con este acuerdo se agota la cantidad, `trg_transaccion_au` pasa la publicación a VENDIDA.
8. El sistema notifica a ambos (TRANSACCION) con los datos de contacto de la contraparte.
9. Continúa en UC-06 (con transporte) o con la confirmación de recepción sin transporte (UC-06 A1).

**Flujos alternos:**

**A1 — Rechazo:**
- La otra parte pulsa "Rechazar": estado RECHAZADA (el trigger fija `fecha_cierre`). El proponente recibe una notificación TRANSACCION.

**A2 — Cancelación:**
- Cualquiera de las partes cancela una transacción PROPUESTA o ACORDADA: estado CANCELADA. La contraparte recibe una notificación TRANSACCION.
- Si estaba ACORDADA y la publicación estaba VENDIDA, `trg_transaccion_au` la devuelve a ACTIVA (o VENCIDA si ya pasó su fecha límite).

**A3 — Cantidad no disponible o publicación no ACTIVA:**
- El sistema responde 422 con el mensaje del trigger (p. ej. "La cantidad supera lo disponible en la publicacion").

**A4 — Ajuste de la propuesta:**
- Mientras está en PROPUESTA, el proponente puede cambiar cantidad o precio; la otra parte recibe una notificación PROPUESTA. Una transacción ACORDADA ya no cambia sus términos.

**Postcondición:**
- Transacción ACORDADA, RECHAZADA o CANCELADA.
- Publicación ACTIVA o VENDIDA según la cantidad restante.

**Requisito:** RF-06 (registro de transacciones), a partir de la negociación de RF-04  
**Módulo:** `transacciones`  
**Tablas:** transacciones, publicaciones, notificaciones

---

## UC-06: Registrar entrega y completar la transacción

**Actores:**
- Agricultor (principal)
- Comprador (principal: confirma la recepción)

**Precondición:**
- La transacción está ACORDADA.
- Las partes acordaron usar transporte (si no, ver A1).

**Flujo básico:**
1. El agricultor (o el comprador) pulsa "Registrar entrega".
2. El sistema muestra el formulario: transportista (buscar por teléfono o crear uno nuevo con nombre y teléfono) y placa del vehículo.
3. El usuario confirma; el sistema crea el transportista si es nuevo y la fila en `entregas`. `trg_entrega_bi` verifica que la transacción esté ACORDADA y guarda la placa en mayúsculas.
4. Al recoger la carga, el agricultor o el comprador registra el evento **RECOGIDO** (con observación opcional).
5. Al salir hacia el destino, registra **EN_CAMINO**.
6. Al llegar, registra **ENTREGADO**.
7. En cada evento, `trg_evento_entrega_bi` valida la secuencia y que quien registra sea parte de la transacción; el service rechaza duplicados y retrocesos. El sistema notifica a la otra parte (ENTREGA).
8. El comprador revisa la carga y pulsa "Confirmar recepción".
9. El sistema registra `fecha_confirmacion_recepcion`; `trg_entrega_bu` verifica que exista ENTREGADO y `trg_entrega_au` pasa la transacción a **COMPLETADA** (fecha de cierre).
10. El sistema notifica a ambos que pueden calificar → UC-07.

**Flujos alternos:**

**A1 — Sin transporte (RF-06):**
- Las partes coordinan directamente (retiro en finca o entrega del propio agricultor).
- No se crea fila en `entregas`.
- El comprador pulsa "Confirmar recepción" y el sistema pasa la transacción de ACORDADA a COMPLETADA.

**A2 — Discrepancia en la entrega:**
- El comprador no confirma la recepción y lo indica por la mensajería (UC-04).
- La transacción sigue ACORDADA hasta que se resuelva: el comprador confirma o una de las partes cancela (UC-05 A2).

**A3 — Evento fuera de secuencia:**
- Se intenta registrar EN_CAMINO sin RECOGIDO (o ENTREGADO sin EN_CAMINO).
- El sistema responde 422: "EN_CAMINO requiere el evento RECOGIDO".

**A4 — Evento registrado por error:**
- Los eventos de entrega no se editan, no se eliminan y no existen eventos de corrección (solo RECOGIDO, EN_CAMINO y ENTREGADO).
- Las partes aclaran el error por la mensajería (UC-04); el registro se conserva para auditoría. Si el error impide continuar, una de las partes cancela la transacción (UC-05 A2).

**Postcondición:**
- Transacción COMPLETADA con su entrega y eventos registrados (o sin entrega en A1).
- Ambas partes pueden calificar.

**Requisito:** RF-11 (A1: RF-06)  
**Módulo:** `transacciones`  
**Tablas:** transportistas, entregas, eventos_entrega, transacciones, notificaciones

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
5. El sistema registra la calificación; `trg_calificacion_bi` repite las validaciones de estado y participante.
6. El sistema crea una notificación CALIFICACION para la contraparte.
7. La reputación se refleja al consultar `v_reputacion_usuario` (no se almacena).

**Flujos alternos:**

**A1 — Transacción no completada:**
- El sistema responde 422: "Solo se califican transacciones COMPLETADAS".

**A2 — Ya calificada:**
- El sistema responde 409 (clave única transacción–evaluador): "Ya calificaste esta transacción". Las calificaciones no se editan.

**A3 — Comentario inapropiado:**
- Si el administrador detecta un comentario ofensivo, aplica una ADVERTENCIA o SUSPENSION al autor (UC-11).

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
2. El sistema consulta `v_historial_transaccion` donde el usuario es `id_comprador` o `id_agricultor`.
3. El usuario puede filtrar por estado (PROPUESTA, ACORDADA, COMPLETADA, RECHAZADA, CANCELADA), rango de fechas y contraparte.
4. El sistema devuelve la lista paginada (20 por página).
5. Cada fila muestra: cultivo y variedad, cantidad y unidad, precio unitario, monto total, contraparte, estado, estado de entrega y fechas de propuesta, acuerdo y cierre.
6. Al abrir una transacción, el sistema muestra:
   - La cronología (propuesta → acuerdo → eventos de entrega → recepción → cierre).
   - Las calificaciones emitidas y recibidas.
   - El transportista y la placa, si hubo entrega.
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
**Tablas:** `v_historial_transaccion`, calificaciones, entregas, transportistas, eventos_entrega

---

## UC-09: Recibir alertas de vencimiento

**Actores:**
- Agricultor (principal)
- Evento programado `ev_tareas_diarias` (disparador temporal, no es actor)

**Precondición:**
- La publicación está ACTIVA.

**Flujo básico:**
1. **Evento diario (06:00, hora del servidor):**
   - Si `fecha_limite = hoy + 3`, crea una notificación VENCIMIENTO "Tu cosecha vence en 3 dias".
   - Si `fecha_limite = hoy + 1`, crea una notificación VENCIMIENTO "Tu cosecha vence en 24 horas".
   - No repite la alerta de una publicación el mismo día.
2. **El agricultor ve la alerta** en su centro de notificaciones (en la app) y la abre; el sistema la marca como leída y muestra la publicación.
3. **El agricultor elige una acción rápida:**
   - **Bajar el precio:** actualiza `precio_referencial`.
   - **Extender la fecha:** elige +7, +14 o +30 días; el sistema valida que la nueva fecha sea ≥ hoy y actualiza `fecha_limite`.
   - **Retirar:** estado RETIRADA (no se venderá).
   - **No hacer nada:** la publicación sigue ACTIVA.
4. **Vencimiento automático:** al día siguiente de la fecha límite, el evento pasa la publicación a VENCIDA; deja de aparecer en la búsqueda.
5. **Reactivación:** el agricultor extiende la fecha de una publicación VENCIDA (actualizando solo `fecha_limite`); `trg_publicacion_bu` la devuelve a ACTIVA.

**Flujos alternos:**

**A1 — Publicación no ACTIVA:**
- Si está VENDIDA, RETIRADA o BLOQUEADA, el evento no genera alertas.

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
   - **Común:** nombres, apellidos, teléfono (obligatorio, será el usuario de acceso), email (opcional) y contraseña (mín. 8 caracteres, con letras y números).
   - **Agricultor:** distrito (catálogo) y referencia de dirección (opcional).
   - **Comprador:** tipo de comprador (MERCADO_MAYORISTA, RESTAURANTE, HOTEL, PROCESADORA, OTRO), razón social (opcional) y RUC (opcional, 11 dígitos).
3. El visitante pulsa "Crear cuenta".
4. El sistema valida: campos obligatorios, formato del teléfono, email y RUC; teléfono, email y RUC no registrados; contraseña segura.
5. El sistema, en una sola transacción:
   - Crea la fila en `usuarios` (rol AGRICULTOR o COMPRADOR, estado ACTIVO, contraseña con BCrypt).
   - Crea la fila en `agricultores` o `compradores`; `trg_agricultor_bi` / `trg_comprador_bi` verifican que el rol coincida.
6. El sistema emite un JWT, el frontend lo guarda en `sessionStorage` y muestra "Bienvenido, [Nombres]".
7. El agricultor puede publicar (UC-01); el comprador, buscar (UC-03).

**Inicio de sesión:**
1. El usuario ingresa teléfono y contraseña.
2. El sistema verifica la contraseña y que el usuario esté ACTIVO, actualiza `ultimo_acceso` y emite el JWT.

**Cambio de contraseña propio:**
1. El usuario con sesión entra a "Mi perfil" → "Cambiar contraseña".
2. Ingresa la contraseña actual y la nueva (mín. 8 caracteres, con letras y números).
3. El sistema verifica la contraseña actual y guarda la nueva con BCrypt.

**Flujos alternos:**

**A1 — Teléfono, email o RUC ya registrado:**
- El sistema responde 409: "Este teléfono ya está registrado".

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
**Tablas:** usuarios, agricultores, compradores, roles, distritos

---

## UC-11: Gestionar la plataforma (Administrador)

**Actores:**
- Administrador (principal)

**Precondición:**
- El administrador tiene sesión iniciada con rol ADMINISTRADOR.

**Flujo básico:**

**Parte 1 — Gestión de usuarios:**
1. El administrador entra a "Usuarios".
2. El sistema lista los usuarios, filtrables por rol y estado, con búsqueda por nombre, teléfono o email.
3. El administrador puede:
   - Ver el detalle: rol, fecha de registro, último acceso, reputación, transacciones y moderaciones recibidas.
   - Editar datos básicos.
   - **Reiniciar la contraseña:** asigna una contraseña temporal (BCrypt) y la comunica al usuario por teléfono.
   - Crear otro administrador (UC-10 A5).
   - Desactivar la cuenta (INACTIVO).
4. Ante una conducta inapropiada, pulsa "Suspender", escribe el motivo y confirma:
   - El sistema registra una moderación SUSPENSION con `id_usuario_afectado` (`trg_moderacion_bi` verifica que es ADMINISTRADOR).
   - El usuario pasa a SUSPENDIDO y pierde el acceso en su siguiente petición (el validador del token verifica el estado del usuario en cada petición).
   - El usuario recibe una notificación MODERACION.
5. "Reactivar" registra REACTIVACION y devuelve el usuario a ACTIVO. "Advertir" registra ADVERTENCIA sin cambiar el estado.

**Parte 2 — Moderación posterior de publicaciones:**
1. El administrador revisa publicaciones desde el listado (filtros por estado, cultivo, agricultor, fecha).
2. Revisa la descripción, los datos del agricultor y sus moderaciones anteriores.
3. Si la publicación es inapropiada, pulsa "Bloquear" con motivo:
   - Moderación BLOQUEO con `id_publicacion`.
   - Publicación → BLOQUEADA (deja de aparecer en la búsqueda).
   - Notificación MODERACION al agricultor.
4. "Desbloquear" registra DESBLOQUEO y devuelve la publicación a ACTIVA (o VENCIDA si ya pasó su fecha límite).

**Parte 3 — Reportes y métricas:**
1. El administrador entra a "Reportes".
2. El sistema muestra:
   - Usuarios activos por rol (con `ultimo_acceso` en los últimos 30 días).
   - Transacciones COMPLETADA por mes: cantidad y monto.
   - Cultivos más publicados (top 5, últimos 30 días).
   - Precio promedio por cultivo (`v_tablero_precios`).
   - Embudo de conversión por publicación (`v_conversion_publicacion`).
   - Usuarios con mejor y peor reputación (`v_reputacion_usuario`).
3. Puede filtrar por fecha, provincia y cultivo.
4. "Exportar a Excel" descarga el reporte (Apache POI).

**Parte 4 — Carga de precios de referencia:**
- Ver UC-02 A2.

**Flujos alternos:**

**A1 — Suspensión con transacciones en curso:**
- Si el usuario tiene transacciones PROPUESTA o ACORDADA, el sistema lo advierte.
- El administrador confirma; las transacciones no cambian de estado automáticamente. Las contrapartes reciben una notificación MODERACION y pueden cancelarlas (UC-05 A2).

**A2 — Reconsideración:**
- El usuario suspendido se comunica con la plataforma por teléfono o WhatsApp; el administrador decide si lo reactiva (REACTIVACION).

**A3 — Motivo vacío:**
- El sistema no registra la moderación sin motivo (400).

**Postcondición:**
- Usuarios y publicaciones moderados con su registro en `moderaciones`.
- Reportes consultados o exportados.

**Requisito:** RF-09 (panel administrativo), RF-08 (gestión de usuarios), RF-02 (carga de precios)  
**Módulo:** `administracion` (usa la `api` de `usuarios`, `publicaciones`, `transacciones`, `calificaciones`, `precios` y `notificaciones`)  
**Tablas:** usuarios, publicaciones, moderaciones, transacciones, notificaciones, `v_conversion_publicacion`, `v_reputacion_usuario`, `v_tablero_precios`

---

## UC-12: Venta agrupada

**Actores:**
- Comprador (principal: crea la propuesta y acepta aportes)
- Agricultores (principales: aportan)

**Precondición:**
- El comprador y los agricultores tienen sesión iniciada y están ACTIVO.

**Flujo básico:**
1. El comprador entra a "Venta agrupada" y consulta `v_oferta_consolidada` para ver dónde hay volumen disponible por cultivo, variedad, provincia y unidad.
2. Pulsa "Crear propuesta grupal" e ingresa: variedad (y con ella el cultivo), provincia, unidad, cantidad total requerida, precio unitario ofrecido, condiciones (opcional) y fecha límite de respuesta.
3. El sistema valida (cantidad > 0, precio > 0, fecha ≥ hoy) y registra la propuesta en estado ABIERTA.
4. El sistema notifica (PROPUESTA) a los agricultores con publicaciones ACTIVA de esa variedad, provincia y unidad.
5. Un agricultor abre la propuesta, elige una de sus publicaciones compatibles y la cantidad que aporta.
6. El sistema crea, a través del módulo `transacciones`, una transacción **PROPUESTA** vinculada a la propuesta grupal:
   - `id_comprador` = el comprador de la propuesta.
   - `id_proponente` = el agricultor.
   - Cantidad aportada y precio ofrecido.
   - `trg_transaccion_bi` verifica que la propuesta esté ABIERTA, que coincidan comprador, variedad, provincia y unidad, y que la cantidad no supere lo disponible.
7. El comprador ve el avance: suma aportada frente a la cantidad requerida y la lista de aportes.
8. El comprador acepta cada aporte (ACORDADA, con bloqueo de la publicación como en UC-05) o lo rechaza (RECHAZADA).
9. Cuando reúne la cantidad deseada, el comprador cierra la propuesta (CERRADA); el sistema cancela los aportes que sigan en PROPUESTA.
10. Cada transacción ACORDADA sigue su ciclo normal: entrega (UC-06), recepción, calificación (UC-07) e historial (UC-08).

**Flujos alternos:**

**A1 — Publicación incompatible:**
- La variedad, provincia o unidad no coincide: el sistema responde 422 ("La publicacion no coincide con la propuesta grupal ...").

**A2 — Vencimiento sin cierre:**
- Pasada la fecha límite de respuesta, `ev_tareas_diarias` cancela los aportes en PROPUESTA y cierra la propuesta (CERRADA). Los aportes ya ACORDADA continúan.

**A3 — Cancelación por el comprador:**
- El comprador cancela la propuesta (CANCELADA); el sistema cancela los aportes en PROPUESTA y notifica a los agricultores. Los aportes ACORDADA se cancelan individualmente si corresponde (UC-05 A2).

**A4 — Propuesta no ABIERTA:**
- Un agricultor intenta aportar a una propuesta CERRADA o CANCELADA: 422 ("La propuesta grupal no esta ABIERTA").

**A5 — Aporte mayor a lo disponible:**
- 422 ("La cantidad supera lo disponible en la publicacion").

**Postcondición:**
- Propuesta grupal CERRADA o CANCELADA, con N transacciones vinculadas (una por aporte) en su estado final o en curso.

**Requisito:** RF-10  
**Módulo:** `ventaagrupada` (aportes: `transacciones`)  
**Tablas:** propuestas_grupales, transacciones, publicaciones, `v_oferta_consolidada`, notificaciones

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

1. **UC-01** dispara `trg_publicacion_bi`; refrescar la entidad tras guardar para obtener `fecha_limite`. Los catálogos se consultan a través de `catalogo::api`.
2. **UC-02** consulta `v_tablero_precios` (vista normal, se recalcula en cada consulta); la carga dispara `trg_carga_precio_bi`.
3. **UC-03** usa `v_publicacion_disponible` con JPA Specifications y paginado de 20 (`@PageableDefault(size = 20)`).
4. **UC-04** dispara `trg_mensaje_bi`; el service valida participante y conversación ABIERTA.
5. **UC-05** dispara `trg_transaccion_bi`, `trg_transaccion_bu` y `trg_transaccion_au`; bloqueo pesimista antes de ACORDADA; refrescar la entidad tras guardar.
6. **UC-06** dispara `trg_entrega_bi`, `trg_entrega_bu`, `trg_entrega_au` y `trg_evento_entrega_bi`; el service valida duplicados y retrocesos. Tras confirmar la recepción, releer la transacción: el paso a COMPLETADA lo hace el trigger.
7. **UC-07** dispara `trg_calificacion_bi`; la UK transacción–evaluador produce 409.
8. **UC-08** lee `v_historial_transaccion`; exportación con Apache POI.
9. **UC-09** alimentado por `ev_tareas_diarias` — no usar `@Scheduled`; la reactivación la hace `trg_publicacion_bu` al cambiar solo `fecha_limite`.
10. **UC-10** dispara `trg_agricultor_bi` o `trg_comprador_bi`; usuario y subtipo en la misma transacción.
11. **UC-11** dispara `trg_moderacion_bi` (y `trg_carga_precio_bi` en la carga de precios); el service actualiza el estado del usuario o de la publicación en la misma transacción que la moderación.
12. **UC-12** crea los aportes a través de `transacciones::api` (dispara `trg_transaccion_bi` con `id_propuesta_grupal`); el cierre por vencimiento lo hace `ev_tareas_diarias`.
13. **Notificaciones** (MENSAJE, PROPUESTA, TRANSACCION, ENTREGA, CALIFICACION, MODERACION): el módulo que ejecuta la acción llama a `notificaciones::api` dentro de la misma transacción. Usar solo los tipos definidos en `modelo-bd.sql`.

---

**Documento preparado para el equipo de desarrollo y Claude Code.**  
**Próxima revisión:** después de actualizar el BPMN TO-BE (APF2).
