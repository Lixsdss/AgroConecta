/**
 * Barra inferior del celular (diseño: navegación principal), para agricultor y comprador:
 * - Agricultor: Inicio · Vender · Mis ventas · Mensajes.
 * - Comprador: Inicio · Buscar · Mis compras · Mensajes ("Agrupados" llega con RF-10).
 * Se muestra solo en las pantallas principales; los pasos de "Vender", el chat y la entrega usan "atrás".
 * "Mensajes" muestra los no leídos (GET /api/v1/conversaciones/no-leidos) y la campana de avisos los avisos sin leer
 * (GET /api/v1/notificaciones/no-leidas), cada 30 segundos.
 * Desde RF-07, "Mis ventas" del agricultor es la pantalla de sus cosechas (diseño "ventas").
 */
import { api } from '../api.js';
import { obtenerSesion } from './sesion.js';
import { icono } from './iconos.js';

const CADA_MS = 30_000;

const OPCIONES = {
    AGRICULTOR: [
        ['inicio', 'Inicio', 'house', '/pages/usuario/inicio.html'],
        ['vender', 'Vender', 'sprout', '/pages/publicacion/paso1.html'],
        ['ventas', 'Mis ventas', 'clipboard-list', '/pages/publicacion/mis-cosechas.html'],
        ['mensajes', 'Mensajes', 'message-circle', '/pages/mensajeria/conversaciones.html']
    ],
    COMPRADOR: [
        ['inicio', 'Inicio', 'house', '/pages/usuario/inicio.html'],
        ['buscar', 'Buscar', 'search', '/pages/publicacion/buscar.html'],
        ['ventas', 'Mis compras', 'clipboard-list', '/pages/venta/historial.html'],
        ['mensajes', 'Mensajes', 'message-circle', '/pages/mensajeria/conversaciones.html']
    ]
};

/** Pone el total en las insignias de un selector. */
async function contar(selector, ruta) {
    const insignias = document.querySelectorAll(selector);
    if (insignias.length === 0) {
        return;
    }
    try {
        const { total } = await api.get(ruta);
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
 * Actualiza los contadores de la página: mensajes no leídos ({@code data-no-leidos}) y avisos sin leer
 * ({@code data-avisos-no-leidos}).
 */
export async function contarNoLeidos() {
    await Promise.all([
        contar('[data-no-leidos]', '/conversaciones/no-leidos'),
        contar('[data-avisos-no-leidos]', '/notificaciones/no-leidas')
    ]);
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
