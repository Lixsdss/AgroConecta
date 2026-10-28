# Evidencia de pruebas

Resultado de la última ejecución de `./mvnw -B clean verify` en la rama `chore/apf3-cierre` (24/10/2026, 00:40, duración 1 min 15 s), con el código del commit `3a1dfa7`. Las pruebas de integración corrieron contra MariaDB local (base `agroconecta_test`, perfil `test`, sin H2).

Se reproduce con:

```bash
set -a; . ./.env; set +a   # DB_PASSWORD y JWT_SECRET
./mvnw -B clean verify
```

Informes: `target/surefire-reports/` (pruebas) y `target/site/jacoco/index.html` (cobertura).

## Resumen

| Indicador | Valor |
|---|---|
| Clases de prueba | 43 |
| Pruebas ejecutadas | **249** |
| Fallas / errores / omitidas | 0 / 0 / 0 |
| Arquitectura | 1 clase, 5 pruebas |
| Integración | 16 clases, 109 pruebas |
| Unitaria | 26 clases, 135 pruebas |
| Cobertura de líneas de todo el proyecto | 96,1 % (2814/2928) |
| Regla JaCoCo (≥ 70 % de líneas en cada paquete `aplicacion`) | Cumplida: "All coverage checks have been met." |
| Resultado | **BUILD SUCCESS** |

## Cobertura por paquete `aplicacion`

| Paquete | Líneas cubiertas | Cobertura de líneas | Cobertura de ramas | Mínimo 70 % |
|---|---|---|---|---|
| `administracion.aplicacion` | 384/386 | 99,5 % | 90,9 % | Cumple |
| `catalogo.aplicacion` | 39/39 | 100,0 % | 100,0 % | Cumple |
| `mensajeria.aplicacion` | 161/165 | 97,6 % | 93,1 % | Cumple |
| `notificacion.aplicacion` | 28/28 | 100,0 % | 100,0 % | Cumple |
| `precio.aplicacion` | 165/173 | 95,4 % | 72,6 % | Cumple |
| `publicacion.aplicacion` | 268/270 | 99,3 % | 88,3 % | Cumple |
| `usuario.aplicacion` | 264/266 | 99,2 % | 93,2 % | Cumple |
| `venta.aplicacion` | 387/392 | 98,7 % | 85,6 % | Cumple |
| `ventaagrupada.aplicacion` | 102/104 | 98,1 % | 83,3 % | Cumple |

## Pruebas por clase

