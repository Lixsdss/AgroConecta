package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReglasUsuarioTest {

    @Test
    void normalizaCelularYTextos() {
        assertThat(ReglasUsuario.normalizarCelular(" 987 654 321 ")).isEqualTo("987654321");
        assertThat(ReglasUsuario.normalizarTexto("  Juan   Chunga ")).isEqualTo("Juan Chunga");
        assertThat(ReglasUsuario.normalizarTexto("   ")).isNull();
    }

    @Test
    void enmascaraElCelularParaLosLogs() {
        assertThat(ReglasUsuario.enmascararCelular("987654321")).isEqualTo("98*****21");
        assertThat(ReglasUsuario.enmascararCelular(null)).isEqualTo("*********");
    }

    @ParameterizedTest
    @ValueSource(strings = {"clave2026", "Ñandú2026", "12345678a"})
    void aceptaContrasenasConLetrasYNumeros(String contrasena) {
        assertThat(contrasena).matches(ReglasUsuario.PATRON_CONTRASENA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"corta1", "solotexto", "12345678"})
    void rechazaContrasenasDebiles(String contrasena) {
        assertThat(contrasena).doesNotMatch(ReglasUsuario.PATRON_CONTRASENA);
    }

    @Test
    void validaCelularYRuc() {
        assertThat("987 654 321").matches(ReglasUsuario.PATRON_CELULAR_CON_ESPACIOS);
        assertThat("887654321").doesNotMatch(ReglasUsuario.PATRON_CELULAR_CON_ESPACIOS);
        assertThat("").matches(ReglasUsuario.PATRON_RUC_OPCIONAL);
        assertThat("20123456789").matches(ReglasUsuario.PATRON_RUC_OPCIONAL);
        assertThat("30123456789").doesNotMatch(ReglasUsuario.PATRON_RUC_OPCIONAL);
    }
}
