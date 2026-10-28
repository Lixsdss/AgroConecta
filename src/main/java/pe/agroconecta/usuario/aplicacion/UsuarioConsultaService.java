package pe.agroconecta.usuario.aplicacion;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.usuario.dominio.Comprador;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.CompradorRepository;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

/**
 * Consultas de solo lectura sobre usuarios para los demás módulos. Devuelve DTO, nunca entidades.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioConsultaService {

    private final UsuarioRepository usuarios;
    private final CompradorRepository compradores;
    private final CatalogoService catalogo;

    /**
     * Crea el servicio.
     *
     * @param usuarios    repositorio de usuarios
     * @param compradores repositorio de compradores (nombre del negocio)
     * @param catalogo    catálogo de distritos
     */
    public UsuarioConsultaService(UsuarioRepository usuarios, CompradorRepository compradores,
            CatalogoService catalogo) {
        this.usuarios = usuarios;
        this.compradores = compradores;
        this.catalogo = catalogo;
    }

    /**
     * Devuelve el resumen de varios usuarios, indexado por id. Los ids inexistentes no aparecen.
     *
     * @param ids ids de los usuarios
     * @return resumen de cada usuario encontrado
     */
    public Map<Long, UsuarioResumenDto> buscarResumenes(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> distritos = catalogo.listarDistritos(null).stream()
                .collect(Collectors.toMap(DistritoDto::id, DistritoDto::nombre));
        Map<Long, String> negocios = compradores.findAllById(ids).stream()
                .collect(Collectors.toMap(Comprador::getId, Comprador::getNombreNegocio));
        return usuarios.findAllById(ids).stream()
                .map(u -> aDto(u, distritos.get(u.getIdDistrito()), negocios.get(u.getId())))
                .collect(Collectors.toMap(UsuarioResumenDto::id, Function.identity()));
    }

    /**
     * Indica si un celular ya está registrado.
     *
     * @param celular celular (puede traer espacios)
     * @return {@code true} si algún usuario ya lo usa
     */
    public boolean existeCelular(String celular) {
        return usuarios.existsByCelular(ReglasUsuario.normalizarCelular(celular));
    }

    private static UsuarioResumenDto aDto(Usuario u, String distrito, String negocio) {
        return new UsuarioResumenDto(u.getId(), u.getNombreCompleto(), u.getCelular(), distrito,
                u.getFechaRegistro(), u.getRol().name(), negocio);
    }
}