| Módulo | Clase | Tipo | Pruebas | Fallas | Errores | Omitidas |
|---|---|---|---|---|---|---|
| `(raíz)` | `AgroconectaApplicationTests` | Integración (MariaDB) | 1 | 0 | 0 | 0 |
| `(raíz)` | `ArquitecturaTest` | Arquitectura (ArchUnit) | 5 | 0 | 0 | 0 |
| `administracion` | `AdministracionIntegracionTest` | Integración (MariaDB + MockMvc) | 11 | 0 | 0 | 0 |
| `administracion` | `ModeracionIntegracionTest` | Integración (MariaDB + MockMvc) | 9 | 0 | 0 | 0 |
| `administracion` | `ModeracionServiceTest` | Unitaria (Mockito) | 6 | 0 | 0 | 0 |
| `administracion` | `PanelServiceTest` | Unitaria (Mockito) | 2 | 0 | 0 | 0 |
| `administracion` | `ReclamoServiceTest` | Unitaria (Mockito) | 4 | 0 | 0 | 0 |
| `administracion` | `UsuarioAdminServiceTest` | Unitaria (Mockito) | 4 | 0 | 0 | 0 |
| `catalogo` | `CatalogoIntegracionTest` | Integración (MariaDB + MockMvc) | 4 | 0 | 0 | 0 |
| `catalogo` | `CatalogoServiceTest` | Unitaria (Mockito) | 8 | 0 | 0 | 0 |
| `compartido` | `ManejadorGlobalExcepcionesTest` | Unitaria | 14 | 0 | 0 | 0 |
| `compartido` | `SeguridadUnitariaTest` | Unitaria | 2 | 0 | 0 | 0 |
| `mensajeria` | `ConversacionServiceTest` | Unitaria (Mockito) | 6 | 0 | 0 | 0 |
| `mensajeria` | `MensajeriaIntegracionTest` | Integración (MariaDB + MockMvc) | 8 | 0 | 0 | 0 |
| `notificacion` | `AvisosIntegracionTest` | Integración (MariaDB + MockMvc) | 2 | 0 | 0 | 0 |
| `notificacion` | `NotificacionServiceTest` | Unitaria (Mockito) | 2 | 0 | 0 | 0 |
| `precio` | `PrecioIntegracionTest` | Integración (MariaDB + MockMvc) | 7 | 0 | 0 | 0 |
| `precio` | `ReferenciaPrecioServiceTest` | Unitaria (Mockito) | 4 | 0 | 0 | 0 |
| `publicacion` | `BusquedaIntegracionTest` | Integración (MariaDB + MockMvc) | 8 | 0 | 0 | 0 |
| `publicacion` | `BusquedaServiceTest` | Unitaria (Mockito) | 5 | 0 | 0 | 0 |
| `publicacion` | `MisCosechasIntegracionTest` | Integración (MariaDB + MockMvc) | 6 | 0 | 0 | 0 |
| `publicacion` | `MisCosechasServiceTest` | Unitaria (Mockito) | 4 | 0 | 0 | 0 |
| `publicacion` | `PublicacionIntegracionTest` | Integración (MariaDB + MockMvc) | 13 | 0 | 0 | 0 |
| `publicacion` | `PublicacionServiceTest` | Unitaria (Mockito) | 11 | 0 | 0 | 0 |
| `usuario` | `AuthIntegracionTest` | Integración (MariaDB + MockMvc) | 17 | 0 | 0 | 0 |
| `usuario` | `EmisorTokenTest` | Unitaria | 1 | 0 | 0 | 0 |
| `usuario` | `GestionCuentasServiceTest` | Unitaria (Mockito) | 4 | 0 | 0 | 0 |
| `usuario` | `InicializadorAdministradorTest` | Unitaria (Mockito) | 5 | 0 | 0 | 0 |
| `usuario` | `LimiteIntentosLoginIntegracionTest` | Integración (MariaDB + MockMvc) | 4 | 0 | 0 | 0 |
| `usuario` | `LimiteIntentosLoginTest` | Unitaria | 5 | 0 | 0 | 0 |
| `usuario` | `PerfilServiceTest` | Unitaria (Mockito) | 8 | 0 | 0 | 0 |
| `usuario` | `RegistroServiceTest` | Unitaria (Mockito) | 8 | 0 | 0 | 0 |
| `usuario` | `ReglasUsuarioTest` | Unitaria | 9 | 0 | 0 | 0 |
| `usuario` | `SesionServiceTest` | Unitaria (Mockito) | 6 | 0 | 0 | 0 |
| `usuario` | `TriggersUsuarioIntegracionTest` | Integración (MariaDB) | 3 | 0 | 0 | 0 |
| `usuario` | `UsuarioConsultaServiceTest` | Unitaria (Mockito) | 2 | 0 | 0 | 0 |
| `venta` | `CalificacionIntegracionTest` | Integración (MariaDB + MockMvc) | 3 | 0 | 0 | 0 |
| `venta` | `CalificacionServiceTest` | Unitaria (Mockito) | 4 | 0 | 0 | 0 |
| `venta` | `HistorialVentasServiceTest` | Unitaria (Mockito) | 2 | 0 | 0 | 0 |
| `venta` | `VentaIntegracionTest` | Integración (MariaDB + MockMvc) | 7 | 0 | 0 | 0 |
| `venta` | `VentaServiceTest` | Unitaria (Mockito) | 6 | 0 | 0 | 0 |
| `ventaagrupada` | `RepartoTest` | Unitaria | 3 | 0 | 0 | 0 |
| `ventaagrupada` | `VentaAgrupadaIntegracionTest` | Integración (MariaDB + MockMvc) | 6 | 0 | 0 | 0 |
| **Total** | | | **249** | 0 | 0 | 0 |

## Notas

- Un `@ParameterizedTest` cuenta un caso por cada conjunto de datos (por eso `ReglasUsuarioTest` tiene 9).
- `ArquitecturaTest` verifica las 5 reglas de CLAUDE.md §5.3 (todas pasan).
- `LimiteIntentosLoginIntegracionTest` se confirmó primero en rojo (commit `fd95723`, 4 de 4 fallaban con 401/200 en lugar de 429) y pasó a verde con la implementación (commit `28e0576`).
- La ejecución en GitHub Actions (CI) es **No verificado** desde este equipo: el repositorio es privado y no hay `gh` instalado.
