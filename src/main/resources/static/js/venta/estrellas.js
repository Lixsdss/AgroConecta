/**
 * Estrellas de las calificaciones (RF-05): texto para leer ("★★★★☆") con su descripción para lectores de pantalla,
 * y los textos de la hoja "Calificar" del diseño.
 */

/** Texto de cada puntuación en la hoja "¿Cómo le fue con…?" (índice = estrellas). */
export const TEXTOS_ESTRELLAS = ['Toque una estrella', 'Muy mal', 'Mal', 'Regular', 'Bien', 'Muy bien'];

/**
 * Crea las estrellas de una puntuación, para mostrar.
 *
 * @param {number} puntuacion estrellas (1 a 5; acepta decimales en un promedio)
 * @returns {HTMLSpanElement} elemento con "★★★★☆" y su aria-label
 */
export function estrellas(puntuacion) {
    const llenas = Math.round(Number(puntuacion));
    const span = document.createElement('span');
    span.className = 'ac-estrellas';
    span.textContent = '★'.repeat(llenas) + '☆'.repeat(5 - llenas);
    span.setAttribute('role', 'img');
    span.setAttribute('aria-label', `${Number(puntuacion).toLocaleString('es-PE', { maximumFractionDigits: 1 })} de 5 estrellas`);
    return span;
}

/**
 * Crea la tarjeta de una calificación: estrellas, autor, fecha y comentario.
 *
 * @param {{puntuacion: number, comentario: string|null, fecha: string, autor: string}} c calificación
 * @param {string} [titulo] texto antes del autor (por ejemplo, "Usted calificó")
 * @returns {HTMLElement} tarjeta
 */
export function tarjetaCalificacion(c, titulo) {
    const tarjeta = document.createElement('article');
    tarjeta.className = 'ac-calificacion';
    const fila = document.createElement('div');
    fila.className = 'd-flex justify-content-between align-items-center gap-2';
    const quien = document.createElement('strong');
    quien.textContent = titulo ?? c.autor;
    const fecha = document.createElement('span');
    fecha.className = 'ac-suave small';
    fecha.textContent = new Date(c.fecha).toLocaleDateString('es-PE');
    fila.append(quien, fecha);
    tarjeta.append(fila, estrellas(c.puntuacion));
    if (c.comentario) {
        const texto = document.createElement('p');
        texto.className = 'mb-0 mt-1';
        texto.textContent = `“${c.comentario}”`;
        tarjeta.append(texto);
    }
    return tarjeta;
}
