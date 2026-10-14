/**
 * "Panel" del administrador (diseño 11a, formato escritorio, RF-09 / UC-11 parte 4): tarjetas de resumen, cola de
 * moderación, kilos vendidos por cultivo en el mes, precios de hoy, embudo y reputaciones.
 * GET /api/v1/admin/panel y GET /api/v1/admin/panel/exportar (Excel).
 */
import { api, descargar } from '../api.js';
import { mostrarAlerta } from '../comun/formulario.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import { iniciarAdministracion, irAIngresarSi401, crear, numero, soles } from './comun.js';

const MARCAS = {
    PRIMERA_PUBLICACION: 'Primera publicación',
    CANTIDAD_INUSUAL: 'Cantidad inusual',
    PRECIO_FUERA_RANGO: 'Precio fuera de rango',
    CORREGIDA: 'Corregida'
};
const ROLES = { AGRICULTOR: 'Agricultor', COMPRADOR: 'Comprador' };

const aviso = document.getElementById('aviso');

function haceCuanto(iso) {
    const minutos = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
    if (minutos < 60) {
        return `hace ${minutos} min`;
    }
    const horas = Math.round(minutos / 60);
    return horas < 24 ? `hace ${horas} h` : `hace ${Math.round(horas / 24)} días`;
}

