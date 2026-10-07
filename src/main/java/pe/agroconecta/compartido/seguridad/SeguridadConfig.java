package pe.agroconecta.compartido.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad HTTP: API stateless protegida con JWT (OAuth2 Resource Server), endpoints públicos de CLAUDE.md §8
 * y roles con {@code @PreAuthorize}.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    /** Claim del token con los roles del usuario. */
    public static final String CLAIM_ROLES = "roles";

    private static final String[] ARCHIVOS_ESTATICOS = {
        "/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/img/**", "/vendor/**", "/pages/**"
    };

    private static final String[] DOCUMENTACION_API = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    /**
     * Cadena de filtros de seguridad.
     *
     * @param http     configurador de Spring Security
     * @param errores  respuestas 401 y 403 con el formato de la API
     * @return cadena de filtros
     * @throws Exception si la configuración es inválida
     */
    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ManejadorErroresSeguridad errores)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/registro", "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**", "/api/v1/publicaciones/**",
                        "/api/v1/precios/**", "/api/v1/reputaciones/**").permitAll()
                .requestMatchers(HttpMethod.GET, ARCHIVOS_ESTATICOS).permitAll()
                .requestMatchers(DOCUMENTACION_API).permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeRoles()))
                .authenticationEntryPoint(errores)
                .accessDeniedHandler(errores))
            .exceptionHandling(e -> e.authenticationEntryPoint(errores).accessDeniedHandler(errores));
        return http.build();
    }

    /**
     * Convierte el claim {@code roles} del token en autoridades {@code ROLE_*} para {@code @PreAuthorize}.
     *
     * @return convertidor de autenticación JWT
     */
    static JwtAuthenticationConverter convertidorDeRoles() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(CLAIM_ROLES);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }
}
