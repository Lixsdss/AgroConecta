/**
 * Reclamo sobre una venta (RF-09 / UC-11 parte 3, paso 1) en la pantalla "Entrega": si la venta llegó a acordarse,
 * muestra el reclamo que el usuario ya registró (con su estado y la respuesta) o el botón "Registrar reclamo".
 * GET /api/v1/reclamos/mios?idVenta= y POST /api/v1/reclamos.
 */
import { api } from '../api.js';
import { icono } from '../comun/iconos.js';
import { mostrarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';

/** Motivos de reclamo (chk_reclamo_motivo). */
const MOTIVOS = [
    ['NO_SE_PRESENTO', 'No se presentó al recojo'],
    ['PRODUCTO_NO_COINCIDE', 'El producto no es el acordado'],
    ['CANTIDAD_INCOMPLETA', 'Llegó menos cantidad'],
    ['INCUMPLIO_PAGO', 'No pagó lo acordado'],
    ['MAL_TRATO', 'Mal trato']
];
const RESULTADOS = { FUNDADO: 'fundado (usted tiene razón)', INFUNDADO: 'infundado' };

const seccion = document.getElementById('seccion-reclamo');
const contenedor = document.getElementById('reclamo');
const boton = document.getElementById('reclamar');
const dialogo = document.getElementById('dialogo-reclamo');
const formulario = document.getElementById('form-reclamo');
const alerta = document.getElementById('alerta-reclamo');
let idVenta = null;

function texto(etiqueta, clase, valor) {
    const el = document.createElement(etiqueta);
    el.className = clase;
    el.textContent = valor;
    return el;
}

function dibujar(reclamo) {
    const motivo = MOTIVOS.find(([clave]) => clave === reclamo.motivo)?.[1] ?? reclamo.motivo;
    const caja = document.createElement('div');
    const cuerpo = document.createElement('div');
    cuerpo.append(texto('strong', 'd-block', `Usted reclamó: ${motivo}`), texto('span', 'd-block', `“${reclamo.descripcion}”`));
    if (reclamo.estado === 'PENDIENTE') {
        caja.className = 'ac-alerta ac-alerta-info';
        cuerpo.append(texto('span', 'd-block mt-1', 'En revisión. AgroConecta le avisará cuando lo resuelva.'));
        caja.append(icono('clock'), cuerpo);
    } else {
        caja.className = 'ac-alerta ac-alerta-exito';
        cuerpo.append(texto('span', 'd-block mt-1 fw-semibold', `Resuelto: ${RESULTADOS[reclamo.estado]}.`),
            texto('span', 'd-block', reclamo.respuesta ?? ''));
        caja.append(icono('circle-check'), cuerpo);
    }
    contenedor.replaceChildren(caja);
    boton.hidden = true;
}

/**
 * Muestra la sección de reclamo de la venta.
 *
 * @param {object} venta venta de GET /api/v1/ventas/{id}
 */
export async function mostrarReclamo(venta) {
    idVenta = venta.id;
    const llegoAAcordada = venta.lineaDeTiempo.some((p) => p.estado === 'ACORDADA');
    seccion.hidden = !llegoAAcordada;
    if (!llegoAAcordada) {
        return;
    }
    try {
        const mios = await api.get(`/reclamos/mios?idVenta=${venta.id}`);
        if (mios.length > 0) {
            dibujar(mios[0]);
        } else {
            contenedor.replaceChildren(texto('p', 'ac-suave mb-2',
                '¿Hubo un problema con esta venta? Regístrelo y AgroConecta lo revisará.'));
            boton.hidden = false;
        }
    } catch {
        seccion.hidden = true; // La venta se ve igual aunque no se pueda consultar el reclamo
    }
}

function abrir() {
    limpiarErrores(formulario, alerta);
    const lista = document.getElementById('motivos-reclamo');
    lista.replaceChildren(...MOTIVOS.map(([clave, etiqueta]) => {
        const label = document.createElement('label');
        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'motivo';
        radio.value = clave;
        label.append(radio, texto('span', '', etiqueta));
        return label;
    }));
    formulario.elements.descripcion.value = '';
    dialogo.showModal();
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    const enviar = document.getElementById('enviar-reclamo');
    marcarOcupado(enviar, true, 'Enviando…');
    try {
        const reclamo = await api.post('/reclamos', {
            idVenta,
            motivo: formulario.querySelector('input[name="motivo"]:checked')?.value ?? null,
            descripcion: formulario.elements.descripcion.value
        });
        dialogo.close();
        dibujar(reclamo);
        const principal = document.getElementById('alerta');
        mostrarAlerta(principal, 'Reclamo enviado. AgroConecta lo revisará y le avisará.', 'exito');
        principal.focus();
    } catch (error) {
        mostrarErrores(formulario, alerta, error);
    } finally {
        marcarOcupado(enviar, false);
    }
});

boton.addEventListener('click', abrir);
document.getElementById('cerrar-reclamo').addEventListener('click', () => dialogo.close());
