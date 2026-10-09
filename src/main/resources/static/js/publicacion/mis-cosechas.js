/**
 * "Mis ventas" del agricultor (diseño "ventas", RF-07 / UC-09): sus cosechas por estado, el aviso de la que vence
 * pronto y las acciones rápidas (bajar precio, extender, retirar) más "Corregir" una cosecha oculta (RF-09).
 * GET /api/v1/publicaciones/mias y POST/PUT de las acciones; GET /api/v1/catalogo/unidades (peso variable).
 * Con #cosecha-{id} en la dirección (desde un aviso) resalta esa cosecha.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import { mostrarAlerta, ocultarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';
import { crearChips } from '../comun/precios.js';
import { ponerBarra } from '../comun/barra.js';
import { numero, soles, fechaCorta } from './borrador.js';

const ESTADOS = {
    PUBLICADA: ['Activa', 'ac-estado-venta'],
    BLOQUEADA: ['Oculta', 'ac-estado-venta ac-estado-cancelada'],
    VENCIDA: ['Vencida', 'ac-estado-venta ac-estado-cancelada'],
    VENDIDA: ['Vendida', 'ac-estado-venta ac-estado-final'],
    RETIRADA: ['Retirada', 'ac-estado-venta']
};

const lista = document.getElementById('lista');
const alerta = document.getElementById('alerta');
const verMas = document.getElementById('ver-mas');
const dialogo = document.getElementById('dialogo');
const form = document.getElementById('form-accion');
const alertaDialogo = document.getElementById('alerta-dialogo');
const pestanas = document.querySelectorAll('[data-grupo]');

let grupo = 'ACTIVAS';
let pagina = 0;
let unidadesVariables = new Set();
let accion = null;
let cosecha = null;
let dias = 7;

function hoy() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function sumarDias(iso, n) {
    const d = new Date(`${iso}T00:00:00`);
    d.setDate(d.getDate() + n);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function titulo(c) {
    return c.variedad === 'Común' ? `${c.cultivo} común` : `${c.cultivo} ${c.variedad}`;
}

function textoVence(c) {
    if (c.diasRestantes <= 0) {
        return 'vence hoy';
    }
    return c.diasRestantes === 1 ? 'vence mañana' : `vence en ${c.diasRestantes} días`;
}

/** Nota bajo la cosecha según su estado. */
function nota(c) {
    switch (c.estado) {
        case 'PUBLICADA': return [c.porVencer ? 'clock' : 'calendar', `${textoVence(c)[0].toUpperCase()}${textoVence(c).slice(1)}`];
        case 'VENCIDA': return ['circle-x', `Venció el ${fechaCorta(c.fechaLimite)}. Puede publicarla otra vez.`];
        case 'BLOQUEADA': return ['shield-check', 'Oculta por la moderación. El motivo está en Avisos; corríjala para que la revisen.'];
        case 'VENDIDA': return ['circle-check', 'Vendida. Vea el detalle en el historial de ventas.'];
        default: return ['minus', 'Retirada: ya no está a la venta.'];
    }
}

function boton(texto, nombreIcono, clase, alPulsar) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = `btn ${clase}`;
    ponerIconoYTexto(b, nombreIcono, texto);
    b.addEventListener('click', alPulsar);
    return b;
}

function tarjeta(c) {
    const art = document.createElement('article');
    art.className = 'ac-resultado';
    art.id = `cosecha-${c.id}`;
    const cabecera = document.createElement('div');
    cabecera.className = 'd-flex justify-content-between align-items-start gap-2 mb-1';
    const nombre = document.createElement('span');
    nombre.className = 'ac-resultado-titulo';
    nombre.textContent = titulo(c);
    const [textoEstado, clase] = c.estado === 'PUBLICADA' && c.porVencer ? ['Por vencer', 'ac-estado-venta ac-estado-cancelada']
        : ESTADOS[c.estado];
    const estado = document.createElement('span');
    estado.className = clase;
    estado.textContent = textoEstado;
    cabecera.append(nombre, estado);

    const cantidad = document.createElement('div');
    cantidad.textContent = `≈ ${numero(c.unidadesDisponibles)} de ${numero(c.cantidad)} ${c.unidadPlural} disponibles `
        + `(≈ ${numero(c.disponibleKg, 0)} kg) · ${c.distrito}`;
    const precio = document.createElement('div');
    precio.className = 'fw-semibold';
    precio.textContent = `${soles(c.precioUnitario)} por ${c.unidad.toLowerCase()} · ≈ ${soles(c.precioKg)} por kilo`;
    const [iconoNota, textoNota] = nota(c);
    const lineaNota = document.createElement('div');
    lineaNota.className = 'd-flex align-items-center gap-2 ac-suave mt-1';
    lineaNota.append(icono(iconoNota), Object.assign(document.createElement('span'), { textContent: textoNota }));
    art.append(cabecera, cantidad, precio, lineaNota);

    const acciones = document.createElement('div');
    acciones.className = 'd-flex flex-wrap gap-2 mt-2';
    for (const a of c.acciones) {
        if (a === 'BAJAR_PRECIO') {
            acciones.append(boton('Bajar precio', 'trending-down', 'btn-outline-primary flex-grow-1', () => abrir('precio', c)));
        } else if (a === 'EXTENDER') {
            acciones.append(boton(c.estado === 'VENCIDA' ? 'Publicar otra vez' : 'Extender', 'calendar-plus',
                c.estado === 'VENCIDA' ? 'btn-primary flex-grow-1' : 'btn-outline-primary flex-grow-1', () => abrir('extender', c)));
        } else if (a === 'RETIRAR') {
            acciones.append(boton('Retirar', 'x', 'btn-outline-danger flex-grow-1', () => abrir('retirar', c)));
        } else if (a === 'CORREGIR') {
            acciones.append(boton('Corregir', 'sliders-horizontal', 'btn-primary flex-grow-1', () => abrir('corregir', c)));
        }
    }
    if (c.estado === 'BLOQUEADA') {
        const enlace = document.createElement('a');
        enlace.className = 'btn btn-link flex-grow-1';
        enlace.href = '/pages/notificacion/avisos.html';
        ponerIconoYTexto(enlace, 'bell', 'Ver el aviso');
        acciones.append(enlace);
    }
    if (acciones.children.length > 0) {
        art.append(acciones);
    }
    return art;
}

