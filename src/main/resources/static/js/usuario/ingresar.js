/**
 * Pantalla "Ingresar" (diseño 02, UC-10: inicio de sesión).
 * POST /api/v1/auth/login → guarda la sesión y lleva al inicio.
 */
import { api } from '../api.js';
import { guardarSesion, obtenerSesion, irAlInicio } from '../comun/sesion.js';
import {
    limpiarErrores, mostrarErrores, mostrarAlerta, marcarOcupado, conectarMostrarContrasena
} from '../comun/formulario.js';

const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const enviar = document.getElementById('enviar');

if (obtenerSesion()) {
    irAlInicio();
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
        irAlInicio();
    } catch (error) {
        mostrarErrores(formulario, alerta, error);
        marcarOcupado(enviar, false);
    }
});
