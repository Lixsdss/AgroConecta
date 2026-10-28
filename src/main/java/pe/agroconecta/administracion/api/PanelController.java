package pe.agroconecta.administracion.api;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.administracion.aplicacion.ArchivoPanelDto;
import pe.agroconecta.administracion.aplicacion.PanelDto;
import pe.agroconecta.administracion.aplicacion.PanelService;

/**
 * Panel del administrador ({@code /api/v1/admin/panel}, RF-09, UC-11 parte 4).
 */
@RestController
@RequestMapping("/api/v1/admin/panel")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PanelController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PanelService panel;

    /**
     * Crea el controlador.
     *
     * @param panel servicio del panel
     */
    public PanelController(PanelService panel) {
        this.panel = panel;
    }

    /**
     * Datos del panel.
     *
     * @return tarjetas, cola, ventas del mes, embudo y reputaciones
     */
    @GetMapping
    public PanelDto obtener() {
        return panel.obtener();
    }

    /**
     * Excel del panel.
     *
     * @return archivo {@code AgroConecta_Panel_{fecha}.xlsx}
     */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar() {
        ArchivoPanelDto archivo = panel.exportar();
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .body(archivo.contenido());
    }
}
