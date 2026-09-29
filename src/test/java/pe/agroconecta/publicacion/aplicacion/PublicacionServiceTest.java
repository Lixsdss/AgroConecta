package pe.agroconecta.publicacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.catalogo.aplicacion.VariedadDto;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.precio.aplicacion.EvaluacionPrecio;
import pe.agroconecta.precio.aplicacion.ReferenciaPrecioService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import pe.agroconecta.publicacion.dominio.EstadoPublicacion;
import pe.agroconecta.publicacion.dominio.MarcaRevision;
import pe.agroconecta.publicacion.dominio.Publicacion;
import pe.agroconecta.publicacion.infraestructura.EstadisticaCantidad;
import pe.agroconecta.publicacion.infraestructura.PublicacionRepository;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 27);
    private static final VariedadDto KENT = new VariedadDto(1, "Kent", 1, "Mango", 14);
    private static final UnidadDto JABA = new UnidadDto(2, "Jaba", "jabas", null);
    private static final UnidadDto QUINTAL = new UnidadDto(4, "Quintal", "quintales", new BigDecimal("46.000"));
    private static final DistritoDto TAMBOGRANDE = new DistritoDto(9, "Tambogrande", 1, "Piura", true);

    @Mock private PublicacionRepository publicaciones;
    @Mock private CatalogoService catalogo;
    @Mock private EntityManager entityManager;
    @Mock private ReferenciaPrecioService referencias;

    private PublicacionService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-09-27T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new PublicacionService(publicaciones, catalogo, referencias, entityManager, reloj);
        lenient().when(referencias.evaluar(anyInt(), anyInt(), any())).thenReturn(EvaluacionPrecio.SIN_REFERENCIA);
    }

    @Test
    void publicaEnJabasConElPesoIndicadoYRefrescaLoQueCalculaLaBaseDeDatos() {
        catalogoCon(JABA);
        when(publicaciones.saveAndFlush(any(Publicacion.class))).thenAnswer(i -> conId(i.getArgument(0), 15L));

        PublicacionDto creada = servicio.publicar(3L, datos(2, "20", null, HOY));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicaciones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getPesoUnidadKg()).isEqualByComparingTo("20");
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoPublicacion.PUBLICADA);
        assertThat(guardada.getValue().getFechaLimite()).isNull();
        verify(entityManager).refresh(guardada.getValue());
        assertThat(creada.id()).isEqualTo(15L);
        assertThat(creada.cultivo()).isEqualTo("Mango");
        assertThat(creada.unidadPlural()).isEqualTo("jabas");
        assertThat(creada.distrito()).isEqualTo("Tambogrande");
    }

    @Test
    void enUnidadesDePesoFijoUsaElFactorDelCatalogo() {
        catalogoCon(QUINTAL);
        when(publicaciones.saveAndFlush(any(Publicacion.class))).thenAnswer(i -> conId(i.getArgument(0), 16L));

        servicio.publicar(3L, datos(4, "50", null, HOY));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicaciones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getPesoUnidadKg()).isEqualByComparingTo("46");
    }

    @Test
    void exigeElPesoEnJabaYSaco() {
        catalogoCon(JABA);

        assertThatThrownBy(() -> servicio.publicar(3L, datos(2, null, null, HOY)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(PublicacionService.MENSAJE_PESO);
        verify(publicaciones, never()).saveAndFlush(any());
    }

    @Test
    void rechazaUnaFechaLimitePasadaOAnteriorALaCosecha() {
        catalogoCon(JABA);

        assertThatThrownBy(() -> servicio.publicar(3L, datos(2, "20", HOY.minusDays(1), HOY.minusDays(3))))
                .hasMessage(PublicacionService.MENSAJE_LIMITE_PASADO);
        assertThatThrownBy(() -> servicio.publicar(3L, datos(2, "20", HOY, HOY.plusDays(1))))
                .hasMessage(PublicacionService.MENSAJE_LIMITE_ANTES_COSECHA);
    }

    @Test
    void rechazaUnaCosechaQueYaPasoSuVidaUtil() {
        catalogoCon(JABA);

        assertThatThrownBy(() -> servicio.publicar(3L, datos(2, "20", null, HOY.minusDays(15))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(PublicacionService.MENSAJE_COSECHA_VENCIDA);
    }

    @Test
    void rechazaDatosDeCatalogoInexistentes() {
        when(catalogo.buscarVariedad(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.publicar(3L, datos(2, "20", null, HOY)))
                .hasMessage(PublicacionService.MENSAJE_VARIEDAD);

        when(catalogo.buscarVariedad(1)).thenReturn(Optional.of(KENT));
        when(catalogo.buscarUnidad(2)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.publicar(3L, datos(2, "20", null, HOY)))
                .hasMessage(PublicacionService.MENSAJE_UNIDAD);

        when(catalogo.buscarUnidad(2)).thenReturn(Optional.of(JABA));
        when(catalogo.buscarDistrito(9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.publicar(3L, datos(2, "20", null, HOY)))
                .hasMessage(PublicacionService.MENSAJE_DISTRITO);
    }

    @Test
    void unaPublicacionNoPublicadaSoloLaVeSuDueno() {
        Publicacion bloqueada = conId(new Publicacion(3L, 1, 9, 2, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
                HOY, HOY.plusDays(14)), 15L);
        ReflectionTestUtils.setField(bloqueada, "estado", EstadoPublicacion.BLOQUEADA);
        when(publicaciones.findById(15L)).thenReturn(Optional.of(bloqueada));
        catalogoCon(JABA);

        assertThat(servicio.obtener(15L, 3L).estado()).isEqualTo(EstadoPublicacion.BLOQUEADA);
        assertThatThrownBy(() -> servicio.obtener(15L, 7L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.obtener(15L, null)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void marcaCantidadInusualSiSuperaTresVecesElPromedioDeLaZona() {
        catalogoCon(JABA);
        when(catalogo.listarVariedades(1)).thenReturn(List.of(KENT));
        when(publicaciones.estadisticaCantidad(any(), any(), any())).thenReturn(new EstadisticaCantidad(3, 100.0));
        when(publicaciones.saveAndFlush(any(Publicacion.class))).thenAnswer(i -> conId(i.getArgument(0), 17L));

        servicio.publicar(3L, datos(2, "20", null, HOY));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicaciones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getMarcaRevision()).isEqualTo(MarcaRevision.CANTIDAD_INUSUAL);
    }

    @Test
    void elPrecioFueraDeRangoTienePrioridadSobreLaCantidadInusual() {
        catalogoCon(JABA);
        when(referencias.evaluar(1, 9, new BigDecimal("1.4000"))).thenReturn(EvaluacionPrecio.ALTO);
        when(publicaciones.saveAndFlush(any(Publicacion.class))).thenAnswer(i -> conId(i.getArgument(0), 19L));

        servicio.publicar(3L, datos(2, "20", null, HOY));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicaciones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getMarcaRevision()).isEqualTo(MarcaRevision.PRECIO_FUERA_RANGO);
        verify(publicaciones, never()).estadisticaCantidad(any(), any(), any());
    }

    @Test
    void noMarcaSinSuficientesPublicacionesParaComparar() {
        catalogoCon(JABA);
        when(catalogo.listarVariedades(1)).thenReturn(List.of(KENT));
        when(publicaciones.estadisticaCantidad(any(), any(), any())).thenReturn(new EstadisticaCantidad(2, 10.0));
        when(publicaciones.saveAndFlush(any(Publicacion.class))).thenAnswer(i -> conId(i.getArgument(0), 18L));

        servicio.publicar(3L, datos(2, "20", null, HOY));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicaciones).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getMarcaRevision()).isNull();
    }

    @Test
    void listaYObtieneParaModeracionConEstadoYMarcaComoTexto() {
        Publicacion p = conId(new Publicacion(3L, 1, 9, 2, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, HOY,
                HOY.plusDays(14)), 15L);
        p.marcarParaRevision(MarcaRevision.PRIMERA_PUBLICACION);
        PageRequest pagina = PageRequest.of(0, 20);
        when(publicaciones.buscarPorRevisar(pagina)).thenReturn(new PageImpl<>(List.of(p), pagina, 1));
        when(publicaciones.buscarOcultas(pagina)).thenReturn(new PageImpl<>(List.of(), pagina, 0));
        when(publicaciones.findById(15L)).thenReturn(Optional.of(p));
        when(catalogo.listarVariedades(null)).thenReturn(List.of(KENT));
        when(catalogo.listarUnidades()).thenReturn(List.of(JABA));
        when(catalogo.listarDistritos(null)).thenReturn(List.of(TAMBOGRANDE));

        assertThat(servicio.listarParaModeracion(VistaModeracion.POR_REVISAR, pagina).getContent()).singleElement()
                .satisfies(d -> {
                    assertThat(d.marcaRevision()).isEqualTo("PRIMERA_PUBLICACION");
                    assertThat(d.estado()).isEqualTo("PUBLICADA");
                    assertThat(d.cultivo()).isEqualTo("Mango");
                });
        assertThat(servicio.listarParaModeracion(VistaModeracion.OCULTAS, pagina)).isEmpty();
        assertThat(servicio.obtenerParaModeracion(15L).distrito()).isEqualTo("Tambogrande");
        verify(entityManager).refresh(p);
        when(publicaciones.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.obtenerParaModeracion(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    private void catalogoCon(UnidadDto unidad) {
        when(catalogo.buscarVariedad(1)).thenReturn(Optional.of(KENT));
        when(catalogo.buscarUnidad(unidad.id())).thenReturn(Optional.of(unidad));
        when(catalogo.buscarDistrito(9)).thenReturn(Optional.of(TAMBOGRANDE));
    }

    private static NuevaPublicacionDto datos(int idUnidad, String peso, LocalDate limite, LocalDate cosecha) {
        return new NuevaPublicacionDto(1, idUnidad, new BigDecimal("30"), peso == null ? null : new BigDecimal(peso),
                new BigDecimal("28"), 9, cosecha, limite);
    }

    private static Publicacion conId(Publicacion publicacion, long id) {
        ReflectionTestUtils.setField(publicacion, "id", id);
        return publicacion;
    }
}
