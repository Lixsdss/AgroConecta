package pe.agroconecta.compartido.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI (Swagger UI en {@code /swagger-ui.html}) con el esquema de seguridad Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    /**
     * Descripción de la API y del esquema de autenticación.
     *
     * @return definición OpenAPI
     */
    @Bean
    public OpenAPI apiAgroConecta() {
        return new OpenAPI()
                .info(new Info()
                        .title("API AgroConecta")
                        .version("v1")
                        .description("Plataforma que conecta a agricultores de Piura con compradores. "
                                + "Contrato completo en docs/05-api/endpoints.md."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
