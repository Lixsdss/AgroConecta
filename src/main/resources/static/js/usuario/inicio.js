/**
 * Inicio del agricultor (diseño 06) y del comprador (diseño 14): saludo según el rol y precios de hoy (RF-02).
 * GET /api/v1/usuarios/me confirma que el token sigue válido y que el usuario está ACTIVO; GET
 * /api/v1/precios/vigentes da los precios de la zona del usuario, o de todas las zonas si su distrito no es zona.
 * GET /api/v1/conversaciones/no-leidos muestra los mensajes sin leer (cada 30 segundos, RF-04).
 */
import { api } from '../api.js';
import { requerirSesion, cerrarSesion } from '../comun/sesion.js';
import { mostrarAlerta } from '../comun/formulario.js';
import { mostrarTarjetas } from '../comun/precios.js';

const ROLES = {
    AGRICULTOR: 'Cuenta de Agricultor',
    COMPRADOR: 'Cuenta de Comprador',
    ADMINISTRADOR: 'Cuenta de Administrador'
};

const sesion = requerirSesion();

// El administrador trabaja en su panel de escritorio (diseño 11a–11e)
if (sesion?.usuario.rol === 'ADMINISTRADOR') {
    window.location.replace('/pages/administracion/moderacion.html');
}

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
        document.getElementById('texto-ventas').textContent = perfil.rol === 'COMPRADOR' ? 'Mis compras' : 'Mis ventas';
        // "Buscar productos" es la acción principal del comprador (diseño 14); el agricultor también puede buscar
        document.getElementById('buscar').classList.add(perfil.rol === 'COMPRADOR' ? 'btn-primary' : 'btn-outline-primary');
        contarNoLeidos();
        setInterval(() => {
            if (!document.hidden) {
                contarNoLeidos();
            }
        }, 30_000);
        await cargarPrecios(perfil);
    } catch (error) {
        if (error.estado === 401) {
            window.location.replace('/pages/usuario/ingresar.html');
            return;
        }
        mostrarAlerta(document.getElementById('alerta'), error.message, 'error');
    }
}

async function contarNoLeidos() {
    try {
        const { total } = await api.get('/conversaciones/no-leidos');
        const insignia = document.getElementById('no-leidos');
        insignia.textContent = String(total);
        insignia.setAttribute('aria-label', `${total} sin leer`);
        insignia.hidden = total === 0;
    } catch {
        // El aviso es opcional: se reintenta en la próxima consulta
    }
}

async function cargarPrecios(perfil) {
    let precios = await api.get(`/precios/vigentes?idDistrito=${perfil.idDistrito}`);
    if (precios.length > 0) {
        document.getElementById('titulo-precios').textContent = `Precios de hoy en ${perfil.distrito}`;
    } else {
        precios = await api.get('/precios/vigentes');
    }
    mostrarTarjetas(document.getElementById('precios'), precios);
}

if (sesion) {
    cargar();
}
