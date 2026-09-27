package pe.agroconecta.compartido.seguridad;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza el token de un usuario que ya no está ACTIVO (SUSPENDIDO, INACTIVO o eliminado), aunque la firma
 * y la vigencia sean válidas.
 */
public class ValidadorUsuarioActivo implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error USUARIO_NO_ACTIVO =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "El usuario no está activo", null);

    private final VerificadorEstadoUsuario verificador;

    /**
     * Crea el validador.
     *
     * @param verificador consulta del estado del usuario, implementada por el módulo {@code usuario}
     */
    public ValidadorUsuarioActivo(VerificadorEstadoUsuario verificador) {
        this.verificador = verificador;
    }

    /**
     * Valida que el claim {@code sub} sea el id de un usuario ACTIVO.
     *
     * @param token token ya verificado en firma y vigencia
     * @return éxito si el usuario está ACTIVO; error {@code invalid_token} en otro caso
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sujeto = token.getSubject();
        if (!NumberUtils.isDigits(sujeto) || !verificador.estaActivo(Long.parseLong(sujeto))) {
            return OAuth2TokenValidatorResult.failure(USUARIO_NO_ACTIVO);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
