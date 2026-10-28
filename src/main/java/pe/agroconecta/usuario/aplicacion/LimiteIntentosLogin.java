package pe.agroconecta.usuario.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pe.agroconecta.compartido.error.DemasiadosIntentosException;

/**
 * Límite de intentos de inicio de sesión (RF-08, seguridad): tras {@value #MAXIMO_FALLOS} intentos fallidos con
 * el mismo celular, los siguientes se rechazan hasta que pasan 15 minutos desde el primer fallo.
 *
 * <p>Los contadores viven en memoria (Guava Cache con {@code expireAfterWrite}), sin infraestructura externa:
 * se pierden al reiniciar la aplicación y no se comparten entre instancias. El tiempo se toma del {@link Clock}
 * de la aplicación para que las pruebas puedan simular el paso del tiempo.</p>
 */
@Component
public class LimiteIntentosLogin {

    /** Intentos fallidos permitidos dentro de la ventana. */
    public static final int MAXIMO_FALLOS = 5;

    /** Duración de la ventana, contada desde el primer intento fallido. */
    public static final Duration VENTANA = Duration.ofMinutes(15);

    static final String MENSAJE_BLOQUEO = "Demasiados intentos fallidos. Espere 15 minutos e intente de nuevo.";

    /** Tope de celulares en memoria, para que intentos con celulares al azar no agoten la memoria. */
    private static final long MAXIMO_CELULARES = 10_000;

    private static final Logger LOG = LoggerFactory.getLogger(LimiteIntentosLogin.class);

    private final Cache<String, AtomicInteger> fallos;

    /**
     * Crea el límite.
     *
     * @param reloj reloj de la aplicación
     */
    public LimiteIntentosLogin(Clock reloj) {
        this.fallos = CacheBuilder.newBuilder()
                .expireAfterWrite(VENTANA)
                .maximumSize(MAXIMO_CELULARES)
                .ticker(tickerDe(reloj))
                .build();
    }

    /**
     * Rechaza el intento si el celular ya agotó sus intentos en la ventana vigente.
     *
     * @param celular celular normalizado
     * @throws DemasiadosIntentosException si el celular está bloqueado
     */
    public void verificarPermitido(String celular) {
        AtomicInteger contador = fallos.getIfPresent(celular);
        if (contador != null && contador.get() >= MAXIMO_FALLOS) {
            throw new DemasiadosIntentosException(MENSAJE_BLOQUEO);
        }
    }

    /**
     * Cuenta un intento fallido. Al llegar al máximo, registra el bloqueo con el celular enmascarado.
     *
     * @param celular celular normalizado
     */
    public void registrarFallo(String celular) {
        // La entrada se escribe solo en el primer fallo: la ventana se cuenta desde ese momento.
        // Cache.get respeta el vencimiento; asMap().computeIfAbsent reutilizaría un contador ya vencido.
        int total = contadorDe(celular).incrementAndGet();
        if (total == MAXIMO_FALLOS) {
            LOG.warn("Inicio de sesión bloqueado por {} intentos fallidos: celular={}", MAXIMO_FALLOS,
                    ReglasUsuario.enmascararCelular(celular));
        }
    }

    /**
     * Borra el contador del celular tras un inicio de sesión correcto.
     *
     * @param celular celular normalizado
     */
    public void reiniciar(String celular) {
        fallos.invalidate(celular);
    }

    private AtomicInteger contadorDe(String celular) {
        try {
            return fallos.get(celular, AtomicInteger::new);
        } catch (ExecutionException e) {
            // AtomicInteger::new no lanza excepciones; no debería ocurrir
            throw new IllegalStateException(e);
        }
    }

    private static Ticker tickerDe(Clock reloj) {
        return new Ticker() {
            @Override
            public long read() {
                return TimeUnit.MILLISECONDS.toNanos(reloj.millis());
            }
        };
    }
}
