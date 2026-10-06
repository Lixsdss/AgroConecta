package pe.agroconecta.publicacion.api;

import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
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

import pe.agroconecta.publicacion.aplicacion.BusquedaService;
import pe.agroconecta.publicacion.aplicacion.FiltroBusquedaDto;
import pe.agroconecta.publicacion.aplicacion.OrdenBusqueda;
import pe.agroconecta.publicacion.aplicacion.PublicacionDisponibleDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionService;

/**
 * Publicaciones de cosechas ({@code /api/v1/publicaciones}): publicar (RF-01), buscar (RF-03) y ver el detalle.
 */
@RestController
@RequestMapping("/api/v1/publicaciones")
public class PublicacionController {

    private final PublicacionService publicaciones;
    private final BusquedaService busqueda;

    /**
     * Crea el controlador.
     *
     * @param publicaciones servicio de publicaciones
     * @param busqueda      búsqueda multicriterio
     */
    public PublicacionController(PublicacionService publicaciones, BusquedaService busqueda) {
        this.publicaciones = publicaciones;
        this.busqueda = busqueda;
    }

    /**
     * Busca publicaciones disponibles (RF-03, UC-03). Público. Todos los filtros son opcionales y se combinan;
     * siempre 20 resultados por página.
     *
     * @param idCultivo    cultivo (el filtro "Categoría")
     * @param idVariedad   variedad
     * @param idProvincia  provincia
     * @param idDistrito   distrito
     * @param idUnidad     unidad de venta
     * @param cosechaDesde fecha de cosecha mínima
     * @param cosechaHasta fecha de cosecha máxima
     * @param orden        orden de los resultados (por defecto, precio por kilo)
     * @param page         número de página desde 0
     * @return página de publicaciones disponibles
     */
    @GetMapping
    public PagedModel<PublicacionDisponibleDto> buscar(@RequestParam(required = false) Integer idCultivo,
            @RequestParam(required = false) Integer idVariedad, @RequestParam(required = false) Integer idProvincia,
            @RequestParam(required = false) Integer idDistrito, @RequestParam(required = false) Integer idUnidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cosechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cosechaHasta,
            @RequestParam(defaultValue = "PRECIO_KG") OrdenBusqueda orden,
            @RequestParam(defaultValue = "0") int page) {
        FiltroBusquedaDto filtro = new FiltroBusquedaDto(idCultivo, idVariedad, idProvincia, idDistrito, idUnidad,
                cosechaDesde, cosechaHasta);
        return new PagedModel<>(busqueda.buscar(filtro, orden, page));
    }

    /**
     * Publica una cosecha del agricultor de la sesión.
     *
     * @param jwt     token de la sesión
     * @param request datos de los 3 pasos
     * @return 201 con la publicación y {@code Location}
     */
    @PostMapping
    @PreAuthorize("hasRole('AGRICULTOR')")
    public ResponseEntity<PublicacionDto> publicar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PublicarRequest request) {
        PublicacionDto creada = publicaciones.publicar(Long.parseLong(jwt.getSubject()), request.aDto());
        return ResponseEntity.created(URI.create("/api/v1/publicaciones/" + creada.id())).body(creada);
    }

    /**
     * Devuelve una publicación PUBLICADA, o de cualquier estado si la pide su dueño.
     *
     * @param id  id de la publicación
     * @param jwt token de la sesión, o {@code null} si no inició sesión
     * @return publicación
     */
    @GetMapping("/{id}")
    public PublicacionDto obtener(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return publicaciones.obtener(id, jwt == null ? null : Long.valueOf(jwt.getSubject()));
    }
}
