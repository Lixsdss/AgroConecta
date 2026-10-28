package pe.agroconecta.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.demo.aplicacion.CargaDatosDemoService;
import pe.agroconecta.demo.aplicacion.ResumenDemoDto;

/**
 * Prueba de integración de los datos de demostración (ADR-006) contra MariaDB ({@code agroconecta_test}): la carga
 * completa pasa por los servicios y los triggers reales, y una segunda carga no duplica nada. Se revierte.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatosDemoIntegracionTest {

    @Autowired
    private CargaDatosDemoService carga;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    @Test
    void cargaTodosLosDatosDeDemostracionUnaSolaVez() {
        ResumenDemoDto resumen = carga.cargar();
        em.flush();

        assertThat(resumen).isEqualTo(new ResumenDemoDto(8, 224, 12, 8));
        assertThat(jdbc.queryForObject("select count(*) from usuarios where celular in "
                + "('900000001','911000001','911000002','911000003','911000004','922000001','922000002','922000003')",
                Integer.class)).isEqualTo(8);
        assertThat(contar("select marca_revision, count(*) from publicaciones p join usuarios u "
                + "on u.id_usuario = p.id_agricultor where u.celular like '91100000_' and marca_revision is not null "
                + "group by marca_revision")).containsExactlyInAnyOrderEntriesOf(
                        Map.of("PRIMERA_PUBLICACION", 4L, "PRECIO_FUERA_RANGO", 1L));
        assertThat(contar("select t.estado, count(*) from transacciones t join conversaciones c "
                + "on c.id_conversacion = t.id_conversacion join usuarios u on u.id_usuario = c.id_comprador "
                + "where u.celular like '92200000_' group by t.estado")).containsExactlyInAnyOrderEntriesOf(
                        Map.of("ACORDADA", 2L, "RECOGIDO", 1L, "EN_CAMINO", 1L, "COMPLETADA", 2L, "CANCELADA", 1L,
                                "PROPUESTA", 1L));
        assertThat(jdbc.queryForObject("select count(*) from calificaciones c join usuarios u "
                + "on u.id_usuario = c.id_evaluador where u.celular regexp '^9(11|22)00000[0-9]$'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from mensajes m join usuarios u on u.id_usuario = m.id_remitente "
                + "where u.celular like '92200000_' and m.tipo = 'OFERTA'", Integer.class)).isEqualTo(1);

        assertThat(carga.cargar()).isNull();
    }

    private Map<String, Long> contar(String sql) {
        List<Map<String, Object>> filas = jdbc.queryForList(sql);
        return filas.stream().collect(Collectors.toMap(f -> (String) f.values().toArray()[0],
                f -> ((Number) f.values().toArray()[1]).longValue()));
    }
}
