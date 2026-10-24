package pe.agroconecta;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Arranque de AgroConecta cuando el WAR se despliega en un Tomcat externo (empaquetado WAR, CLAUDE.md §4).
 */
public class ServletInitializer extends SpringBootServletInitializer {

	/** Crea el inicializador (lo instancia el contenedor de servlets). */
	public ServletInitializer() {
	}

	/**
	 * Indica a Tomcat cuál es la clase principal de la aplicación.
	 *
	 * @param application constructor de la aplicación que entrega el contenedor
	 * @return el mismo constructor con {@link AgroconectaApplication} como fuente
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(AgroconectaApplication.class);
	}

}
