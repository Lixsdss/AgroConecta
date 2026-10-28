package pe.agroconecta.compartido.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación. Se inyecta en lugar de llamar a {@code now()} directamente para poder fijar la hora
 * en las pruebas.
 */
@Configuration
public class RelojConfig {

    /** Crea la configuración del reloj. */
    public RelojConfig() {
    }

    /**
     * Reloj del sistema en la zona horaria del servidor.
     *
     * @return reloj del sistema
     */
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
