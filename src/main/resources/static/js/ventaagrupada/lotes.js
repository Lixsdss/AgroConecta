/**
 * "Lotes agrupados" (diseño "agrupados", RF-10 / UC-12 paso 1): cada lote con total de kilos, precio promedio y
 * agricultores; "Ver agricultores" y "Enviar propuesta conjunta". GET /api/v1/lotes y /api/v1/lotes/agricultores.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import { mostrarAlerta } from '../comun/formulario.js';
import { ponerBarra } from '../comun/barra.js';
import { numero, soles } from '../publicacion/borrador.js';

const lista = document.getElementById('lista');
const alerta = document.getElementById('alerta');
const verMas = document.getElementById('ver-mas');
let pagina = 0;

function titulo(l) {
    return l.variedad === 'Común' ? `${l.cultivo} común` : `${l.cultivo} ${l.variedad}`;
}

function dato(etiqueta, valor, detalle) {
    const caja = document.createElement('div');
    caja.className = 'ac-dato-lote';
    const e = document.createElement('div');
    e.className = 'ac-suave small';
    e.textContent = etiqueta;
    const v = document.createElement('strong');
    v.className = 'd-block fs-5';
    v.textContent = valor;
    caja.append(e, v);
    if (detalle) {
        const d = document.createElement('div');
        d.className = 'ac-suave small';
        d.textContent = detalle;
        caja.append(d);
    }
    return caja;
}

async function mostrarAgricultores(lote, contenedor, boton) {
    const abierto = boton.getAttribute('aria-expanded') === 'true';
    boton.setAttribute('aria-expanded', String(!abierto));
    ponerIconoYTexto(boton, abierto ? 'chevron-down' : 'chevron-up', abierto ? 'Ver agricultores' : 'Ocultar agricultores');
    contenedor.hidden = abierto;
    if (abierto || contenedor.childElementCount > 0) {
        return;
    }
    try {
        const cosechas = await api.get(`/lotes/agricultores?idVariedad=${lote.idVariedad}&idDistrito=${lote.idDistrito}`);
        cosechas.forEach((c) => {
            const fila = document.createElement('div');
            fila.className = 'd-flex justify-content-between gap-2 py-1 border-bottom';
            const nombre = document.createElement('strong');
            nombre.textContent = c.agricultor;
            const detalle = document.createElement('span');
            detalle.className = 'text-end';
            const unidad = Number(c.unidadesDisponibles) === 1 ? c.unidad.toLowerCase() : c.unidadPlural;
            detalle.textContent = `${numero(c.unidadesDisponibles)} ${unidad} · ${soles(c.precioUnitario)} por ${c.unidad.toLowerCase()}`;
            fila.append(nombre, detalle);
            contenedor.append(fila);
        });
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    }
}

function tarjeta(l) {
    const art = document.createElement('article');
    art.className = 'ac-resultado';
    const cabecera = document.createElement('div');
    cabecera.className = 'd-flex justify-content-between align-items-start gap-2 mb-2';
    const nombre = document.createElement('span');
    nombre.className = 'ac-resultado-titulo';
    nombre.textContent = titulo(l);
    const zona = document.createElement('span');
    zona.className = 'd-inline-flex align-items-center gap-1 ac-suave';
    zona.append(icono('map-pin'), Object.assign(document.createElement('span'), { textContent: l.distrito }));
    cabecera.append(nombre, zona);

    const datos = document.createElement('div');
    datos.className = 'ac-datos-lote';
    datos.append(dato('Total', `${numero(l.volumenKg, 0)} kg`), dato('Promedio', soles(l.precioPromedioKg), 'por kilo'),
        dato('Agricultores', String(l.totalAgricultores)));

    const agricultores = document.createElement('div');
    agricultores.className = 'mt-2';
    agricultores.hidden = true;
    const ver = document.createElement('button');
    ver.type = 'button';
    ver.className = 'btn btn-link w-100';
    ver.setAttribute('aria-expanded', 'false');
    ponerIconoYTexto(ver, 'chevron-down', 'Ver agricultores');
    ver.addEventListener('click', () => mostrarAgricultores(l, agricultores, ver));

    const proponer = document.createElement('a');
    proponer.className = 'btn btn-primary w-100';
    proponer.href = `/pages/ventaagrupada/propuesta.html?idVariedad=${l.idVariedad}&idDistrito=${l.idDistrito}`;
    ponerIconoYTexto(proponer, 'send', 'Enviar propuesta conjunta');

    art.append(cabecera, datos, agricultores, ver, proponer);
    return art;
}

async function cargar(siguiente = false) {
    pagina = siguiente ? pagina + 1 : 0;
    try {
        const respuesta = await api.get(`/lotes?page=${pagina}`);
        if (!siguiente) {
            lista.replaceChildren();
        }
        respuesta.content.forEach((l) => lista.append(tarjeta(l)));
        document.getElementById('vacio').hidden = respuesta.page.totalElements > 0;
        verMas.hidden = pagina + 1 >= respuesta.page.totalPages;
    } catch (error) {
        pagina = Math.max(0, pagina - (siguiente ? 1 : 0));
        mostrarAlerta(alerta, error.estado === 403 ? 'Los lotes agrupados son para compradores.' : error.message, 'error');
    }
}

verMas.addEventListener('click', () => cargar(true));
const sesion = requerirSesion();
if (sesion) {
    ponerBarra('agrupados');
    cargar();
}
