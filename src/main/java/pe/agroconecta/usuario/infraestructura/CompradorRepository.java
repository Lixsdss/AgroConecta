package pe.agroconecta.usuario.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.usuario.dominio.Comprador;

/**
 * Acceso a la tabla {@code compradores}.
 */
public interface CompradorRepository extends JpaRepository<Comprador, Long> {

    /**
     * Indica si ya existe un comprador con ese RUC.
     *
     * @param ruc RUC de 11 dígitos
     * @return {@code true} si existe
     */
    boolean existsByRuc(String ruc);

    /**
     * Indica si otro comprador (distinto del indicado) ya tiene ese RUC.
     *
     * @param ruc RUC de 11 dígitos
     * @param id  id del comprador que se excluye
     * @return {@code true} si existe
     */
    boolean existsByRucAndIdNot(String ruc, Long id);
}
