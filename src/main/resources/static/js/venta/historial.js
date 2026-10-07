/**
 * "Mis ventas" (agricultor) o "Mis compras" (comprador) (RF-06 / UC-08): ventas en curso y terminadas, filtros por
 * fecha y persona, "Ver más" y exportación a Excel. GET /api/v1/ventas y GET /api/v1/ventas/exportar.
 */
import { api, descargar } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import { mostrarAlerta, ocultarAlerta, marcarOcupado } from '../comun/formulario.js';
import { numero, soles } from '../publicacion/borrador.js';
import { ESTADOS, textoEstado } from './estados.js';

const lista = document.getElementById('lista');
const vacio = document.getElementById('vacio');
const verMas = document.getElementById('ver-mas');
const alerta = document.getElementById('alerta');
const filtros = document.getElementById('filtros');
const pestanas = document.querySelectorAll('[data-grupo]');

let grupo = 'EN_CURSO';
let pagina = 0;
let comprador = false;

function consulta(conPagina) {
    const p = new URLSearchParams({ grupo });
    for (const campo of ['desde', 'hasta', 'contraparte']) {
        const valor = filtros.elements[campo].value.trim();
        if (valor) {
            p.set(campo, valor);
        }
    }
    if (conPagina) {
        p.set('page', String(pagina));
    }
    return p.toString();
}

function hayFiltros() {
    return ['desde', 'hasta', 'contraparte'].some((c) => filtros.elements[c].value.trim());
}

function diaCorto(iso) {
    return new Date(`${iso}T00:00:00`).toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric', month: 'short' });
}

function elemento(v) {
    const enlace = document.createElement('a');
    enlace.className = 'ac-conversacion d-block';
    enlace.href = `/pages/venta/venta.html?id=${v.id}`;
    const fila1 = document.createElement('span');
    fila1.className = 'd-flex justify-content-between align-items-start gap-2';
    const titulo = document.createElement('strong');
    const unidad = Number(v.cantidad) === 1 ? v.unidad.toLowerCase() : v.unidadPlural;
    titulo.textContent = `${numero(v.cantidad)} ${unidad} de ${v.cultivo.toLowerCase()} ${v.variedad} `
        + `(unos ${numero(v.cantidadKg, 0)} kilos)`;
    const estado = document.createElement('span');
    estado.className = 'ac-estado-venta';
    if (v.estado === 'COMPLETADA') {
        estado.classList.add('ac-estado-final');
    } else if (v.estado === 'CANCELADA' || v.estado === 'RECHAZADA') {
        estado.classList.add('ac-estado-cancelada');
    }
    const textoBadge = document.createElement('span');
    textoBadge.textContent = textoEstado(v.estado);
    estado.append(icono(ESTADOS[v.estado]?.[1] ?? 'info'), textoBadge);
    fila1.append(titulo, estado);
    const fila2 = document.createElement('span');
    fila2.className = 'd-block ac-suave';
    fila2.textContent = `${v.contraparte} · recojo ${diaCorto(v.fechaRecojo)}`;
    const fila3 = document.createElement('span');
    fila3.className = 'd-block fw-semibold';
    fila3.textContent = `Total ${soles(v.montoTotal)} · ${soles(v.precioUnitario)} por ${v.unidad.toLowerCase()}`;
    enlace.append(fila1, fila2, fila3);
    return enlace;
}

function mostrarVacio() {
    const titulo = document.getElementById('vacio-titulo');
    const detalle = document.getElementById('vacio-detalle');
    const accion = document.getElementById('vacio-accion');
    if (hayFiltros()) {
        titulo.textContent = 'Sin resultados para este filtro';
        detalle.textContent = 'Pruebe con otras fechas o quite el nombre.';
        accion.hidden = true;
        return;
    }
    titulo.textContent = grupo === 'EN_CURSO'
        ? (comprador ? 'No tiene compras en curso' : 'No tiene ventas en curso')
        : (comprador ? 'Aún no tiene compras terminadas' : 'Aún no tiene ventas terminadas');
    detalle.textContent = comprador
        ? 'Busque una cosecha, converse con el agricultor y registre el acuerdo en el chat.'
        : 'Publique su cosecha y registre el acuerdo en el chat con el comprador.';
    accion.hidden = false;
    accion.href = comprador ? '/pages/publicacion/buscar.html' : '/pages/publicacion/paso1.html';
    ponerIconoYTexto(accion, comprador ? 'search' : 'sprout', comprador ? 'Buscar productos' : 'Vender mi cosecha');
}

async function cargar(siguiente = false) {
    pagina = siguiente ? pagina + 1 : 0;
    try {
        const respuesta = await api.get(`/ventas?${consulta(true)}`);
        if (!siguiente) {
            lista.replaceChildren();
        }
        respuesta.content.forEach((v) => lista.append(elemento(v)));
        const total = respuesta.page.totalElements;
        document.getElementById('conteo').textContent = total === 0 ? ''
            : `${total} ${total === 1 ? (comprador ? 'compra' : 'venta') : (comprador ? 'compras' : 'ventas')}`;
        vacio.hidden = total > 0;
        if (total === 0) {
            mostrarVacio();
        }
        verMas.hidden = pagina + 1 >= respuesta.page.totalPages;
        document.getElementById('exportar').hidden = total === 0;
        ocultarAlerta(alerta);
    } catch (error) {
        pagina = Math.max(0, pagina - (siguiente ? 1 : 0));
        mostrarAlerta(alerta, error.message, 'error');
    }
}

pestanas.forEach((pestana) => pestana.addEventListener('click', () => {
    grupo = pestana.dataset.grupo;
    pestanas.forEach((p) => p.setAttribute('aria-selected', String(p === pestana)));
    cargar();
}));
filtros.addEventListener('submit', (evento) => {
    evento.preventDefault();
    cargar();
});
document.getElementById('limpiar').addEventListener('click', () => {
    filtros.reset();
    cargar();
});
verMas.addEventListener('click', () => cargar(true));
document.getElementById('exportar').addEventListener('click', async (evento) => {
    const boton = evento.currentTarget;
    marcarOcupado(boton, true, 'Preparando…');
    try {
        await descargar(`/ventas/exportar?${consulta(false)}`, 'AgroConecta_Historial.xlsx');
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    } finally {
        marcarOcupado(boton, false);
    }
});

const sesion = requerirSesion();
if (sesion) {
    comprador = sesion.usuario.rol === 'COMPRADOR';
    const titulo = comprador ? 'Mis compras' : 'Mis ventas';
    document.getElementById('titulo').textContent = titulo;
    document.title = `${titulo} · AgroConecta`;
    document.getElementById('etiqueta-contraparte').textContent = comprador ? 'Agricultor' : 'Comprador';
    cargar();
}
