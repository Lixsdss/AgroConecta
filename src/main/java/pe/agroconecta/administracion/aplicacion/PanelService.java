package pe.agroconecta.administracion.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.administracion.dominio.ConversionPublicacion;
import pe.agroconecta.administracion.dominio.PanelAdmin;
import pe.agroconecta.administracion.dominio.ResumenUsuario;
import pe.agroconecta.administracion.dominio.VentaMensualCultivo;
import pe.agroconecta.administracion.infraestructura.ConversionPublicacionRepository;
import pe.agroconecta.administracion.infraestructura.PanelAdminRepository;
import pe.agroconecta.administracion.infraestructura.ResumenUsuarioRepository;
import pe.agroconecta.administracion.infraestructura.VentaMensualCultivoRepository;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.publicacion.aplicacion.PublicacionResumenDto;
import pe.agroconecta.publicacion.aplicacion.VistaModeracion;

/**
 * Panel del administrador (RF-09, UC-11 parte 4): tarjetas, cola, ventas del mes por cultivo, embudo y reputaciones,
 * todo leído de las vistas; y su Excel con Apache POI.
 */
@Service
@Transactional(readOnly = true)
public class PanelService {

    /** Publicaciones de la cola que muestra el panel. */
    static final int EN_COLA = 3;

    private final PanelAdminRepository panel;
    private final VentaMensualCultivoRepository ventasMensuales;
    private final ConversionPublicacionRepository conversiones;
    private final ResumenUsuarioRepository resumenes;
    private final ModeracionService moderacion;
    private final PublicacionConsultaService publicaciones;
    private final ExportadorExcelPanel excel;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param panel           vista {@code v_panel_admin}
     * @param ventasMensuales vista {@code v_ventas_mensuales_cultivo}
     * @param conversiones    vista {@code v_conversion_publicacion}
     * @param resumenes       vista {@code v_resumen_usuario}
     * @param moderacion      cola "Por revisar"
     * @param publicaciones   producto y agricultor de cada publicación del embudo
     * @param excel           generador del Excel
     * @param reloj           reloj de la aplicación
     */
    public PanelService(PanelAdminRepository panel, VentaMensualCultivoRepository ventasMensuales,
            ConversionPublicacionRepository conversiones, ResumenUsuarioRepository resumenes,
            ModeracionService moderacion, PublicacionConsultaService publicaciones, ExportadorExcelPanel excel,
            Clock reloj) {
        this.panel = panel;
        this.ventasMensuales = ventasMensuales;
        this.conversiones = conversiones;
        this.resumenes = resumenes;
        this.moderacion = moderacion;
        this.publicaciones = publicaciones;
        this.excel = excel;
        this.reloj = reloj;
    }

    /**
     * Arma el panel.
     *
     * @return tarjetas, cola, ventas del mes, embudo y reputaciones
     */
    public PanelDto obtener() {
        String mes = YearMonth.now(reloj).toString();
        List<ColaPanelDto> cola = moderacion.listar(VistaModeracion.POR_REVISAR, PageRequest.of(0, EN_COLA)).stream()
                .map(p -> new ColaPanelDto(p.id(), p.cultivo(), p.variedad(), p.cantidad(), p.unidadPlural(),
                        p.agricultor() == null ? null : p.agricultor().nombreCompleto(), p.distrito(),
                        p.marcaRevision(), p.fechaPublicacion()))
                .toList();
        List<ReputacionPanelDto> mejores = reputaciones(resumenes
                .findTop5ByTotalCalificacionesGreaterThanOrderByPromedioPuntuacionDescTotalCalificacionesDescIdAsc(0));
        Set<Long> idsMejores = mejores.stream().map(ReputacionPanelDto::id).collect(Collectors.toSet());
        List<ReputacionPanelDto> peores = reputaciones(resumenes
                .findTop5ByTotalCalificacionesGreaterThanOrderByPromedioPuntuacionAscTotalCalificacionesDescIdAsc(0))
                .stream().filter(r -> !idsMejores.contains(r.id())).toList();
        return new PanelDto(tarjetas(),
                cola,
                mes,
                ventasMensuales.findByIdMesOrderByKilosVendidosDescCultivoAsc(mes).stream()
                        .map(PanelService::aDto).toList(),
                embudo(conversiones
                        .findTop10ByContactosGreaterThanOrderByContactosDescOfertasDescIdPublicacionDesc(0)),
                mejores,
                peores);
    }

    /**
     * Excel con las hojas Resumen, Ventas por mes, Embudo y Usuarios.
     *
     * @return archivo {@code AgroConecta_Panel_{fecha}.xlsx}
     */
    public ArchivoPanelDto exportar() {
        List<VentaMensualDto> meses = ventasMensuales.findAll(Sort.by(Sort.Order.desc("id.mes"),
                Sort.Order.asc("cultivo"))).stream().map(PanelService::aDto).toList();
        List<EmbudoDto> embudo = embudo(conversiones
                .findByContactosGreaterThanOrderByContactosDescOfertasDescIdPublicacionDesc(0));
        List<UsuarioAdminDto> usuarios = resumenes.findAll(Sort.by(Sort.Order.asc("nombreCompleto"),
                Sort.Order.asc("id"))).stream().map(UsuarioAdminDto::de).toList();
        byte[] contenido = excel.generar(tarjetas(), meses, embudo, usuarios);
        return new ArchivoPanelDto("AgroConecta_Panel_%s.xlsx".formatted(LocalDate.now(reloj)), contenido);
    }

    private TarjetasPanelDto tarjetas() {
        PanelAdmin p = panel.findAll().getFirst();
        return new TarjetasPanelDto(p.getPublicacionesActivas(), p.getPublicadasUltimos7Dias(), p.getPorRevisar(),
                p.getPorRevisarMasAntigua(), p.getConformesHoy(), p.getBloqueadasHoy(), p.getPreciosCargadosHoy(),
                p.getPreciosEsperadosHoy(), p.getReclamosPendientes());
    }

    private List<EmbudoDto> embudo(List<ConversionPublicacion> filas) {
        Collection<Long> ids = filas.stream().map(ConversionPublicacion::getIdPublicacion).toList();
        Map<Long, PublicacionResumenDto> encontradas = publicaciones.buscarVarias(ids);
        return filas.stream().map(c -> {
            PublicacionResumenDto p = encontradas.get(c.getIdPublicacion());
            return new EmbudoDto(c.getIdPublicacion(), p == null ? null : producto(p.cultivo(), p.variedad()),
                    p == null ? null : p.agricultor(), c.getClicsWhatsapp().longValue(), c.getContactos(),
                    c.getOfertas(), c.getAcuerdos(), c.getCompletadas());
        }).toList();
    }

    private static List<ReputacionPanelDto> reputaciones(List<ResumenUsuario> filas) {
        return filas.stream().map(r -> new ReputacionPanelDto(r.getId(),
                r.getNombreNegocio() != null ? r.getNombreNegocio() : r.getNombreCompleto(), r.getRol(),
                r.getPromedioPuntuacion(), r.getTotalCalificaciones())).toList();
    }

    private static String producto(String cultivo, String variedad) {
        return "Común".equals(variedad) ? cultivo + " común" : cultivo + " " + variedad;
    }

    private static VentaMensualDto aDto(VentaMensualCultivo v) {
        return new VentaMensualDto(v.getId().mes(), v.getCultivo(), v.getVentasCompletadas(), v.getKilosVendidos(),
                v.getMontoTotal());
    }

}
