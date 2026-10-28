package pe.agroconecta.usuario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import pe.agroconecta.compartido.error.DemasiadosIntentosException;

class LimiteIntentosLoginTest {

    private static final String CELULAR = "911111111";

    private final RelojManual reloj = new RelojManual(Instant.parse("2026-10-24T15:00:00Z"));
    private final LimiteIntentosLogin limite = new LimiteIntentosLogin(reloj);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(LimiteIntentosLogin.class);

    @BeforeEach
    void capturarLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        logger.detachAppender(logs);
    }

    @Test
    void permiteCuatroFallosYBloqueaAlQuinto() {
        fallar(4);
        assertThatCode(() -> limite.verificarPermitido(CELULAR)).doesNotThrowAnyException();

        fallar(1);
        assertThatThrownBy(() -> limite.verificarPermitido(CELULAR))
                .isInstanceOf(DemasiadosIntentosException.class)
                .hasMessage(LimiteIntentosLogin.MENSAJE_BLOQUEO);
    }

    @Test
    void laVentanaSeCuentaDesdeElPrimerFalloYAlVencerEmpiezaDeCero() {
        fallar(1);
        reloj.avanzar(Duration.ofMinutes(10));
        fallar(4);
        assertThatThrownBy(() -> limite.verificarPermitido(CELULAR)).isInstanceOf(DemasiadosIntentosException.class);

        reloj.avanzar(Duration.ofMinutes(5).plusSeconds(1));
        assertThatCode(() -> limite.verificarPermitido(CELULAR)).doesNotThrowAnyException();

        // Tras vencer, un fallo nuevo cuenta como el primero de otra ventana
        fallar(4);
        assertThatCode(() -> limite.verificarPermitido(CELULAR)).doesNotThrowAnyException();
    }

    @Test
    void reiniciarBorraElContador() {
        fallar(5);
        limite.reiniciar(CELULAR);

        assertThatCode(() -> limite.verificarPermitido(CELULAR)).doesNotThrowAnyException();
    }

    @Test
    void cadaCelularTieneSuPropioContador() {
        fallar(5);

        assertThatCode(() -> limite.verificarPermitido("922222222")).doesNotThrowAnyException();
    }

    @Test
    void registraElBloqueoUnaSolaVezYConElCelularEnmascarado() {
        fallar(7);

        assertThat(logs.list).hasSize(1);
        String mensaje = logs.list.getFirst().getFormattedMessage();
        assertThat(mensaje).contains("91*****11").doesNotContain(CELULAR);
    }

    private void fallar(int veces) {
        for (int i = 0; i < veces; i++) {
            limite.registrarFallo(CELULAR);
        }
    }

    /** Reloj que solo avanza cuando la prueba lo pide. */
    private static final class RelojManual extends Clock {

        private Instant ahora;

        RelojManual(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Lima");
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
