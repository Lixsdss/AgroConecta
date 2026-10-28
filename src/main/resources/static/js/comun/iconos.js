/**
 * Íconos de Lucide servidos desde /img/iconos.svg (un solo archivo, se guarda en caché).
 * Uso en HTML: <svg class="ac-icono" aria-hidden="true"><use href="/img/iconos.svg#arrow-left"></use></svg>
 */
const SVG = 'http://www.w3.org/2000/svg';
const ARCHIVO = '/img/iconos.svg';

/** Ícono de cada cultivo del catálogo, según el diseño (tarjetas ilustradas del paso 1). */
export const ICONO_CULTIVO = { 'Mango': 'apple', 'Limón': 'citrus', 'Arroz': 'wheat', 'Plátano': 'banana' };

/**
 * Crea un ícono decorativo (oculto para lectores de pantalla: el texto de al lado lo describe).
 *
 * @param {string} nombre nombre del ícono de Lucide (por ejemplo, 'arrow-left')
 * @param {string} [claseExtra] clase adicional
 * @returns {SVGSVGElement} elemento SVG
 */
export function icono(nombre, claseExtra = '') {
    const svg = document.createElementNS(SVG, 'svg');
    svg.setAttribute('class', `ac-icono ${claseExtra}`.trim());
    svg.setAttribute('aria-hidden', 'true');
    svg.setAttribute('focusable', 'false');
    const uso = document.createElementNS(SVG, 'use');
    uso.setAttribute('href', `${ARCHIVO}#${nombre}`);
    svg.append(uso);
    return svg;
}

/**
 * Reemplaza el contenido de un elemento por un ícono y un texto.
 *
 * @param {HTMLElement} elemento botón o etiqueta
 * @param {string} nombre nombre del ícono
 * @param {string} texto texto visible
 */
export function ponerIconoYTexto(elemento, nombre, texto) {
    const span = document.createElement('span');
    span.textContent = texto;
    elemento.replaceChildren(icono(nombre), span);
}
