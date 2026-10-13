/**
 * "Entrega" (diseño "entrega", RF-11 / UC-06): línea de tiempo de la venta, detalle del acuerdo y solo las acciones
 * que el usuario puede hacer ahora. GET /api/v1/ventas/{id} y POST /api/v1/ventas/{id}/estado.
 * RF-05: calificaciones dada y recibida, y la hoja "¿Cómo le fue con…?" (POST /api/v1/ventas/{id}/calificacion).
 * Con ?calificar=1 la hoja se abre sola (enlace "Calificar" del historial).
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import {
    mostrarAlerta, ocultarAlerta, marcarOcupado, limpiarErrores, mostrarErrores
} from '../comun/formulario.js';
import { numero, soles } from '../publicacion/borrador.js';
import { ACCIONES, textoEstado } from './estados.js';
import { TEXTOS_ESTRELLAS, tarjetaCalificacion } from './estrellas.js';

const id = new URLSearchParams(window.location.search).get('id');
const alerta = document.getElementById('alerta');
const dialogo = document.getElementById('dialogo-cancelar');
/** Acción que espera confirmación en la ventana (CANCELAR o RECHAZAR). */
let accionPorConfirmar = null;
const dialogoCalificar = document.getElementById('dialogo-calificar');
const formCalificar = document.getElementById('form-calificar');
let ventaActual = null;
let puntuacion = 0;

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
    const base = estados.includes('ENTREGADO') && !estados.includes('RECOGIDO') ? EN_CHACRA : CON_TRANSPORTE;
    // RF-10: la parte de una propuesta conjunta empieza en PROPUESTA y, si se acepta, sigue el camino normal
    const camino = estados.includes('PROPUESTA') ? ['PROPUESTA', ...base] : base;
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
    const limite = diaRecojo(venta.fechaRecojo);
    if (venta.estado === 'PROPUESTA' && !venta.soyComprador) {
        texto = `${venta.contraparte.negocio ?? venta.contraparte.nombre} le propone esta compra como parte de una `
            + `propuesta conjunta. Acepte o rechace su parte antes del ${limite}.`;
    } else if (venta.estado === 'PROPUESTA') {
        texto = `Parte de su propuesta conjunta: el agricultor puede responder hasta el ${limite}.`;
    } else if (venta.estado === 'ACORDADA' && venta.soyComprador) {
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
            if (accion === 'CANCELAR' || accion === 'RECHAZAR') {
                confirmar(venta, accion);
            } else {
                cambiar(accion, boton);
            }
        });
        contenedor.append(boton);
    }
}

/** Nombre de la otra parte para "Calificar a …" y "¿Cómo le fue con …?". */
function nombreOtra(venta) {
    return venta.contraparte.negocio ?? venta.contraparte.nombre;
}

function dibujarCalificaciones(venta) {
    const c = venta.calificaciones;
    const seccion = document.getElementById('seccion-calificaciones');
    const lista = document.getElementById('calificaciones');
    lista.replaceChildren();
    if (c.mia) {
        lista.append(tarjetaCalificacion(c.mia, 'Usted calificó'));
    }
    if (c.recibida) {
        lista.append(tarjetaCalificacion(c.recibida, `${c.recibida.autor} le calificó`));
    }
    const boton = document.getElementById('calificar');
    boton.hidden = !c.puedoCalificar;
    poner('texto-calificar', `Calificar a ${nombreOtra(venta)}`);
    seccion.hidden = !c.mia && !c.recibida && !c.puedoCalificar;
}

function elegirPuntuacion(valor) {
    puntuacion = valor;
    document.querySelectorAll('#elegir-estrellas label').forEach((etiqueta, i) => {
        etiqueta.classList.toggle('ac-encendida', i < valor);
    });
    poner('texto-puntuacion', TEXTOS_ESTRELLAS[valor]);
    document.getElementById('enviar-calificacion').disabled = valor === 0;
}

