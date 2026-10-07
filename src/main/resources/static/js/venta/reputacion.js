/**
 * Bloque de reputación de un usuario (RF-05, SRS §7.2): "★ 4.8 · 23 calificaciones" y sus comentarios, con
 * "Ver más". GET /api/v1/reputaciones/{idUsuario}?page= (público).
 */
import { api } from '../api.js';
import { estrellas, tarjetaCalificacion } from './estrellas.js';

/**
 * Llena el contenedor con la reputación del usuario. Si falla, deja el contenedor oculto (es información extra).
 *
 * @param {HTMLElement} contenedor elemento donde va el bloque
 * @param {number} idUsuario usuario
 * @param {string} sinCalificaciones texto cuando aún no tiene calificaciones
 */
export async function mostrarReputacion(contenedor, idUsuario, sinCalificaciones) {
    let pagina = 0;
    let reputacion;
    try {
        reputacion = await api.get(`/reputaciones/${idUsuario}?page=0`);
    } catch {
        contenedor.hidden = true;
        return;
    }
    contenedor.replaceChildren();
    const resumen = document.createElement('p');
    resumen.className = 'd-flex align-items-center gap-2 mb-2';
    if (reputacion.total === 0) {
        resumen.textContent = sinCalificaciones;
        contenedor.append(resumen);
        contenedor.hidden = false;
        return;
    }
    const promedio = document.createElement('strong');
    promedio.className = 'fs-4';
    promedio.textContent = Number(reputacion.promedio).toLocaleString('es-PE', { minimumFractionDigits: 1 });
    const total = document.createElement('span');
    total.className = 'ac-suave';
    total.textContent = `${reputacion.total} ${reputacion.total === 1 ? 'calificación' : 'calificaciones'}`;
    resumen.append(promedio, estrellas(reputacion.promedio), total);

    const lista = document.createElement('div');
    lista.className = 'ac-calificaciones';
    const verMas = document.createElement('button');
    verMas.type = 'button';
    verMas.className = 'btn btn-link w-100';
    verMas.textContent = 'Ver más comentarios';

    function agregar(pag) {
        pag.content.forEach((c) => lista.append(tarjetaCalificacion(c)));
        verMas.hidden = pag.page.number + 1 >= pag.page.totalPages;
    }
    verMas.addEventListener('click', async () => {
        try {
            pagina += 1;
            agregar((await api.get(`/reputaciones/${idUsuario}?page=${pagina}`)).calificaciones);
        } catch {
            pagina -= 1;
        }
    });
    agregar(reputacion.calificaciones);
    contenedor.append(resumen, lista, verMas);
    contenedor.hidden = false;
}
