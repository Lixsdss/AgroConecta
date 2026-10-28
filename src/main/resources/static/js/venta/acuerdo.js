/**
 * "Registrar acuerdo" (diseño "acuerdo", RF-06 / UC-05). Se abre desde el chat con ?conversacion={id}.
 * GET /api/v1/conversaciones/{id} y /mensajes (última oferta), GET /api/v1/catalogo/unidades y POST /api/v1/ventas.
 * La unidad acordada puede ser distinta de la publicada: al cambiarla se convierten la cantidad y el precio con los
 * kilos por unidad, y para una jaba o saco distinta de la publicada se pide su peso.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { mostrarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';
import { crearChips } from '../comun/precios.js';
import { numero, soles } from '../publicacion/borrador.js';
import { producto } from '../mensajeria/formato.js';

/** @returns {string} "mango Kent" (el cultivo en minúscula dentro de una frase, la variedad como está) */
function enFrase(p) {
    return p.variedad === 'Común' ? `${p.cultivo.toLowerCase()} común` : `${p.cultivo.toLowerCase()} ${p.variedad}`;
}

const idConversacion = new URLSearchParams(window.location.search).get('conversacion');
const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const campos = formulario.elements;

let publicacion = null;
let unidades = [];
let elegida = null;

function poner(id, texto) {
    document.getElementById(id).textContent = texto;
}

function hoy() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function redondear(valor, decimales) {
    const f = 10 ** decimales;
    return Math.round(valor * f) / f;
}

function haciaAbajo(valor, decimales) {
    const f = 10 ** decimales;
    return Math.floor(valor * f + 1e-9) / f;
}

function esLaPublicada(u) {
    return u.nombre === publicacion.unidad;
}

/** @returns {number|null} kilos por unidad: del catálogo, de la publicación o el que escribe el usuario */
function pesoDe(u) {
    if (u.factorKg !== null) {
        return Number(u.factorKg);
    }
    if (esLaPublicada(u)) {
        return Number(publicacion.pesoUnidadKg);
    }
    const escrito = Number(campos.pesoUnidadKg.value);
    return escrito > 0 ? escrito : null;
}

function nombreUnidad(u, cantidad) {
    return Number(cantidad) === 1 ? u.nombre.toLowerCase() : u.nombrePlural;
}

function actualizar() {
    const peso = pesoDe(elegida);
    const cantidad = Number(campos.cantidad.value);
    const precio = Number(campos.precioUnitario.value);
    poner('etiqueta-cantidad', `Cantidad (${elegida.nombrePlural})`);
    poner('etiqueta-precio', `Precio por ${elegida.nombre.toLowerCase()}`);
    poner('etiqueta-peso', `¿Cuántos kilos pesa cada ${elegida.nombre.toLowerCase()}?`);
    document.getElementById('caja-peso').hidden = elegida.factorKg !== null || esLaPublicada(elegida);
    const disponibles = peso && peso !== 1
        ? ` (≈ ${numero(haciaAbajo(Number(publicacion.disponibleKg) / peso, 2))} ${elegida.nombrePlural})` : '';
    poner('disponible', `Disponible: ≈ ${numero(publicacion.disponibleKg, 0)} kilos${disponibles}`);
    if (!(cantidad > 0) || !(precio > 0)) {
        poner('resumen', '');
        poner('por-kilo', '');
        poner('total', '');
        return;
    }
    poner('resumen', peso && peso !== 1
        ? `${numero(cantidad)} ${nombreUnidad(elegida, cantidad)} = unos ${numero(cantidad * peso, 0)} kilos de ${enFrase(publicacion)}`
        : `${numero(cantidad)} ${nombreUnidad(elegida, cantidad)} de ${enFrase(publicacion)}`);
    poner('por-kilo', peso ? `≈ ${soles(precio / peso)} por kilo` : '');
    poner('total', `Total: ${soles(cantidad * precio)}`);
}

