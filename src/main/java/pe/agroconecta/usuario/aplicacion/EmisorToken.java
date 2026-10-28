package pe.agroconecta.usuario.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import pe.agroconecta.compartido.seguridad.PropiedadesJwt;
import pe.agroconecta.compartido.seguridad.SeguridadConfig;
import pe.agroconecta.usuario.dominio.Usuario;

/**
 * Emite los tokens JWT de sesión (ADR-003): {@code sub} = id del usuario, claim {@code roles} con su rol.
 */
@Component
public class EmisorToken {

    private final JwtEncoder encoder;
    private final PropiedadesJwt propiedades;
    private final Clock reloj;

    /**
     * Crea el emisor.
     *
     * @param encoder     codificador HS256
     * @param propiedades emisor y vigencia del token
     * @param reloj       reloj de la aplicación
     */
    public EmisorToken(JwtEncoder encoder, PropiedadesJwt propiedades, Clock reloj) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /**
     * Emite un token para el usuario.
     *
     * @param usuario usuario autenticado (con id)
     * @return sesión con el token y su vencimiento
     */
    public SesionDto emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(propiedades.expiracion());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(SeguridadConfig.CLAIM_ROLES, List.of(usuario.getRol().name()))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new SesionDto(token, expira, usuario.getId(), usuario.getNombreCompleto(), usuario.getRol());
    }
}
