/**
 * Sesión del usuario en el navegador. El token vive en sessionStorage (no en localStorage, SRS §9.3):
 * se borra al cerrar la pestaña.
 */
const CLAVE = 'agroconecta.sesion';
const RAIZ = '/pages/usuario';

/**
 * Guarda la sesión que devuelven el registro o el inicio de sesión.
 *
 * @param {{token: string, expiraEn: string, usuario: {id: number, nombreCompleto: string, rol: string}}} sesion
 */
export function guardarSesion(sesion) {
    sessionStorage.setItem(CLAVE, JSON.stringify({
        token: sesion.token,
        expiraEn: sesion.expiraEn,
        usuario: sesion.usuario
    }));
}

/**
 * Devuelve la sesión guardada si todavía no venció.
 *
 * @returns {{token: string, expiraEn: string, usuario: object} | null}
 */
export function obtenerSesion() {
    const texto = sessionStorage.getItem(CLAVE);
    if (!texto) {
        return null;
    }
    try {
        const sesion = JSON.parse(texto);
        if (new Date(sesion.expiraEn) <= new Date()) {
            cerrarSesion();
            return null;
        }
        return sesion;
    } catch {
        cerrarSesion();
        return null;
    }
}

/** @returns {string | null} token JWT vigente */
export function obtenerToken() {
    return obtenerSesion()?.token ?? null;
}

/** Borra la sesión del navegador. */
export function cerrarSesion() {
    sessionStorage.removeItem(CLAVE);
}

/**
 * Exige una sesión vigente; si no hay, lleva a "Ingresar".
 *
 * @returns {object | null} la sesión, o null si se redirigió
 */
export function requerirSesion() {
    const sesion = obtenerSesion();
    if (!sesion) {
        window.location.replace(`${RAIZ}/ingresar.html`);
        return null;
    }
    return sesion;
}

/** Lleva al usuario a su inicio (la página muestra lo que corresponde a su rol). */
export function irAlInicio() {
    window.location.assign(`${RAIZ}/inicio.html`);
}
