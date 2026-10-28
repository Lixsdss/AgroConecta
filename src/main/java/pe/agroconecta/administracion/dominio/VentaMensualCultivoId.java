package pe.agroconecta.administracion.dominio;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Clave de {@link VentaMensualCultivo}: mes y cultivo.
 *
 * @param mes       mes {@code yyyy-MM}
 * @param idCultivo cultivo
 */
@Embeddable
public record VentaMensualCultivoId(
        @Column(name = "mes") String mes,
        @Column(name = "id_cultivo") Integer idCultivo) implements Serializable {
}
