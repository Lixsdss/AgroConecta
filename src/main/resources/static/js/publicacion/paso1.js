/**
 * "Vender · Paso 1" (diseño 07, UC-01): cultivo y variedad (por defecto "Común").
 * GET /api/v1/catalogo/cultivos y GET /api/v1/catalogo/variedades?idCultivo=
 */
import { api } from '../api.js';
import { mostrarAlerta } from '../comun/formulario.js';
import {
    exigirAgricultor, leerBorrador, guardarBorrador, crearOpcion, marcarElegida, RUTA
} from './borrador.js';

const contCultivos = document.getElementById('cultivos');
const contVariedades = document.getElementById('variedades');
const seccionVariedad = document.getElementById('seccion-variedad');
const siguiente = document.getElementById('siguiente');
const alerta = document.getElementById('alerta');

function actualizarBoton() {
    const b = leerBorrador();
    siguiente.disabled = !(b.idCultivo && b.idVariedad);
}

async function elegirCultivo(cultivo, boton, conservarVariedad) {
    marcarElegida(contCultivos, boton);
    const anterior = leerBorrador();
    if (anterior.idCultivo !== cultivo.id) {
        // Otro cultivo: la unidad y el peso del paso 2 ya no aplican
        guardarBorrador({
            idCultivo: cultivo.id, cultivo: cultivo.nombre, idUnidadLocal: cultivo.idUnidadLocal,
            pesoSugeridoKg: cultivo.pesoSugeridoKg, idVariedad: null, variedad: null,
            idUnidad: null, pesoUnidadKg: null
        });
    }
    const variedades = await api.get(`/catalogo/variedades?idCultivo=${cultivo.id}`);
    contVariedades.replaceChildren();
    seccionVariedad.hidden = false;
    const actual = conservarVariedad ? leerBorrador().idVariedad : null;
    const porDefecto = variedades.find((v) => v.id === actual) ?? variedades.find((v) => v.nombre === 'Común');
    for (const v of variedades) {
        const id = `variedad-${v.id}`;
        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'variedad';
        radio.id = id;
        radio.checked = porDefecto?.id === v.id;
        const etiqueta = document.createElement('label');
        etiqueta.htmlFor = id;
        etiqueta.textContent = v.nombre;
        radio.addEventListener('change', () => {
            guardarBorrador({ idVariedad: v.id, variedad: v.nombre });
            actualizarBoton();
        });
        contVariedades.append(radio, etiqueta);
    }
    if (porDefecto) {
        guardarBorrador({ idVariedad: porDefecto.id, variedad: porDefecto.nombre });
    }
    actualizarBoton();
}

async function iniciar() {
    if (!exigirAgricultor()) {
        return;
    }
    try {
        const cultivos = await api.get('/catalogo/cultivos');
        const borrador = leerBorrador();
        for (const c of cultivos) {
            const boton = crearOpcion(c.nombre, `Se vende en ${c.unidadLocal.toLowerCase()}`);
            boton.addEventListener('click', () => elegirCultivo(c, boton, false));
            contCultivos.append(boton);
            if (borrador.idCultivo === c.id) {
                await elegirCultivo(c, boton, true);
            }
        }
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    }
}

siguiente.addEventListener('click', () => window.location.assign(`${RUTA}/paso2.html`));

iniciar();
