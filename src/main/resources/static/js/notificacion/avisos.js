/**
 * "Avisos" (diseño "notif", RF-07 / UC-09 paso 2): lista de avisos del usuario, cada uno con un botón que lleva a su
 * publicación, chat o venta; al abrirlo se marca como leído. GET /api/v1/notificaciones,
 * POST /api/v1/notificaciones/{id}/leida y POST /api/v1/notificaciones/leidas.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import { mostrarAlerta, marcarOcupado } from '../comun/formulario.js';
import { ponerBarra } from '../comun/barra.js';

/** Tipo → [etiqueta, ícono, texto del botón, ruta a la que lleva según idReferencia]. */
const TIPOS = {
    POR_VENCER: ['Por vencer', 'triangle-alert', 'Ver opciones', (id) => `/pages/publicacion/mis-cosechas.html#cosecha-${id}`],
    MODERACION: ['Moderación', 'shield-check', 'Ver mi cosecha', (id) => `/pages/publicacion/mis-cosechas.html#cosecha-${id}`],
    OFERTA: ['Oferta', 'hand-coins', 'Ver oferta', (id) => `/pages/mensajeria/chat.html?id=${id}`],
    MENSAJE: ['Mensaje', 'message-circle', 'Abrir chat', (id) => `/pages/mensajeria/chat.html?id=${id}`],
    VENTA: ['Venta', 'handshake', 'Ver entrega', (id) => `/pages/venta/venta.html?id=${id}`],
    CALIFICACION: ['Calificación', 'star', 'Ver calificación', (id) => `/pages/venta/venta.html?id=${id}`],
    PROPUESTA_CONJUNTA: ['Propuesta conjunta', 'layers', 'Ver propuesta', (id) => `/pages/venta/venta.html?id=${id}`],
    RECLAMO: ['Reclamo', 'flag', 'Ver venta', (id) => `/pages/venta/venta.html?id=${id}`],
    CUENTA: ['Cuenta', 'user', null, null]
};

const lista = document.getElementById('lista');
const alerta = document.getElementById('alerta');
const verMas = document.getElementById('ver-mas');
const marcarTodos = document.getElementById('marcar-todos');
let pagina = 0;

function cuando(iso) {
    const fecha = new Date(iso);
    const hoy = new Date();
    const ayer = new Date();
    ayer.setDate(hoy.getDate() - 1);
    const hora = fecha.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' });
    if (fecha.toDateString() === hoy.toDateString()) {
        return `Hoy, ${hora}`;
    }
    if (fecha.toDateString() === ayer.toDateString()) {
        return `Ayer, ${hora}`;
    }
    return `${fecha.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' })}, ${hora}`;
}

async function abrir(aviso, ruta, boton) {
    marcarOcupado(boton, true, 'Abriendo…');
    try {
        if (!aviso.leida) {
            await api.post(`/notificaciones/${aviso.id}/leida`);
        }
    } catch {
        // Si no se pudo marcar, igual se abre: se marcará la próxima vez
    }
    window.location.assign(ruta);
}

function elemento(aviso) {
    const [etiqueta, nombreIcono, textoBoton, ruta] = TIPOS[aviso.tipo] ?? [aviso.tipo, 'bell', null, null];
    const tarjeta = document.createElement('article');
    tarjeta.className = aviso.leida ? 'ac-aviso' : 'ac-aviso ac-aviso-nuevo';

    const cabecera = document.createElement('div');
    cabecera.className = 'd-flex justify-content-between align-items-center gap-2 mb-1';
    const tipo = document.createElement('span');
    tipo.className = `ac-aviso-tipo ac-aviso-${aviso.tipo.toLowerCase()}`;
    const textoTipo = document.createElement('span');
    textoTipo.textContent = aviso.leida ? etiqueta : `${etiqueta} · nuevo`;
    tipo.append(icono(nombreIcono), textoTipo);
    const fecha = document.createElement('span');
    fecha.className = 'ac-suave small';
    fecha.textContent = cuando(aviso.fechaCreacion);
    cabecera.append(tipo, fecha);

    const titulo = document.createElement('p');
    titulo.className = aviso.leida ? 'mb-1' : 'mb-1 fw-bold';
    titulo.textContent = aviso.titulo;
    const contenido = document.createElement('p');
    contenido.className = 'ac-suave mb-0';
    contenido.textContent = aviso.contenido;
    tarjeta.append(cabecera, titulo, contenido);

    if (textoBoton && aviso.idReferencia !== null) {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = aviso.tipo === 'POR_VENCER' ? 'btn btn-primary w-100 mt-2' : 'btn btn-outline-primary w-100 mt-2';
        ponerIconoYTexto(boton, 'chevron-right', textoBoton);
        boton.addEventListener('click', () => abrir(aviso, ruta(aviso.idReferencia), boton));
        tarjeta.append(boton);
    }
    return tarjeta;
}

async function cargar(siguiente = false) {
    pagina = siguiente ? pagina + 1 : 0;
    try {
        const respuesta = await api.get(`/notificaciones?page=${pagina}`);
        if (!siguiente) {
            lista.replaceChildren();
        }
        respuesta.content.forEach((a) => lista.append(elemento(a)));
        document.getElementById('vacio').hidden = respuesta.page.totalElements > 0;
        verMas.hidden = pagina + 1 >= respuesta.page.totalPages;
        const { total } = await api.get('/notificaciones/no-leidas');
        marcarTodos.hidden = total === 0;
    } catch (error) {
        pagina = Math.max(0, pagina - (siguiente ? 1 : 0));
        mostrarAlerta(alerta, error.message, 'error');
    }
}

marcarTodos.addEventListener('click', async () => {
    marcarOcupado(marcarTodos, true, 'Marcando…');
    try {
        await api.post('/notificaciones/leidas');
        await cargar();
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    } finally {
        marcarOcupado(marcarTodos, false);
    }
});
verMas.addEventListener('click', () => cargar(true));

if (requerirSesion()) {
    ponerBarra(null);
    cargar();
}
