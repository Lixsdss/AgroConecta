package pe.agroconecta.precio.api;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.precio.aplicacion.CargaPrecioService;
import pe.agroconecta.precio.aplicacion.InformeImportacionDto;
import pe.agroconecta.precio.aplicacion.PrecioRegistradoDto;
import pe.agroconecta.precio.aplicacion.PrecioService;
import pe.agroconecta.precio.aplicacion.PrecioVigenteDto;
import pe.agroconecta.precio.aplicacion.PuntoHistorialDto;
import pe.agroconecta.precio.aplicacion.ReferenciaPrecioDto;
import pe.agroconecta.precio.aplicacion.ReferenciaPrecioService;
import pe.agroconecta.precio.dominio.FuentePrecio;

/**
 * Precios de referencia ({@code /api/v1/precios}, RF-02). Las consultas son públicas; la carga y la edición, solo
 * para administradores.
 */
@RestController
@RequestMapping("/api/v1/precios")
public class PrecioController {

    private final PrecioService consultas;
    private final ReferenciaPrecioService referencias;
    private final CargaPrecioService carga;

    /**
     * Crea el controlador.
     *
     * @param consultas   tablero e historial
     * @param referencias referencia de precio
     * @param carga       carga y edición de precios
     */
    public PrecioController(PrecioService consultas, ReferenciaPrecioService referencias, CargaPrecioService carga) {
        this.consultas = consultas;
        this.referencias = referencias;
        this.carga = carga;
    }

    /**
     * Precios vigentes ("Precios de hoy").
     *
     * @param idCultivo  id del cultivo (opcional)
     * @param idDistrito id de la zona (opcional)
     * @return precios vigentes
     */
    @GetMapping("/vigentes")
    public List<PrecioVigenteDto> vigentes(@RequestParam(required = false) Integer idCultivo,
            @RequestParam(required = false) Integer idDistrito) {
        return consultas.vigentes(idCultivo, idDistrito);
    }

    /**
     * Serie de precios para el gráfico.
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id de la zona
     * @param dias       días hacia atrás (1 a 365)
     * @return puntos de la serie
     */
    @GetMapping("/historial")
    public List<PuntoHistorialDto> historial(@RequestParam int idCultivo, @RequestParam int idDistrito,
            @RequestParam(defaultValue = "30")
            @Min(value = 1, message = "Pida al menos 1 día.")
            @Max(value = 365, message = "Pida como máximo 365 días.") int dias) {
        return consultas.historial(idCultivo, idDistrito, dias);
    }

    /**
     * Referencia de precio para el paso 3 de "Vender".
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id del distrito de la cosecha
     * @return 200 con la referencia, o 204 si no hay precios recientes
     */
    @GetMapping("/referencia")
    public ResponseEntity<ReferenciaPrecioDto> referencia(@RequestParam int idCultivo, @RequestParam int idDistrito) {
        return referencias.referencia(idCultivo, idDistrito).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Carga el precio del día por formulario.
     *
     * @param jwt     token del administrador
     * @param request cultivo, zona, fecha, precio y fuente
     * @return 201 con el precio guardado
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PrecioRegistradoDto> registrar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegistrarPrecioRequest request) {
        PrecioRegistradoDto guardado = carga.registrar(Long.parseLong(jwt.getSubject()), request.idCultivo(),
                request.idDistrito(), request.fecha(), request.precioKg(), request.fuente());
        return ResponseEntity.created(URI.create("/api/v1/precios/" + guardado.id())).body(guardado);
    }

    /**
     * Corrige un precio.
     *
     * @param id      id del precio
     * @param jwt     token del administrador
     * @param request precio y fuente
     * @return precio actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PrecioRegistradoDto editar(@PathVariable long id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EditarPrecioRequest request) {
        return carga.editar(Long.parseLong(jwt.getSubject()), id, request.precioKg(), request.fuente());
    }

    /**
     * Importa un archivo Excel o CSV de precios.
     *
     * @param jwt     token del administrador
     * @param archivo archivo {@code .xlsx} o {@code .csv}
     * @param fuente  fuente de todos los precios del archivo
     * @return filas importadas y rechazadas
     * @throws IOException si no se puede leer el archivo recibido
     */
    @PostMapping(path = "/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public InformeImportacionDto importar(@AuthenticationPrincipal Jwt jwt, @RequestParam("archivo") MultipartFile archivo,
            @RequestParam FuentePrecio fuente) throws IOException {
        if (archivo.isEmpty()) {
            throw new SolicitudInvalidaException("El archivo está vacío.");
        }
        return carga.importar(Long.parseLong(jwt.getSubject()), archivo.getOriginalFilename(), archivo.getBytes(),
                fuente);
    }
}
