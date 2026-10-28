/**
 * "Propuesta conjunta" (diseño "propuesta", RF-10 / UC-12 pasos 2–6): precio por kilo, kilos y fecha de recojo para
 * un lote (?idVariedad=&idDistrito=); al enviar muestra cómo se repartió. GET /api/v1/lotes y POST /api/v1/propuestas.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { mostrarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';
import { numero, soles, fechaCorta } from '../publicacion/borrador.js';
import { filaParte } from './partes.js';

const parametros = new URLSearchParams(window.location.search);
const idVariedad = Number(parametros.get('idVariedad'));
const idDistrito = Number(parametros.get('idDistrito'));
const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const campos = formulario.elements;

function hoyMas(dias) {
    const d = new Date();
    d.setDate(d.getDate() + dias);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function actualizarTotal() {
    const total = Number(campos.precioKg.value) * Number(campos.cantidadKg.value);
    document.getElementById('total').textContent = soles(total > 0 ? total : 0);
}

formulario.addEventListener('input', actualizarTotal);

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    const boton = document.getElementById('enviar');
    marcarOcupado(boton, true, 'Enviando…');
    try {
        const propuesta = await api.post('/propuestas', {
            idVariedad, idDistrito,
            cantidadKg: campos.cantidadKg.value || null,
            precioKg: campos.precioKg.value || null,
            fechaRecojo: campos.fechaRecojo.value || null
        });
        formulario.hidden = true;
        const repartidos = Number(propuesta.kgRepartidos);
        const pedidos = Number(propuesta.cantidadKg);
        document.getElementById('resultado-texto').textContent = repartidos < pedidos
            ? `Propuesta enviada a ${propuesta.partes.length} agricultores. Se repartieron ${numero(repartidos, 0)} de `
              + `${numero(pedidos, 0)} kg; el resto no completa una unidad entera.`
            : `Propuesta enviada a ${propuesta.partes.length} agricultores. Responden hasta el `
              + `${fechaCorta(propuesta.fechaRecojo)}.`;
        document.getElementById('partes').replaceChildren(...propuesta.partes.map(filaParte));
        document.getElementById('resultado').hidden = false;
    } catch (error) {
        mostrarErrores(formulario, alerta, error);
        marcarOcupado(boton, false);
    }
});

async function iniciar() {
    if (!requerirSesion()) {
        return;
    }
    if (!idVariedad || !idDistrito) {
        mostrarAlerta(alerta, 'Elija un lote desde "Lotes agrupados".', 'error');
        return;
    }
    try {
        const respuesta = await api.get(`/lotes?idDistrito=${idDistrito}`);
        const lote = respuesta.content.find((l) => l.idVariedad === idVariedad);
        if (!lote) {
            mostrarAlerta(alerta, 'Ese lote ya no está disponible: necesita 2 o más agricultores con cosechas a la venta.',
                'error');
            return;
        }
        const nombre = lote.variedad === 'Común' ? `${lote.cultivo} común` : `${lote.cultivo} ${lote.variedad}`;
        document.getElementById('lote-titulo').textContent = `${nombre} · ${lote.distrito}`;
        document.getElementById('lote-detalle').textContent = `${lote.totalAgricultores} agricultores · `
            + `${numero(lote.volumenKg, 0)} kg disponibles · promedio ${soles(lote.precioPromedioKg)} por kilo`;
        document.getElementById('maximo').textContent = `Máximo ${numero(lote.volumenKg, 0)} kg en este lote.`;
        campos.cantidadKg.max = lote.volumenKg;
        campos.precioKg.value = Number(lote.precioPromedioKg).toFixed(2);
        campos.fechaRecojo.min = hoyMas(0);
        campos.fechaRecojo.value = hoyMas(2);
        formulario.hidden = false;
        actualizarTotal();
    } catch (error) {
        mostrarAlerta(alerta, error.estado === 403 ? 'Las propuestas conjuntas son para compradores.' : error.message,
            'error');
    }
}

iniciar();
