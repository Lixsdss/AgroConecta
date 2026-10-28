package pe.agroconecta.usuario.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.compartido.seguridad.VerificadorEstadoUsuario;
import pe.agroconecta.usuario.dominio.EstadoUsuario;
import pe.agroconecta.usuario.infraestructura.UsuarioRepository;

/**
 * Implementa {@link VerificadorEstadoUsuario} con una consulta por clave primaria en cada petición autenticada
 * (sin caché; ver SRS RNF-02).
 */
@Service
public class VerificadorEstadoUsuarioBd implements VerificadorEstadoUsuario {

    private final UsuarioRepository usuarios;

    /**
     * Crea el verificador.
     *
     * @param usuarios repositorio de usuarios
     */
    public VerificadorEstadoUsuarioBd(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public boolean estaActivo(long idUsuario) {
        return usuarios.existsByIdAndEstado(idUsuario, EstadoUsuario.ACTIVO);
    }
}
