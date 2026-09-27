package pe.agroconecta.catalogo.aplicacion;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.dominio.Distrito;
import pe.agroconecta.catalogo.infraestructura.DistritoRepository;
import pe.agroconecta.catalogo.infraestructura.ProvinciaRepository;

/**
 * Consultas de los catálogos de ubicación (provincias y distritos de Piura). Es la puerta de entrada al módulo
 * {@code catalogo} para la API y para los demás módulos.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final ProvinciaRepository provincias;
    private final DistritoRepository distritos;

    /**
     * Crea el servicio.
     *
     * @param provincias repositorio de provincias
     * @param distritos  repositorio de distritos
     */
    public CatalogoService(ProvinciaRepository provincias, DistritoRepository distritos) {
        this.provincias = provincias;
        this.distritos = distritos;
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

    private static DistritoDto aDto(Distrito d) {
        return new DistritoDto(d.getId(), d.getNombre(), d.getProvincia().getId(), d.getProvincia().getNombre(),
                d.isEsZonaPrecio());
    }
}
