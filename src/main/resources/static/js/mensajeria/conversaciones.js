/**
 * "Mensajes" (diseño "chats", RF-04): conversaciones del usuario, con actividad más reciente primero.
 * GET /api/v1/conversaciones?page=; se actualiza cada 30 segundos.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { mostrarAlerta, ocultarAlerta } from '../comun/formulario.js';
import { cuando, producto, textoOferta } from './formato.js';

const CADA_MS = 30_000;
const lista = document.getElementById('lista');
const vacio = document.getElementById('vacio');
const verMas = document.getElementById('ver-mas');
const alerta = document.getElementById('alerta');
let paginasCargadas = 1;

function iniciales(nombre) {
    return nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

/** @returns {string} vista previa del último mensaje */
function vistaPrevia(c) {
    const m = c.ultimoMensaje;
    if (!m) {
        return 'Todavía no hay mensajes';
    }
    const texto = m.tipo === 'OFERTA'
        ? `Oferta: ${textoOferta(c.publicacion, m.ofertaCantidad, m.ofertaPrecioUnitario)}`
        : m.contenido;
    return m.mio ? `Usted: ${texto}` : texto;
}

function elemento(c) {
    const enlace = document.createElement('a');
    enlace.className = 'ac-conversacion';
    enlace.href = `/pages/mensajeria/chat.html?id=${c.id}`;
    const nombre = c.otraParte.negocio ?? c.otraParte.nombre;

    const circulo = document.createElement('span');
    circulo.className = 'ac-iniciales';
    circulo.setAttribute('aria-hidden', 'true');
    circulo.textContent = iniciales(nombre);

    const texto = document.createElement('span');
    texto.className = 'ac-conversacion-texto';
    const fila1 = document.createElement('span');
    fila1.className = 'd-flex justify-content-between gap-2';
    const quien = document.createElement(c.noLeidos > 0 ? 'strong' : 'span');
    quien.className = 'ac-recortado fw-semibold';
    quien.textContent = nombre;
    const hora = document.createElement('span');
    hora.className = 'ac-suave small flex-shrink-0';
    hora.textContent = cuando(c.ultimoMensaje?.fechaEnvio ?? c.fechaInicio);
    fila1.append(quien, hora);

    const fila2 = document.createElement('span');
    fila2.className = 'd-block ac-suave small ac-recortado';
    fila2.textContent = c.otraParte.negocio
        ? `${c.otraParte.nombre} · ${producto(c.publicacion)}`
        : `${producto(c.publicacion)} · ${c.publicacion.distrito}`;

    const fila3 = document.createElement('span');
    fila3.className = 'd-flex justify-content-between align-items-center gap-2';
    const previa = document.createElement('span');
    previa.className = c.noLeidos > 0 ? 'ac-recortado fw-bold' : 'ac-recortado';
    previa.textContent = vistaPrevia(c);
    fila3.append(previa);
    if (c.noLeidos > 0) {
        const insignia = document.createElement('span');
        insignia.className = 'ac-no-leidos';
        insignia.textContent = String(c.noLeidos);
        insignia.setAttribute('aria-label', `${c.noLeidos} sin leer`);
        fila3.append(insignia);
    }
    texto.append(fila1, fila2, fila3);
    enlace.append(circulo, texto);
    return enlace;
}

/** Carga las páginas ya vistas (para la actualización periódica) o la siguiente. */
async function cargar(siguiente = false) {
    const desde = siguiente ? paginasCargadas : 0;
    const hasta = siguiente ? paginasCargadas + 1 : paginasCargadas;
    try {
        const paginas = [];
        for (let i = desde; i < hasta; i++) {
            paginas.push(await api.get(`/conversaciones?page=${i}`));
        }
        if (!siguiente) {
            lista.replaceChildren();
        }
        paginas.forEach((pagina) => pagina.content.forEach((c) => lista.append(elemento(c))));
        const ultima = paginas.at(-1).page;
        paginasCargadas = hasta;
        vacio.hidden = ultima.totalElements > 0;
        verMas.hidden = paginasCargadas >= ultima.totalPages;
        ocultarAlerta(alerta);
    } catch (error) {
        if (error.estado === 401) {
            window.location.replace('/pages/usuario/ingresar.html');
            return;
        }
        mostrarAlerta(alerta, error.message, 'error');
    }
}

const sesion = requerirSesion();
if (sesion) {
    const comprador = sesion.usuario.rol === 'COMPRADOR';
    document.getElementById('vacio-detalle').textContent = comprador
        ? 'Busque una cosecha y pulse "Escribir" para hablar con el agricultor.'
        : 'Cuando un comprador le escriba sobre su cosecha, aparecerá aquí.';
    document.getElementById('vacio-buscar').hidden = !comprador;
    verMas.addEventListener('click', () => cargar(true));
    cargar();
    setInterval(() => {
        if (!document.hidden) {
            cargar();
        }
    }, CADA_MS);
}
