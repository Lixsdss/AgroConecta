package pe.agroconecta.precio.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.CultivoDto;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.compartido.error.ConflictoException;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.precio.aplicacion.InformeImportacionDto.FilaRechazadaDto;
import pe.agroconecta.precio.dominio.FuentePrecio;
import pe.agroconecta.precio.dominio.MetodoCarga;
import pe.agroconecta.precio.dominio.PrecioMercado;
import pe.agroconecta.precio.infraestructura.FilaArchivoPrecio;
import pe.agroconecta.precio.infraestructura.LectorArchivoPrecios;
import pe.agroconecta.precio.infraestructura.PrecioMercadoRepository;

/**
 * Carga de precios de referencia por el administrador (RF-02, UC-02 A2): formulario, edición y archivo Excel/CSV.
 * Valida con mensajes amigables lo mismo que {@code trg_precios_mercado_bi}/{@code _bu} y la restricción única.
 */
@Service
public class CargaPrecioService {

    static final String MENSAJE_CULTIVO = "El cultivo elegido no existe.";
    static final String MENSAJE_NO_ZONA = "El distrito elegido no es una zona de precio.";
    static final String MENSAJE_NO_EXISTE = "El precio no existe.";

    private static final DateTimeFormatter DIA_MES_ANIO = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Logger LOG = LoggerFactory.getLogger(CargaPrecioService.class);

    private final PrecioMercadoRepository precios;
    private final CatalogoService catalogo;
    private final LectorArchivoPrecios lector;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param precios  repositorio de precios
     * @param catalogo catálogo de cultivos y zonas
     * @param lector   lector de archivos Excel y CSV
     * @param reloj    reloj de la aplicación
     */
    public CargaPrecioService(PrecioMercadoRepository precios, CatalogoService catalogo, LectorArchivoPrecios lector,
            Clock reloj) {
        this.precios = precios;
        this.catalogo = catalogo;
        this.lector = lector;
        this.reloj = reloj;
    }

    /**
     * Carga el precio del día por formulario.
     *
     * @param idAdministrador id del administrador de la sesión
     * @param idCultivo       id del cultivo
     * @param idDistrito      id de la zona de precio
     * @param fecha           día, o {@code null} para hoy
     * @param precioKg        soles por kilo
     * @param fuente          fuente
     * @return precio guardado
     * @throws ReglaNegocioException si el cultivo no existe o el distrito no es zona de precio
     * @throws ConflictoException    si ya hay precio de ese cultivo, zona y fecha
     */
    @Transactional
    public PrecioRegistradoDto registrar(long idAdministrador, Integer idCultivo, Integer idDistrito, LocalDate fecha,
            BigDecimal precioKg, FuentePrecio fuente) {
        Preconditions.checkNotNull(precioKg, "precioKg");
        CultivoDto cultivo = cultivosActivos().get(idCultivo);
        if (cultivo == null) {
            throw new ReglaNegocioException(MENSAJE_CULTIVO);
        }
        DistritoDto zona = zonas().get(idDistrito);
        if (zona == null) {
            throw new ReglaNegocioException(MENSAJE_NO_ZONA);
        }
        LocalDate dia = fecha == null ? LocalDate.now(reloj) : fecha;
        if (precios.existsByIdCultivoAndIdDistritoAndFecha(cultivo.id(), zona.id(), dia)) {
            throw new ConflictoException("Ya hay un precio de %s en %s para esa fecha. Use Editar para cambiarlo."
                    .formatted(cultivo.nombre(), zona.nombre()));
        }
        PrecioMercado guardado = precios.saveAndFlush(new PrecioMercado(cultivo.id(), zona.id(), dia, precioKg, fuente,
                MetodoCarga.FORMULARIO, idAdministrador));
        LOG.info("Precio cargado: id={}, cultivo={}, zona={}, fecha={}, administrador={}", guardado.getId(),
                cultivo.nombre(), zona.nombre(), dia, idAdministrador);
        return aDto(guardado, cultivo, zona);
    }

