/**
 * "Vender · Paso 3" (diseño 09, UC-01): precio por unidad con su equivalente por kilo, y distrito de la cosecha
 * (tomado del perfil, se puede cambiar). GET /api/v1/usuarios/me y GET /api/v1/catalogo/distritos.
 * La referencia de precio y el veredicto de color llegan con RF-02 (slice 4).
 */
import { api } from '../api.js';
import { mostrarAlerta, limpiarErrores } from '../comun/formulario.js';
import { exigirAgricultor, leerBorrador, guardarBorrador, volverSiFalta, soles, RUTA } from './borrador.js';

const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const campos = formulario.elements;
const precioKilo = document.getElementById('precio-kilo');
const selectDistrito = campos.idDistrito;
let distritos = [];

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
    precioKilo.textContent = `Por kilo sale a ≈ ${soles(precio / kilosPorUnidad(b))}`;
    precioKilo.hidden = false;
}

function mostrarDistrito(id) {
    const d = distritos.find((x) => x.id === id);
    document.getElementById('distrito-actual').textContent = d ? `${d.nombre}, ${d.provincia}` : '—';
    selectDistrito.value = d ? String(d.id) : '';
    if (d) {
        guardarBorrador({ idDistrito: d.id, distrito: d.nombre, provincia: d.provincia });
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
