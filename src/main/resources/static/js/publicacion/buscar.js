/**
 * "Buscar productos" (diseño "buscar", RF-03 / UC-03). Pública.
 * GET /api/v1/publicaciones con filtros, orden y página; GET /api/v1/catalogo/cultivos, /variedades, /distritos,
 * /provincias y /unidades para los filtros; "Escribir" y "WhatsApp" con js/mensajeria/contacto.js (RF-04). Los filtros quedan en la dirección de la página, así que "atrás" desde
 * el detalle vuelve a la misma búsqueda.
 */
import { api } from '../api.js';
import { obtenerSesion } from '../comun/sesion.js';
import { mostrarAlerta } from '../comun/formulario.js';
import { crearChips } from '../comun/precios.js';
import { tarjetaResultado } from './resultado.js';

const TODOS = 'todos';
const ORDENES = [
    { valor: 'PRECIO_KG', texto: 'Precio por kilo', descripcion: 'precio por kilo' },
    { valor: 'FECHA_COSECHA', texto: 'Fecha', descripcion: 'fecha de cosecha' },
    { valor: 'CALIFICACION', texto: 'Calificación', descripcion: 'calificación' }
];
const FECHAS = [
    { valor: TODOS, texto: 'Cualquier fecha', dias: null },
    { valor: 'semana', texto: 'Esta semana', dias: 7 },
    { valor: 'mes', texto: 'Este mes', dias: 30 }
];

const resultados = document.getElementById('resultados');
const conteo = document.getElementById('conteo');
const vacio = document.getElementById('vacio');
const error = document.getElementById('error');
const verMas = document.getElementById('ver-mas');
const dialogo = document.getElementById('filtros');
const selectLugar = document.getElementById('f-lugar');
const alerta = document.getElementById('alerta');

/** Muestra el error de "Escribir" o "WhatsApp" (por ejemplo, la cosecha ya se vendió). */
function errorContacto(mensaje) {
    mostrarAlerta(alerta, mensaje, 'error');
    alerta.focus();
}

/** Criterios de la búsqueda; se leen y se guardan en la dirección de la página. */
let criterios = leerDireccion();
/** Copia que se edita en la hoja de filtros hasta pulsar "Ver resultados". */
let borrador = { ...criterios };
let pagina = 0;
let catalogo = { cultivos: [], unidades: [] };

function leerDireccion() {
    const p = new URLSearchParams(window.location.search);
    return {
        cultivo: p.get('cultivo') ?? TODOS,
        variedad: p.get('variedad') ?? TODOS,
        lugar: p.get('lugar') ?? TODOS,
        fecha: p.get('fecha') ?? TODOS,
        unidad: p.get('unidad') ?? TODOS,
        orden: ORDENES.some((o) => o.valor === p.get('orden')) ? p.get('orden') : 'PRECIO_KG'
    };
}

function guardarDireccion() {
    const p = new URLSearchParams();
    for (const [clave, valor] of Object.entries(criterios)) {
        if (valor !== TODOS && !(clave === 'orden' && valor === 'PRECIO_KG')) {
            p.set(clave, valor);
        }
    }
    const consulta = p.toString();
    history.replaceState(null, '', consulta ? `?${consulta}` : window.location.pathname);
}

