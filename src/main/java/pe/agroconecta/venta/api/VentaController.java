package pe.agroconecta.venta.api;

import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.venta.aplicacion.ArchivoDto;
import pe.agroconecta.venta.aplicacion.FiltroVentasDto;
import pe.agroconecta.venta.aplicacion.HistorialVentasService;
import pe.agroconecta.venta.aplicacion.VentaDetalleDto;
import pe.agroconecta.venta.aplicacion.VentaResumenDto;
import pe.agroconecta.venta.aplicacion.VentaService;

/**
 * Ventas ({@code /api/v1/ventas}, RF-06 y RF-11): acuerdo, entrega, historial y Excel. Quien no es parte de una
 * venta recibe 403.
 */
@RestController
@RequestMapping("/api/v1/ventas")
@PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
public class VentaController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final VentaService ventas;
    private final HistorialVentasService historial;

    /**
     * Crea el controlador.
     *
     * @param ventas    acuerdo y cambios de estado
     * @param historial historial y Excel
     */
    public VentaController(VentaService ventas, HistorialVentasService historial) {
        this.ventas = ventas;
        this.historial = historial;
    }

    /**
     * "Registrar acuerdo": la venta nace ACORDADA.
     *
     * @param jwt     token de la sesión
     * @param request términos del acuerdo
     * @return 201 con {@code Location} y la venta
     */
    @PostMapping
    public ResponseEntity<VentaDetalleDto> registrar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegistrarVentaRequest request) {
        VentaDetalleDto venta = ventas.registrar(usuario(jwt), request.aDto());
        return ResponseEntity.created(URI.create("/api/v1/ventas/" + venta.id())).body(venta);
    }

    /**
     * Cambia el estado de la venta ("Ya recogieron", "Va en camino", "Confirmar que recibí" o "Cancelar venta").
     *
     * @param jwt     token de la sesión
     * @param id      id de la venta
     * @param request acción pedida
     * @return la venta actualizada
     */
    @PostMapping("/{id}/estado")
    public VentaDetalleDto cambiarEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return ventas.cambiarEstado(usuario(jwt), id, request.accion());
    }

    /**
     * Detalle con la línea de tiempo y las acciones posibles.
     *
     * @param jwt token de la sesión
     * @param id  id de la venta
     * @return la venta
     */
    @GetMapping("/{id}")
    public VentaDetalleDto obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return ventas.obtener(usuario(jwt), id);
    }

    /**
     * "Mis ventas" o "Mis compras", la más reciente primero (20 por página).
     *
     * @param jwt            token de la sesión
     * @param grupo          {@code EN_CURSO} o {@code TERMINADAS}
     * @param desde          primer día del acuerdo
     * @param hasta          último día del acuerdo
     * @param contraparte    texto del nombre de la otra parte
     * @param idConversacion solo las ventas de esa conversación
     * @param page           página desde 0
     * @return página de ventas
     */
    @GetMapping
    public PagedModel<VentaResumenDto> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String contraparte,
            @RequestParam(required = false) Long idConversacion,
            @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(historial.listar(usuario(jwt),
                new FiltroVentasDto(grupo, desde, hasta, contraparte, idConversacion), page));
    }

    /**
     * Excel con las ventas del filtro (Apache POI).
     *
     * @param jwt         token de la sesión
     * @param grupo       {@code EN_CURSO} o {@code TERMINADAS}
     * @param desde       primer día del acuerdo
     * @param hasta       último día del acuerdo
     * @param contraparte texto del nombre de la otra parte
     * @return archivo {@code .xlsx} como descarga
     */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String contraparte) {
        ArchivoDto archivo = historial.exportar(usuario(jwt),
                new FiltroVentasDto(grupo, desde, hasta, contraparte, null));
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .body(archivo.contenido());
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
