package pe.agroconecta.precio.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.precio.dominio.ClaveCultivoDistrito;
import pe.agroconecta.precio.dominio.PrecioVigente;
import pe.agroconecta.precio.infraestructura.PrecioVigenteRepository;
import pe.agroconecta.precio.infraestructura.RangoPrecioPlataformaRepository;

/**
 * Referencia de precio de un cultivo en un distrito (umbrales aprobados en los slices 3 y 4):
 *
 * <ul>
 * <li>Si el distrito es zona de precio y tiene un precio de 7 días o menos, se usa ese precio.</li>
 * <li>Si no, el promedio de los precios de 7 días o menos de las zonas que tengan ese cultivo.</li>
 * <li>"Buen precio" es la referencia ±15 %.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ReferenciaPrecioService {

    /** Antigüedad máxima, en días, de un precio para servir de referencia. */
    static final int DIAS_VIGENCIA = 7;
    /** Tolerancia de "Buen precio" alrededor de la referencia. */
    static final BigDecimal TOLERANCIA = new BigDecimal("0.15");
    /** Texto de la zona cuando la referencia es el promedio de las zonas. */
    static final String PROMEDIO_DE_ZONAS = "las zonas de Piura";

    private final PrecioVigenteRepository vigentes;
    private final RangoPrecioPlataformaRepository rangos;
    private final CatalogoService catalogo;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param vigentes repositorio de la vista de precios vigentes
     * @param rangos   repositorio de la vista del rango de la plataforma
     * @param catalogo catálogo de cultivos y unidades
     * @param reloj    reloj de la aplicación
     */
    public ReferenciaPrecioService(PrecioVigenteRepository vigentes, RangoPrecioPlataformaRepository rangos,
            CatalogoService catalogo, Clock reloj) {
        this.vigentes = vigentes;
        this.rangos = rangos;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    /**
     * Calcula la referencia de un cultivo en un distrito.
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id del distrito de la cosecha
     * @return referencia, o vacío si no hay precios de 7 días o menos
     */
    public Optional<ReferenciaPrecioDto> referencia(int idCultivo, int idDistrito) {
        LocalDate limite = LocalDate.now(reloj).minusDays(DIAS_VIGENCIA);
        List<PrecioVigente> recientes = vigentes.findByIdCultivoOrderByZonaAsc(idCultivo).stream()
                .filter(v -> !v.getFecha().isBefore(limite))
                .toList();
        if (recientes.isEmpty()) {
            return Optional.empty();
        }
        Optional<PrecioVigente> deLaZona = recientes.stream()
                .filter(v -> v.getIdDistrito() == idDistrito).findFirst();
        BigDecimal precioKg = deLaZona.map(PrecioVigente::getPrecioKg).orElseGet(() -> promedio(recientes));
        String zona = deLaZona.map(PrecioVigente::getZona).orElse(PROMEDIO_DE_ZONAS);
        LocalDate fecha = deLaZona.map(PrecioVigente::getFecha)
                .orElseGet(() -> recientes.stream().map(PrecioVigente::getFecha).max(Comparator.naturalOrder())
                        .orElseThrow());
        BigDecimal kilosUnidadLocal = kilosUnidadLocal(idCultivo);
        ReferenciaPrecioDto.RangoPlataformaDto rango = rangos.findById(new ClaveCultivoDistrito(idCultivo, idDistrito))
                .map(r -> new ReferenciaPrecioDto.RangoPlataformaDto(r.getMinimoKg(), r.getMaximoKg(),
                        r.getPromedioKg(), r.getVentas()))
                .orElse(null);
        PrecioVigente cualquiera = recientes.getFirst();
        return Optional.of(new ReferenciaPrecioDto(idCultivo, cualquiera.getCultivo(), zona, deLaZona.isEmpty(), fecha,
                precioKg, escalar(precioKg, BigDecimal.ONE.subtract(TOLERANCIA)),
                escalar(precioKg, BigDecimal.ONE.add(TOLERANCIA)), cualquiera.getUnidadLocal(),
                kilosUnidadLocal == null ? null : escalar(precioKg, kilosUnidadLocal), rango));
    }

    /**
     * Compara un precio por kilo con la referencia.
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id del distrito de la cosecha
     * @param precioKg   precio por kilo a evaluar
     * @return DENTRO, BAJO, ALTO o SIN_REFERENCIA
     */
    public EvaluacionPrecio evaluar(int idCultivo, int idDistrito, BigDecimal precioKg) {
        return referencia(idCultivo, idDistrito).map(r -> {
            if (precioKg.compareTo(r.minimoKg()) < 0) {
                return EvaluacionPrecio.BAJO;
            }
            return precioKg.compareTo(r.maximoKg()) > 0 ? EvaluacionPrecio.ALTO : EvaluacionPrecio.DENTRO;
        }).orElse(EvaluacionPrecio.SIN_REFERENCIA);
    }

    private static BigDecimal promedio(List<PrecioVigente> precios) {
        return precios.stream().map(PrecioVigente::getPrecioKg).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(precios.size()), 2, RoundingMode.HALF_UP);
    }

    /** Kilos de la unidad local del cultivo: el factor del catálogo o, si es variable, el peso sugerido. */
    private BigDecimal kilosUnidadLocal(int idCultivo) {
        return catalogo.listarCultivos().stream().filter(c -> c.id() == idCultivo).findFirst()
                .map(c -> catalogo.buscarUnidad(c.idUnidadLocal()).map(UnidadDto::factorKg)
                        .filter(Objects::nonNull).orElse(c.pesoSugeridoKg()))
                .orElse(null);
    }

    private static BigDecimal escalar(BigDecimal valor, BigDecimal factor) {
        return valor.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
