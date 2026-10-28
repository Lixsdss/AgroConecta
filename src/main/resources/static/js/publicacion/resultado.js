/**
 * Piezas de una publicación disponible (RF-03): tarjeta de resultado y textos compartidos con el detalle.
 * Todo el texto se escribe con textContent.
 */
import { icono } from '../comun/iconos.js';
import { numero, soles, fechaCorta } from './borrador.js';
import { botonesContacto, puedeContactar } from '../mensajeria/contacto.js';

/** @returns {string} "Vence hoy", "Vence mañana" o "Vence en N días" */
export function textoVence(dias) {
    if (dias <= 0) {
        return 'Vence hoy';
    }
    return dias === 1 ? 'Vence mañana' : `Vence en ${dias} días`;
}

/** @returns {HTMLElement} etiqueta "Vence en …", en rojo si faltan 3 días o menos (ícono y texto, no solo color) */
export function etiquetaVence(p) {
    const etiqueta = document.createElement('span');
    etiqueta.className = p.porVencer ? 'ac-vence ac-vence-pronto' : 'ac-vence';
    const texto = document.createElement('span');
    texto.textContent = textoVence(p.diasRestantes);
    etiqueta.append(icono(p.porVencer ? 'hourglass' : 'calendar'), texto);
    return etiqueta;
}

/** @returns {string} "Mango Kent" (sin repetir "Común") */
export function titulo(p) {
    return p.variedad === 'Común' ? `${p.cultivo} común` : `${p.cultivo} ${p.variedad}`;
}

/** @returns {string} "≈ 25 jabas disponibles (≈ 500 kg)" */
export function textoDisponible(p) {
    const unidades = Number(p.unidadesDisponibles);
    const nombre = unidades === 1 ? p.unidad.toLowerCase() : p.unidadPlural;
    return `≈ ${numero(unidades)} ${nombre} disponibles (≈ ${numero(p.disponibleKg, 0)} kg)`;
}

/** @returns {string} "S/ 28.00 por jaba · ≈ S/ 1.40 por kilo" */
export function textoPrecio(p) {
    return `${soles(p.precioUnitario)} por ${p.unidad.toLowerCase()} · ≈ ${soles(p.precioKg)} por kilo`;
}

/** @returns {string} "★ 4.8" o "Sin calificaciones" */
export function textoCalificacion(p) {
    return p.calificacionAgricultor === null || p.calificacionAgricultor === undefined
        ? 'Sin calificaciones'
        : `★ ${numero(p.calificacionAgricultor, 1)}`;
}

function linea(nombreIcono, texto, clase = '') {
    const div = document.createElement('div');
    div.className = `d-flex align-items-center gap-2 ${clase}`;
    const span = document.createElement('span');
    span.textContent = texto;
    if (nombreIcono) {
        div.append(icono(nombreIcono));
    }
    div.append(span);
    return div;
}

/**
 * Crea la tarjeta de un resultado: enlace al detalle y, para el comprador o el visitante, "Escribir" y
 * "WhatsApp" (RF-04).
 *
 * @param {object} p publicación disponible de GET /api/v1/publicaciones
 * @param {(mensaje: string) => void} alFallar muestra el error de un contacto
 * @returns {HTMLElement} tarjeta
 */
export function tarjetaResultado(p, alFallar) {
    const tarjeta = document.createElement('article');
    tarjeta.className = 'ac-resultado';
    const enlace = document.createElement('a');
    enlace.className = 'ac-resultado-enlace';
    enlace.href = `/pages/publicacion/detalle.html?id=${encodeURIComponent(p.id)}`;

    const cabecera = document.createElement('div');
    cabecera.className = 'd-flex justify-content-between align-items-start gap-2 mb-1';
    const nombre = document.createElement('span');
    nombre.className = 'ac-resultado-titulo';
    nombre.textContent = titulo(p);
    cabecera.append(nombre, etiquetaVence(p));

    enlace.append(cabecera,
        linea(null, textoDisponible(p)),
        linea(null, textoPrecio(p), 'fw-semibold'),
        linea('map-pin', `${p.distrito}, ${p.provincia} · Cosecha ${fechaCorta(p.fechaCosecha)}`, 'ac-suave mt-1'),
        linea('user', `${p.agricultor} · ${textoCalificacion(p)}`, 'ac-suave'));
    tarjeta.append(enlace);
    if (puedeContactar()) {
        tarjeta.append(botonesContacto(p.id, alFallar));
    }
    return tarjeta;
}
