package pe.agroconecta.administracion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.administracion.dominio.ConversionPublicacion;
import pe.agroconecta.administracion.dominio.PanelAdmin;
import pe.agroconecta.administracion.dominio.ResumenUsuario;
import pe.agroconecta.administracion.dominio.VentaMensualCultivo;
import pe.agroconecta.administracion.dominio.VentaMensualCultivoId;
import pe.agroconecta.administracion.infraestructura.ConversionPublicacionRepository;
import pe.agroconecta.administracion.infraestructura.PanelAdminRepository;
import pe.agroconecta.administracion.infraestructura.ResumenUsuarioRepository;
import pe.agroconecta.administracion.infraestructura.VentaMensualCultivoRepository;
import pe.agroconecta.publicacion.aplicacion.PublicacionConsultaService;
import pe.agroconecta.publicacion.aplicacion.PublicacionResumenDto;
import pe.agroconecta.publicacion.aplicacion.VistaModeracion;

@ExtendWith(MockitoExtension.class)
class PanelServiceTest {

    @Mock private PanelAdminRepository panel;
    @Mock private VentaMensualCultivoRepository ventasMensuales;
    @Mock private ConversionPublicacionRepository conversiones;
    @Mock private ResumenUsuarioRepository resumenes;
    @Mock private ModeracionService moderacion;
    @Mock private PublicacionConsultaService publicaciones;

    private PanelService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-14T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new PanelService(panel, ventasMensuales, conversiones, resumenes, moderacion, publicaciones,
                new ExportadorExcelPanel(), reloj);
        PanelAdmin fila = BeanUtils.instantiateClass(PanelAdmin.class);
        for (String campo : List.of("publicacionesActivas", "publicadasUltimos7Dias", "porRevisar", "conformesHoy",
                "bloqueadasHoy", "preciosCargadosHoy", "preciosEsperadosHoy", "reclamosPendientes")) {
            ReflectionTestUtils.setField(fila, campo, 2L);
        }
        when(panel.findAll()).thenReturn(List.of(fila));
    }

    @Test
    void armaElPanelSinRepetirLasMejoresReputacionesEntreLasPeores() {
        when(moderacion.listar(any(VistaModeracion.class), any(Pageable.class))).thenReturn(Page.empty());
        when(ventasMensuales.findByIdMesOrderByKilosVendidosDescCultivoAsc("2026-10")).thenReturn(List.of(venta()));
        when(conversiones.findTop10ByContactosGreaterThanOrderByContactosDescOfertasDescIdPublicacionDesc(0))
                .thenReturn(List.of(conversion()));
        when(publicaciones.buscarVarias(anyCollection())).thenReturn(Map.of(15L, publicacion()));
        ResumenUsuario juan = usuario(3L, "4.5");
        ResumenUsuario rosa = usuario(7L, "2.0");
        when(resumenes.findTop5ByTotalCalificacionesGreaterThanOrderByPromedioPuntuacionDescTotalCalificacionesDescIdAsc(0))
                .thenReturn(List.of(juan, rosa));
        when(resumenes.findTop5ByTotalCalificacionesGreaterThanOrderByPromedioPuntuacionAscTotalCalificacionesDescIdAsc(0))
                .thenReturn(List.of(rosa, juan));

        PanelDto dto = servicio.obtener();

        assertThat(dto.mes()).isEqualTo("2026-10");
        assertThat(dto.tarjetas().porRevisar()).isEqualTo(2);
        assertThat(dto.kilosPorCultivo()).singleElement().extracting(VentaMensualDto::cultivo).isEqualTo("Mango");
        assertThat(dto.embudo()).singleElement().satisfies(e -> {
            assertThat(e.producto()).isEqualTo("Mango Kent");
            assertThat(e.clicsWhatsapp()).isEqualTo(2);
        });
        assertThat(dto.mejorReputacion()).hasSize(2);
        assertThat(dto.peorReputacion()).isEmpty();
    }

    @Test
    void exportaUnExcelConFechaEnElNombre() {
        when(ventasMensuales.findAll(any(Sort.class))).thenReturn(List.of(venta()));
        when(conversiones.findByContactosGreaterThanOrderByContactosDescOfertasDescIdPublicacionDesc(0))
                .thenReturn(List.of(conversion()));
        when(publicaciones.buscarVarias(anyCollection())).thenReturn(Map.of());
        when(resumenes.findAll(any(Sort.class))).thenReturn(List.of(usuario(3L, "4.5")));

        ArchivoPanelDto archivo = servicio.exportar();

        assertThat(archivo.nombre()).isEqualTo("AgroConecta_Panel_2026-10-14.xlsx");
        assertThat(archivo.contenido()).isNotEmpty();
    }

    private static VentaMensualCultivo venta() {
        VentaMensualCultivo v = BeanUtils.instantiateClass(VentaMensualCultivo.class);
        ReflectionTestUtils.setField(v, "id", new VentaMensualCultivoId("2026-10", 1));
        ReflectionTestUtils.setField(v, "cultivo", "Mango");
        ReflectionTestUtils.setField(v, "ventasCompletadas", 1L);
        ReflectionTestUtils.setField(v, "kilosVendidos", new BigDecimal("100"));
        ReflectionTestUtils.setField(v, "montoTotal", new BigDecimal("140"));
        return v;
    }

    private static ConversionPublicacion conversion() {
        ConversionPublicacion c = BeanUtils.instantiateClass(ConversionPublicacion.class);
        ReflectionTestUtils.setField(c, "idPublicacion", 15L);
        ReflectionTestUtils.setField(c, "clicsWhatsapp", new BigDecimal("2"));
        for (String campo : List.of("contactos", "ofertas", "acuerdos", "completadas")) {
            ReflectionTestUtils.setField(c, campo, 1L);
        }
        return c;
    }

    private static PublicacionResumenDto publicacion() {
        return new PublicacionResumenDto(15L, 3L, "Juan Chunga", "Mango", "Kent", "Tambogrande", "PUBLICADA", "jaba",
                "jabas", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, 5, 2);
    }

    private static ResumenUsuario usuario(long id, String promedio) {
        ResumenUsuario r = BeanUtils.instantiateClass(ResumenUsuario.class);
        ReflectionTestUtils.setField(r, "id", id);
        ReflectionTestUtils.setField(r, "rol", "AGRICULTOR");
        ReflectionTestUtils.setField(r, "estado", "ACTIVO");
        ReflectionTestUtils.setField(r, "nombreCompleto", "Usuario " + id);
        ReflectionTestUtils.setField(r, "promedioPuntuacion", new BigDecimal(promedio));
        ReflectionTestUtils.setField(r, "totalCalificaciones", 1L);
        return r;
    }
}