function nombreMes(mes) {
    const [anio, numeroMes] = mes.split('-').map(Number);
    const texto = new Date(anio, numeroMes - 1, 1).toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
    return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function tarjeta(etiqueta, valor, detalle, nombreIcono) {
    const caja = crear('div', 'ac-kpi');
    const cabecera = crear('div', 'ac-kpi-etiqueta');
    cabecera.append(icono(nombreIcono), crear('span', '', etiqueta));
    caja.append(cabecera, crear('strong', 'ac-kpi-valor', valor), crear('span', 'ac-suave', detalle));
    return caja;
}

function mostrarTarjetas(t) {
    document.getElementById('tarjetas').replaceChildren(
        tarjeta('Publicaciones activas', numero(t.publicacionesActivas, 0),
            t.publicadasUltimos7Dias === 1 ? '1 nueva en los últimos 7 días'
                : `${numero(t.publicadasUltimos7Dias, 0)} nuevas en los últimos 7 días`, 'sprout'),
        tarjeta('Por revisar', numero(t.porRevisar, 0),
            t.porRevisarMasAntigua ? `La más antigua, ${haceCuanto(t.porRevisarMasAntigua)}` : 'La cola está al día',
            'shield-check'),
        tarjeta('Decisiones de hoy', numero(t.conformesHoy + t.bloqueadasHoy, 0),
            `${numero(t.conformesHoy, 0)} conformes · ${numero(t.bloqueadasHoy, 0)} ocultadas`, 'circle-check'),
        tarjeta('Reclamos pendientes', numero(t.reclamosPendientes, 0),
            t.reclamosPendientes > 0 ? 'Revíselos en “Reclamos”' : 'No hay reclamos por resolver', 'flag'));
    document.getElementById('precios-hoy').textContent =
        `${numero(t.preciosCargadosHoy, 0)} de ${numero(t.preciosEsperadosHoy, 0)} cargados`;
}

function mostrarCola(cola, total) {
    const contenedor = document.getElementById('cola');
    document.getElementById('cola-total').textContent = `${numero(total, 0)} por revisar`;
    if (cola.length === 0) {
        contenedor.replaceChildren(crear('p', 'ac-suave mb-0', 'Nada por revisar.'));
        return;
    }
    contenedor.replaceChildren(...cola.map((p) => {
        const item = crear('div', 'ac-item-cola');
        const titulo = crear('div', 'd-flex justify-content-between gap-2');
        const producto = p.variedad === 'Común' ? `${p.cultivo} común` : `${p.cultivo} ${p.variedad}`;
        titulo.append(crear('strong', '', `${producto} · ${numero(p.cantidad)} ${p.unidadPlural}`),
            crear('span', 'ac-etiqueta-marca', MARCAS[p.marcaRevision] ?? p.marcaRevision));
        item.append(titulo, crear('div', 'ac-suave',
            `${p.agricultor ?? 'Agricultor'} · ${p.distrito} · ${haceCuanto(p.fechaPublicacion)}`));
        return item;
    }));
}

/** Barras horizontales de una sola serie: largo proporcional a los kilos y el valor escrito al lado. */
function mostrarKilos(mes, filas) {
    document.getElementById('mes').textContent = nombreMes(mes);
    const contenedor = document.getElementById('kilos');
    if (filas.length === 0) {
        contenedor.replaceChildren(crear('p', 'ac-suave mb-0', 'Todavía no hay ventas terminadas este mes.'));
        return;
    }
    const maximo = Math.max(...filas.map((f) => Number(f.kilosVendidos)));
    const lista = crear('ul', 'ac-barras');
    lista.setAttribute('aria-label', `Kilos vendidos por cultivo, ${nombreMes(mes)}`);
    for (const f of filas) {
        const item = crear('li');
        const barra = crear('span', 'ac-barra-dato');
        barra.style.width = `${Math.max(2, (Number(f.kilosVendidos) / maximo) * 100)}%`;
        const pista = crear('span', 'ac-barra-pista');
        pista.append(barra);
        item.append(crear('span', 'ac-barra-etiqueta', f.cultivo), pista,
            crear('span', 'ac-barra-valor', `${numero(f.kilosVendidos, 0)} kg`));
        item.title = `${f.cultivo}: ${numero(f.kilosVendidos)} kg en ${numero(f.ventasCompletadas, 0)} ventas, ${soles(f.montoTotal)}`;
        lista.append(item);
    }
    contenedor.replaceChildren(lista);
}

function mostrarEmbudo(filas) {
    const cuerpo = document.getElementById('embudo');
    if (filas.length === 0) {
        const fila = crear('tr');
        const celda = crear('td', 'ac-suave', 'Todavía ningún comprador escribió por una publicación.');
        celda.colSpan = 7;
        fila.append(celda);
        cuerpo.replaceChildren(fila);
        return;
    }
    cuerpo.replaceChildren(...filas.map((e) => {
        const fila = crear('tr');
        fila.append(crear('td', '', e.producto ?? `Publicación ${e.idPublicacion}`), crear('td', '', e.agricultor ?? ''),
            ...[e.clicsWhatsapp, e.contactos, e.ofertas, e.acuerdos, e.completadas]
                .map((v) => crear('td', 'text-end', numero(v, 0))));
        return fila;
    }));
}

function mostrarRanking(id, filas, vacio) {
    const lista = document.getElementById(id);
    if (filas.length === 0) {
        const item = crear('li', 'ac-suave', vacio);
        item.style.listStyle = 'none';
        lista.replaceChildren(item);
        return;
    }
    lista.replaceChildren(...filas.map((r) => {
        const item = crear('li');
        const nombre = crear('div');
        nombre.append(crear('strong', 'd-block', r.nombre), crear('span', 'ac-suave', ROLES[r.rol] ?? r.rol));
        item.append(nombre, crear('span', 'ac-ranking-valor',
            `★ ${numero(r.calificacion, 1)} (${numero(r.totalCalificaciones, 0)})`));
        return item;
    }));
}

async function cargar() {
    try {
        const panel = await api.get('/admin/panel');
        mostrarTarjetas(panel.tarjetas);
        mostrarCola(panel.colaPorRevisar, panel.tarjetas.porRevisar);
        mostrarKilos(panel.mes, panel.kilosPorCultivo);
        mostrarEmbudo(panel.embudo);
        mostrarRanking('mejor', panel.mejorReputacion, 'Todavía no hay calificaciones.');
        mostrarRanking('peor', panel.peorReputacion, 'No hay otras calificaciones.');
    } catch (error) {
        if (!irAIngresarSi401(error)) {
            mostrarAlerta(aviso, error.message, 'error');
        }
    }
}

document.getElementById('exportar').addEventListener('click', async (evento) => {
    const boton = evento.currentTarget;
    boton.disabled = true;
    try {
        await descargar('/admin/panel/exportar', 'AgroConecta_Panel.xlsx');
    } catch (error) {
        if (!irAIngresarSi401(error)) {
            mostrarAlerta(aviso, error.message, 'error');
        }
    } finally {
        boton.disabled = false;
        ponerIconoYTexto(boton, 'clipboard-list', 'Descargar Excel');
    }
});

if (iniciarAdministracion()) {
    const hoy = new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
    document.getElementById('fecha-hoy').textContent = `${hoy.charAt(0).toUpperCase()}${hoy.slice(1)} · Piura`;
    cargar();
}
