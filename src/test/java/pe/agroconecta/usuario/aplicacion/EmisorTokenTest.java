package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import pe.agroconecta.compartido.seguridad.PropiedadesJwt;
import pe.agroconecta.usuario.dominio.Rol;
import pe.agroconecta.usuario.dominio.Usuario;

class EmisorTokenTest {

    private static final PropiedadesJwt PROPIEDADES =
            new PropiedadesJwt("clave-de-prueba-de-al-menos-32-bytes!!", Duration.ofHours(12), "agroconecta");

    @Test
    void emiteUnTokenHs256ConSujetoRolesEmisorYVencimiento() {
        SecretKey clave = new javax.crypto.spec.SecretKeySpec(PROPIEDADES.secreto().getBytes(), "HmacSHA256");
        Instant ahora = Instant.now();
        EmisorToken emisor = new EmisorToken(new NimbusJwtEncoder(new ImmutableSecret<>(clave)), PROPIEDADES,
                Clock.fixed(ahora, ZoneOffset.UTC));
        Usuario usuario = RegistroServiceTest.conId(new Usuario(Rol.COMPRADOR, "Rosa", "922222222", "h", 1), 12L);

        SesionDto sesion = emisor.emitir(usuario);

        Jwt jwt = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build()
                .decode(sesion.token());
        assertThat(jwt.getSubject()).isEqualTo("12");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("COMPRADOR");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("agroconecta");
        assertThat(sesion.expiraEn()).isEqualTo(ahora.plus(Duration.ofHours(12)));
        assertThat(sesion.idUsuario()).isEqualTo(12L);
        assertThat(sesion.rol()).isEqualTo(Rol.COMPRADOR);
    }
}
