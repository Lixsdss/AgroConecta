/**
 * Pantalla "Mi perfil" (UC-10: editar perfil y cambiar la propia contraseña).
 * GET y PUT /api/v1/usuarios/me, PUT /api/v1/usuarios/me/contrasena, GET /api/v1/catalogo/distritos.
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { mostrarReputacion } from '../venta/reputacion.js';
import { limpiarErrores, mostrarErrores, mostrarAlerta, marcarOcupado } from '../comun/formulario.js';

const ROLES = { AGRICULTOR: 'Agricultor', COMPRADOR: 'Comprador', ADMINISTRADOR: 'Administrador' };

const formDatos = document.getElementById('form-datos');
const alertaDatos = document.getElementById('alerta-datos');
const formContrasena = document.getElementById('form-contrasena');
const alertaContrasena = document.getElementById('alerta-contrasena');
let rol = null;

async function cargarDistritos() {
    const select = formDatos.elements.idDistrito;
    const distritos = await api.get('/catalogo/distritos');
    const grupos = new Map();
    for (const d of distritos) {
        if (!grupos.has(d.provincia)) {
            const grupo = document.createElement('optgroup');
            grupo.label = `Provincia de ${d.provincia}`;
            grupos.set(d.provincia, grupo);
            select.append(grupo);
        }
        grupos.get(d.provincia).append(new Option(d.nombre, String(d.id)));
    }
}

function mostrarPerfil(perfil) {
    rol = perfil.rol;
    document.getElementById('dato-celular').textContent = perfil.celular;
    document.getElementById('dato-rol').textContent = ROLES[perfil.rol] ?? perfil.rol;
    document.querySelectorAll('[data-solo]').forEach((bloque) => {
        bloque.hidden = bloque.dataset.solo !== perfil.rol;
    });
    const campos = formDatos.elements;
    campos.nombreCompleto.value = perfil.nombreCompleto;
    campos.idDistrito.value = String(perfil.idDistrito);
    campos.nombreChacra.value = perfil.nombreChacra ?? '';
    campos.nombreNegocio.value = perfil.nombreNegocio ?? '';
    campos.tipoComprador.value = perfil.tipoComprador ?? '';
    campos.ruc.value = perfil.ruc ?? '';
}

formDatos.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formDatos, alertaDatos);
    const boton = document.getElementById('guardar-datos');
    marcarOcupado(boton, true, 'Guardando…');
    const campos = formDatos.elements;
    const datos = {
        nombreCompleto: campos.nombreCompleto.value,
        idDistrito: Number(campos.idDistrito.value) || null
    };
    if (rol === 'AGRICULTOR') {
        datos.nombreChacra = campos.nombreChacra.value || null;
    } else if (rol === 'COMPRADOR') {
        datos.nombreNegocio = campos.nombreNegocio.value || null;
        datos.tipoComprador = campos.tipoComprador.value || null;
        datos.ruc = campos.ruc.value || null;
    }
    try {
        mostrarPerfil(await api.put('/usuarios/me', datos));
        mostrarAlerta(alertaDatos, 'Sus datos se guardaron.', 'exito');
    } catch (error) {
        mostrarErrores(formDatos, alertaDatos, error);
    } finally {
        marcarOcupado(boton, false);
    }
});

formContrasena.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formContrasena, alertaContrasena);
    const boton = document.getElementById('guardar-contrasena');
    marcarOcupado(boton, true, 'Cambiando…');
    try {
        await api.put('/usuarios/me/contrasena', {
            contrasenaActual: formContrasena.elements.contrasenaActual.value,
            contrasenaNueva: formContrasena.elements.contrasenaNueva.value
        });
        formContrasena.reset();
        mostrarAlerta(alertaContrasena, 'Su contraseña se cambió. Úsela la próxima vez que ingrese.', 'exito');
    } catch (error) {
        mostrarErrores(formContrasena, alertaContrasena, error);
    } finally {
        marcarOcupado(boton, false);
    }
});

async function iniciar() {
    if (!requerirSesion()) {
        return;
    }
    try {
        await cargarDistritos();
        const perfil = await api.get('/usuarios/me');
        mostrarPerfil(perfil);
        if (perfil.rol !== 'ADMINISTRADOR') {
            mostrarReputacion(document.getElementById('reputacion'), perfil.id,
                'Todavía no tiene calificaciones. Las recibe cuando termina una venta.');
        }
    } catch (error) {
        if (error.estado === 401) {
            window.location.replace('/pages/usuario/ingresar.html');
            return;
        }
        mostrarAlerta(alertaDatos, error.message, 'error');
    }
}

iniciar();