/** Aviso de arriba: la primera cosecha a la venta que vence pronto (UC-09). */
function banner(cosechas) {
    const pronto = cosechas.find((c) => c.estado === 'PUBLICADA' && c.porVencer);
    const caja = document.getElementById('banner');
    caja.hidden = !pronto;
    if (pronto) {
        document.getElementById('banner-texto').textContent =
            `Su ${pronto.cultivo.toLowerCase()} ${textoVence(pronto)}. ¿Qué desea hacer?`;
        document.getElementById('banner-precio').onclick = () => abrir('precio', pronto);
        document.getElementById('banner-extender').onclick = () => abrir('extender', pronto);
    }
}

function resaltar() {
    const destino = window.location.hash ? document.querySelector(window.location.hash) : null;
    if (destino) {
        destino.classList.add('ac-cosecha-destacada');
        destino.scrollIntoView({ block: 'center' });
    }
}

async function cargar(siguiente = false) {
    pagina = siguiente ? pagina + 1 : 0;
    try {
        const respuesta = await api.get(`/publicaciones/mias?grupo=${grupo}&page=${pagina}`);
        if (!siguiente) {
            lista.replaceChildren();
            banner(grupo === 'ACTIVAS' ? respuesta.content : []);
        }
        respuesta.content.forEach((c) => lista.append(tarjeta(c)));
        const vacio = document.getElementById('vacio');
        vacio.hidden = respuesta.page.totalElements > 0;
        document.getElementById('vacio-titulo').textContent = grupo === 'ACTIVAS'
            ? 'No tiene cosechas a la venta' : 'Aún no tiene cosechas vendidas ni retiradas';
        verMas.hidden = pagina + 1 >= respuesta.page.totalPages;
        if (!siguiente) {
            resaltar();
        }
    } catch (error) {
        pagina = Math.max(0, pagina - (siguiente ? 1 : 0));
        mostrarAlerta(alerta, error.message, 'error');
    }
}

function mostrarCampos(nombre) {
    form.querySelectorAll('[data-campo]').forEach((c) => { c.hidden = c.dataset.campo !== nombre; });
}

function actualizarPrecioKilo() {
    const precio = Number(form.elements.precioUnitario.value);
    document.getElementById('precio-kilo').textContent = precio > 0
        ? `≈ ${soles(precio / Number(cosecha.pesoUnidadKg))} por kilo` : '';
}

function actualizarNuevaFecha() {
    const base = cosecha.fechaLimite > hoy() ? cosecha.fechaLimite : hoy();
    document.getElementById('nueva-fecha').textContent = `Se venderá hasta el ${fechaCorta(sumarDias(base, dias))}.`;
}

