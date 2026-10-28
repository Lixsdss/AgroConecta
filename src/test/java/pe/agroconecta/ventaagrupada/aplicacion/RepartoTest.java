package pe.agroconecta.ventaagrupada.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class RepartoTest {

    @Test
    void repartePorKilosDisponiblesEnUnidadesEnterasHaciaAbajo() {
        // 600 kg sobre un lote de 1000 kg: 60 % para cada cosecha según sus kilos
        List<Reparto.Parte> partes = Reparto.repartir(new BigDecimal("600"), List.of(
                cosecha(1, "500", "20"),     // jabas de 20 kg: le tocan 300 kg = 15 jabas
                cosecha(2, "300", "45"),     // sacos de 45 kg: le tocan 180 kg = 4 sacos (180 kg)
                cosecha(3, "200", "1")));    // kilos: le tocan 120 kg

        assertThat(partes).extracting(Reparto.Parte::idPublicacion).containsExactly(1L, 2L, 3L);
        assertThat(partes.get(0).unidades()).isEqualByComparingTo("15");
        assertThat(partes.get(0).kilos()).isEqualByComparingTo("300");
        assertThat(partes.get(1).unidades()).isEqualByComparingTo("4");
        assertThat(partes.get(1).kilos()).isEqualByComparingTo("180");
        assertThat(partes.get(2).unidades()).isEqualByComparingTo("120");
    }

    @Test
    void elRedondeoNuncaPasaLoPedidoNiLoDisponible() {
        List<Reparto.Parte> partes = Reparto.repartir(new BigDecimal("590"), List.of(
                cosecha(1, "500", "20"), cosecha(2, "500", "46")));

        // 295 kg a cada una: 14 jabas (280 kg) y 6 quintales (276 kg)
        assertThat(partes.get(0).unidades()).isEqualByComparingTo("14");
        assertThat(partes.get(1).unidades()).isEqualByComparingTo("6");
        BigDecimal total = partes.stream().map(Reparto.Parte::kilos).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isLessThanOrEqualTo(new BigDecimal("590"));
    }

    @Test
    void sinUnaUnidadEnteraNoHayParte() {
        // 30 kg sobre 1000: a la cosecha de toneladas le tocan 27 kg (0 toneladas)
        List<Reparto.Parte> partes = Reparto.repartir(new BigDecimal("30"), List.of(
                cosecha(1, "900", "1000"), cosecha(2, "100", "1")));

        assertThat(partes).extracting(Reparto.Parte::idPublicacion).containsExactly(2L);
        assertThat(partes.get(0).unidades()).isEqualByComparingTo("3");
        assertThat(Reparto.repartir(new BigDecimal("5"), List.of(cosecha(1, "900", "1000")))).isEmpty();
        assertThatThrownBy(() -> Reparto.repartir(BigDecimal.ONE, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    private static Reparto.Cosecha cosecha(long id, String disponibleKg, String pesoUnidadKg) {
        return new Reparto.Cosecha(id, new BigDecimal(disponibleKg), new BigDecimal(pesoUnidadKg));
    }
}