function abrirCalificar() {
    const v = ventaActual;
    limpiarErrores(formCalificar, document.getElementById('alerta-calificar'));
    poner('titulo-calificar', `¿Cómo le fue con ${nombreOtra(v)}?`);
    const unidad = Number(v.cantidad) === 1 ? v.unidad.toLowerCase() : v.unidadPlural;
    poner('calificar-que', `${numero(v.cantidad)} ${unidad} de ${v.cultivo.toLowerCase()} ${v.variedad} · Venta terminada`);
    const contenedor = document.getElementById('elegir-estrellas');
    contenedor.replaceChildren();
    for (let i = 1; i <= 5; i++) {
        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'puntuacion';
        radio.id = `estrella-${i}`;
        radio.value = String(i);
        radio.addEventListener('change', () => elegirPuntuacion(i));
        const etiqueta = document.createElement('label');
        etiqueta.htmlFor = radio.id;
        etiqueta.textContent = '★';
        etiqueta.setAttribute('aria-label', `${i} ${i === 1 ? 'estrella' : 'estrellas'}: ${TEXTOS_ESTRELLAS[i]}`);
        contenedor.append(radio, etiqueta);
    }
    formCalificar.elements.comentario.value = '';
    elegirPuntuacion(0);
    dialogoCalificar.showModal();
}

formCalificar.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const alertaCalificar = document.getElementById('alerta-calificar');
    limpiarErrores(formCalificar, alertaCalificar);
    const boton = document.getElementById('enviar-calificacion');
    marcarOcupado(boton, true, 'Enviando…');
    try {
        const venta = await api.post(`/ventas/${id}/calificacion`, {
            puntuacion,
            comentario: formCalificar.elements.comentario.value
        });
        dialogoCalificar.close();
        mostrar(venta);
        mostrarAlerta(alerta, '¡Gracias por calificar! Su opinión ayuda a otros a vender y comprar con confianza.',
            'exito');
        alerta.focus();
    } catch (error) {
        mostrarErrores(formCalificar, alertaCalificar, error);
    } finally {
        marcarOcupado(boton, false);
        boton.disabled = puntuacion === 0;
    }
});
document.getElementById('calificar').addEventListener('click', abrirCalificar);
document.getElementById('cerrar-calificar').addEventListener('click', () => dialogoCalificar.close());

function mostrar(venta) {
    ventaActual = venta;
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
    dibujarCalificaciones(venta);
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

/** Pide confirmación antes de cancelar la venta, cancelar una parte o rechazarla. */
function confirmar(venta, accion) {
    accionPorConfirmar = accion;
    const propuesta = venta.estado === 'PROPUESTA';
    poner('titulo-cancelar', accion === 'RECHAZAR' ? '¿Rechazar su parte?'
        : propuesta ? '¿Cancelar esta parte de la propuesta?' : '¿Cancelar la venta?');
    poner('texto-cancelar', accion === 'RECHAZAR'
        ? 'El comprador sabrá que no acepta esta parte. Puede seguir conversando por el chat.'
        : propuesta ? 'El agricultor ya no podrá aceptarla. Las demás partes siguen.'
            : 'La cosecha vuelve a quedar disponible para otros compradores. Esta acción no se puede deshacer.');
    poner('si-cancelar', accion === 'RECHAZAR' ? 'Sí, rechazar' : 'Sí, cancelar');
    dialogo.showModal();
}

document.getElementById('no-cancelar').addEventListener('click', () => dialogo.close());
document.getElementById('si-cancelar').addEventListener('click', (evento) => {
    dialogo.close();
    cambiar(accionPorConfirmar ?? 'CANCELAR', evento.currentTarget);
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
        if (new URLSearchParams(window.location.search).get('calificar') === '1'
            && ventaActual.calificaciones.puedoCalificar) {
            abrirCalificar();
        }
    } catch (error) {
        mostrarAlerta(alerta, error.estado === 403 ? 'Esta venta no es suya.' : error.message, 'error');
    }
}

iniciar();
