package pe.agroconecta.compartido.error;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import pe.agroconecta.compartido.error.ErrorResponse.ErrorCampo;

/**
 * Traduce las excepciones de todos los controladores al formato {@link ErrorResponse}.
 *
 * <ul>
 * <li>Errores de Bean Validation: 400 con el mensaje de cada campo.</li>
 * <li>Errores de los triggers ({@code SIGNAL SQLSTATE '45000'}): 422 con el texto del trigger.</li>
 * <li>Violaciones de {@code CHECK}, clave única o clave foránea: 409 con un mensaje genérico, sin detalles de SQL.</li>
 * <li>Cualquier otro error: 500 con un mensaje genérico; el detalle solo va al log.</li>
 * </ul>
 *
 * <p>Las excepciones de Spring Security no se manejan aquí: se relanzan para que Spring Security
 * responda 401 o 403.</p>
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    /** SQLSTATE que usan los triggers de {@code modelo-bd.sql} para rechazar una regla de negocio. */
    static final String SQLSTATE_TRIGGER = "45000";

    private static final int MYSQL_CLAVE_DUPLICADA = 1062;
    private static final Set<Integer> MYSQL_CLAVE_FORANEA = Set.of(1451, 1452);
    /** 3819: CHECK en MySQL; 4025: CHECK en MariaDB. */
    private static final Set<Integer> MYSQL_CHECK = Set.of(3819, 4025);

    static final String MENSAJE_VALIDACION = "Los datos enviados no son válidos.";
    static final String MENSAJE_DUPLICADO = "Ya existe un registro con esos datos.";
    static final String MENSAJE_CLAVE_FORANEA = "La operación hace referencia a datos que no existen o que están en uso.";
    static final String MENSAJE_CHECK = "Los datos no cumplen las reglas del sistema.";
    static final String MENSAJE_INTERNO = "Ocurrió un error interno. Intente nuevamente más tarde.";

    /**
     * Traduce los errores de acceso a datos según el error de base de datos que los originó.
     *
     * @param ex excepción de Spring Data, JPA o Hibernate
     * @return 422 si lo rechazó un trigger, 409 si violó una restricción de integridad, 500 en otro caso
     */
    @ExceptionHandler({DataAccessException.class, PersistenceException.class})
    public ResponseEntity<ErrorResponse> manejarErrorBaseDatos(RuntimeException ex) {
        Optional<SQLException> sql = buscarSqlException(ex);
        if (sql.isPresent()) {
            SQLException causa = sql.get();
            if (SQLSTATE_TRIGGER.equals(causa.getSQLState())) {
                LOG.debug("Regla de negocio rechazada por la base de datos: {}", causa.getMessage());
                return responder(HttpStatus.UNPROCESSABLE_CONTENT, causa.getMessage());
            }
            Optional<String> mensajeIntegridad = mensajeIntegridad(causa.getErrorCode());
            if (mensajeIntegridad.isPresent()) {
                LOG.warn("Violación de integridad (código {}): {}", causa.getErrorCode(), causa.getMessage());
                return responder(HttpStatus.CONFLICT, mensajeIntegridad.get());
            }
        }
        return errorInterno(ex);
    }

    /**
     * Traduce las violaciones de Bean Validation lanzadas fuera de los controladores (por ejemplo, en
     * servicios anotados con {@code @Validated}).
     *
     * @param ex excepción con las violaciones
     * @return 400 con el mensaje de cada campo
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarViolacionRestricciones(ConstraintViolationException ex) {
        List<ErrorCampo> errores = ex.getConstraintViolations().stream()
                .map(v -> new ErrorCampo(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST, MENSAJE_VALIDACION, errores));
    }

    /**
     * Responde 500 ante cualquier error no previsto y lo registra en el log. Las excepciones de
     * Spring Security se relanzan para que las resuelva su propio filtro (401 o 403).
     *
     * @param ex excepción no prevista
     * @return 500 con un mensaje genérico
     * @throws Exception la misma excepción, si es de Spring Security
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(Exception ex) throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        return errorInterno(ex);
    }

    /** {@inheritDoc} Responde 400 con el mensaje de cada campo del cuerpo inválido. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().headers(headers)
                .body(ErrorResponse.de(HttpStatus.BAD_REQUEST, MENSAJE_VALIDACION, errores));
    }

    /** {@inheritDoc} Responde 400 con el mensaje de cada parámetro inválido del método del controlador. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ErrorCampo(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return ResponseEntity.badRequest().headers(headers)
                .body(ErrorResponse.de(HttpStatus.BAD_REQUEST, MENSAJE_VALIDACION, errores));
    }

    /**
     * {@inheritDoc} Usa el formato {@link ErrorResponse} para las demás excepciones de Spring MVC
     * (404, 405, 415, JSON mal formado, etc.) conservando su código de estado.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            LOG.error("Error interno de Spring MVC", ex);
        }
        return ResponseEntity.status(statusCode).headers(headers)
                .body(ErrorResponse.de(statusCode, mensajePorEstado(statusCode)));
    }

    private static Optional<SQLException> buscarSqlException(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                return Optional.of(sql);
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> mensajeIntegridad(int codigoError) {
        if (codigoError == MYSQL_CLAVE_DUPLICADA) {
            return Optional.of(MENSAJE_DUPLICADO);
        }
        if (MYSQL_CLAVE_FORANEA.contains(codigoError)) {
            return Optional.of(MENSAJE_CLAVE_FORANEA);
        }
        if (MYSQL_CHECK.contains(codigoError)) {
            return Optional.of(MENSAJE_CHECK);
        }
        return Optional.empty();
    }

    private static String mensajePorEstado(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return MENSAJE_INTERNO;
        }
        return switch (status.value()) {
            case 400 -> "La solicitud no es válida.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "Método HTTP no permitido para este recurso.";
            case 406 -> "No se puede generar la respuesta en el formato solicitado.";
            case 415 -> "Tipo de contenido no soportado.";
            default -> "La solicitud no se pudo procesar.";
        };
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status, mensaje));
    }

    private static ResponseEntity<ErrorResponse> errorInterno(Exception ex) {
        LOG.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_INTERNO);
    }
}
