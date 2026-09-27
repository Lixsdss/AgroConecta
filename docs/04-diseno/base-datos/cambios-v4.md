# AgroConecta — Modelo de datos v4

Revisión del modelo v3 contra las interfaces de Claude Design (7 módulos, RF-01 a RF-11). El script `modelo-bd.sql` v4 fue ejecutado y probado en MariaDB 10.11 con los datos de las pantallas.

## 1. El problema de la "telaraña"

Un modelo tipo telaraña es aquel en que las tablas se conectan por muchos caminos, de modo que el diagrama se llena de líneas cruzadas y un mismo dato se puede alcanzar por dos rutas distintas. El docente tenía razón: v3 tenía tres causas concretas.

| Causa en v3 | Ejemplo | Corrección en v4 |
|---|---|---|
| Listas fijas modeladas como tablas | `roles`, `categorias` | Columna con `CHECK` (`usuarios.rol`); el filtro "Categoría" de la UI es el cultivo |
| Rutas duplicadas hacia el mismo dato | `transacciones` apuntaba a publicación y a comprador, y `propuestas_grupales` también al comprador; un trigger tenía que verificar que coincidieran | `transacciones` apunta solo a `conversaciones`, que ya es "publicación + comprador" |
| Tablas que ninguna pantalla usa | `transportistas`, `entregas` (placa), `cargas_precio`, `leads_whatsapp` | Se funden o se eliminan (ver sección 3) |
| Una tabla que apunta "a una cosa o a otra" | `moderaciones` con FK a publicación o a usuario | Se separa en `revisiones_publicacion` y `suspensiones` |

Resultado: de 24 a 20 tablas y de 42 a 36 claves foráneas, cubriendo además reclamos, suspensiones justificadas, revisión de publicaciones y unidades de venta, que v3 no tenía.

**Regla aplicada:** es tabla solo lo que el administrador amplía o tiene atributos propios (distritos, cultivos, variedades, unidades). Estados, roles, motivos y fuentes son listas fijas de negocio y van como `CHECK`.

**Para el diagrama lógico:** no dibujar las 20 tablas en una sola hoja. Hacer un diagrama por módulo (usuarios, publicación y precios, comercial, administración). Cada diagrama muestra sus tablas completas y las tablas compartidas (`usuarios`, `distritos`, `cultivos`) como referencia reducida en el borde. Así cada hoja tiene entre 5 y 8 tablas y ninguna línea cruza el dibujo completo.

## 2. Decisiones incorporadas

| Tema | Decisión | Cómo queda en la BD |
|---|---|---|
| Distrito "Otro" | El alcance es toda la región Piura | Se cargan los 65 distritos (ubigeo INEI). Las 4 zonas de precio llevan `distritos.es_zona_precio = TRUE` |
| Unidad del arroz | Se comercializa en cáscara, en sacos de 50 kg (fuente: prensa regional, abril 2026) | `cultivos.id_unidad_local = Saco`, `peso_sugerido_kg = 50`. El quintal (46 kg) sigue disponible si el agricultor lo elige |
| Rango de precios | Es el promedio de ventas reales de la plataforma, contrastado con el precio del mercado real | Mercado real: `precios_mercado` (un precio por kilo por día). Plataforma: vista `v_rango_precio_plataforma` (mínimo, máximo y promedio de ventas completadas en 30 días) |
| Reclamos | Son parte de RF-09; los motivos no son libres | Tabla `reclamos` con motivo de catálogo; `suspensiones` exige motivo de catálogo y detalle, y el motivo `RECLAMOS_FUNDADOS` exige al menos 3 reclamos fundados |
| Métricas del panel | Se resuelven con vistas | `v_panel_admin` y `v_ventas_mensuales_cultivo` |

## 3. Cambios de v3 a v4

