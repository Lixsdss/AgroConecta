/**
 * Borrador de "Vender mi cosecha" (RF-01): los 3 pasos lo comparten y se guarda en sessionStorage después de cada
 * cambio (indicador "Guardado" del diseño). Se borra al publicar o al cerrar la pestaña.
 */
import { requerirSesion } from '../comun/sesion.js';

const CLAVE = 'agroconecta.borrador-publicacion';
const CLAVE_PUBLICADA = 'agroconecta.publicacion-creada';
export const RUTA = '/pages/publicacion';

/**
 * Exige una sesión de agricultor; si no lo es, lo lleva al inicio.
 *
 * @returns {object | null} la sesión, o null si se redirigió
 */
export function exigirAgricultor() {
    const sesion = requerirSesion();
    if (sesion && sesion.usuario.rol !== 'AGRICULTOR') {
        window.location.replace('/pages/usuario/inicio.html');
        return null;
    }
    return sesion;
}

/** @returns {object} el borrador actual (vacío si no hay) */
export function leerBorrador() {
    try {
        return JSON.parse(sessionStorage.getItem(CLAVE)) ?? {};
    } catch {
        return {};
    }
}

/**
 * Agrega datos al borrador y muestra "Guardado".
 *
 * @param {object} cambios campos a guardar
 * @returns {object} el borrador completo
 */
export function guardarBorrador(cambios) {
    const borrador = { ...leerBorrador(), ...cambios };
    sessionStorage.setItem(CLAVE, JSON.stringify(borrador));
    const indicador = document.getElementById('guardado');
    if (indicador) {
        indicador.hidden = false;
    }
    return borrador;
}

/** Borra el borrador. */
export function borrarBorrador() {
    sessionStorage.removeItem(CLAVE);
}

/**
 * Si falta un dato de un paso anterior, vuelve a ese paso.
 *
 * @param {object} borrador borrador actual
 * @param {Array<[string, string]>} requisitos pares [campo, página a la que volver]
 * @returns {boolean} true si se redirigió
 */
export function volverSiFalta(borrador, requisitos) {
    for (const [campo, pagina] of requisitos) {
        if (borrador[campo] === undefined || borrador[campo] === null || borrador[campo] === '') {
            window.location.replace(`${RUTA}/${pagina}`);
            return true;
        }
    }
    return false;
}

/** @param {object} publicacion respuesta del POST, para la pantalla "Cosecha publicada" */
export function guardarPublicada(publicacion) {
    sessionStorage.setItem(CLAVE_PUBLICADA, JSON.stringify(publicacion));
}

/** @returns {object | null} la última publicación creada */
export function leerPublicada() {
    try {
        return JSON.parse(sessionStorage.getItem(CLAVE_PUBLICADA));
    } catch {
        return null;
    }
}

/** Fecha de hoy en formato AAAA-MM-DD, en la zona horaria del teléfono. */
export function hoy() {
    const d = new Date();
    const dos = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}`;
}

/**
 * Formatea un número al estilo peruano.
 *
 * @param {number} valor número
 * @param {number} decimales decimales máximos
 * @returns {string} número formateado
 */
export function numero(valor, decimales = 2) {
    return Number(valor).toLocaleString('es-PE', { maximumFractionDigits: decimales });
}

/** @returns {string} monto en soles, por ejemplo "S/ 1.40" */
export function soles(valor) {
    return `S/ ${Number(valor).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** @returns {string} fecha AAAA-MM-DD como "27/09/2026" */
export function fechaCorta(iso) {
    const [a, m, d] = iso.split('-');
    return `${d}/${m}/${a}`;
}

/**
 * Crea un botón de opción grande (tarjeta) con aria-pressed.
 *
 * @param {string} titulo texto principal
 * @param {string} [detalle] texto secundario
 * @returns {HTMLButtonElement} botón
 */
export function crearOpcion(titulo, detalle) {
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.className = 'ac-opcion';
    boton.setAttribute('aria-pressed', 'false');
    const fuerte = document.createElement('strong');
    fuerte.textContent = titulo;
    boton.append(fuerte);
    if (detalle) {
        const span = document.createElement('span');
        span.textContent = detalle;
        boton.append(span);
    }
    return boton;
}

/**
 * Marca una opción como elegida dentro de su grupo.
 *
 * @param {HTMLElement} grupo contenedor
 * @param {HTMLElement} elegida opción elegida
 */
export function marcarElegida(grupo, elegida) {
    grupo.querySelectorAll('[aria-pressed]').forEach((o) => o.setAttribute('aria-pressed', String(o === elegida)));
}
