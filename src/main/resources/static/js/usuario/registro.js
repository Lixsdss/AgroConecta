/**
 * Pantalla "Cree su cuenta" (diseño 04, UC-10 pasos 2 a 6).
 * GET /api/v1/catalogo/distritos y POST /api/v1/auth/registro.
 * El distrito se elige con las 4 zonas de precio como atajo y "Otro" para los 65 distritos.
 */
import { api } from '../api.js';
import { guardarSesion } from '../comun/sesion.js';
import {
    limpiarErrores, mostrarErrores, mostrarAlerta, marcarOcupado, conectarMostrarContrasena
} from '../comun/formulario.js';

const ROLES = { AGRICULTOR: 'Cuenta de Agricultor', COMPRADOR: 'Cuenta de Comprador' };
const OTRO = 'otro';

const parametros = new URLSearchParams(window.location.search);
const rol = ROLES[parametros.get('rol')] ? parametros.get('rol') : 'AGRICULTOR';

const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const enviar = document.getElementById('enviar');
const zonas = document.getElementById('zonas');
const cajaOtro = document.getElementById('caja-otro-distrito');
const selectOtro = document.getElementById('otroDistrito');

document.getElementById('tipo-cuenta').textContent = ROLES[rol];
document.querySelectorAll('[data-solo]').forEach((bloque) => {
    bloque.hidden = bloque.dataset.solo !== rol;
});
conectarMostrarContrasena(document.getElementById('mostrar'), formulario.elements.contrasena);

/** Crea un chip de distrito (radio) dentro del grupo de zonas. */
function agregarChip(valor, texto) {
    const id = `zona-${valor}`;
    const radio = document.createElement('input');
    radio.type = 'radio';
    radio.name = 'zona';
    radio.id = id;
    radio.value = String(valor);
    const etiqueta = document.createElement('label');
    etiqueta.htmlFor = id;
    etiqueta.textContent = texto;
    radio.addEventListener('change', () => {
        cajaOtro.hidden = radio.value !== OTRO;
    });
    zonas.append(radio, etiqueta);
}

async function cargarDistritos() {
    try {
        const distritos = await api.get('/catalogo/distritos');
        distritos.filter((d) => d.esZonaPrecio).forEach((d) => agregarChip(d.id, d.nombre));
        agregarChip(OTRO, 'Otro');

        const grupos = new Map();
        for (const d of distritos) {
            if (!grupos.has(d.provincia)) {
                const grupo = document.createElement('optgroup');
                grupo.label = `Provincia de ${d.provincia}`;
                grupos.set(d.provincia, grupo);
                selectOtro.append(grupo);
            }
            grupos.get(d.provincia).append(new Option(d.nombre, String(d.id)));
        }
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    }
}

/** Devuelve el id del distrito elegido, o null. */
function distritoElegido() {
    const zona = formulario.querySelector('input[name="zona"]:checked');
    if (!zona) {
        return null;
    }
    if (zona.value === OTRO) {
        return selectOtro.value ? Number(selectOtro.value) : null;
    }
    return Number(zona.value);
}

function datosDelFormulario() {
    const campos = formulario.elements;
    const datos = {
        rol,
        nombreCompleto: campos.nombreCompleto.value,
        celular: campos.celular.value,
        idDistrito: distritoElegido(),
        contrasena: campos.contrasena.value
    };
    if (rol === 'AGRICULTOR') {
        datos.nombreChacra = campos.nombreChacra.value || null;
    } else {
        datos.nombreNegocio = campos.nombreNegocio.value || null;
        datos.tipoComprador = campos.tipoComprador.value || null;
        datos.ruc = campos.ruc.value || null;
    }
    return datos;
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    marcarOcupado(enviar, true, 'Creando su cuenta…');
    try {
        const sesion = await api.post('/auth/registro', datosDelFormulario());
        guardarSesion(sesion);
        // El agricultor ve la bienvenida; el comprador va directo a su inicio (diseño 04 → 05 / 14)
        window.location.assign(rol === 'AGRICULTOR' ? '/pages/usuario/bienvenida.html' : '/pages/usuario/inicio.html');
    } catch (error) {
        // El error de "datos del comprador" se muestra junto al nombre del negocio
        mostrarErrores(formulario, alerta, error, {
            datosDeCompradorCompletos: 'nombreNegocio',
            rolPermitido: 'nombreCompleto'
        });
        marcarOcupado(enviar, false);
    }
});

cargarDistritos();
