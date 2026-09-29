package pe.agroconecta.precio.dominio;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta (cultivo + distrito) de las vistas de precios, que no tienen columna id propia.
 */
public class ClaveCultivoDistrito implements Serializable {

    private Integer idCultivo;
    private Integer idDistrito;

    /** Constructor requerido por JPA. */
    public ClaveCultivoDistrito() {
    }

    /**
     * Crea la clave.
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id del distrito
     */
    public ClaveCultivoDistrito(Integer idCultivo, Integer idDistrito) {
        this.idCultivo = idCultivo;
        this.idDistrito = idDistrito;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof ClaveCultivoDistrito c && Objects.equals(idCultivo, c.idCultivo)
                && Objects.equals(idDistrito, c.idDistrito);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(idCultivo, idDistrito);
    }
}