/** Abre la hoja de una acción sobre una cosecha. */
function abrir(tipo, c) {
    accion = tipo;
    cosecha = c;
    limpiarErrores(form, alertaDialogo);
    const unidad = c.unidad.toLowerCase();
    const confirmar = document.getElementById('confirmar');
    confirmar.className = 'btn btn-primary w-100 mb-2';
    if (tipo === 'precio') {
        document.getElementById('dialogo-titulo').textContent = `Bajar el precio de su ${titulo(c).toLowerCase()}`;
        document.getElementById('dialogo-detalle').textContent = `Hoy lo vende a ${soles(c.precioUnitario)} por ${unidad}.`;
        document.getElementById('etiqueta-precio').textContent = `Nuevo precio por ${unidad}`;
        form.elements.precioUnitario.value = '';
        form.elements.precioUnitario.max = c.precioUnitario;
        mostrarCampos('precio');
        actualizarPrecioKilo();
        confirmar.textContent = 'Bajar precio';
    } else if (tipo === 'extender') {
        document.getElementById('dialogo-titulo').textContent = c.estado === 'VENCIDA'
            ? `Publicar otra vez su ${titulo(c).toLowerCase()}` : `Extender su ${titulo(c).toLowerCase()}`;
        document.getElementById('dialogo-detalle').textContent = c.estado === 'VENCIDA'
            ? 'Vuelve a estar a la venta con una nueva fecha límite.' : `Hoy se vende hasta el ${fechaCorta(c.fechaLimite)}.`;
        dias = 7;
        crearChips(document.getElementById('dias'), 'dias',
            [7, 14, 30].map((d) => ({ valor: d, texto: `${d} días` })), dias, (valor) => {
                dias = Number(valor);
                actualizarNuevaFecha();
            });
        mostrarCampos('dias');
        actualizarNuevaFecha();
        confirmar.textContent = c.estado === 'VENCIDA' ? 'Publicar otra vez' : 'Extender';
    } else if (tipo === 'retirar') {
        document.getElementById('dialogo-titulo').textContent = `¿Retirar su ${titulo(c).toLowerCase()}?`;
        document.getElementById('dialogo-detalle').textContent =
            'Deja de estar a la venta. Las ventas ya acordadas siguen; nadie más podrá comprar.';
        mostrarCampos(null);
        confirmar.className = 'btn btn-danger w-100 mb-2';
        confirmar.textContent = 'Sí, retirar';
    } else {
        document.getElementById('dialogo-titulo').textContent = `Corregir su ${titulo(c).toLowerCase()}`;
        document.getElementById('dialogo-detalle').textContent =
            'Revise los datos que pidió la moderación. Al guardar, AgroConecta la revisa otra vez antes de mostrarla.';
        document.getElementById('etiqueta-cantidad').textContent = `Cantidad (${c.unidadPlural})`;
        document.getElementById('etiqueta-peso').textContent = `¿Cuántos kilos pesa cada ${unidad}?`;
        document.getElementById('etiqueta-precio-corregido').textContent = `Precio por ${unidad}`;
        form.elements.cantidad.value = Number(c.cantidad);
        form.elements.pesoUnidadKg.value = Number(c.pesoUnidadKg);
        form.elements.precioCorregido.value = Number(c.precioUnitario).toFixed(2);
        document.getElementById('caja-peso').hidden = !unidadesVariables.has(c.unidad);
        mostrarCampos('corregir');
        confirmar.textContent = 'Guardar corrección';
    }
    dialogo.showModal();
}

form.addEventListener('input', (evento) => {
    if (evento.target === form.elements.precioUnitario) {
        actualizarPrecioKilo();
    }
});

form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(form, alertaDialogo);
    const confirmar = document.getElementById('confirmar');
    marcarOcupado(confirmar, true, 'Guardando…');
    const base = `/publicaciones/${cosecha.id}`;
    try {
        let mensaje;
        if (accion === 'precio') {
            await api.post(`${base}/precio`, { precioUnitario: form.elements.precioUnitario.value || null });
            mensaje = 'Listo. Bajó el precio de su cosecha.';
        } else if (accion === 'extender') {
            const actualizada = await api.post(`${base}/extender`, { dias });
            mensaje = `Listo. Su ${actualizada.cultivo.toLowerCase()} sigue a la venta hasta el ${fechaCorta(actualizada.fechaLimite)}.`;
        } else if (accion === 'retirar') {
            await api.post(`${base}/retirar`);
            mensaje = 'Listo. Su cosecha ya no está a la venta.';
        } else {
            await api.put(`${base}/correccion`, {
                cantidad: form.elements.cantidad.value || null,
                pesoUnidadKg: document.getElementById('caja-peso').hidden ? null : (form.elements.pesoUnidadKg.value || null),
                precioUnitario: form.elements.precioCorregido.value || null
            });
            mensaje = 'Listo. AgroConecta revisará su corrección y le avisará.';
        }
        dialogo.close();
        await cargar();
        mostrarAlerta(alerta, mensaje, 'exito');
        alerta.focus();
    } catch (error) {
        mostrarErrores(form, alertaDialogo, error, { precioUnitario: accion === 'corregir' ? 'precioCorregido' : 'precioUnitario' });
    } finally {
        marcarOcupado(confirmar, false);
    }
});

document.getElementById('cancelar').addEventListener('click', () => dialogo.close());
pestanas.forEach((p) => p.addEventListener('click', () => {
    grupo = p.dataset.grupo;
    pestanas.forEach((o) => o.setAttribute('aria-selected', String(o === p)));
    ocultarAlerta(alerta);
    cargar();
}));
verMas.addEventListener('click', () => cargar(true));

async function iniciar() {
    const sesion = requerirSesion();
    if (!sesion) {
        return;
    }
    if (sesion.usuario.rol !== 'AGRICULTOR') {
        window.location.replace('/pages/usuario/inicio.html');
        return;
    }
    ponerBarra('ventas');
    try {
        const unidades = await api.get('/catalogo/unidades');
        unidadesVariables = new Set(unidades.filter((u) => u.factorKg === null).map((u) => u.nombre));
    } catch {
        // Sin catálogo, "Corregir" no muestra el peso: el servidor igual lo ignora en unidades fijas
    }
    cargar();
}

iniciar();
