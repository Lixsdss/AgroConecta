package pe.agroconecta.usuario;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica que los triggers de subtipo de {@code modelo-bd.sql} rechacen un rol que no coincide (la base de datos
 * como última barrera de RF-08).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TriggersUsuarioIntegracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void trgAgricultoresBiRechazaAUnComprador() {
        long id = insertarUsuario("COMPRADOR", "900000001");

        assertThatThrownBy(() -> jdbc.update("insert into agricultores (id_usuario) values (?)", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("El usuario no tiene rol AGRICULTOR");
    }

    @Test
    void trgCompradoresBiRechazaAUnAgricultor() {
        long id = insertarUsuario("AGRICULTOR", "900000002");

        assertThatThrownBy(() -> jdbc.update(
                "insert into compradores (id_usuario, tipo_comprador, nombre_negocio) values (?, 'OTRO', 'X')", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("El usuario no tiene rol COMPRADOR");
    }

    @Test
    void chkUsuarioCelularRechazaUnCelularQueNoEmpiezaEn9() {
        assertThatThrownBy(() -> insertarUsuario("AGRICULTOR", "812345678"))
                .isInstanceOf(DataAccessException.class);
    }

    private long insertarUsuario(String rol, String celular) {
        jdbc.update("insert into usuarios (rol, nombre_completo, celular, password_hash, id_distrito) "
                + "values (?, 'Prueba', ?, 'x', 1)", rol, celular);
        return jdbc.queryForObject("select id_usuario from usuarios where celular = ?", Long.class, celular);
    }
}
