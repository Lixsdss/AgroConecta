package pe.agroconecta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de AgroConecta: plataforma web que conecta a agricultores de Piura con compradores.
 *
 * <p>Un único WAR dividido en módulos de negocio bajo {@code pe.agroconecta} (ADR-001). Se ejecuta con
 * {@link #main(String[])} en desarrollo o se despliega en Tomcat mediante {@link ServletInitializer}.</p>
 */
@SpringBootApplication
public class AgroconectaApplication {

	/** Crea la aplicación (la instancia Spring como clase de configuración). */
	public AgroconectaApplication() {
	}

	/**
	 * Arranca la aplicación con Tomcat embebido.
	 *
	 * @param args argumentos de la línea de comandos (por ejemplo, {@code --spring.profiles.active=dev})
	 */
	public static void main(String[] args) {
		SpringApplication.run(AgroconectaApplication.class, args);
	}

}
