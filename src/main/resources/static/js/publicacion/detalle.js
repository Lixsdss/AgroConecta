/**
 * Detalle de una publicación (UC-03 paso 7). Pública. GET /api/v1/publicaciones/{id}.
 * "Escribir" y "WhatsApp" llegan con RF-04 (slice 6).
 */
import { api } from '../api.js';
import { mostrarAlerta } from '../comun/formulario.js';
import { fechaCorta, soles } from './borrador.js';
import { etiquetaVence, textoCalificacion, textoDisponible, titulo } from './resultado.js';

const id = new URLSearchParams(window.location.search).get('id');

// Vuelve a la búsqueda con sus filtros, si se llegó desde ella
if (document.referrer.includes('/pages/publicacion/buscar.html')) {
    document.getElementById('volver').href = document.referrer;
}

function poner(idElemento, texto) {
    document.getElementById(idElemento).textContent = texto;
}

async function cargar() {
    if (!id || !/^\d+$/.test(id)) {
        mostrarAlerta(document.getElementById('alerta'), 'La publicación no existe.', 'error');
        return;
    }
    try {
        const p = await api.get(`/publicaciones/${id}`);
        document.title = `${titulo(p)} · AgroConecta`;
        poner('titulo', titulo(p));
        if (p.diasRestantes !== null) {
            document.getElementById('vence').replaceChildren(etiquetaVence(p));
        }
        poner('precio-unidad', `${soles(p.precioUnitario)} por ${p.unidad.toLowerCase()}`);
        poner('precio-kilo', `≈ ${soles(p.precioKg)} por kilo`);
        poner('disponible', textoDisponible(p));
        poner('lugar', `${p.distrito}, provincia de ${p.provincia}`);
        poner('cosecha', fechaCorta(p.fechaCosecha));
        poner('limite', fechaCorta(p.fechaLimite));
        poner('agricultor', `${p.agricultor} · ${textoCalificacion(p)}`);
        document.getElementById('detalle').hidden = false;
    } catch (e) {
        mostrarAlerta(document.getElementById('alerta'), e.message, 'error');
    }
}

cargar();