/** Cambia la unidad y convierte la cantidad y el precio por los kilos, si se conocen ambos pesos. */
function elegirUnidad(idUnidad) {
    const anterior = elegida;
    const nueva = unidades.find((u) => String(u.id) === String(idUnidad));
    const pesoAnterior = pesoDe(anterior);
    elegida = nueva;
    if (nueva.factorKg === null && !esLaPublicada(nueva)) {
        campos.pesoUnidadKg.value = '';
    }
    const pesoNuevo = pesoDe(nueva);
    const cantidad = Number(campos.cantidad.value);
    const precio = Number(campos.precioUnitario.value);
    if (pesoAnterior && pesoNuevo) {
        if (cantidad > 0) {
            campos.cantidad.value = haciaAbajo(cantidad * pesoAnterior / pesoNuevo, 2);
        }
        if (precio > 0) {
            campos.precioUnitario.value = redondear(precio / pesoAnterior * pesoNuevo, 2).toFixed(2);
        }
    }
    actualizar();
}

function sumar(paso) {
    const actual = Number(campos.cantidad.value) || 0;
    campos.cantidad.value = String(Math.max(1, Math.floor(actual) + paso));
    actualizar();
}

document.getElementById('menos').addEventListener('click', () => sumar(-1));
document.getElementById('mas').addEventListener('click', () => sumar(1));
formulario.addEventListener('input', actualizar);

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    const boton = document.getElementById('enviar');
    marcarOcupado(boton, true, 'Registrando…');
    try {
        const venta = await api.post('/ventas', {
            idConversacion: Number(idConversacion),
            idUnidad: elegida.id,
            cantidad: campos.cantidad.value || null,
            pesoUnidadKg: document.getElementById('caja-peso').hidden ? null : (campos.pesoUnidadKg.value || null),
            precioUnitario: campos.precioUnitario.value || null,
            fechaRecojo: campos.fechaRecojo.value || null
        });
        window.location.assign(`/pages/venta/venta.html?id=${venta.id}`);
    } catch (error) {
        mostrarErrores(formulario, alerta, error);
        marcarOcupado(boton, false);
    }
});

async function iniciar() {
    if (!requerirSesion()) {
        return;
    }
    if (!idConversacion || !/^\d+$/.test(idConversacion)) {
        mostrarAlerta(alerta, 'La conversación no existe.', 'error');
        return;
    }
    document.getElementById('volver').href = `/pages/mensajeria/chat.html?id=${idConversacion}`;
    try {
        const [conversacion, mensajes, catalogo] = await Promise.all([
            api.get(`/conversaciones/${idConversacion}`),
            api.get(`/conversaciones/${idConversacion}/mensajes`),
            api.get('/catalogo/unidades')]);
        publicacion = conversacion.publicacion;
        unidades = catalogo;
        if (publicacion.estado !== 'PUBLICADA') {
            mostrarAlerta(alerta, 'La cosecha ya no está disponible.', 'error');
            return;
        }
        poner('otra-parte', conversacion.otraParte.negocio ?? conversacion.otraParte.nombre);
        poner('producto', producto(publicacion));
        elegida = unidades.find(esLaPublicada) ?? unidades[0];

        // UC-05 paso 1: se proponen los datos de la última oferta (siempre en la unidad publicada)
        // sin pasar de lo que queda disponible ahora
        const oferta = mensajes.filter((m) => m.tipo === 'OFERTA').at(-1);
        const disponibles = Number(publicacion.unidadesDisponibles);
        campos.cantidad.value = Math.min(oferta ? Number(oferta.ofertaCantidad) : 1, disponibles);
        campos.precioUnitario.value = Number(oferta ? oferta.ofertaPrecioUnitario : publicacion.precioUnitario).toFixed(2);
        campos.fechaRecojo.min = hoy();
        campos.fechaRecojo.value = hoy();
        crearChips(document.getElementById('unidades'), 'idUnidad',
            unidades.map((u) => ({ valor: u.id, texto: u.nombre })), elegida.id, elegirUnidad);
        formulario.hidden = false;
        actualizar();
    } catch (error) {
        mostrarAlerta(alerta, error.estado === 403 ? 'Esta conversación no es suya.' : error.message, 'error');
    }
}

iniciar();
