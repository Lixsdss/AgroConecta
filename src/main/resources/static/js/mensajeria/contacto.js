/**
 * Botones "Escribir" y "WhatsApp" de una cosecha (RF-04, UC-04 pasos 1–3 y A1). Solo los ve el comprador o el
 * visitante; al visitante lo llevan a "Ingresar" y luego de vuelta a la misma página (UC-03 A2).
 * POST /api/v1/conversaciones y POST /api/v1/conversaciones/whatsapp.
 */
import { api } from '../api.js';
import { obtenerSesion, rutaIngresar } from '../comun/sesion.js';
import { ponerIconoYTexto } from '../comun/iconos.js';
import { marcarOcupado } from '../comun/formulario.js';

/** @returns {boolean} true si el usuario de la sesión puede contactar (comprador o visitante) */
export function puedeContactar() {
    const sesion = obtenerSesion();
    return !sesion || sesion.usuario.rol === 'COMPRADOR';
}

function irAIngresar() {
    window.location.assign(rutaIngresar(window.location.pathname + window.location.search));
}

/**
 * Abre el chat con la cosecha (lo crea si no existe).
 *
 * @param {number} idPublicacion publicación
 * @param {HTMLButtonElement} boton botón pulsado (se marca ocupado)
 * @param {(mensaje: string) => void} alFallar muestra el error
 */
export async function escribir(idPublicacion, boton, alFallar) {
    if (!obtenerSesion()) {
        irAIngresar();
        return;
    }
    marcarOcupado(boton, true, 'Abriendo…');
    try {
        const conversacion = await api.post('/conversaciones', { idPublicacion });
        window.location.assign(`/pages/mensajeria/chat.html?id=${conversacion.id}`);
    } catch (error) {
        marcarOcupado(boton, false);
        alFallar(error.message);
    }
}

/**
 * Registra el contacto por WhatsApp y abre WhatsApp con el agricultor.
 *
 * @param {number} idPublicacion publicación
 * @param {HTMLButtonElement} boton botón pulsado (se marca ocupado)
 * @param {(mensaje: string) => void} alFallar muestra el error
 */
export async function whatsapp(idPublicacion, boton, alFallar) {
    if (!obtenerSesion()) {
        irAIngresar();
        return;
    }
    marcarOcupado(boton, true, 'Abriendo…');
    try {
        const { enlace } = await api.post('/conversaciones/whatsapp', { idPublicacion });
        marcarOcupado(boton, false);
        window.location.assign(enlace);
    } catch (error) {
        marcarOcupado(boton, false);
        alFallar(error.message);
    }
}

/**
 * Crea la fila con los botones "Escribir" y "WhatsApp".
 *
 * @param {number} idPublicacion publicación
 * @param {(mensaje: string) => void} alFallar muestra el error
 * @returns {HTMLDivElement} fila de botones
 */
export function botonesContacto(idPublicacion, alFallar) {
    const fila = document.createElement('div');
    fila.className = 'd-flex gap-2 mt-2';
    const botonEscribir = document.createElement('button');
    botonEscribir.type = 'button';
    botonEscribir.className = 'btn btn-primary flex-grow-1';
    ponerIconoYTexto(botonEscribir, 'message-square-text', 'Escribir');
    botonEscribir.addEventListener('click', () => escribir(idPublicacion, botonEscribir, alFallar));
    const botonWhatsapp = document.createElement('button');
    botonWhatsapp.type = 'button';
    botonWhatsapp.className = 'btn btn-outline-primary flex-grow-1';
    ponerIconoYTexto(botonWhatsapp, 'message-circle', 'WhatsApp');
    botonWhatsapp.addEventListener('click', () => whatsapp(idPublicacion, botonWhatsapp, alFallar));
    fila.append(botonEscribir, botonWhatsapp);
    return fila;
}
