/**
 * Inicio mínimo del walking skeleton: saluda al usuario de la sesión según su rol.
 * GET /api/v1/usuarios/me confirma que el token sigue válido y que el usuario está ACTIVO.
 */
import { api } from '../api.js';
import { requerirSesion, cerrarSesion } from '../comun/sesion.js';
import { mostrarAlerta } from '../comun/formulario.js';

const ROLES = {
    AGRICULTOR: 'Cuenta de Agricultor',
    COMPRADOR: 'Cuenta de Comprador',
    ADMINISTRADOR: 'Cuenta de Administrador'
};

const sesion = requerirSesion();

document.getElementById('salir').addEventListener('click', () => {
    cerrarSesion();
    window.location.assign('/pages/usuario/ingresar.html');
});

async function cargar() {
    try {
        const perfil = await api.get('/usuarios/me');
        const nombre = perfil.nombreCompleto.split(' ')[0];
        document.getElementById('saludo').textContent = `Hola, ${nombre}`;
        document.getElementById('rol').textContent = `${ROLES[perfil.rol] ?? ''} · ${perfil.distrito}`;
        document.getElementById('vender').hidden = perfil.rol !== 'AGRICULTOR';
    } catch (error) {
        if (error.estado === 401) {
            window.location.replace('/pages/usuario/ingresar.html');
            return;
        }
        mostrarAlerta(document.getElementById('alerta'), error.message, 'error');
    }
}

if (sesion) {
    cargar();
}
