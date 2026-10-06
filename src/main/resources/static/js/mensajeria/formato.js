/**
 * Textos compartidos de la mensajería (RF-04): producto, hora, día y resumen de una oferta.
 */
import { numero, soles } from '../publicacion/borrador.js';

/** @returns {string} "Mango Kent" (sin repetir "Común") */
export function producto(p) {
    return p.variedad === 'Común' ? `${p.cultivo} común` : `${p.cultivo} ${p.variedad}`;
}

/** @returns {string} nombre de la unidad en singular o plural según la cantidad */
export function unidad(p, cantidad) {
    return Number(cantidad) === 1 ? p.unidad.toLowerCase() : p.unidadPlural;
}

/** @returns {string} "30 jabas a S/ 27.00 por jaba" */
export function textoOferta(p, cantidad, precio) {
    return `${numero(cantidad)} ${unidad(p, cantidad)} a ${soles(precio)} por ${p.unidad.toLowerCase()}`;
}

/** @returns {string} "10:02 a. m." */
export function hora(iso) {
    return new Date(iso).toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' });
}

/** @returns {string} "Hoy", "Ayer" o "lun. 5 oct." */
export function dia(iso) {
    const fecha = new Date(iso);
    const hoy = new Date();
    const ayer = new Date();
    ayer.setDate(hoy.getDate() - 1);
    if (fecha.toDateString() === hoy.toDateString()) {
        return 'Hoy';
    }
    if (fecha.toDateString() === ayer.toDateString()) {
        return 'Ayer';
    }
    return fecha.toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric', month: 'short' });
}

/** @returns {string} hora si es de hoy, "Ayer" o la fecha corta (lista de conversaciones) */
export function cuando(iso) {
    const d = dia(iso);
    return d === 'Hoy' ? hora(iso) : d;
}
