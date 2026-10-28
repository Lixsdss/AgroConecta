package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.TipoComprador;

class RegistroDtoTest {

    @Test
    void deTextoConvierteElRolYElTipoDeComprador() {
        RegistroDto comprador = RegistroDto.deTexto("COMPRADOR", "Rosa", "922000001", 1, "Demo2026", null,
                "Restaurante Rosa", "RESTAURANTE", null);

        assertThat(comprador.rol()).isEqualTo(Rol.COMPRADOR);
        assertThat(comprador.tipoComprador()).isEqualTo(TipoComprador.RESTAURANTE);
    }

    @Test
    void deTextoSinTipoDeCompradorLoDejaVacio() {
        RegistroDto agricultor = RegistroDto.deTexto("AGRICULTOR", "Juan", "911000001", 1, "Demo2026",
                "Fundo Santa Rosa", null, null, null);

        assertThat(agricultor.rol()).isEqualTo(Rol.AGRICULTOR);
        assertThat(agricultor.tipoComprador()).isNull();
    }

    @Test
    void deTextoRechazaUnRolQueNoExiste() {
        assertThatThrownBy(() -> RegistroDto.deTexto("TRANSPORTISTA", "Luis", "933000001", 1, "Demo2026", null,
                null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
