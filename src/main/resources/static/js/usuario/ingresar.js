/**
 * Pantalla "Ingresar" (diseño 02, UC-10: inicio de sesión).
 * POST /api/v1/auth/login → guarda la sesión y lleva al inicio, o a la página de "volver" (por ejemplo, la cosecha
 * que el visitante quería contactar).
 */
import { api } from '../api.js';
import { guardarSesion, obtenerSesion, irAlInicio, rutaDeVuelta } from '../comun/sesion.js';
import {
    limpiarErrores, mostrarErrores, mostrarAlerta, marcarOcupado, conectarMostrarContrasena
} from '../comun/formulario.js';

const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const enviar = document.getElementById('enviar');
const volver = rutaDeVuelta(new URLSearchParams(window.location.search).get('volver'));

function continuar() {
    if (volver) {
        window.location.assign(volver);
    } else {
        irAlInicio();
    }
}

if (obtenerSesion()) {
    continuar();
}

conectarMostrarContrasena(document.getElementById('mostrar'), formulario.elements.contrasena);

document.getElementById('olvido').addEventListener('click', () => {
    mostrarAlerta(alerta, 'Comuníquese con el administrador para reiniciar su contraseña.', 'info');
});

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    marcarOcupado(enviar, true, 'Ingresando…');
    try {
        const sesion = await api.post('/auth/login', {
            celular: formulario.elements.celular.value,
            contrasena: formulario.elements.contrasena.value
        });
        guardarSesion(sesion);
        continuar();
    } catch (error) {
        mostrarErrores(formulario, alerta, error);
        marcarOcupado(enviar, false);
    }
});
