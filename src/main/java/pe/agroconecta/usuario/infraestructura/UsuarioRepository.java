package pe.agroconecta.usuario.infraestructura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.usuario.dominio.EstadoUsuario;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;

/**
 * Acceso a la tabla {@code usuarios}.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su celular (usuario de inicio de sesión).
     *
     * @param celular celular de 9 dígitos
     * @return usuario, si existe
     */
    Optional<Usuario> findByCelular(String celular);

    /**
     * Indica si ya existe un usuario con ese celular.
     *
     * @param celular celular de 9 dígitos
     * @return {@code true} si existe
     */
    boolean existsByCelular(String celular);

    /**
     * Indica si existe algún usuario con ese rol.
     *
     * @param rol rol buscado
     * @return {@code true} si existe al menos uno
     */
    boolean existsByRol(Rol rol);

    /**
     * Indica si existe un usuario con ese id y ese estado.
     *
     * @param id     id del usuario
     * @param estado estado buscado
     * @return {@code true} si existe
     */
    boolean existsByIdAndEstado(Long id, EstadoUsuario estado);
}