/** @returns {string} fecha AAAA-MM-DD de hace {dias} días */
function haceDias(dias) {
    const d = new Date();
    d.setDate(d.getDate() - dias);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Arma la consulta de la API a partir de los criterios. */
function consultaApi(numeroPagina) {
    const p = new URLSearchParams({ orden: criterios.orden, page: String(numeroPagina) });
    if (criterios.cultivo !== TODOS) {
        p.set('idCultivo', criterios.cultivo);
    }
    if (criterios.variedad !== TODOS) {
        p.set('idVariedad', criterios.variedad);
    }
    if (criterios.lugar !== TODOS) {
        const [tipo, id] = criterios.lugar.split('-');
        p.set(tipo === 'p' ? 'idProvincia' : 'idDistrito', id);
    }
    const fecha = FECHAS.find((f) => f.valor === criterios.fecha);
    if (fecha?.dias) {
        p.set('cosechaDesde', haceDias(fecha.dias));
    }
    if (criterios.unidad !== TODOS) {
        p.set('idUnidad', criterios.unidad);
    }
    return p.toString();
}

function filtrosActivos() {
    return ['cultivo', 'variedad', 'lugar', 'fecha', 'unidad'].filter((c) => criterios[c] !== TODOS).length;
}

async function buscar(agregar = false) {
    pagina = agregar ? pagina + 1 : 0;
    error.hidden = true;
    verMas.disabled = true;
    if (!agregar) {
        conteo.textContent = 'Cargando…';
    }
    try {
        const respuesta = await api.get(`/publicaciones?${consultaApi(pagina)}`);
        if (!agregar) {
            resultados.replaceChildren();
        }
        respuesta.content.forEach((p) => resultados.append(tarjetaResultado(p, errorContacto)));
        const total = respuesta.page.totalElements;
        const orden = ORDENES.find((o) => o.valor === criterios.orden).descripcion;
        conteo.textContent = total === 0 ? '' : `${total} ${total === 1 ? 'oferta' : 'ofertas'} · ordenadas por ${orden}`;
        vacio.hidden = total > 0;
        verMas.hidden = pagina + 1 >= respuesta.page.totalPages;
        if (agregar) {
            // Lleva el foco al primer resultado nuevo (navegación con teclado)
            resultados.children[pagina * respuesta.page.size]?.querySelector('a')?.focus();
        }
    } catch (e) {
        pagina = Math.max(0, pagina - (agregar ? 1 : 0));
        conteo.textContent = '';
        document.getElementById('error-detalle').textContent = e.estado === 0
            ? 'Revise su señal de internet e intente otra vez.'
            : e.message;
        error.hidden = false;
        vacio.hidden = true;
    } finally {
        verMas.disabled = false;
    }
}

function actualizarBotonFiltros() {
    const n = filtrosActivos();
    document.getElementById('texto-filtros').textContent = n === 0 ? 'Filtrar' : `Filtrar (${n})`;
}

async function dibujarVariedades() {
    const grupo = document.getElementById('grupo-variedad');
    grupo.hidden = borrador.cultivo === TODOS;
    if (grupo.hidden) {
        return;
    }
    const variedades = await api.get(`/catalogo/variedades?idCultivo=${borrador.cultivo}`);
    if (!variedades.some((v) => String(v.id) === borrador.variedad)) {
        borrador.variedad = TODOS;
    }
    crearChips(document.getElementById('f-variedad'), 'variedad',
        [{ valor: TODOS, texto: 'Todas' }, ...variedades.map((v) => ({ valor: v.id, texto: v.nombre }))],
        borrador.variedad, (valor) => { borrador.variedad = valor; });
}

function dibujarFiltros() {
    crearChips(document.getElementById('f-cultivo'), 'cultivo',
        [{ valor: TODOS, texto: 'Todos' }, ...catalogo.cultivos.map((c) => ({ valor: c.id, texto: c.nombre }))],
        borrador.cultivo, (valor) => {
            borrador.cultivo = valor;
            borrador.variedad = TODOS;
            dibujarVariedades();
        });
    selectLugar.value = borrador.lugar;
    crearChips(document.getElementById('f-fecha'), 'fecha', FECHAS, borrador.fecha,
        (valor) => { borrador.fecha = valor; });
    crearChips(document.getElementById('f-unidad'), 'unidad',
        [{ valor: TODOS, texto: 'Todas' }, ...catalogo.unidades.map((u) => ({ valor: u.id, texto: u.nombre }))],
        borrador.unidad, (valor) => { borrador.unidad = valor; });
    dibujarVariedades();
}

function llenarLugares(provincias, distritos) {
    selectLugar.append(new Option('Todas', TODOS));
    for (const provincia of provincias) {
        const grupo = document.createElement('optgroup');
        grupo.label = `Provincia de ${provincia.nombre}`;
        grupo.append(new Option(`Toda la provincia de ${provincia.nombre}`, `p-${provincia.id}`));
        distritos.filter((d) => d.idProvincia === provincia.id)
            .forEach((d) => grupo.append(new Option(d.nombre, `d-${d.id}`)));
        selectLugar.append(grupo);
    }
}

function aplicar() {
    criterios = { ...borrador };
    guardarDireccion();
    actualizarBotonFiltros();
    buscar();
}

document.getElementById('abrir-filtros').addEventListener('click', () => {
    borrador = { ...criterios };
    dibujarFiltros();
    dialogo.showModal();
});
document.getElementById('cerrar-filtros').addEventListener('click', () => dialogo.close());
selectLugar.addEventListener('change', () => { borrador.lugar = selectLugar.value; });
document.getElementById('limpiar').addEventListener('click', () => {
    borrador = { ...borrador, cultivo: TODOS, variedad: TODOS, lugar: TODOS, fecha: TODOS, unidad: TODOS };
    dibujarFiltros();
});
document.getElementById('form-filtros').addEventListener('submit', () => aplicar());
document.getElementById('quitar-filtros').addEventListener('click', () => {
    borrador = { ...criterios, cultivo: TODOS, variedad: TODOS, lugar: TODOS, fecha: TODOS, unidad: TODOS };
    aplicar();
});
document.getElementById('reintentar').addEventListener('click', () => buscar());
verMas.addEventListener('click', () => buscar(true));

async function iniciar() {
    const sesion = obtenerSesion();
    if (sesion) {
        document.getElementById('volver').href = '/pages/usuario/inicio.html';
    }
    crearChips(document.getElementById('orden'), 'orden', ORDENES, criterios.orden, (valor) => {
        criterios.orden = valor;
        guardarDireccion();
        buscar();
    });
    actualizarBotonFiltros();
    buscar();
    try {
        const [cultivos, unidades, provincias, distritos] = await Promise.all([
            api.get('/catalogo/cultivos'), api.get('/catalogo/unidades'),
            api.get('/catalogo/provincias'), api.get('/catalogo/distritos')]);
        catalogo = { cultivos, unidades };
        llenarLugares(provincias, distritos);
    } catch {
        document.getElementById('abrir-filtros').disabled = true; // Sin catálogo no se puede filtrar
    }
}

iniciar();
