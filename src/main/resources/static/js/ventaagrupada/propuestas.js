/**
 * "Mis propuestas" (RF-10 / UC-12 pasos 8–9 y A3): cada propuesta con su avance (kilos acordados frente a los
 * pedidos), sus partes y los botones Cerrar y Cancelar mientras está ABIERTA. GET /api/v1/propuestas,
 * POST /api/v1/propuestas/{id}/cerrar y /cancelar.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { ponerIconoYTexto } from '../comun/iconos.js';
import { mostrarAlerta, marcarOcupado } from '../comun/formulario.js';
import { ponerBarra } from '../comun/barra.js';
import { numero, soles, fechaCorta } from '../publicacion/borrador.js';
import { filaParte } from './partes.js';

const ESTADOS = {
    ABIERTA: ['Abierta', 'ac-estado-venta'],
    CERRADA: ['Cerrada', 'ac-estado-venta ac-estado-final'],
    CANCELADA: ['Cancelada', 'ac-estado-venta ac-estado-cancelada']
};

const lista = document.getElementById('lista');
const alerta = document.getElementById('alerta');
const verMas = document.getElementById('ver-mas');
const dialogo = document.getElementById('dialogo');
let pagina = 0;
let pendiente = null;

function tarjeta(p) {
    const art = document.createElement('article');
    art.className = 'ac-resultado';
    const cabecera = document.createElement('div');
    cabecera.className = 'd-flex justify-content-between align-items-start gap-2 mb-1';
    const nombre = document.createElement('span');
    nombre.className = 'ac-resultado-titulo';
    nombre.textContent = `${p.variedad === 'Común' ? `${p.cultivo} común` : `${p.cultivo} ${p.variedad}`} · ${p.distrito}`;
    const [textoEstado, clase] = ESTADOS[p.estado];
    const estado = document.createElement('span');
    estado.className = clase;
    estado.textContent = textoEstado;
    cabecera.append(nombre, estado);

    const pedido = document.createElement('div');
    pedido.textContent = `${numero(p.cantidadKg, 0)} kg a ${soles(p.precioKg)} por kilo · recojo ${fechaCorta(p.fechaRecojo)}`;
    const acordados = Number(p.kgAcordados);
    const pedidos = Number(p.cantidadKg);
    const avance = document.createElement('div');
    avance.className = 'ac-avance my-2';
    avance.setAttribute('role', 'progressbar');
    avance.setAttribute('aria-valuemin', '0');
    avance.setAttribute('aria-valuemax', String(pedidos));
    avance.setAttribute('aria-valuenow', String(acordados));
    avance.setAttribute('aria-label', 'Kilos acordados');
    const barra = document.createElement('span');
    barra.style.width = `${Math.min(100, Math.round((acordados / pedidos) * 100))}%`;
    avance.append(barra);
    const resumen = document.createElement('div');
    resumen.className = 'fw-semibold';
    resumen.textContent = `Acordados ${numero(acordados, 0)} de ${numero(pedidos, 0)} kg`
        + (Number(p.kgPendientes) > 0 ? ` · ${numero(p.kgPendientes, 0)} kg esperando respuesta` : '');

    const partes = document.createElement('div');
    partes.className = 'ac-calificaciones mt-2';
    partes.append(...p.partes.map(filaParte));
    art.append(cabecera, pedido, avance, resumen, partes);

    if (p.estado === 'ABIERTA') {
        const botones = document.createElement('div');
        botones.className = 'd-flex gap-2 mt-2';
        const cerrar = document.createElement('button');
        cerrar.type = 'button';
        cerrar.className = 'btn btn-primary flex-grow-1';
        ponerIconoYTexto(cerrar, 'check', 'Cerrar');
        cerrar.addEventListener('click', () => confirmar(p, 'cerrar', cerrar));
        const cancelar = document.createElement('button');
        cancelar.type = 'button';
        cancelar.className = 'btn btn-outline-danger flex-grow-1';
        ponerIconoYTexto(cancelar, 'x', 'Cancelar');
        cancelar.addEventListener('click', () => confirmar(p, 'cancelar', cancelar));
        botones.append(cerrar, cancelar);
        art.append(botones);
    }
    return art;
}

function confirmar(propuesta, accion, boton) {
    pendiente = { propuesta, accion, boton };
    document.getElementById('dialogo-titulo').textContent = accion === 'cerrar'
        ? '¿Cerrar la propuesta?' : '¿Cancelar la propuesta?';
    document.getElementById('dialogo-texto').textContent = (accion === 'cerrar'
        ? 'Use esto cuando ya tenga lo que necesitaba. '
        : 'Use esto si ya no quiere comprar este lote. ')
        + 'Las partes que nadie respondió se cancelan; las ya aceptadas siguen como ventas normales.';
    document.getElementById('dialogo-si').textContent = accion === 'cerrar' ? 'Sí, cerrar' : 'Sí, cancelar';
    dialogo.showModal();
}

document.getElementById('dialogo-no').addEventListener('click', () => dialogo.close());
document.getElementById('dialogo-si').addEventListener('click', async () => {
    dialogo.close();
    const { propuesta, accion, boton } = pendiente;
    marcarOcupado(boton, true, 'Guardando…');
    try {
        await api.post(`/propuestas/${propuesta.id}/${accion}`);
        await cargar();
        mostrarAlerta(alerta, accion === 'cerrar' ? 'Listo. La propuesta quedó cerrada.' : 'Listo. La propuesta quedó cancelada.',
            'exito');
        alerta.focus();
    } catch (error) {
        marcarOcupado(boton, false);
        mostrarAlerta(alerta, error.message, 'error');
    }
});

async function cargar(siguiente = false) {
    pagina = siguiente ? pagina + 1 : 0;
    try {
        const respuesta = await api.get(`/propuestas?page=${pagina}`);
        if (!siguiente) {
            lista.replaceChildren();
        }
        respuesta.content.forEach((p) => lista.append(tarjeta(p)));
        document.getElementById('vacio').hidden = respuesta.page.totalElements > 0;
        verMas.hidden = pagina + 1 >= respuesta.page.totalPages;
    } catch (error) {
        pagina = Math.max(0, pagina - (siguiente ? 1 : 0));
        mostrarAlerta(alerta, error.message, 'error');
    }
}

verMas.addEventListener('click', () => cargar(true));
if (requerirSesion()) {
    ponerBarra('agrupados');
    cargar();
}
