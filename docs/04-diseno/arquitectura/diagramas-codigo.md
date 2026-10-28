# Diagramas del código (APF3)

Tres diagramas Mermaid obtenidos del código de `src/main/java` en la auditoría del APF3 (24/10/2026; ver `docs/academico/apf3-auditoria.md`, sección 7). Complementan la vista general y la matriz de dependencias de [arquitectura.md](arquitectura.md). Las dependencias entre módulos salen de los `import pe.agroconecta.<modulo>.` reales; el módulo `precio` (RF-02) se usa como ejemplo porque recorre todas las capas, tiene vistas `@Immutable` y usa Apache POI, Guava y Commons Lang3.

GitHub muestra los bloques `mermaid` como diagramas. Para el informe se pueden exportar a PNG o SVG con [Mermaid Live Editor](https://mermaid.live) o con `mmdc` (mermaid-cli).

## 1. Estructura de módulos y paquetes

Cliente estático, `compartido` (seguridad, errores, configuración), los nueve módulos de negocio con sus dependencias (siempre hacia la capa `aplicacion` del otro módulo, verificado por `ArquitecturaTest`) y la base de datos.

```mermaid
flowchart TD
    subgraph cliente["static/ (HTML + Bootstrap + JS)"]
        UI["pages/&lt;modulo&gt;/*.html<br/>js/&lt;modulo&gt;/*.js<br/>js/api.js"]
    end
    UI -->|"HTTP /api/v1 + JWT"| SEG

    subgraph compartido["pe.agroconecta.compartido"]
        SEG["seguridad<br/>(SeguridadConfig, JwtConfig,<br/>ValidadorUsuarioActivo)"]
        ERR["error<br/>(ManejadorGlobalExcepciones)"]
        CFG["config<br/>(OpenApi, Cors, Reloj)"]
    end

    catalogo
    usuario --> catalogo
    publicacion --> catalogo
    publicacion --> precio
    precio --> catalogo
    mensajeria --> publicacion
    mensajeria --> usuario
    mensajeria --> notificacion
    venta --> catalogo
    venta --> publicacion
    venta --> usuario
    venta --> mensajeria
    venta --> notificacion
    ventaagrupada --> catalogo
    ventaagrupada --> publicacion
    ventaagrupada --> usuario
    ventaagrupada --> mensajeria
    ventaagrupada --> notificacion
    ventaagrupada --> venta
    administracion --> publicacion
    administracion --> usuario
    administracion --> venta
    administracion --> notificacion
    notificacion
    usuario -. "implementa VerificadorEstadoUsuario" .-> SEG

    catalogo & usuario & publicacion & precio & mensajeria & venta & ventaagrupada & notificacion & administracion --> DB[("MariaDB / MySQL<br/>Flyway V1–V3, triggers,<br/>vistas, ev_tareas_diarias")]
```

## 2. Capas del módulo `precio`

`api` → `aplicacion` → `infraestructura` → base de datos; `dominio` mapea solo las tablas y vistas propias del módulo. `publicacion` usa `ReferenciaPrecioService` para la marca PRECIO_FUERA_RANGO (RF-01).

```mermaid
flowchart LR
    subgraph api["precio.api"]
        PC[PrecioController]
        RPR[RegistrarPrecioRequest]
        EPR[EditarPrecioRequest]
    end
    subgraph aplicacion["precio.aplicacion"]
        PS[PrecioService]
        RS[ReferenciaPrecioService]
        CS[CargaPrecioService]
        DTO["PrecioVigenteDto, PuntoHistorialDto,<br/>ReferenciaPrecioDto, PrecioRegistradoDto,<br/>InformeImportacionDto, EvaluacionPrecio"]
    end
    subgraph infraestructura["precio.infraestructura"]
        PMR[PrecioMercadoRepository]
        PVR[PrecioVigenteRepository]
        RPPR[RangoPrecioPlataformaRepository]
        LAP["LectorArchivoPrecios<br/>(Apache POI / CSV)"]
    end
    subgraph dominio["precio.dominio"]
        PM["PrecioMercado → precios_mercado"]
        PV["PrecioVigente (@Immutable) → v_precios_vigentes"]
        RPP["RangoPrecioPlataforma (@Immutable) → v_rango_precio_plataforma"]
    end
    PC --> PS & RS & CS
    PS --> PVR & PMR
    RS --> PVR & RPPR
    CS --> PMR & LAP
    CS --> CAT["catalogo.aplicacion.CatalogoService"]
    RS --> CAT
    PUB["publicacion.aplicacion.PublicacionService"] --> RS
    PMR --> PM
    PVR --> PV
    RPPR --> RPP
```

## 3. Diagrama de clases del módulo `precio`

```mermaid
classDiagram
    class PrecioController {
        -PrecioService consultas
        -ReferenciaPrecioService referencias
        -CargaPrecioService carga
        +vigentes(Integer idCultivo, Integer idDistrito) List~PrecioVigenteDto~
        +historial(int idCultivo, int idDistrito, int dias) List~PuntoHistorialDto~
        +referencia(int idCultivo, int idDistrito) ResponseEntity~ReferenciaPrecioDto~
        +registrar(Jwt jwt, RegistrarPrecioRequest) ResponseEntity~PrecioRegistradoDto~
        +editar(long id, Jwt jwt, EditarPrecioRequest) PrecioRegistradoDto
        +importar(Jwt jwt, MultipartFile archivo) InformeImportacionDto
    }
    class PrecioService {
        -PrecioVigenteRepository vigentes
        -PrecioMercadoRepository precios
        -Clock reloj
        +vigentes(Integer idCultivo, Integer idDistrito) List~PrecioVigenteDto~
        +historial(int idCultivo, int idDistrito, int dias) List~PuntoHistorialDto~
    }
    class ReferenciaPrecioService {
        -PrecioVigenteRepository vigentes
        -RangoPrecioPlataformaRepository rangos
        -CatalogoService catalogo
        -Clock reloj
        +referencia(int idCultivo, int idDistrito) Optional~ReferenciaPrecioDto~
        +evaluar(int idCultivo, int idDistrito, BigDecimal precioKg) EvaluacionPrecio
    }
    class CargaPrecioService {
        -PrecioMercadoRepository precios
        -CatalogoService catalogo
        -LectorArchivoPrecios lector
        -Clock reloj
        +registrar(long idAdministrador, Integer idCultivo, Integer idDistrito, LocalDate fecha, ...) PrecioRegistradoDto
        +editar(long idAdministrador, long idPrecio, BigDecimal precioKg, FuentePrecio fuente) PrecioRegistradoDto
        +importar(long idAdministrador, String nombreArchivo, byte[] contenido, ...) InformeImportacionDto
    }
    class LectorArchivoPrecios {
        +List~String~ COLUMNAS$
        +int MAXIMO_FILAS$
        +leer(String nombreArchivo, byte[] contenido) List~FilaArchivoPrecio~
    }
    class PrecioMercadoRepository {
        <<interface>>
    }
    class PrecioVigenteRepository {
        <<interface>>
    }
    class RangoPrecioPlataformaRepository {
        <<interface>>
    }
    class JpaRepository~T,ID~ {
        <<interface>>
    }
    class PrecioMercado {
        -Long id
        -Integer idCultivo
        -Integer idDistrito
        -LocalDate fecha
        -BigDecimal precioKg
        -FuentePrecio fuente
        -MetodoCarga metodo
        -Long idAdministrador
        +editar(BigDecimal nuevoPrecioKg, FuentePrecio nuevaFuente, Long idAdministrador)
    }
    class PrecioVigente {
        <<Immutable>>
        +getPrecioKg() BigDecimal
        +getPrecioUnidadLocal() BigDecimal
        +getTendencia() String
    }
    class RangoPrecioPlataforma {
        <<Immutable>>
        +getMinimoKg() BigDecimal
        +getMaximoKg() BigDecimal
        +getPromedioKg() BigDecimal
        +getVentas() Long
    }
    class ClaveCultivoDistrito
    class FuentePrecio {
        <<enumeration>>
    }
    class MetodoCarga {
        <<enumeration>>
    }
    class EvaluacionPrecio {
        <<enumeration>>
        +fueraDeRango() boolean
    }
    class FilaArchivoPrecio {
        <<record>>
        int numero
        String cultivo
        String zona
        String fecha
        String precioKg
    }
    class CatalogoService

    PrecioController --> PrecioService
    PrecioController --> ReferenciaPrecioService
    PrecioController --> CargaPrecioService
    PrecioService --> PrecioVigenteRepository
    PrecioService --> PrecioMercadoRepository
    ReferenciaPrecioService --> PrecioVigenteRepository
    ReferenciaPrecioService --> RangoPrecioPlataformaRepository
    ReferenciaPrecioService --> CatalogoService
    ReferenciaPrecioService ..> EvaluacionPrecio
    CargaPrecioService --> PrecioMercadoRepository
    CargaPrecioService --> LectorArchivoPrecios
    CargaPrecioService --> CatalogoService
    LectorArchivoPrecios ..> FilaArchivoPrecio
    PrecioMercadoRepository --|> JpaRepository
    PrecioVigenteRepository --|> JpaRepository
    RangoPrecioPlataformaRepository --|> JpaRepository
    PrecioMercadoRepository ..> PrecioMercado
    PrecioVigenteRepository ..> PrecioVigente
    RangoPrecioPlataformaRepository ..> RangoPrecioPlataforma
    PrecioVigente ..> ClaveCultivoDistrito : id
    PrecioMercado --> FuentePrecio
    PrecioMercado --> MetodoCarga
```