| v3 | v4 | Motivo |
|---|---|---|
| `roles` | `usuarios.rol` (CHECK) | 3 valores fijos |
| `categorias` | eliminada | La UI filtra por cultivo |
| `usuarios.nombres/apellidos/email/telefono` | `nombre_completo`, `celular` (login, 9 dígitos, empieza en 9) | El registro pide un solo nombre y celular; no hay email |
| `agricultores.id_distrito` | `usuarios.id_distrito` | Comprador y administrador también tienen distrito |
| — | `agricultores.nombre_chacra`, `compradores.nombre_negocio`, tipo `MOLINO` | Pantalla Usuarios y perfil |
| `publicaciones.precio_referencial`, `fecha_disponible` | `precio_unitario`, `peso_unidad_kg`, `fecha_cosecha` + columnas generadas `cantidad_kg`, `precio_kg` | Unidad elegida por el agricultor y peso editable por jaba o saco; todo se compara en kg |
| Estados ACTIVA/BLOQUEADA | EN_REVISION, PUBLICADA, RECHAZADA, VENDIDA, VENCIDA, RETIRADA + `marca_revision` | Cola de moderación |
| `cargas_precio` + `precios_mercado` (mín/máx/prom por provincia y unidad) | `precios_mercado` (precio por kilo, por distrito zona, fuente y admin) | El admin carga un solo precio por kilo; la unidad local y la tendencia se calculan |
| `leads_whatsapp` | `conversaciones.canal_inicio`, `clics_whatsapp` | El lead es el inicio de la conversación |
| `mensajes.contenido` | `tipo` TEXTO/OFERTA + `oferta_cantidad`, `oferta_precio_unitario` | Oferta estructurada del chat |
| `propuestas_grupales` por provincia y unidad | por distrito, en kg, con `fecha_recojo` | El lote mezcla unidades y se agrupa por zona |
| `transacciones` (publicación + comprador + proponente) | `id_conversacion`, unidad acordada propia, `fecha_recojo`, estados de entrega | La unidad acordada puede diferir de la publicación |
| `transportistas`, `entregas`, `eventos_entrega` | `transacciones_historial` (lo llena un trigger) | El transportista no es actor; la UI solo muestra la línea de tiempo |
| `moderaciones` | `revisiones_publicacion`, `suspensiones`, `reclamos` | Motivos justificados y sin FK "de una u otra" |
| `notificaciones.id_publicacion` | `id_referencia` sin FK | El aviso puede apuntar a publicación, venta o conversación |

## 4. Matriz pantalla → dato → columna

### Usuarios y roles (RF-08)
| Pantalla | Dato | Tabla.columna |
|---|---|---|
| Ingresar | Celular, contraseña | `usuarios.celular`, `password_hash` |
| ¿Quién es usted? | Agricultor / Comprador | `usuarios.rol` |
| Crear cuenta | Nombre, celular, distrito | `usuarios.nombre_completo`, `celular`, `id_distrito` |

### Publicación y vencimiento (RF-01, RF-07)
| Pantalla | Dato | Tabla.columna |
|---|---|---|
| Paso 1 | Cultivo, variedad | `publicaciones.id_variedad` → `variedades` → `cultivos` |
| Paso 2 | Unidad, cantidad, peso por jaba | `id_unidad`, `cantidad`, `peso_unidad_kg`; "≈600 kilos" = `cantidad_kg` |
| Paso 3 | Precio por unidad, precio por kilo | `precio_unitario`, `precio_kg` |
| Paso 3 | "Hoy pagan entre…", "Buen precio" | `v_rango_precio_plataforma`, `v_precios_vigentes` |
| Paso 3 | Dónde está su cosecha | `publicaciones.id_distrito` |
| Confirmación | Fecha de cosecha | `fecha_cosecha` |
| Mis ventas | Estado, "3 ofertas", "vence en 3 días" | `estado`, `v_conversion_publicacion.ofertas`, `v_publicaciones_disponibles.dias_restantes` |
| Mis ventas | Bajar precio / Extender | UPDATE de `precio_unitario` / `fecha_limite` |
| Avisos | Tipo, título, hora, acción | `notificaciones.tipo`, `titulo`, `fecha_creacion`, `id_referencia` |

