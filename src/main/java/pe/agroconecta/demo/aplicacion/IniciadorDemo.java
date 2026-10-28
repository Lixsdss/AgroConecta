package pe.agroconecta.demo.aplicacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Carga los datos de demostración al arrancar, solo con el perfil {@code demo} (ADR-006). Se niega a hacerlo si
 * también está activo {@code prod}, y un error en la carga nunca detiene el arranque.
 */
@Component
@Profile("demo")
public class IniciadorDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(IniciadorDemo.class);

    private final CargaDatosDemoService carga;
    private final Environment entorno;

    /**
     * Crea el iniciador.
     *
     * @param carga   carga de los datos de demostración
     * @param entorno entorno de Spring, para revisar los perfiles activos
     */
    public IniciadorDemo(CargaDatosDemoService carga, Environment entorno) {
        this.carga = carga;
        this.entorno = entorno;
    }

    /**
     * Carga los datos si corresponde.
     *
     * @param args argumentos de la aplicación (no se usan)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (entorno.acceptsProfiles(Profiles.of("prod"))) {
            LOG.error("El perfil demo no se usa en producción: no se cargaron datos de demostración");
            return;
        }
        try {
            carga.cargar();
        } catch (RuntimeException e) {
            LOG.error("No se cargaron los datos de demostración; la base quedó como estaba", e);
        }
    }
}
