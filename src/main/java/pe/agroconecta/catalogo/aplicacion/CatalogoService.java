package pe.agroconecta.catalogo.aplicacion;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.dominio.Cultivo;
import pe.agroconecta.catalogo.dominio.Distrito;
import pe.agroconecta.catalogo.dominio.UnidadMedida;
import pe.agroconecta.catalogo.dominio.Variedad;
import pe.agroconecta.catalogo.infraestructura.CultivoRepository;
import pe.agroconecta.catalogo.infraestructura.DistritoRepository;
import pe.agroconecta.catalogo.infraestructura.ProvinciaRepository;
import pe.agroconecta.catalogo.infraestructura.UnidadMedidaRepository;
import pe.agroconecta.catalogo.infraestructura.VariedadRepository;

/**
 * Consultas de los catálogos de solo lectura: ubicación (provincias y distritos de Piura), cultivos, variedades y
 * unidades de venta. Es la puerta de entrada al módulo
 * {@code catalogo} para la API y para los demás módulos.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final ProvinciaRepository provincias;
    private final DistritoRepository distritos;
    private final CultivoRepository cultivos;
    private final VariedadRepository variedades;
    private final UnidadMedidaRepository unidades;

    /**
     * Crea el servicio.
     *
     * @param provincias  repositorio de provincias
     * @param distritos   repositorio de distritos
     * @param cultivos    repositorio de cultivos
     * @param variedades  repositorio de variedades
     * @param unidades    repositorio de unidades de medida
     */
    public CatalogoService(ProvinciaRepository provincias, DistritoRepository distritos, CultivoRepository cultivos,
            VariedadRepository variedades, UnidadMedidaRepository unidades) {
        this.provincias = provincias;
        this.distritos = distritos;
        this.cultivos = cultivos;
        this.variedades = variedades;
        this.unidades = unidades;
    }

    /**
     * Lista las provincias ordenadas por nombre.
     *
     * @return provincias
     */
    public List<ProvinciaDto> listarProvincias() {
        return provincias.findAllByOrderByNombreAsc().stream()
                .map(p -> new ProvinciaDto(p.getId(), p.getNombre()))
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Lista los distritos, todos o los de una provincia.
     *
     * @param idProvincia id de la provincia, o {@code null} para todos
     * @return distritos ordenados por provincia y nombre
     */
    public List<DistritoDto> listarDistritos(Integer idProvincia) {
        List<Distrito> encontrados = idProvincia == null
                ? distritos.listarTodos()
                : distritos.listarPorProvincia(idProvincia);
        return encontrados.stream().map(CatalogoService::aDto).collect(ImmutableList.toImmutableList());
    }

    /**
     * Busca un distrito por su id.
     *
     * @param idDistrito id del distrito
     * @return distrito con su provincia, si existe
     */
    public Optional<DistritoDto> buscarDistrito(Integer idDistrito) {
        return idDistrito == null ? Optional.empty() : distritos.buscarConProvincia(idDistrito).map(CatalogoService::aDto);
    }

    /**
     * Busca un distrito por su ubigeo del INEI.
     *
     * @param ubigeo código de 6 dígitos
     * @return distrito con su provincia, si existe
     */
    public Optional<DistritoDto> buscarDistritoPorUbigeo(String ubigeo) {
        return distritos.buscarPorUbigeo(ubigeo).map(CatalogoService::aDto);
    }

    /**
     * Indica si existe un distrito con ese id.
     *
     * @param idDistrito id del distrito
     * @return {@code true} si existe
     */
    public boolean existeDistrito(Integer idDistrito) {
        return idDistrito != null && distritos.existsById(idDistrito);
    }

    /**
     * Lista los cultivos activos ordenados por nombre.
     *
     * @return cultivos con su unidad local y peso sugerido
     */
    public List<CultivoDto> listarCultivos() {
        return cultivos.listarActivos().stream().map(CatalogoService::aDto).collect(ImmutableList.toImmutableList());
    }

    /**
     * Lista las variedades, todas o las de un cultivo.
     *
     * @param idCultivo id del cultivo, o {@code null} para todas
     * @return variedades ordenadas por nombre
     */
    public List<VariedadDto> listarVariedades(Integer idCultivo) {
        List<Variedad> encontradas = idCultivo == null
                ? variedades.listarTodas()
                : variedades.listarPorCultivo(idCultivo);
        return encontradas.stream().map(CatalogoService::aDto).collect(ImmutableList.toImmutableList());
    }

    /**
     * Busca una variedad con su cultivo.
     *
     * @param idVariedad id de la variedad
     * @return variedad, si existe
     */
    public Optional<VariedadDto> buscarVariedad(Integer idVariedad) {
        return idVariedad == null ? Optional.empty() : variedades.buscarConCultivo(idVariedad).map(CatalogoService::aDto);
    }

    /**
     * Lista las unidades de venta en el orden del catálogo.
     *
     * @return unidades
     */
    public List<UnidadDto> listarUnidades() {
        return unidades.findAllByOrderByIdAsc().stream().map(CatalogoService::aDto)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Busca una unidad de venta.
     *
     * @param idUnidad id de la unidad
     * @return unidad, si existe
     */
    public Optional<UnidadDto> buscarUnidad(Integer idUnidad) {
        return idUnidad == null ? Optional.empty() : unidades.findById(idUnidad).map(CatalogoService::aDto);
    }

    private static CultivoDto aDto(Cultivo c) {
        return new CultivoDto(c.getId(), c.getNombre(), c.getVidaUtilDias().intValue(), c.getUnidadLocal().getId(),
                c.getUnidadLocal().getNombre(), c.getPesoSugeridoKg());
    }

    private static VariedadDto aDto(Variedad v) {
        return new VariedadDto(v.getId(), v.getNombre(), v.getCultivo().getId(), v.getCultivo().getNombre(),
                v.getCultivo().getVidaUtilDias().intValue());
    }

    private static UnidadDto aDto(UnidadMedida u) {
        return new UnidadDto(u.getId(), u.getNombre(), u.getNombrePlural(), u.getFactorKg());
    }

    private static DistritoDto aDto(Distrito d) {
        return new DistritoDto(d.getId(), d.getNombre(), d.getProvincia().getId(), d.getProvincia().getNombre(),
                d.isEsZonaPrecio());
    }
}
