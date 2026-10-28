# ADR-006: Datos de demostración con Datafaker

**Estado:** Aceptada  
**Fecha:** 27 de octubre de 2026  
**Responsable:** equipo AgroConecta

## Contexto

El docente pidió poder clonar el repositorio, crear su propia base de datos y ver la aplicación funcionando con datos. Con una base nueva la aplicación arranca vacía: solo tiene el catálogo de `V2` (provincias, distritos, cultivos, variedades y unidades). Para mostrar búsqueda, precios, mensajería, ventas, calificaciones, moderación y venta agrupada hace falta crear a mano usuarios, publicaciones y ventas que pasen por todos los estados.

Los datos no pueden insertarse con SQL sin más: los triggers exigen recorrer el ciclo de cada venta (ACORDADA → RECOGIDO → … → COMPLETADA), las contraseñas van con BCrypt y las fechas límite dependen del día actual.

## Decisión

1. Un módulo nuevo, `pe.agroconecta.demo`, sin tablas propias, que crea los datos **solo a través de la capa `aplicacion` de los demás módulos**. Así pasan por las mismas validaciones, triggers, avisos e historial que el uso real. Ningún módulo depende de `demo`; `ArquitecturaTest.MODULOS` lo incluye.
2. La carga corre al arrancar únicamente con el perfil `demo` (`SPRING_PROFILES_ACTIVE=dev,demo`). Se niega a correr si también está activo `prod`, no hace nada si los datos ya existen (celular del administrador de demostración) y se hace en una sola transacción: o se crean todos o ninguno. Un error no detiene el arranque.
3. **Datafaker** (`net.datafaker:datafaker`, Apache 2.0, gratuito) genera nombres, negocios, RUC, textos de mensajes y calificaciones, y la variación de los precios, con locale `es` y **semilla fija**: una base nueva siempre recibe los mismos datos. Distritos, cultivos, variedades y unidades salen del catálogo, nunca de Datafaker.
4. La demo crea su propio ADMINISTRADOR (`900000001`) para cargar los precios de referencia. Todos los usuarios de demostración usan la contraseña `Demo2026`.
5. Para no usar enums del `dominio` de otros módulos, `usuario` y `precio` exponen variantes con texto: `RegistroDto.deTexto(...)` y `CargaPrecioService.registrar(..., String fuente)`, además de `UsuarioConsultaService.existeCelular(...)`.

## Alternativas consideradas

- **Script SQL que el docente ejecuta a mano.** Un paso más para el docente, contraseñas con BCrypt copiadas al script y cada venta llevada por sus estados con SQL; frágil ante cambios del esquema.
- **Migración de Flyway con datos.** Se aplicaría también en producción y en las pruebas.
- **Mockaroo u otro generador web.** Produce filas sueltas que no respetan los triggers ni el ciclo de las ventas.
- **Poner la carga dentro de `administracion`.** Mezcla datos de demostración con un módulo de negocio y le agrega dependencias hacia casi todos los demás.

## Consecuencias

- El docente obtiene una aplicación con datos realistas con un solo cambio en el comando de arranque.
- **Riesgo:** con el perfil `demo` existe un administrador con contraseña conocida. Mitigación: el perfil nunca se activa en el servidor desplegado y la carga se niega a correr con `prod`.
- El WAR crece unos 3 MB por Datafaker y sus dependencias (SnakeYAML, RgxGen, libphonenumber).
- Si cambian las reglas de los servicios, la carga de demostración debe ajustarse; `DatosDemoIntegracionTest` lo detecta en CI.
