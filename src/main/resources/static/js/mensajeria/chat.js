/**
 * Chat (diseño "chat", RF-04 / UC-04): mensajes de texto y ofertas entre el comprador y el agricultor.
 * GET /api/v1/conversaciones/{id}, GET y POST /api/v1/conversaciones/{id}/mensajes. Los mensajes nuevos se
 * consultan cada 10 segundos (solo los posteriores al último recibido). "Registrar acuerdo" y la franja de ventas de la
 * conversación (GET /api/v1/ventas?idConversacion=) son de RF-06.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { icono } from '../comun/iconos.js';
import {
    mostrarAlerta, ocultarAlerta, limpiarErrores, mostrarErrores, marcarOcupado
} from '../comun/formulario.js';
import { numero, soles } from '../publicacion/borrador.js';
import { dia, hora, producto, textoOferta, unidad } from './formato.js';
import { ESTADOS, textoEstado } from '../venta/estados.js';

const CADA_MS = 10_000;
const CABECERA_CADA_MS = 30_000;

const id = new URLSearchParams(window.location.search).get('id');
const alerta = document.getElementById('alerta');
const contenedor = document.getElementById('mensajes');
const formTexto = document.getElementById('form-texto');
const campoTexto = document.getElementById('texto');
const botonOferta = document.getElementById('hacer-oferta');
const dialogo = document.getElementById('dialogo-oferta');
const formOferta = document.getElementById('form-oferta');
const alertaOferta = document.getElementById('alerta-oferta');

let conversacion = null;
let ultimoId = 0;
let ultimoDia = null;
let enviando = false;

function poner(idElemento, texto) {
    document.getElementById(idElemento).textContent = texto;
}

function cercaDelFinal() {
    return window.innerHeight + window.scrollY >= document.body.scrollHeight - 160;
}

function irAlFinal() {
    window.scrollTo({ top: document.body.scrollHeight });
}

/** Muestra la cabecera: la otra persona y la cosecha. */
function mostrarCabecera(c) {
    conversacion = c;
    const p = c.publicacion;
    const nombre = c.otraParte.negocio ? `${c.otraParte.negocio} · ${c.otraParte.nombre}` : c.otraParte.nombre;
    poner('otra-parte', nombre);
    document.title = `${c.otraParte.negocio ?? c.otraParte.nombre} · AgroConecta`;
    poner('producto-titulo', `${producto(p)} · ${p.distrito}`);
    poner('producto-cantidad',
        `≈ ${numero(p.unidadesDisponibles)} ${unidad(p, p.unidadesDisponibles)} · ≈ ${numero(p.disponibleKg, 0)} kilos`);
    poner('producto-precio', `${soles(p.precioUnitario)} por ${p.unidad.toLowerCase()} · ≈ ${soles(p.precioKg)} por kilo`);
    const vence = p.diasRestantes === null || p.estado !== 'PUBLICADA' ? ''
        : ` · Vence en ${p.diasRestantes} ${p.diasRestantes === 1 ? 'día' : 'días'}`;
    poner('producto-peso', `Cada ${p.unidad.toLowerCase()} pesa ${numero(p.pesoUnidadKg)} kilos${vence}`);
    document.getElementById('producto').hidden = false;
    const disponible = p.estado === 'PUBLICADA';
    botonOferta.hidden = !disponible;
    const acuerdo = document.getElementById('registrar-acuerdo');
    acuerdo.hidden = !disponible;
    acuerdo.href = `/pages/venta/acuerdo.html?conversacion=${c.id}`;
    document.getElementById('no-disponible').hidden = disponible;
}

function burbujaTexto(m) {
    const div = document.createElement('div');
    div.className = m.mio ? 'ac-burbuja ac-burbuja-mia' : 'ac-burbuja ac-burbuja-ajena';
    const texto = document.createElement('span');
    texto.textContent = m.contenido;
    const cuando = document.createElement('span');
    cuando.className = 'ac-hora';
    cuando.textContent = hora(m.fechaEnvio);
    div.append(texto, cuando);
    return div;
}

