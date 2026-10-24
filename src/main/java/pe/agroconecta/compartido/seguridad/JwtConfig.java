package pe.agroconecta.compartido.seguridad;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Preconditions;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Beans de JWT (HS256) y de contraseñas (BCrypt), según ADR-003.
 *
 * <p>La clave sale de {@code JWT_SECRET} y debe tener al menos 32 bytes; si falta o es más corta, la aplicación
 * no arranca.</p>
 */
@Configuration
@EnableConfigurationProperties(PropiedadesJwt.class)
public class JwtConfig {

    /** Longitud mínima de la clave para HS256 (256 bits). */
    static final int BYTES_MINIMOS_SECRETO = 32;

    /** Crea la configuración de JWT y de contraseñas. */
    public JwtConfig() {
    }

    /**
     * Clave simétrica con la que se firman y validan los tokens.
     *
     * @param propiedades configuración {@code agroconecta.jwt.*}
     * @return clave HMAC-SHA256
     */
    @Bean
    public SecretKey claveJwt(PropiedadesJwt propiedades) {
        Preconditions.checkState(StringUtils.isNotBlank(propiedades.secreto()),
                "Falta la variable de entorno JWT_SECRET");
        byte[] bytes = propiedades.secreto().getBytes(StandardCharsets.UTF_8);
        Preconditions.checkState(bytes.length >= BYTES_MINIMOS_SECRETO,
                "JWT_SECRET debe tener al menos %s bytes", BYTES_MINIMOS_SECRETO);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * Codificador con el que el módulo {@code usuario} emite los tokens.
     *
     * @param clave clave HS256
     * @return codificador Nimbus
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    /**
     * Decodificador del Resource Server: valida firma HS256, vigencia, emisor y que el usuario siga ACTIVO.
     *
     * @param clave       clave HS256
     * @param propiedades configuración {@code agroconecta.jwt.*}
     * @param verificador consulta del estado del usuario, implementada por el módulo {@code usuario}
     * @return decodificador Nimbus con los validadores
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey clave, PropiedadesJwt propiedades, VerificadorEstadoUsuario verificador) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(propiedades.emisor()),
                new ValidadorUsuarioActivo(verificador)));
        return decoder;
    }

    /**
     * Codificador de contraseñas.
     *
     * @return BCrypt con la fuerza por defecto
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
