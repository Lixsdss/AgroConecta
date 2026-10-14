/**
 * "Reclamos" del administrador (formato escritorio, RF-09 / UC-11 parte 3): reclamos por estado, detalle con la
 * venta y su línea de tiempo, y resolución (fundado o infundado, una sola vez).
 * GET /api/v1/admin/reclamos?estado=, GET /api/v1/admin/reclamos/{id} y
 * POST /api/v1/admin/reclamos/{id}/resolucion.
 */
import { api } from '../api.js';
import { mostrarAlerta, ocultarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';
import { icono } from '../comun/iconos.js';
import { iniciarAdministracion, irAIngresarSi401, crear, fechaHora, numero, soles } from './comun.js';

/** Motivos de reclamo (chk_reclamo_motivo), con el texto que ven las partes. */
const MOTIVOS_RECLAMO = {
    NO_SE_PRESENTO: 'No se presentó',
    PRODUCTO_NO_COINCIDE: 'El producto no coincide',
    CANTIDAD_INCOMPLETA: 'Cantidad incompleta',
    INCUMPLIO_PAGO: 'No pagó lo acordado',
    MAL_TRATO: 'Mal trato'
};
const ESTADOS = { PENDIENTE: 'Pendiente', FUNDADO: 'Fundado', INFUNDADO: 'Infundado' };
const ESTADOS_VENTA = {
    PROPUESTA: 'Propuesta', ACORDADA: 'Venta acordada', RECOGIDO: 'Ya recogieron', EN_CAMINO: 'En camino',
    ENTREGADO: 'Ya entregado', COMPLETADA: 'Venta terminada', RECHAZADA: 'Rechazada', CANCELADA: 'Cancelada'
};

const aviso = document.getElementById('aviso');
const lista = document.getElementById('lista');
const detalle = document.getElementById('detalle');

let estado = 'PENDIENTE';
let pagina = 0;

function etiquetaEstado(valor) {
    return crear('span', `ac-estado ac-estado-${valor}`, ESTADOS[valor] ?? valor);
}

async function cargar() {
    detalle.hidden = true;
    try {
        const respuesta = await api.get(`/admin/reclamos?estado=${estado}&page=${pagina}`);
        mostrarLista(respuesta);
    } catch (e) {
        if (!irAIngresarSi401(e)) {
            mostrarAlerta(aviso, e.message, 'error');
        }
    }
}

function mostrarLista(respuesta) {
    const { totalPages, number, totalElements } = respuesta.page;
    if (estado === 'PENDIENTE') {
        document.querySelector('[data-estado="PENDIENTE"]').textContent = `Pendientes (${totalElements})`;
    }
    document.getElementById('paginas').classList.toggle('d-none', totalPages <= 1);
    document.getElementById('pagina').textContent = `Página ${number + 1} de ${Math.max(totalPages, 1)}`;
    document.getElementById('anterior').disabled = number === 0;
    document.getElementById('siguiente').disabled = number + 1 >= totalPages;
    if (respuesta.content.length === 0) {
        const caja = crear('div', 'ac-vacio');
        caja.append(icono('circle-check', 'ac-icono-grande'),
            crear('p', 'fw-semibold mt-2 mb-1', estado === 'PENDIENTE' ? 'No hay reclamos por resolver' : 'No hay reclamos aquí'),
            crear('p', 'ac-suave mb-0', 'Los reclamos de compradores y agricultores aparecerán aquí.'));
        lista.replaceChildren(caja);
        return;
    }
    lista.replaceChildren(...respuesta.content.map((r) => {
        const item = crear('button', 'ac-item-cola');
        item.type = 'button';
        const titulo = crear('div', 'd-flex justify-content-between align-items-center gap-2 mb-1');
        titulo.append(crear('strong', '', `${MOTIVOS_RECLAMO[r.motivo] ?? r.motivo} · ${r.producto}`), etiquetaEstado(r.estado));
        item.append(titulo, crear('div', 'ac-suave', `${r.reclamante} reclama a ${r.contraparte} · ${fechaHora(r.fechaRegistro)}`));
        item.addEventListener('click', () => {
            lista.querySelectorAll('.ac-item-cola').forEach((i) => i.removeAttribute('aria-current'));
            item.setAttribute('aria-current', 'true');
            abrir(r.id);
        });
        return item;
    }));
}

async function abrir(id) {
    try {
        mostrarDetalle(await api.get(`/admin/reclamos/${id}`));
    } catch (e) {
        if (!irAIngresarSi401(e)) {
            mostrarAlerta(aviso, e.message, 'error');
        }
    }
}

function dato(dl, etiqueta, valor, extra) {
    const fila = crear('div', 'ac-dato');
    const dd = crear('dd', '', valor);
    if (extra) {
        dd.append(crear('div', 'ac-suave fw-normal', extra));
    }
    fila.append(crear('dt', '', etiqueta), dd);
    dl.append(fila);
}

function mostrarDetalle({ reclamo, venta }) {
    detalle.replaceChildren();
    const cabecera = crear('div', 'd-flex justify-content-between align-items-start gap-2');
    cabecera.append(crear('h2', 'h4 fw-semibold mb-1', MOTIVOS_RECLAMO[reclamo.motivo] ?? reclamo.motivo),
        etiquetaEstado(reclamo.estado));
    detalle.append(cabecera, crear('p', 'ac-suave', `Registrado ${fechaHora(reclamo.fechaRegistro)}`),
        crear('blockquote', 'ac-vista-celular mb-3', `“${reclamo.descripcion}”`));

    const dl = crear('dl', 'mb-3');
    dato(dl, 'Reclama', reclamo.reclamante, `contra ${reclamo.contraparte}`);
    dato(dl, 'Venta', `${venta.cultivo} ${venta.variedad} · ${numero(venta.cantidad)} ${venta.unidadPlural}`,
        `≈${numero(venta.cantidadKg)} kg · ${soles(venta.montoTotal)} · ${ESTADOS_VENTA[venta.estado] ?? venta.estado}`);
    dato(dl, 'Partes', venta.comprador, venta.agricultor);
    detalle.append(dl, crear('h3', 'h6 fw-semibold', 'Línea de tiempo de la venta'));
    const linea = crear('ol', 'ac-linea-admin');
    linea.append(...venta.lineaDeTiempo.map((p) => {
        const item = crear('li');
        item.append(crear('strong', 'd-block', ESTADOS_VENTA[p.estado] ?? p.estado),
            crear('span', 'ac-suave', `${fechaHora(p.fecha)}${p.autor ? ` · ${p.autor}` : ' · Automático'}`));
        return item;
    }));
    detalle.append(linea);

    if (reclamo.estado === 'PENDIENTE') {
        detalle.append(formularioResolucion(reclamo));
    } else {
        const caja = crear('div', 'ac-alerta ac-alerta-info');
        const texto = crear('div');
        texto.append(crear('strong', 'd-block', `Resuelto: ${ESTADOS[reclamo.estado].toLowerCase()} · ${fechaHora(reclamo.fechaResolucion)}`),
            crear('span', '', reclamo.respuesta ?? ''));
        caja.append(icono('circle-check'), texto);
        detalle.append(caja);
    }
    detalle.hidden = false;
}

function formularioResolucion(reclamo) {
    const form = crear('form');
    form.noValidate = true;
    const grupo = crear('fieldset', 'ac-motivos mb-3');
    grupo.append(crear('legend', 'form-label', 'Resultado'));
    for (const [valor, texto] of [['FUNDADO', 'Fundado: tiene razón quien reclama'], ['INFUNDADO', 'Infundado: no tiene razón']]) {
        const etiqueta = crear('label');
        const radio = crear('input');
        radio.type = 'radio';
        radio.name = 'estado';
        radio.value = valor;
        etiqueta.append(radio, crear('span', '', texto));
        grupo.append(etiqueta);
    }
    const errorEstado = crear('div', 'invalid-feedback d-block');
    errorEstado.dataset.errorDe = 'estado';
    grupo.append(errorEstado);

    const campo = crear('div', 'mb-3');
    const etiqueta = crear('label', 'form-label', 'Respuesta para las dos partes');
    etiqueta.htmlFor = 'respuesta';
    const area = crear('textarea', 'form-control');
    area.id = 'respuesta';
    area.name = 'respuesta';
    area.rows = 3;
    area.maxLength = 300;
    const errorRespuesta = crear('div', 'invalid-feedback d-block');
    errorRespuesta.dataset.errorDe = 'respuesta';
    campo.append(etiqueta, area, crear('div', 'form-text', 'Use palabras sencillas. Las dos partes la verán en su aviso.'),
        errorRespuesta);

    const alerta = crear('div', 'ac-alerta');
    alerta.setAttribute('role', 'alert');
    alerta.tabIndex = -1;
    alerta.hidden = true;
    const enviar = crear('button', 'btn btn-primary w-100', 'Resolver y avisar');
    enviar.type = 'submit';
    form.append(grupo, campo, alerta, enviar);

    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limpiarErrores(form, alerta);
        marcarOcupado(enviar, true, 'Guardando…');
        try {
            await api.post(`/admin/reclamos/${reclamo.id}/resolucion`, {
                estado: form.querySelector('input[name="estado"]:checked')?.value ?? null,
                respuesta: area.value
            });
            mostrarAlerta(aviso, 'Reclamo resuelto. Se avisó a las dos partes.', 'exito');
            await cargar();
        } catch (e) {
            marcarOcupado(enviar, false);
            if (!irAIngresarSi401(e)) {
                mostrarErrores(form, alerta, e);
            }
        }
    });
    return form;
}

document.querySelectorAll('[role="tab"]').forEach((pestana) => {
    pestana.addEventListener('click', () => {
        estado = pestana.dataset.estado;
        pagina = 0;
        document.querySelectorAll('[role="tab"]').forEach((p) => p.setAttribute('aria-selected', String(p === pestana)));
        ocultarAlerta(aviso);
        cargar();
    });
});
document.getElementById('anterior').addEventListener('click', () => { pagina -= 1; cargar(); });
document.getElementById('siguiente').addEventListener('click', () => { pagina += 1; cargar(); });

if (iniciarAdministracion()) {
    cargar();
}
