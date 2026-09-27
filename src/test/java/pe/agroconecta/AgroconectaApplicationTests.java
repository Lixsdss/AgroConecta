package pe.agroconecta;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring arranque contra la base de datos de pruebas
 * (MariaDB local {@code agroconecta_test}), con Flyway aplicando las migraciones
 * y Hibernate validando el esquema.
 */
@SpringBootTest
@ActiveProfiles("test")
class AgroconectaApplicationTests {

	@Test
	void contextLoads() {
	}

}
