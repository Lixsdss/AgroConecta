package pe.agroconecta.ventaagrupada.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Reparto proporcional de una propuesta conjunta (RF-10, CLAUDE.md §6.3): a cada cosecha le toca
 * {@code cantidadKg × (sus kg disponibles ÷ kg del lote)}, en unidades enteras de su publicación, redondeando hacia
 * abajo. Con menos de una unidad no recibe parte (UC-12 A6).
 */
final class Reparto {

    private Reparto() {
    }

    /**
     * Cosecha que entra al reparto.
     *
     * @param idPublicacion publicación
     * @param disponibleKg  kilos disponibles
     * @param pesoUnidadKg  kilos por unidad de su publicación
     */
    record Cosecha(long idPublicacion, BigDecimal disponibleKg, BigDecimal pesoUnidadKg) {
    }

    /**
     * Parte que le toca a una cosecha.
     *
     * @param idPublicacion publicación
     * @param unidades      unidades enteras de su publicación
     * @param kilos         unidades × kilos por unidad
     */
    record Parte(long idPublicacion, BigDecimal unidades, BigDecimal kilos) {
    }

    /**
     * Reparte los kilos pedidos entre las cosechas.
     *
     * @param cantidadKg kilos pedidos (no más que el total disponible)
     * @param cosechas   cosechas del lote
     * @return partes con al menos una unidad, en el mismo orden
     */
    static List<Parte> repartir(BigDecimal cantidadKg, List<Cosecha> cosechas) {
        BigDecimal total = cosechas.stream().map(Cosecha::disponibleKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        Preconditions.checkArgument(total.signum() > 0, "el lote no tiene kilos");
        List<Parte> partes = new ArrayList<>();
        for (Cosecha c : cosechas) {
            BigDecimal kilosQueLeTocan = cantidadKg.multiply(c.disponibleKg()).divide(total, 6, RoundingMode.DOWN);
            BigDecimal unidades = kilosQueLeTocan.divide(c.pesoUnidadKg(), 0, RoundingMode.DOWN);
            if (unidades.signum() > 0) {
                partes.add(new Parte(c.idPublicacion(), unidades,
                        unidades.multiply(c.pesoUnidadKg()).setScale(2, RoundingMode.HALF_UP)));
            }
        }
        return partes;
    }
}