### Búsqueda y precios (RF-02, RF-03)
| Pantalla | Dato | Tabla.columna |
|---|---|---|
| Inicio comprador | Precios de hoy, sube/baja, precio en jaba/saco | `v_precios_vigentes` |
| Buscar | Orden por precio/kilo, fecha, calificación | `precio_kg`, `fecha_cosecha`, `calificacion_agricultor` |
| Filtros | Cultivo, distrito, fecha, unidad | `v_publicaciones_disponibles` |
| Tarjeta | Vence en N días, agricultor, estrellas | `dias_restantes`, `agricultor`, `calificacion_agricultor` |
| Tarjeta | Botón WhatsApp | `conversaciones.clics_whatsapp` |

### Mensajería, venta y calificación (RF-04, RF-05, RF-06)
| Pantalla | Dato | Tabla.columna |
|---|---|---|
| Mensajes | Conversaciones, no leídos | `conversaciones`, `mensajes.leido` |
| Chat | Oferta "30 jabas a S/ 27" | `mensajes.tipo = OFERTA`, `oferta_cantidad`, `oferta_precio_unitario` |
| Registrar acuerdo | Unidad, cantidad, precio, día de recojo | `transacciones.id_unidad`, `cantidad`, `precio_unitario`, `fecha_recojo`; total = `monto_total` |
| Mi perfil | En curso / Completadas, "32 compras" | `v_historial_transacciones`, `v_resumen_usuario` |
| Calificar | Estrellas, comentario | `calificaciones.puntuacion`, `comentario` |

### Venta agrupada y entrega (RF-10, RF-11)
| Pantalla | Dato | Tabla.columna |
|---|---|---|
| Lotes agrupados | Total kg, promedio S/ por kilo, agricultores | `v_lotes_agrupados` |
| Propuesta conjunta | Precio por kilo, kg, fecha de recojo | `propuestas_grupales.precio_kg`, `cantidad_kg`, `fecha_recojo` |
| Cada agricultor | Propuesta en su unidad, acepta o rechaza | `transacciones` en estado PROPUESTA |
| Entrega | Línea de tiempo con fecha y hora | `transacciones_historial` |
| Entrega | Confirmar que recibí | Solo el comprador pasa a ENTREGADO y COMPLETADA |

### Panel administrativo (RF-09)
| Pantalla | Dato | Tabla.columna |
|---|---|---|
| Panel | Activas, en revisión, ventas y kilos del mes | `v_panel_admin`, `v_ventas_mensuales_cultivo` |
| Panel | "16 de 16 precios cargados" | `v_panel_admin.precios_cargados_hoy` / `precios_esperados_hoy` |
| Precios de mercado | Cultivo, zona, precio/kilo, fuente | `precios_mercado` |
| Moderación | Cola, marca del sistema | `publicaciones.estado = EN_REVISION`, `marca_revision` |
| Rechazar | Motivo, mensaje al agricultor | `revisiones_publicacion.motivo_rechazo`, `mensaje` |
| Usuarios | Rol, distrito, calificación, ventas, estado | `v_resumen_usuario` |
| Suspender | Motivo justificado | `suspensiones.motivo`, `detalle` |

## 5. Pendientes después de este cambio

1. **Flyway:** este script reemplaza `V1__esquema_inicial.sql` y `V2__datos_iniciales.sql`. Si V1 ya se aplicó en local, ejecutar `flyway clean` o borrar la BD antes de migrar.
2. **JPA:** las columnas generadas (`cantidad_kg`, `precio_kg`, `monto_total`) se mapean con `@Column(insertable = false, updatable = false)`.
3. **Service de transacciones:** debe fijar `id_actualizado_por` en cada cambio de estado; el trigger usa ese valor para la línea de tiempo y los permisos. "Confirmar que recibí" hace dos updates (ENTREGADO y COMPLETADA) en una misma transacción `@Transactional`.
4. **Pantallas faltantes:** RF-09 ahora tiene reclamos, pero no hay pantalla para que comprador o agricultor registren un reclamo ni para que el administrador lo resuelva. Hay que agregarlas en Claude Design.
5. **Documentación:** actualizar CLAUDE.md, el SRS y `modelo-logico.md` (diagramas por módulo, sección 1).
