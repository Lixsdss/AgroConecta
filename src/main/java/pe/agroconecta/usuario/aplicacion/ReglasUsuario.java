package pe.agroconecta.usuario.aplicacion;

import org.apache.commons.lang3.StringUtils;

/**
 * Formatos de los datos de usuario (coinciden con los {@code CHECK} de {@code modelo-bd.sql}) y normalización de
 * los textos que escribe el usuario. Los usan la validación de {@code api} y los servicios.
 */
public final class ReglasUsuario {

    /** Celular peruano: 9 dígitos que empiezan en 9 ({@code chk_usuario_celular}). */
    public static final String PATRON_CELULAR = "^9[0-9]{8}$";

    /** Celular tal como lo escribe el usuario: se admiten espacios entre los dígitos. */
    public static final String PATRON_CELULAR_CON_ESPACIOS = "^\\s*9(\\s*[0-9]){8}\\s*$";

    /** RUC opcional: vacío o 11 dígitos que empiezan en 1 o 2 ({@code chk_comprador_ruc}). */
    public static final String PATRON_RUC_OPCIONAL = "^\\s*$|^\\s*[12][0-9]{10}\\s*$";

    /** Contraseña: 8 a 72 caracteres (límite de BCrypt) con al menos una letra y un número (UC-10). */
    public static final String PATRON_CONTRASENA = "^(?=.*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ])(?=.*[0-9]).{8,72}$";

    private ReglasUsuario() {
    }

    /**
     * Quita todos los espacios del celular.
     *
     * @param celular celular tal como lo escribió el usuario
     * @return celular sin espacios, o {@code null}
     */
    public static String normalizarCelular(String celular) {
        return StringUtils.deleteWhitespace(celular);
    }

    /**
     * Quita espacios sobrantes de un nombre (al inicio, al final y repetidos).
     *
     * @param texto nombre tal como lo escribió el usuario
     * @return nombre normalizado, o {@code null} si queda vacío
     */
    public static String normalizarTexto(String texto) {
        return StringUtils.trimToNull(StringUtils.normalizeSpace(texto));
    }

    /**
     * Oculta los dígitos centrales del celular para los logs (Ley N.° 29733).
     *
     * @param celular celular de 9 dígitos
     * @return celular enmascarado, por ejemplo {@code 98*****21}
     */
    public static String enmascararCelular(String celular) {
        if (StringUtils.length(celular) != 9) {
            return "*********";
        }
        return StringUtils.overlay(celular, "*****", 2, 7);
    }
}
