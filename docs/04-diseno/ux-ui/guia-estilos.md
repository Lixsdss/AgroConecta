# Guía de estilos — AgroConecta

Tomada del prototipo de Claude Design (exportación del 27/09/2026, guardada fuera del repositorio; ver `pantallas.md`). La implementa `src/main/resources/static/css/estilos.css` sobre Bootstrap 5.3 servido localmente (`static/vendor/bootstrap/`).

## Principios (RNF-01, RNF-03)

- El diseño tiene dos formatos: **móvil 360×800** para agricultor, comprador y pantallas públicas, y **escritorio 1440×900** para las 5 pantallas del administrador (11a Panel, 11b Precios de mercado, 11c Moderación, 11d Rechazar con motivo, 11e Usuarios). El formato de escritorio se construye con la primera pantalla del administrador (slice 3).
- Pantallas móviles: máx. 480 px de ancho, centradas en pantallas grandes.
- Una acción principal por pantalla, botones grandes y navegación lineal (atrás / continuar).
- Textos cortos, de usted, sin tecnicismos. Los errores dicen cómo corregir ("Faltan números. Escriba los 9 números de su celular.").
- Página liviana: solo Bootstrap (CSS), los íconos y las fuentes locales, y JavaScript propio en módulos ES; sin librerías adicionales ni CDN.
- Los datos del usuario se escriben con `textContent`, nunca con `innerHTML`.

## Paleta

| Token CSS | Color | Uso |
|---|---|---|
| `--ac-verde` | `#306D35` | Color principal: botones, enlaces, selección |
| `--ac-verde-oscuro` | `#22572A` | Botón presionado o con el cursor encima |
| `--ac-verde-claro` | `#E7EFE3` | Fondo de elementos seleccionados y mensajes de éxito |
| `--ac-ambar` | `#F9A825` | Acento y foco del teclado |
| `--ac-ambar-claro` | `#FEF3D6` | Mensajes informativos |
| `--ac-texto` | `#2C3C46` | Texto principal |
| `--ac-texto-suave` | `#5B646F` | Texto secundario y ayudas |
| `--ac-error` | `#A23B2A` | Errores |
| `--ac-error-claro` | `#F7E7E2` | Fondo de mensajes de error |
| `--ac-linea` | `#D3D5C8` | Bordes |
| `--ac-superficie` | `#FDFDFD` | Fondo de la pantalla |
| `--ac-fondo` | `#F9F6F0` | Fondo de la página alrededor de la pantalla |

## Tipografía

- Texto: *Work Sans* (400, 600 y 700) con respaldo `system-ui`. Tamaño base 17 px; campos y botones 18 px.
- Títulos grandes y marca: *Cormorant Garamond* 600 con respaldo `Georgia`, 32 px (títulos) y 40 px (marca).
- Se sirven desde `static/vendor/fuentes/` (subconjunto latino en WOFF2, con tildes y ñ): Work Sans 50 KB (un solo archivo variable) y Cormorant Garamond 23 KB. `font-display: swap` muestra el texto con la fuente del sistema mientras cargan (RNF-01). Licencia SIL OFL 1.1 (`OFL-*.txt` en la misma carpeta).

## Íconos

- Los 67 íconos de **Lucide** que usa el diseño, en un solo archivo `static/img/iconos.svg` (14,6 KB; 4 KB comprimido). Licencia ISC (`static/vendor/lucide/LICENSE`).
- En HTML: `<svg class="ac-icono" aria-hidden="true" focusable="false"><use href="/img/iconos.svg#arrow-left"></use></svg>`.
- En JavaScript: `icono('arrow-left')` o `ponerIconoYTexto(boton, 'save', 'Guardar')` de `js/comun/iconos.js`.
- Son decorativos (`aria-hidden`): el botón siempre lleva texto visible o `aria-label`.
- Si un slice necesita un ícono que no está en el archivo, se agrega desde `lucide-static` (misma versión) como un `<symbol>` más.

| Uso | Ícono |
|---|---|
| Atrás / siguiente | `arrow-left` / `arrow-right` |
| Ingresar / salir / crear cuenta | `log-in` / `log-out` / `user-plus` |
| Mostrar / ocultar contraseña | `eye` / `eye-off` |
| Agricultor / comprador | `sprout` / `store` |
| Mango / limón / arroz / plátano | `apple` / `citrus` / `wheat` / `banana` |
| Unidad de peso fijo / variable | `scale` / `weight` |
| Distrito | `map-pin` |
| Mensajes de error / información / éxito | `circle-alert` / `info` / `circle-check` |
| Botón esperando respuesta | `loader` |

## Medidas

| Elemento | Medida |
|---|---|
| Botones y campos | alto mínimo 56 px, radio 10 px |
| Botones secundarios, chips y "atrás" | alto mínimo 48 px |
| Tarjetas | radio 12 px |
| Chips | radio 999 px (píldora) |

## Componentes

| Clase | Uso |
|---|---|
| `.ac-pantalla` | Contenedor de cada pantalla |
| `.ac-barra`, `.ac-atras` | Barra superior con botón "atrás" (←) y título |
| `.ac-titulo`, `.ac-marca` | Título grande de la pantalla y marca AgroConecta |
| `.btn-primary`, `.btn-outline-primary`, `.btn-link` | Acción principal, secundaria y enlace de texto |
| `.ac-chips` | Opciones cortas como píldoras (distritos zona de precio + "Otro") |
| `.ac-opcion` | Tarjeta grande de elección (rol) con `aria-pressed` |
| `.ac-alerta` + `-error` / `-info` / `-exito` | Mensaje general de la pantalla |
| `.invalid-feedback.d-block[data-error-de]` | Error de un campo; lo llena `js/comun/formulario.js` con el `campo` de la API |
| `.ac-tarjeta-bienvenida` | Tarjetas de la bienvenida |
| `.ac-dato` | Fila etiqueta–valor en el perfil |
| `.ac-icono`, `.ac-icono-grande` | Ícono del mismo color y tamaño que el texto; versión de 32 px |
| `.ac-opcion.ac-con-icono` + `.ac-icono-opcion` | Tarjeta de elección con ícono en un recuadro verde claro a la izquierda |

## Accesibilidad

- Cada campo tiene `<label>`; las ayudas se enlazan con `aria-describedby`.
- Mensajes generales con `role="alert"`; tras un error se lleva el foco al primer campo inválido.
- Foco visible en ámbar sobre los chips; contraste del texto principal sobre la superficie ≥ 7:1.
