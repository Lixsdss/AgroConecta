/**
 * "Entrega" (diseño "entrega", RF-11 / UC-06): línea de tiempo de la venta, detalle del acuerdo y solo las acciones
 * que el usuario puede hacer ahora. GET /api/v1/ventas/{id} y POST /api/v1/ventas/{id}/estado.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import { mostrarAlerta, ocultarAlerta, marcarOcupado } from '../comun/formulario.js';
import { numero, soles } from '../publicacion/borrador.js';
import { ACCIONES, textoEstado } from './estados.js';

const id = new URLSearchParams(window.location.search).get('id');
const alerta = document.getElementById('alerta');
const dialogo = document.getElementById('dialogo-cancelar');

/** Camino con transporte y camino de recojo en chacra (UC-06 A1). */
const CON_TRANSPORTE = ['ACORDADA', 'RECOGIDO', 'EN_CAMINO', 'ENTREGADO', 'COMPLETADA'];
const EN_CHACRA = ['ACORDADA', 'ENTREGADO', 'COMPLETADA'];

function poner(idElemento, texto) {
    document.getElementById(idElemento).textContent = texto;
}

function fechaHora(iso) {
    const d = new Date(iso);
    return `${d.toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric', month: 'short' })}, `
        + d.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' });
}

function diaRecojo(iso) {
    return new Date(`${iso}T00:00:00`).toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
}

/** Línea de tiempo: los pasos hechos (con fecha y autor) y los que faltan en el camino que sigue la venta. */
function dibujarLinea(venta) {
    const hechos = venta.lineaDeTiempo;
    const estados = hechos.map((p) => p.estado);
    const camino = estados.includes('ENTREGADO') && !estados.includes('RECOGIDO') ? EN_CHACRA : CON_TRANSPORTE;
    const pendientes = venta.estado === 'CANCELADA' || venta.estado === 'RECHAZADA'
        ? [] : camino.filter((e) => !estados.includes(e));
    const lista = document.getElementById('linea');
    lista.replaceChildren();

    hechos.forEach((paso) => {
        const item = document.createElement('li');
        item.className = paso.estado === 'CANCELADA' || paso.estado === 'RECHAZADA' ? 'ac-cancelado' : 'ac-hecho';
        const punto = document.createElement('span');
        punto.className = 'ac-punto';
        punto.append(icono(item.className === 'ac-cancelado' ? 'x' : 'check'));
        const titulo = document.createElement('strong');
        titulo.className = 'd-block';
        titulo.textContent = textoEstado(paso.estado);
        const detalle = document.createElement('span');
        detalle.className = 'ac-suave';
        const autor = paso.autor === null ? 'automático' : paso.esUsted ? 'usted' : paso.autor;
        detalle.textContent = `${fechaHora(paso.fecha)} · Por ${autor}`;
        item.append(punto, titulo, detalle);
        lista.append(item);
    });
    pendientes.forEach((estado, i) => {
        const item = document.createElement('li');
        item.className = i === 0 ? 'ac-siguiente' : '';
        const punto = document.createElement('span');
        punto.className = 'ac-punto';
        const titulo = document.createElement('strong');
        titulo.className = 'd-block';
        titulo.textContent = textoEstado(estado);
        const detalle = document.createElement('span');
        detalle.className = 'ac-suave';
        detalle.textContent = i === 0 ? 'Siguiente paso' : 'Pendiente';
        item.append(punto, titulo, detalle);
        lista.append(item);
    });
}

function ayuda(venta) {
    const caja = document.getElementById('ayuda');
    let texto = '';
    if (venta.estado === 'ACORDADA' && venta.soyComprador) {
        texto = 'Si recoge usted mismo en la chacra, pulse «Confirmar que recibí» cuando tenga la carga.';
    } else if (venta.estado === 'EN_CAMINO' && !venta.soyComprador) {
        texto = 'El comprador confirmará cuando reciba la carga. Desde el recojo, la venta ya no se cancela.';
    } else if (['RECOGIDO', 'EN_CAMINO'].includes(venta.estado)) {
        texto = 'Desde que recogen la carga, la venta ya no se cancela. Si hay un problema, escriba por el chat.';
    }
    caja.textContent = texto;
    caja.hidden = !texto;
}