function tarjetaOferta(m) {
    const p = conversacion.publicacion;
    const div = document.createElement('div');
    div.className = m.mio ? 'ac-oferta ac-oferta-mia' : 'ac-oferta ac-oferta-ajena';
    const etiqueta = document.createElement('span');
    etiqueta.className = 'ac-etiqueta-oferta';
    const textoEtiqueta = document.createElement('span');
    textoEtiqueta.textContent = m.mio ? 'Su oferta' : 'Oferta';
    etiqueta.append(icono('hand-coins'), textoEtiqueta);
    const titulo = document.createElement('strong');
    titulo.className = 'fs-5';
    titulo.textContent = textoOferta(p, m.ofertaCantidad, m.ofertaPrecioUnitario);
    const detalle = document.createElement('span');
    detalle.textContent = `≈ ${numero(m.ofertaKg, 0)} kilos · Total ${soles(m.ofertaTotal)}`;
    const cuando = document.createElement('span');
    cuando.className = 'ac-hora';
    cuando.textContent = hora(m.fechaEnvio);
    div.append(etiqueta, titulo, detalle, cuando);
    return div;
}

/** Agrega mensajes al final, con separadores de día. */
function agregar(mensajes) {
    if (mensajes.length === 0) {
        return;
    }
    const bajar = cercaDelFinal() || ultimoId === 0 || mensajes.some((m) => m.mio);
    for (const m of mensajes) {
        if (m.id <= ultimoId) {
            continue; // Ya se mostró (por ejemplo, el propio mensaje recién enviado)
        }
        const d = dia(m.fechaEnvio);
        if (d !== ultimoDia) {
            const separador = document.createElement('div');
            separador.className = 'ac-dia';
            separador.textContent = d;
            contenedor.append(separador);
            ultimoDia = d;
        }
        contenedor.append(m.tipo === 'OFERTA' ? tarjetaOferta(m) : burbujaTexto(m));
        ultimoId = m.id;
    }
    if (bajar) {
        irAlFinal();
    }
}

/** Franja con las ventas de esta conversación: estado y enlace a la entrega. */
async function mostrarVentas() {
    try {
        const respuesta = await api.get(`/ventas?idConversacion=${id}`);
        const contenedor = document.getElementById('ventas');
        contenedor.replaceChildren(...respuesta.content.map((v) => {
            const enlace = document.createElement('a');
            enlace.className = 'ac-producto-chat text-decoration-none';
            enlace.href = `/pages/venta/venta.html?id=${v.id}`;
            const texto = document.createElement('span');
            texto.className = 'flex-grow-1';
            const titulo = document.createElement('strong');
            titulo.className = 'd-block';
            titulo.textContent = textoEstado(v.estado);
            const detalle = document.createElement('span');
            detalle.className = 'ac-suave';
            const u = Number(v.cantidad) === 1 ? v.unidad.toLowerCase() : v.unidadPlural;
            detalle.textContent = `${numero(v.cantidad)} ${u} · Total ${soles(v.montoTotal)} · Ver entrega`;
            texto.append(titulo, detalle);
            enlace.append(icono(ESTADOS[v.estado]?.[1] ?? 'handshake'), texto, icono('chevron-right'));
            return enlace;
        }));
    } catch {
        // La franja es informativa: se reintenta con la próxima actualización de la cabecera
    }
}

async function traerNuevos() {
    try {
        agregar(await api.get(`/conversaciones/${id}/mensajes?despuesDe=${ultimoId}`));
    } catch {
        // Sin señal: se reintenta en la próxima consulta
    }
}

async function enviar(cuerpo) {
    const mensaje = await api.post(`/conversaciones/${id}/mensajes`, cuerpo);
    agregar([mensaje]);
}

