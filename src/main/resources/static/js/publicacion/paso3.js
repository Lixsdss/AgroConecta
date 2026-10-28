/**
 * "Vender · Paso 3" (diseño 09, UC-01): precio por unidad con su equivalente por kilo, y distrito de la cosecha
 * (tomado del perfil, se puede cambiar). GET /api/v1/usuarios/me y GET /api/v1/catalogo/distritos.
 * RF-02: GET /api/v1/precios/referencia da el rango de hoy (±15 %) y el veredicto se calcula con el precio por kilo.
 */
import { api } from '../api.js';
import { mostrarAlerta, limpiarErrores } from '../comun/formulario.js';
import { icono } from '../comun/iconos.js';
import { exigirAgricultor, leerBorrador, guardarBorrador, volverSiFalta, soles, RUTA } from './borrador.js';

const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const campos = formulario.elements;
const precioKilo = document.getElementById('precio-kilo');
const selectDistrito = campos.idDistrito;
let distritos = [];
let referencia = null;

/** Veredictos del diseño (VV): título, detalle, ícono y estilo. El color nunca va solo. */
const VEREDICTOS = {
    dentro: ['Buen precio', 'Está dentro de lo que pagan hoy.', 'circle-check'],
    bajo: ['Precio bajo, podría ganar más', 'Otros venden más caro hoy. Pruebe subir un poco.', 'triangle-alert'],
    alto: ['Precio muy alto, puede tardar en vender', 'Pruebe un precio más cerca del mercado.', 'circle-alert']
};

function kilosPorUnidad(b) {
    return Number(b.factorKg ?? b.pesoUnidadKg);
}

function actualizarPrecioKilo() {
    const b = leerBorrador();
    const precio = Number(campos.precioUnitario.value);
    if (!(precio > 0)) {
        precioKilo.hidden = true;
        return;
    }
    const porKilo = precio / kilosPorUnidad(b);
    precioKilo.textContent = `Por kilo sale a ≈ ${soles(porKilo)}`;
    precioKilo.hidden = false;
    mostrarVeredicto(porKilo);
}

/** Muestra el veredicto frente a la referencia; sin referencia no se muestra (UC-01 A3). */
function mostrarVeredicto(porKilo) {
    const caja = document.getElementById('veredicto');
    if (!referencia || !(porKilo > 0)) {
        caja.hidden = true;
        return;
    }
    const redondeado = Math.round(porKilo * 10000) / 10000;
    let tipo = 'dentro';
    if (redondeado < Number(referencia.minimoKg)) {
        tipo = 'bajo';
    } else if (redondeado > Number(referencia.maximoKg)) {
        tipo = 'alto';
    }
    const [titulo, detalle, nombreIcono] = VEREDICTOS[tipo];
    const textoTitulo = document.createElement('span');
    textoTitulo.textContent = titulo;
    document.getElementById('veredicto-titulo').replaceChildren(icono(nombreIcono), textoTitulo);
    document.getElementById('veredicto-detalle').textContent = detalle;
    caja.className = `ac-veredicto ac-veredicto-${tipo}`;
    caja.hidden = false;
}

/** Pide la referencia del cultivo en el distrito elegido y escribe "Hoy en {zona} pagan entre …". */
async function cargarReferencia() {
    const b = leerBorrador();
    const caja = document.getElementById('referencia');
    referencia = null;
    caja.hidden = true;
    if (!b.idCultivo || !b.idDistrito) {
        actualizarPrecioKilo();
        return;
    }
    try {
        referencia = await api.get(`/precios/referencia?idCultivo=${b.idCultivo}&idDistrito=${b.idDistrito}`);
    } catch {
        referencia = null; // La referencia orienta, no bloquea la publicación
    }
    if (referencia) {
        const kilos = kilosPorUnidad(b);
        const unidad = b.unidad.toLowerCase();
        const minimo = Number(referencia.minimoKg);
        const maximo = Number(referencia.maximoKg);
        document.getElementById('referencia-texto').textContent = `Hoy en ${referencia.zona} pagan entre `
            + `${soles(minimo * kilos)} y ${soles(maximo * kilos)} por ${unidad} `
            + `(${soles(minimo)} a ${soles(maximo)} por kilo).`;
        const plataforma = document.getElementById('referencia-plataforma');
        const rango = referencia.rangoPlataforma;
        plataforma.hidden = !rango;
        if (rango) {
            plataforma.textContent = `En AgroConecta se vendió entre ${soles(rango.minimoKg)} y `
                + `${soles(rango.maximoKg)} por kilo (${rango.ventas} ventas en 30 días).`;
        }
        caja.hidden = false;
    }
    actualizarPrecioKilo();
}

function mostrarDistrito(id) {
    const d = distritos.find((x) => x.id === id);
    document.getElementById('distrito-actual').textContent = d ? `${d.nombre}, ${d.provincia}` : '—';
    selectDistrito.value = d ? String(d.id) : '';
    if (d) {
        guardarBorrador({ idDistrito: d.id, distrito: d.nombre, provincia: d.provincia });
        cargarReferencia();
    }
}

async function iniciar() {
    if (!exigirAgricultor()) {
        return;
    }
    const b = leerBorrador();
    if (volverSiFalta(b, [['idVariedad', 'paso1.html'], ['idUnidad', 'paso2.html'], ['cantidad', 'paso2.html'],
        ['fechaCosecha', 'paso2.html']])) {
        return;
    }
    document.getElementById('etiqueta-precio').textContent = `Precio por ${b.unidad.toLowerCase()}`;
    campos.precioUnitario.value = b.precioUnitario ?? '';
    actualizarPrecioKilo();
    try {
        distritos = await api.get('/catalogo/distritos');
        const grupos = new Map();
        for (const d of distritos) {
            if (!grupos.has(d.provincia)) {
                const grupo = document.createElement('optgroup');
                grupo.label = `Provincia de ${d.provincia}`;
                grupos.set(d.provincia, grupo);
                selectDistrito.append(grupo);
            }
            grupos.get(d.provincia).append(new Option(d.nombre, String(d.id)));
        }
        if (b.idDistrito) {
            document.getElementById('origen-distrito').hidden = true;
            mostrarDistrito(b.idDistrito);
        } else {
            const perfil = await api.get('/usuarios/me');
            mostrarDistrito(perfil.idDistrito);
        }
    } catch (e) {
        mostrarAlerta(alerta, e.message, 'error');
    }
}

campos.precioUnitario.addEventListener('input', () => {
    guardarBorrador({ precioUnitario: campos.precioUnitario.value });
    actualizarPrecioKilo();
});

document.getElementById('cambiar').addEventListener('click', () => {
    document.getElementById('caja-distrito').hidden = false;
    selectDistrito.focus();
});

selectDistrito.addEventListener('change', () => {
    document.getElementById('origen-distrito').hidden = true;
    mostrarDistrito(Number(selectDistrito.value));
});

formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    if (!(Number(campos.precioUnitario.value) > 0)) {
        formulario.querySelector('[data-error-de="precioUnitario"]').textContent = 'Escriba el precio.';
        campos.precioUnitario.classList.add('is-invalid');
        campos.precioUnitario.focus();
        return;
    }
    if (!leerBorrador().idDistrito) {
        formulario.querySelector('[data-error-de="idDistrito"]').textContent = 'Elija dónde está su cosecha.';
        return;
    }
    guardarBorrador({ precioUnitario: campos.precioUnitario.value });
    window.location.assign(`${RUTA}/confirmar.html`);
});

iniciar();