function dibujarAcciones(venta) {
    const contenedor = document.getElementById('acciones');
    contenedor.replaceChildren();
    for (const accion of venta.acciones) {
        const [texto, nombreIcono, clase] = ACCIONES[accion];
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = `btn ${clase}`;
        ponerIconoYTexto(boton, nombreIcono, texto);
        boton.addEventListener('click', () => {
            if (accion === 'CANCELAR') {
                dialogo.showModal();
            } else {
                cambiar(accion, boton);
            }
        });
        contenedor.append(boton);
    }
}

function mostrar(venta) {
    poner('titulo', textoEstado(venta.estado));
    dibujarLinea(venta);
    const unidad = Number(venta.cantidad) === 1 ? venta.unidad.toLowerCase() : venta.unidadPlural;
    poner('d-cantidad', `${numero(venta.cantidad)} ${unidad} de ${venta.cultivo.toLowerCase()} ${venta.variedad} `
        + `(unos ${numero(venta.cantidadKg, 0)} kilos)`);
    poner('d-precio', `${soles(venta.precioUnitario)} por ${venta.unidad.toLowerCase()} · ≈ ${soles(venta.precioKg)} por kilo`);
    poner('d-total', `Total: ${soles(venta.montoTotal)}`);
    const recojo = document.getElementById('d-recojo');
    recojo.replaceChildren(icono('calendar'), Object.assign(document.createElement('span'),
        { textContent: `Recojo: ${diaRecojo(venta.fechaRecojo)}` }));
    const lugar = document.getElementById('d-lugar');
    lugar.replaceChildren(icono('map-pin'), Object.assign(document.createElement('span'),
        { textContent: venta.distrito }));
    const c = venta.contraparte;
    const quien = venta.soyComprador ? `Agricultor: ${c.nombre}` : `Comprador: ${c.negocio ?? c.nombre} · ${c.nombre}`;
    const contraparte = document.getElementById('d-contraparte');
    contraparte.replaceChildren(icono('user'), Object.assign(document.createElement('span'), { textContent: quien }));
    const celular = document.getElementById('d-celular');
    celular.href = `tel:+51${c.celular}`;
    ponerIconoYTexto(celular, 'message-circle', `Llamar al ${c.celular}`);
    document.getElementById('ir-chat').href = `/pages/mensajeria/chat.html?id=${venta.idConversacion}`;
    ayuda(venta);
    dibujarAcciones(venta);
    document.getElementById('contenido').hidden = false;
}

async function cambiar(accion, boton) {
    marcarOcupado(boton, true, 'Guardando…');
    try {
        const venta = await api.post(`/ventas/${id}/estado`, { accion });
        ocultarAlerta(alerta);
        mostrar(venta);
        mostrarAlerta(alerta, `Listo: ${textoEstado(venta.estado).toLowerCase()}.`, 'exito');
        alerta.focus();
    } catch (error) {
        marcarOcupado(boton, false);
        mostrarAlerta(alerta, error.message, 'error');
        alerta.focus();
    }
}

document.getElementById('no-cancelar').addEventListener('click', () => dialogo.close());
document.getElementById('si-cancelar').addEventListener('click', (evento) => {
    dialogo.close();
    cambiar('CANCELAR', evento.currentTarget);
});

async function iniciar() {
    if (!requerirSesion()) {
        return;
    }
    if (!id || !/^\d+$/.test(id)) {
        mostrarAlerta(alerta, 'La venta no existe.', 'error');
        return;
    }
    try {
        mostrar(await api.get(`/ventas/${id}`));
    } catch (error) {
        mostrarAlerta(alerta, error.estado === 403 ? 'Esta venta no es suya.' : error.message, 'error');
    }
}

iniciar();