    /**
     * Corrige un precio ("Editar"). Quien edita queda como responsable.
     *
     * @param idAdministrador id del administrador de la sesión
     * @param idPrecio        id del precio
     * @param precioKg        nuevo precio por kilo
     * @param fuente          nueva fuente
     * @return precio actualizado
     * @throws RecursoNoEncontradoException si el precio no existe
     */
    @Transactional
    public PrecioRegistradoDto editar(long idAdministrador, long idPrecio, BigDecimal precioKg, FuentePrecio fuente) {
        PrecioMercado precio = precios.findById(idPrecio)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_NO_EXISTE));
        precio.editar(precioKg, fuente, idAdministrador);
        precios.flush();
        LOG.info("Precio editado: id={}, administrador={}", idPrecio, idAdministrador);
        return aDto(precio, cultivosActivos().get(precio.getIdCultivo()), zonas().get(precio.getIdDistrito()));
    }

    /**
     * Importa un archivo de precios: guarda las filas válidas e informa las inválidas con su motivo.
     *
     * @param idAdministrador id del administrador de la sesión
     * @param nombreArchivo   nombre original del archivo
     * @param contenido       bytes del archivo
     * @param fuente          fuente de todos los precios del archivo
     * @return filas importadas y rechazadas
     */
    @Transactional
    public InformeImportacionDto importar(long idAdministrador, String nombreArchivo, byte[] contenido,
            FuentePrecio fuente) {
        List<FilaArchivoPrecio> filas = lector.leer(nombreArchivo, contenido);
        Map<String, CultivoDto> cultivos = cultivosActivos().values().stream()
                .collect(Collectors.toMap(c -> normalizar(c.nombre()), Function.identity()));
        Map<String, DistritoDto> zonasPorNombre = zonas().values().stream()
                .collect(Collectors.toMap(d -> normalizar(d.nombre()), Function.identity(), (a, b) -> a));
        Map<String, DistritoDto> distritos = catalogo.listarDistritos(null).stream()
                .collect(Collectors.toMap(d -> normalizar(d.nombre()), Function.identity(), (a, b) -> a));

        List<PrecioMercado> validos = new ArrayList<>();
        List<FilaRechazadaDto> rechazadas = new ArrayList<>();
        Map<String, Integer> vistas = new HashMap<>();
        for (FilaArchivoPrecio fila : filas) {
            try {
                CultivoDto cultivo = Optional.ofNullable(cultivos.get(normalizar(fila.cultivo())))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El cultivo \"%s\" no existe.".formatted(fila.cultivo())));
                DistritoDto zona = zonaDe(fila.zona(), zonasPorNombre, distritos);
                LocalDate fecha = fechaDe(fila.fecha());
                BigDecimal precioKg = precioDe(fila.precioKg());
                String clave = cultivo.id() + "|" + zona.id() + "|" + fecha;
                Integer anterior = vistas.putIfAbsent(clave, fila.numero());
                if (anterior != null) {
                    throw new IllegalArgumentException(
                            "Ese cultivo, zona y fecha ya están en la fila %d.".formatted(anterior));
                }
                if (precios.existsByIdCultivoAndIdDistritoAndFecha(cultivo.id(), zona.id(), fecha)) {
                    throw new IllegalArgumentException("Ya hay un precio de %s en %s para el %s."
                            .formatted(cultivo.nombre(), zona.nombre(), fecha.format(DIA_MES_ANIO)));
                }
                validos.add(new PrecioMercado(cultivo.id(), zona.id(), fecha, precioKg, fuente, MetodoCarga.ARCHIVO,
                        idAdministrador));
            } catch (IllegalArgumentException e) {
                rechazadas.add(new FilaRechazadaDto(fila.numero(), e.getMessage()));
            }
        }
        precios.saveAllAndFlush(validos);
        LOG.info("Archivo de precios importado: archivo={}, importadas={}, rechazadas={}, administrador={}",
                nombreArchivo, validos.size(), rechazadas.size(), idAdministrador);
        return new InformeImportacionDto(validos.size(), List.copyOf(rechazadas));
    }

    private static DistritoDto zonaDe(String texto, Map<String, DistritoDto> zonas, Map<String, DistritoDto> distritos) {
        String clave = normalizar(texto);
        if (zonas.containsKey(clave)) {
            return zonas.get(clave);
        }
        throw new IllegalArgumentException(distritos.containsKey(clave)
                ? "La zona \"%s\" no es una zona de precio.".formatted(texto)
                : "La zona \"%s\" no existe.".formatted(texto));
    }

    private LocalDate fechaDe(String texto) {
        LocalDate fecha;
        try {
            fecha = texto.contains("/") ? LocalDate.parse(texto, DIA_MES_ANIO) : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "La fecha \"%s\" no es válida (use AAAA-MM-DD o dd/mm/aaaa).".formatted(texto));
        }
        if (fecha.isAfter(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("La fecha no puede ser futura.");
        }
        return fecha;
    }

    private static BigDecimal precioDe(String texto) {
        try {
            BigDecimal precio = new BigDecimal(StringUtils.replaceChars(StringUtils.trimToEmpty(texto), ',', '.'));
            if (precio.signum() > 0 && precio.stripTrailingZeros().scale() <= 2 && precio.precision() <= 10) {
                return precio;
            }
        } catch (NumberFormatException e) {
            // se informa abajo
        }
        throw new IllegalArgumentException(
                "El precio \"%s\" no es válido (mayor que cero, hasta 2 decimales).".formatted(texto));
    }

    private Map<Integer, CultivoDto> cultivosActivos() {
        return catalogo.listarCultivos().stream().collect(Collectors.toMap(CultivoDto::id, Function.identity()));
    }

    private Map<Integer, DistritoDto> zonas() {
        return catalogo.listarDistritos(null).stream().filter(DistritoDto::esZonaPrecio)
                .collect(Collectors.toMap(DistritoDto::id, Function.identity()));
    }

    private static String normalizar(String texto) {
        return StringUtils.lowerCase(StringUtils.stripAccents(StringUtils.normalizeSpace(texto)), Locale.ROOT);
    }

    private static PrecioRegistradoDto aDto(PrecioMercado p, CultivoDto cultivo, DistritoDto zona) {
        return new PrecioRegistradoDto(p.getId(), p.getIdCultivo(), cultivo == null ? null : cultivo.nombre(),
                p.getIdDistrito(), zona == null ? null : zona.nombre(), p.getFecha(), p.getPrecioKg(), p.getFuente(),
                p.getMetodo());
    }
}
