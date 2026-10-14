/**
 * Piezas compartidas de los precios de referencia (RF-02): tarjeta "Precios de hoy", textos de tendencia y
 * fuente, y formato de soles. Todo el texto se escribe con textContent.
 */
import { icono } from './iconos.js';

/** Fuentes del precio (chk_precio_fuente), con el texto del diseño. */
export const FUENTES = {
    MERCADO_MAYORISTA_PIURA: 'Mercado Mayorista de Piura',
    MERCADO_SULLANA: 'Mercado de Sullana',
    ENCUESTA_ACOPIADORES: 'Encuesta a acopiadores'
};

/** Tendencia de v_precios_vigentes: texto e ícono (el color nunca va solo). */
export const TENDENCIAS = {
    SUBE: ['Sube', 'trending-up'],
    BAJA: ['Baja', 'trending-down'],
    IGUAL: ['Igual', 'minus'],
    SIN_DATO: ['Primer precio', 'minus']
};

/** @returns {string} monto en soles, por ejemplo "S/ 1.40" */
export function soles(valor) {
    return `S/ ${Number(valor).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** @returns {string} "hoy, 7:30 a. m." o "27/09/2026" */
export function textoActualizado(isoLocal) {
    const fecha = new Date(isoLocal);
    const hoy = new Date();
    if (fecha.toDateString() === hoy.toDateString()) {
        return `hoy, ${fecha.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' })}`;
    }
    return fecha.toLocaleDateString('es-PE');
}

/** @returns {string} fecha AAAA-MM-DD como "28 sep." */
export function fechaCorta(iso) {
    return new Date(`${iso}T00:00:00`).toLocaleDateString('es-PE', { day: 'numeric', month: 'short' });
}

/**
 * Crea la tarjeta de un precio vigente.
 *
 * @param {object} p precio vigente de GET /api/v1/precios/vigentes
 * @param {{compacta?: boolean, conZona?: boolean}} [opciones] compacta: sin la línea "Actualizado" (inicio,
 *        diseños 06 y 14); conZona: muestra la zona junto al cultivo (por defecto sí)
 * @returns {HTMLElement} tarjeta
 */
export function tarjetaPrecio(p, { compacta = false, conZona = true } = {}) {
    const tarjeta = document.createElement('article');
    tarjeta.className = 'ac-tarjeta-precio';

    const cabecera = document.createElement('div');
    cabecera.className = 'd-flex justify-content-between align-items-center gap-2';
    const nombre = document.createElement('strong');
    nombre.textContent = p.cultivo;
    const zona = document.createElement('span');
    zona.className = 'ac-suave fw-normal';
    zona.textContent = ` · ${p.zona}`;
    if (conZona) {
        nombre.append(zona);
    }
    const [textoTendencia, iconoTendencia] = TENDENCIAS[p.tendencia] ?? TENDENCIAS.SIN_DATO;
    const tendencia = document.createElement('span');
    tendencia.className = `ac-tendencia ac-tendencia-${p.tendencia.toLowerCase()}`;
    const etiqueta = document.createElement('span');
    etiqueta.textContent = textoTendencia;
    tendencia.append(icono(iconoTendencia), etiqueta);
    cabecera.append(nombre, tendencia);

    const precio = document.createElement('div');
    precio.className = 'ac-precio-kilo';
    precio.textContent = soles(p.precioKg);
    const porKilo = document.createElement('span');
    porKilo.className = 'fw-normal fs-6';
    porKilo.textContent = ' por kilo';
    precio.append(porKilo);

    tarjeta.append(cabecera, precio);
    if (p.precioUnidadLocal !== null) {
        const local = document.createElement('div');
        local.textContent = `≈ ${soles(p.precioUnidadLocal)} por ${p.unidadLocal.toLowerCase()}`;
        tarjeta.append(local);
    }
    if (!compacta) {
        const pie = document.createElement('div');
        pie.className = 'ac-suave small mt-1';
        pie.textContent = `Actualizado ${textoActualizado(p.actualizado)} · ${FUENTES[p.fuente] ?? p.fuente}`;
        tarjeta.append(pie);
    }
    return tarjeta;
}

/**
 * Llena un contenedor con tarjetas de precios, o con un aviso si no hay.
 *
 * @param {HTMLElement} contenedor contenedor
 * @param {Array<object>} precios precios vigentes
 * @param {string} [mensajeVacio] texto cuando no hay precios
 * @param {{compacta?: boolean, conZona?: boolean}} [opciones] opciones de cada tarjeta
 */
export function mostrarTarjetas(contenedor, precios, mensajeVacio = 'Todavía no hay precios cargados.', opciones = {}) {
    contenedor.replaceChildren();
    if (precios.length === 0) {
        const vacio = document.createElement('p');
        vacio.className = 'ac-suave';
        vacio.textContent = mensajeVacio;
        contenedor.append(vacio);
        return;
    }
    precios.forEach((p) => contenedor.append(tarjetaPrecio(p, opciones)));
}

/**
 * Crea un grupo de chips de elección única (radios) y avisa cuando cambia.
 *
 * @param {HTMLElement} contenedor elemento con la clase ac-chips
 * @param {string} nombre nombre del grupo de radios
 * @param {Array<{valor: string|number, texto: string}>} opciones opciones en orden
 * @param {string|number} elegido valor marcado al inicio
 * @param {(valor: string) => void} alCambiar función que recibe el valor elegido
 */
export function crearChips(contenedor, nombre, opciones, elegido, alCambiar) {
    contenedor.replaceChildren();
    for (const { valor, texto } of opciones) {
        const id = `${nombre}-${valor}`;
        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = nombre;
        radio.id = id;
        radio.value = String(valor);
        radio.checked = String(valor) === String(elegido);
        radio.addEventListener('change', () => alCambiar(radio.value));
        const etiqueta = document.createElement('label');
        etiqueta.htmlFor = id;
        etiqueta.textContent = texto;
        contenedor.append(radio, etiqueta);
    }
}
