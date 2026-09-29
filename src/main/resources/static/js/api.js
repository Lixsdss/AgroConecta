/**
 * Cliente HTTP de AgroConecta: URL base /api/v1, token JWT en la cabecera Authorization y errores con el
 * formato ErrorResponse de la API (SRS §9.2).
 */
import { obtenerToken, cerrarSesion } from './comun/sesion.js';

const URL_BASE = '/api/v1';

/**
 * Error devuelto por la API. Conserva el estado HTTP, el mensaje en español y los errores por campo.
 */
export class ErrorApi extends Error {
    /**
     * @param {number} estado código HTTP (0 si no hubo conexión)
     * @param {string} mensaje mensaje para mostrar al usuario
     * @param {Array<{campo: string, mensaje: string}>} errores errores por campo (solo en 400)
     */
    constructor(estado, mensaje, errores = []) {
        super(mensaje);
        this.estado = estado;
        this.errores = errores;
    }
}

/**
 * Hace una petición a la API.
 *
 * @param {string} metodo GET, POST, PUT, PATCH o DELETE
 * @param {string} ruta ruta relativa a /api/v1 (por ejemplo, '/auth/login')
 * @param {object|FormData} [cuerpo] objeto que se envía como JSON, o FormData para subir archivos
 * @returns {Promise<any>} cuerpo de la respuesta, o null si no tiene
 * @throws {ErrorApi} si la API responde con error o no hay conexión
 */
export async function solicitar(metodo, ruta, cuerpo) {
    const cabeceras = { 'Accept': 'application/json' };
    const token = obtenerToken();
    if (token) {
        cabeceras['Authorization'] = `Bearer ${token}`;
    }
    const esFormulario = cuerpo instanceof FormData;
    if (cuerpo !== undefined && !esFormulario) {
        cabeceras['Content-Type'] = 'application/json';
    }

    let respuesta;
    try {
        respuesta = await fetch(URL_BASE + ruta, {
            method: metodo,
            headers: cabeceras,
            body: cuerpo === undefined || esFormulario ? cuerpo : JSON.stringify(cuerpo)
        });
    } catch {
        throw new ErrorApi(0, 'No hay conexión. Revise su internet e intente de nuevo.');
    }

    const texto = await respuesta.text();
    let datos = null;
    try {
        datos = texto ? JSON.parse(texto) : null;
    } catch {
        datos = null; // Respuesta que no es JSON (por ejemplo, una página de error del servidor)
    }

    if (!respuesta.ok) {
        if (respuesta.status === 401 && token) {
            // La sesión venció o el usuario ya no está activo
            cerrarSesion();
        }
        const mensaje = datos?.mensaje ?? 'Ocurrió un problema. Intente de nuevo.';
        throw new ErrorApi(respuesta.status, mensaje, datos?.errores ?? []);
    }
    return datos;
}

/** Atajos por método HTTP. */
export const api = {
    get: (ruta) => solicitar('GET', ruta),
    post: (ruta, cuerpo) => solicitar('POST', ruta, cuerpo),
    put: (ruta, cuerpo) => solicitar('PUT', ruta, cuerpo),
    del: (ruta) => solicitar('DELETE', ruta)
};
