package pe.agroconecta.compartido.seguridad;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import pe.agroconecta.compartido.error.ErrorResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde los 401 y 403 que genera Spring Security con el formato {@link ErrorResponse} (SRS §9.2), en lugar
 * de la respuesta vacía por defecto.
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MENSAJE_NO_AUTENTICADO = "Inicie sesión para continuar.";
    static final String MENSAJE_SIN_PERMISO = "No tiene permiso para realizar esta acción.";

    private final JsonMapper jsonMapper;

    /**
     * Crea el manejador.
     *
     * @param jsonMapper serializador JSON de la aplicación
     */
    public ManejadorErroresSeguridad(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Responde 401 cuando falta el token o no es válido (incluido un usuario que ya no está ACTIVO).
     *
     * @param request       petición rechazada
     * @param response      respuesta a escribir
     * @param authException causa del rechazo
     * @throws IOException si no se puede escribir la respuesta
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escribir(response, HttpStatus.UNAUTHORIZED, MENSAJE_NO_AUTENTICADO, request.getRequestURI());
    }

    /**
     * Responde 403 cuando el usuario autenticado no tiene el rol requerido.
     *
     * @param request               petición rechazada
     * @param response              respuesta a escribir
     * @param accessDeniedException causa del rechazo
     * @throws IOException si no se puede escribir la respuesta
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, MENSAJE_SIN_PERMISO, request.getRequestURI());
    }

    private void escribir(HttpServletResponse response, HttpStatus status, String mensaje, String ruta)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.de(status, mensaje, ruta));
    }
}
