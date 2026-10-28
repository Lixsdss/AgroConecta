/**
 * "Vender · Paso 2" (diseño 08, UC-01): unidad, cantidad, kilos por jaba o saco y fecha de cosecha.
 * GET /api/v1/catalogo/unidades. El peso sugerido es el del cultivo, solo si la unidad es su unidad local.
 * El quintal, el kilo y la tonelada tienen peso fijo: no se pregunta.
 */
import { api } from '../api.js';
import { mostrarAlerta, limpiarErrores } from '../comun/formulario.js';
import {
    exigirAgricultor, leerBorrador, guardarBorrador, volverSiFalta, crearOpcion, marcarElegida, hoy, numero, RUTA
} from './borrador.js';

const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const contUnidades = document.getElementById('unidades');
const campos = formulario.elements;
const cajaPeso = document.getElementById('caja-peso');
const ayudaPeso = document.getElementById('ayuda-peso');
const resumen = document.getElementById('resumen');
let unidadActual = null;

function kilosPorUnidad() {
    if (!unidadActual) {
        return null;
    }
    return unidadActual.factorKg ?? (Number(campos.pesoUnidadKg.value) || null);
}

function actualizarResumen() {
    const cantidad = Number(campos.cantidad.value);
    const kilos = kilosPorUnidad();
    if (!unidadActual || !(cantidad > 0) || !kilos) {
        resumen.hidden = true;
        return;
    }
    const plural = cantidad === 1 ? unidadActual.nombre.toLowerCase() : unidadActual.nombrePlural;
    resumen.textContent = `${numero(cantidad)} ${plural} ≈ ${numero(cantidad * kilos)} kilos`;
    resumen.hidden = false;
}

function elegirUnidad(unidad, boton) {
    const borrador = leerBorrador();
    marcarElegida(contUnidades, boton);
    unidadActual = unidad;
    document.getElementById('etiqueta-cantidad').textContent = `¿Cuántas ${unidad.nombrePlural} tiene?`;
    cajaPeso.hidden = unidad.factorKg !== null;
    if (unidad.factorKg === null) {
        document.getElementById('etiqueta-peso').textContent = `Cada ${unidad.nombre.toLowerCase()} pesa`;
        const sugerido = unidad.id === borrador.idUnidadLocal ? borrador.pesoSugeridoKg : null;
        const guardado = borrador.idUnidad === unidad.id ? borrador.pesoUnidadKg : null;
        campos.pesoUnidadKg.value = guardado ?? sugerido ?? '';
        ayudaPeso.hidden = sugerido === null || guardado !== null;
    }
    guardarBorrador({
        idUnidad: unidad.id, unidad: unidad.nombre, unidadPlural: unidad.nombrePlural, factorKg: unidad.factorKg
    });
    actualizarResumen();
}

function error(campo, mensaje) {
    formulario.querySelector(`[data-error-de="${campo}"]`).textContent = mensaje;
    campos[campo]?.classList.add('is-invalid');
}

async function iniciar() {
    if (!exigirAgricultor()) {
        return;
    }
    const borrador = leerBorrador();
    if (volverSiFalta(borrador, [['idCultivo', 'paso1.html'], ['idVariedad', 'paso1.html']])) {
        return;
    }
    campos.cantidad.value = borrador.cantidad ?? '';
    campos.fechaCosecha.value = borrador.fechaCosecha ?? hoy();
    campos.fechaCosecha.max = hoy();
    try {
        const unidades = await api.get('/catalogo/unidades');
        for (const u of unidades) {
            const detalle = u.factorKg === null ? 'Usted dice cuántos kilos pesa' : `${numero(u.factorKg, 3)} kilos`;
            const boton = crearOpcion(u.nombre, detalle, u.factorKg === null ? 'weight' : 'scale');
            boton.addEventListener('click', () => elegirUnidad(u, boton));
            contUnidades.append(boton);
            const elegida = borrador.idUnidad ?? borrador.idUnidadLocal;
            if (elegida === u.id) {
                elegirUnidad(u, boton);
            }
        }
    } catch (e) {
        mostrarAlerta(alerta, e.message, 'error');
    }
}

for (const nombre of ['cantidad', 'pesoUnidadKg', 'fechaCosecha']) {
    campos[nombre].addEventListener('input', () => {
        guardarBorrador({ [nombre]: campos[nombre].value });
        if (nombre === 'pesoUnidadKg') {
            ayudaPeso.hidden = true;
        }
        actualizarResumen();
    });
}

formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    let valido = true;
    if (!unidadActual) {
        mostrarAlerta(alerta, 'Elija cómo lo vende.', 'error');
        valido = false;
    }
    if (!(Number(campos.cantidad.value) > 0)) {
        error('cantidad', 'Escriba cuánto tiene.');
        valido = false;
    }
    if (unidadActual?.factorKg === null && !(Number(campos.pesoUnidadKg.value) > 0)) {
        error('pesoUnidadKg', 'Indique cuántos kilos pesa cada una.');
        valido = false;
    }
    if (!campos.fechaCosecha.value || campos.fechaCosecha.value > hoy()) {
        error('fechaCosecha', 'Indique cuándo cosechó (hoy o antes).');
        valido = false;
    }
    if (valido) {
        guardarBorrador({
            cantidad: campos.cantidad.value,
            pesoUnidadKg: unidadActual.factorKg === null ? campos.pesoUnidadKg.value : null,
            fechaCosecha: campos.fechaCosecha.value
        });
        window.location.assign(`${RUTA}/paso3.html`);
    }
});

iniciar();
