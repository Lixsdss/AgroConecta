package pe.agroconecta.compartido.seguridad;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de los tokens JWT ({@code agroconecta.jwt.*}).
 *
 * @param secreto    clave HS256; viene de la variable de entorno {@code JWT_SECRET} y debe tener al menos 32 bytes
 * @param expiracion vigencia del token desde que se emite
 * @param emisor     valor del claim {@code iss}
 */
@ConfigurationProperties("agroconecta.jwt")
public record PropiedadesJwt(String secreto, Duration expiracion, String emisor) {
}
