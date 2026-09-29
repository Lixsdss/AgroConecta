package pe.agroconecta.precio.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agroconecta.precio.dominio.ClaveCultivoDistrito;
import pe.agroconecta.precio.dominio.RangoPrecioPlataforma;

/**
 * Lectura de la vista {@code v_rango_precio_plataforma}.
 */
public interface RangoPrecioPlataformaRepository
        extends JpaRepository<RangoPrecioPlataforma, ClaveCultivoDistrito> {
}
