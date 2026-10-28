package pe.agroconecta.compartido.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Pruebas unitarias de la configuración JWT y del validador de usuario ACTIVO.
 */
class SeguridadUnitariaTest {

    @Test
    void laClaveDebeTenerAlMenos32Bytes() {
        JwtConfig config = new JwtConfig();

        assertThatThrownBy(() -> config.claveJwt(propiedades("corta")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
        assertThatThrownBy(() -> config.claveJwt(propiedades(" ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThat(config.claveJwt(propiedades("x".repeat(32))).getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void elValidadorAceptaSoloUsuariosActivos() {
        ValidadorUsuarioActivo validador = new ValidadorUsuarioActivo(id -> id == 5L);

        assertThat(validador.validate(jwtCon("5")).hasErrors()).isFalse();
        assertThat(validador.validate(jwtCon("6")).hasErrors()).isTrue();
        assertThat(validador.validate(jwtCon("no-es-numero")).hasErrors()).isTrue();
    }

    private static PropiedadesJwt propiedades(String secreto) {
        return new PropiedadesJwt(secreto, Duration.ofHours(12), "agroconecta");
    }

    private static Jwt jwtCon(String sujeto) {
        return Jwt.withTokenValue("t").header("alg", "HS256").subject(sujeto)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}
