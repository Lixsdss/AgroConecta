package pe.agroconecta.compartido.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS solo para desarrollo local: permite abrir las pantallas desde otro puerto (por ejemplo, un servidor
 * estático del editor). En los demás perfiles el frontend se sirve desde la misma aplicación y no hace falta.
 */
@Configuration
@Profile("dev")
public class CorsConfig {

    /**
     * Orígenes permitidos para la API en desarrollo.
     *
     * @param origenes lista de orígenes ({@code agroconecta.cors.origenes})
     * @return configuración CORS para {@code /api/**}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${agroconecta.cors.origenes:http://localhost:5500,http://127.0.0.1:5500}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Location"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }
}
