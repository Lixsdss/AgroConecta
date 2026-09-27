package pe.agroconecta.usuario.aplicacion;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.usuario.dominio.Usuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

/**
 * Consultas de solo lectura sobre usuarios para los demás módulos. Devuelve DTO, nunca entidades.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioConsultaService {

    private final UsuarioRepository usuarios;
    private final CatalogoService catalogo;

    /**
     * Crea el servicio.
     *
     * @param usuarios repositorio de usuarios
     * @param catalogo catálogo de distritos
     */
    public UsuarioConsultaService(UsuarioRepository usuarios, CatalogoService catalogo) {
        this.usuarios = usuarios;
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
        return usuarios.findAllById(ids).stream()
                .map(u -> aDto(u, distritos.get(u.getIdDistrito())))
                .collect(Collectors.toMap(UsuarioResumenDto::id, Function.identity()));
    }

    private static UsuarioResumenDto aDto(Usuario u, String distrito) {
        return new UsuarioResumenDto(u.getId(), u.getNombreCompleto(), u.getCelular(), distrito,
                u.getFechaRegistro());
    }
}
