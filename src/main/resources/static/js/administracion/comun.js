/**
 * Partes comunes de las pantallas de escritorio del administrador (diseño 11a–11e): exige la sesión de un
 * ADMINISTRADOR, muestra su nombre e iniciales en el menú lateral y conecta "Salir".
 */
import { requerirSesion, cerrarSesion } from '../comun/sesion.js';

/**
 * Prepara la pantalla del administrador.
 *
 * @returns {object | null} la sesión, o null si se redirigió (sin sesión o con otro rol)
 */
export function iniciarAdministracion() {
    const sesion = requerirSesion();
    if (!sesion) {
        return null;
    }
    if (sesion.usuario.rol !== 'ADMINISTRADOR') {
        window.location.replace('/pages/usuario/inicio.html');
        return null;
    }
    const nombre = sesion.usuario.nombreCompleto;
    document.getElementById('nombre-admin').textContent = nombre;
    document.getElementById('iniciales').textContent =
        nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
    document.getElementById('salir').addEventListener('click', () => {
        cerrarSesion();
        window.location.assign('/pages/usuario/ingresar.html');
    });
    return sesion;
}

/**
 * Si la API respondió 401 (sesión vencida o cuenta no activa), lleva a "Ingresar".
 *
 * @param {Error} error error de la API
 * @returns {boolean} true si redirigió
 */
export function irAIngresarSi401(error) {
    if (error.estado === 401) {
        window.location.replace('/pages/usuario/ingresar.html');
        return true;
    }
    return false;
}

/**
 * Crea un elemento con clase y texto (el texto siempre con textContent).
 *
 * @param {string} etiqueta nombre de la etiqueta
 * @param {string} [clase] clases CSS
 * @param {string} [texto] texto
 * @returns {HTMLElement} el elemento
 */
export function crear(etiqueta, clase, texto) {
    const el = document.createElement(etiqueta);
    if (clase) {
        el.className = clase;
    }
    if (texto !== undefined && texto !== null) {
        el.textContent = texto;
    }
    return el;
}

/** Fecha y hora cortas en español de Perú. */
export function fechaHora(iso) {
    return iso ? new Date(iso).toLocaleString('es-PE', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—';
}

/** Número con hasta 2 decimales. */
export const numero = (v, d = 2) => Number(v).toLocaleString('es-PE', { maximumFractionDigits: d });

/** Monto en soles. */
export const soles = (v) => `S/ ${Number(v).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
