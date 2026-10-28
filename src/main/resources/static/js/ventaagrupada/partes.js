/**
 * Fila de una parte de propuesta conjunta (RF-10): agricultor, cantidad en su unidad, total y estado, con enlace a la
 * venta.
 */
import { icono } from '../comun/iconos.js';
import { numero, soles } from '../publicacion/borrador.js';
import { ESTADOS, textoEstado } from '../venta/estados.js';

/**
 * Crea el enlace de una parte.
 *
 * @param {object} p parte (ParteDto)
 * @returns {HTMLAnchorElement} fila
 */
export function filaParte(p) {
    const fila = document.createElement('a');
    fila.className = 'ac-calificacion d-block text-decoration-none';
    fila.style.color = 'inherit';
    fila.href = `/pages/venta/venta.html?id=${p.idVenta}`;
    const arriba = document.createElement('div');
    arriba.className = 'd-flex justify-content-between align-items-center gap-2';
    const nombre = document.createElement('strong');
    nombre.textContent = p.agricultor;
    const estado = document.createElement('span');
    estado.className = p.estado === 'RECHAZADA' || p.estado === 'CANCELADA'
        ? 'ac-estado-venta ac-estado-cancelada' : 'ac-estado-venta';
    estado.textContent = p.estado === 'PROPUESTA' ? 'Esperando respuesta' : textoEstado(p.estado);
    arriba.append(nombre, estado);
    const detalle = document.createElement('div');
    detalle.className = 'd-flex align-items-center gap-2 ac-suave';
    const unidad = Number(p.cantidad) === 1 ? p.unidad.toLowerCase() : p.unidadPlural;
    detalle.append(icono(ESTADOS[p.estado]?.[1] ?? 'layers'), Object.assign(document.createElement('span'), {
        textContent: `${numero(p.cantidad)} ${unidad} (${numero(p.cantidadKg, 0)} kg) · ${soles(p.precioUnitario)} por `
            + `${p.unidad.toLowerCase()} · Total ${soles(p.montoTotal)}`
    }));
    fila.append(arriba, detalle);
    return fila;
}
