/**
 * Barra inferior del celular (diseño: navegación principal), para agricultor y comprador:
 * - Agricultor: Inicio · Vender · Mis ventas · Mensajes.
 * - Comprador: Inicio · Buscar · Mis compras · Mensajes ("Agrupados" llega con RF-10).
 * Se muestra solo en las pantallas principales; los pasos de "Vender", el chat y la entrega usan "atrás".
 * "Mensajes" muestra los no leídos (GET /api/v1/conversaciones/no-leidos, cada 30 segundos).
 */
import { api } from '../api.js';
import { obtenerSesion } from './sesion.js';
import { icono } from './iconos.js';

const CADA_MS = 30_000;

const OPCIONES = {
    AGRICULTOR: [
        ['inicio', 'Inicio', 'house', '/pages/usuario/inicio.html'],
        ['vender', 'Vender', 'sprout', '/pages/publicacion/paso1.html'],
        ['ventas', 'Mis ventas', 'clipboard-list', '/pages/venta/historial.html'],
        ['mensajes', 'Mensajes', 'message-circle', '/pages/mensajeria/conversaciones.html']
    ],
    COMPRADOR: [
        ['inicio', 'Inicio', 'house', '/pages/usuario/inicio.html'],
        ['buscar', 'Buscar', 'search', '/pages/publicacion/buscar.html'],
        ['ventas', 'Mis compras', 'clipboard-list', '/pages/venta/historial.html'],
        ['mensajes', 'Mensajes', 'message-circle', '/pages/mensajeria/conversaciones.html']
    ]
};

/**
 * Actualiza todos los contadores de no leídos de la página (elementos con {@code data-no-leidos}).
 */
export async function contarNoLeidos() {
    const insignias = document.querySelectorAll('[data-no-leidos]');
    if (insignias.length === 0) {
        return;
    }
    try {
        const { total } = await api.get('/conversaciones/no-leidos');
        insignias.forEach((insignia) => {
            insignia.textContent = total > 99 ? '99+' : String(total);
            insignia.setAttribute('aria-label', `${total} sin leer`);
            insignia.hidden = total === 0;
        });
    } catch {
        // El contador es opcional: se reintenta en la próxima consulta
    }
}

/**
 * Agrega la barra inferior si hay sesión de agricultor o comprador.
 *
 * @param {string|null} activa opción de la pantalla actual ('inicio', 'vender', 'buscar', 'ventas', 'mensajes'),
 *                             o null si la pantalla no está en la barra
 * @returns {boolean} true si se agregó
 */
export function ponerBarra(activa) {
    const sesion = obtenerSesion();
    const opciones = OPCIONES[sesion?.usuario.rol];
    if (!opciones) {
        return false;
    }
    const barra = document.createElement('nav');
    barra.className = 'ac-barra-inferior';
    barra.setAttribute('aria-label', 'Navegación principal');
    for (const [clave, texto, nombreIcono, ruta] of opciones) {
        const enlace = document.createElement('a');
        enlace.href = ruta;
        if (clave === activa) {
            enlace.setAttribute('aria-current', 'page');
        }
        const caja = document.createElement('span');
        caja.className = 'ac-barra-icono';
        caja.append(icono(nombreIcono));
        if (clave === 'mensajes') {
            const insignia = document.createElement('span');
            insignia.className = 'ac-no-leidos ac-barra-insignia';
            insignia.dataset.noLeidos = '';
            insignia.hidden = true;
            caja.append(insignia);
        }
        const etiqueta = document.createElement('span');
        etiqueta.textContent = texto;
        enlace.append(caja, etiqueta);
        barra.append(enlace);
    }
    document.body.classList.add('ac-con-barra');
    document.body.append(barra);
    contarNoLeidos();
    setInterval(() => {
        if (!document.hidden) {
            contarNoLeidos();
        }
    }, CADA_MS);
    return true;
}