formTexto.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const texto = campoTexto.value.trim();
    if (!texto || enviando) {
        return;
    }
    enviando = true;
    const boton = document.getElementById('enviar');
    marcarOcupado(boton, true, 'Enviando…');
    try {
        await enviar({ tipo: 'TEXTO', contenido: texto });
        campoTexto.value = '';
        ocultarAlerta(alerta);
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    } finally {
        enviando = false;
        marcarOcupado(boton, false);
        campoTexto.focus();
    }
});

function actualizarResumenOferta() {
    const p = conversacion.publicacion;
    const cantidad = Number(formOferta.elements.ofertaCantidad.value);
    const precio = Number(formOferta.elements.ofertaPrecioUnitario.value);
    poner('resumen-oferta', cantidad > 0 && precio > 0
        ? `≈ ${numero(cantidad * Number(p.pesoUnidadKg), 0)} kilos · Total ${soles(cantidad * precio)}`
        : '');
}

botonOferta.addEventListener('click', () => {
    const p = conversacion.publicacion;
    limpiarErrores(formOferta, alertaOferta);
    poner('etiqueta-cantidad', `Cantidad (${p.unidadPlural})`);
    poner('ayuda-cantidad', `Disponible: ≈ ${numero(p.unidadesDisponibles)} ${unidad(p, p.unidadesDisponibles)}`);
    poner('etiqueta-precio', `Precio por ${p.unidad.toLowerCase()}`);
    formOferta.elements.ofertaCantidad.max = p.unidadesDisponibles;
    formOferta.elements.ofertaCantidad.value = '';
    formOferta.elements.ofertaPrecioUnitario.value = Number(p.precioUnitario).toFixed(2);
    actualizarResumenOferta();
    dialogo.showModal();
    formOferta.elements.ofertaCantidad.focus();
});
document.getElementById('cerrar-oferta').addEventListener('click', () => dialogo.close());
formOferta.addEventListener('input', actualizarResumenOferta);

formOferta.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formOferta, alertaOferta);
    const boton = document.getElementById('enviar-oferta');
    marcarOcupado(boton, true, 'Enviando…');
    try {
        await enviar({
            tipo: 'OFERTA',
            ofertaCantidad: formOferta.elements.ofertaCantidad.value || null,
            ofertaPrecioUnitario: formOferta.elements.ofertaPrecioUnitario.value || null
        });
        dialogo.close();
    } catch (error) {
        mostrarErrores(formOferta, alertaOferta, error);
    } finally {
        marcarOcupado(boton, false);
    }
});

document.getElementById('producto').addEventListener('click', (evento) => {
    const boton = evento.currentTarget;
    const abierto = boton.getAttribute('aria-expanded') === 'true';
    boton.setAttribute('aria-expanded', String(!abierto));
    document.getElementById('producto-detalle').hidden = abierto;
});

async function iniciar() {
    if (!requerirSesion()) {
        return;
    }
    if (!id || !/^\d+$/.test(id)) {
        mostrarAlerta(alerta, 'La conversación no existe.', 'error');
        return;
    }
    try {
        mostrarCabecera(await api.get(`/conversaciones/${id}`));
        agregar(await api.get(`/conversaciones/${id}/mensajes`));
        mostrarVentas();
    } catch (error) {
        mostrarAlerta(alerta, error.estado === 403
            ? 'Esta conversación no es suya.'
            : error.message, 'error');
        formTexto.hidden = true;
        return;
    }
    setInterval(() => {
        if (!document.hidden) {
            traerNuevos();
        }
    }, CADA_MS);
    setInterval(async () => {
        if (!document.hidden) {
            try {
                mostrarCabecera(await api.get(`/conversaciones/${id}`));
                mostrarVentas();
            } catch {
                // Se reintenta en la próxima consulta
            }
        }
    }, CABECERA_CADA_MS);
}

iniciar();
