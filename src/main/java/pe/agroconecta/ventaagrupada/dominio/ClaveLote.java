package pe.agroconecta.ventaagrupada.dominio;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave de un lote de {@code v_lotes_agrupados}: variedad y distrito (la variedad ya define el cultivo).
 */
public class ClaveLote implements Serializable {

    private Integer idVariedad;
    private Integer idDistrito;

    /** Constructor requerido por JPA. */
    public ClaveLote() {
    }

    /**
     * Crea la clave.
     *
     * @param idVariedad variedad
     * @param idDistrito distrito
     */
    public ClaveLote(Integer idVariedad, Integer idDistrito) {
        this.idVariedad = idVariedad;
        this.idDistrito = idDistrito;
    }

    /**
     * Dos claves son iguales si tienen la misma variedad y el mismo distrito.
     *
     * @param otro otro objeto
     * @return {@code true} si es la misma clave
     */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof ClaveLote c && Objects.equals(idVariedad, c.idVariedad)
                && Objects.equals(idDistrito, c.idDistrito);
    }

    /** @return código hash de la variedad y el distrito */
    @Override
    public int hashCode() {
        return Objects.hash(idVariedad, idDistrito);
    }
}
