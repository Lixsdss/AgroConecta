/**
 * "Usuarios" del administrador (diseño 11e, formato escritorio, RF-08 y RF-09 / UC-11 parte 1): lista con filtros,
 * detalle, suspender y reactivar, contraseña temporal, desactivar y activar, y crear otro administrador.
 * GET /api/v1/admin/usuarios, GET /api/v1/admin/usuarios/{id}, POST /api/v1/admin/usuarios/{id}/suspension,
 * /reactivacion, /contrasena-temporal, /desactivacion, /activacion, POST /api/v1/admin/administradores y
 * GET /api/v1/catalogo/distritos.
 */
import { api } from '../api.js';
import { mostrarAlerta, ocultarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';
import { iniciarAdministracion, irAIngresarSi401, crear, fechaHora, numero } from './comun.js';

const ROLES = { '': 'Todos', AGRICULTOR: 'Agricultores', COMPRADOR: 'Compradores', ADMINISTRADOR: 'Administradores' };
const ROL_SINGULAR = { AGRICULTOR: 'Agricultor', COMPRADOR: 'Comprador', ADMINISTRADOR: 'Administrador' };
const ESTADOS = { ACTIVO: 'Activo', SUSPENDIDO: 'Suspendido', INACTIVO: 'Desactivado' };
const TIPOS_COMPRADOR = {
    MERCADO_MAYORISTA: 'Mercado mayorista', RESTAURANTE: 'Restaurante', HOTEL: 'Hotel', PROCESADORA: 'Procesadora',
    MOLINO: 'Molino', OTRO: 'Otro'
};
/** Motivos de suspensión (chk_suspension_motivo). */
const MOTIVOS = [
    ['RECLAMOS_FUNDADOS', 'Reclamos fundados (3 o más)'],
    ['DATOS_FALSOS', 'Datos falsos'],
    ['PUBLICACIONES_ENGANOSAS', 'Publicaciones engañosas'],
    ['USO_INDEBIDO', 'Uso indebido de la plataforma']
];
const ESTADOS_VENTA = {
    PROPUESTA: 'propuesta', ACORDADA: 'acordada', RECOGIDO: 'recogida', EN_CAMINO: 'en camino', ENTREGADO: 'entregada'
};

const aviso = document.getElementById('aviso');
const filtros = document.getElementById('filtros');
const filas = document.getElementById('filas');
const detalle = document.getElementById('detalle');
const dialogoSuspender = document.getElementById('dialogo-suspender');
const formSuspender = document.getElementById('form-suspender');
const dialogoContrasena = document.getElementById('dialogo-contrasena');
const dialogoAdmin = document.getElementById('dialogo-admin');
const formAdmin = document.getElementById('form-admin');

const consulta = { rol: '', estado: '', q: '', page: 0 };
let elegido = null;

function error(e, alerta = aviso) {
    if (!irAIngresarSi401(e)) {
        mostrarAlerta(alerta, e.message, 'error');
    }
}

function estado(valor) {
    return crear('span', `ac-estado ac-estado-${valor}`, ESTADOS[valor] ?? valor);
}

function subtitulo(u) {
    if (u.rol === 'COMPRADOR') {
        return `${u.negocio ?? ''} · ${TIPOS_COMPRADOR[u.tipoComprador] ?? ''}`;
    }
    return u.chacra ? `Chacra ${u.chacra}` : '';
}

// ---------- Lista ----------

function dibujarRoles() {
    const contenedor = document.getElementById('roles');
    contenedor.replaceChildren();
    for (const [valor, texto] of Object.entries(ROLES)) {
        const id = `rol-${valor || 'TODOS'}`;
        const radio = crear('input');
        radio.type = 'radio';
        radio.name = 'rol';
        radio.id = id;
        radio.value = valor;
        radio.checked = valor === consulta.rol;
        radio.addEventListener('change', () => {
            consulta.rol = valor;
            consulta.page = 0;
            cargar();
        });
        const etiqueta = crear('label', '', texto);
        etiqueta.htmlFor = id;
        contenedor.append(radio, etiqueta);
    }
}

async function cargar() {
    const parametros = new URLSearchParams({ page: String(consulta.page) });
    for (const clave of ['rol', 'estado', 'q']) {
        if (consulta[clave]) {
            parametros.set(clave, consulta[clave]);
        }
    }
    try {
        const pagina = await api.get(`/admin/usuarios?${parametros}`);
        mostrarLista(pagina);
    } catch (e) {
        error(e);
    }
}

function mostrarLista(pagina) {
    const { totalElements, totalPages, number } = pagina.page;
    document.getElementById('total').textContent =
        totalElements === 1 ? '1 usuario' : `${numero(totalElements, 0)} usuarios`;
    document.getElementById('paginas').classList.toggle('d-none', totalPages <= 1);
    document.getElementById('pagina').textContent = `Página ${number + 1} de ${Math.max(totalPages, 1)}`;
    document.getElementById('anterior').disabled = number === 0;
    document.getElementById('siguiente').disabled = number + 1 >= totalPages;
    if (pagina.content.length === 0) {
        const fila = crear('tr');
        const celda = crear('td', 'ac-suave', 'No hay usuarios con esos filtros.');
        celda.colSpan = 7;
        fila.append(celda);
        filas.replaceChildren(fila);
        return;
    }
    filas.replaceChildren(...pagina.content.map((u) => {
        const fila = crear('tr');
        fila.dataset.id = String(u.id);
        if (elegido?.id === u.id) {
            fila.setAttribute('aria-current', 'true');
        }
        // El nombre abre el detalle (sigue visible aunque la tabla se desplace con el detalle abierto)
        const nombre = crear('td');
        const ver = crear('button', 'btn btn-link p-0 text-start fw-bold', u.nombre);
        ver.type = 'button';
        ver.addEventListener('click', () => abrir(u.id));
        nombre.append(ver, crear('span', 'd-block ac-suave', subtitulo(u)));
        const estadoCelda = crear('td');
        estadoCelda.append(estado(u.estado));
        fila.append(nombre, estadoCelda, crear('td', '', ROL_SINGULAR[u.rol]), crear('td', '', u.distrito),
            crear('td', '', u.celular),
            crear('td', '', u.totalCalificaciones > 0 ? `★ ${numero(u.calificacion, 1)} (${u.totalCalificaciones})` : '—'),
            crear('td', 'text-end', numero(u.operacionesCompletadas, 0)));
        return fila;
    }));
}

// ---------- Detalle ----------

async function abrir(id) {
    try {
        mostrarDetalle(await api.get(`/admin/usuarios/${id}`));
    } catch (e) {
        error(e);
    }
}

function dato(dl, etiqueta, valor, extra) {
    const fila = crear('div', 'ac-dato');
    const dd = crear('dd', '', valor);
    if (extra) {
        dd.append(crear('div', 'ac-suave fw-normal', extra));
    }
    fila.append(crear('dt', '', etiqueta), dd);
    dl.append(fila);
}

function boton(nombreIcono, texto, clase, accion) {
    const b = crear('button', `btn ${clase} w-100 mb-2`);
    b.type = 'button';
    ponerIconoYTexto(b, nombreIcono, texto);
    b.addEventListener('click', () => accion(b));
    return b;
}

function textoVentas(ventas) {
    return ventas.length === 1 ? '1 venta en curso' : `${ventas.length} ventas en curso`;
}

function mostrarDetalle(u) {
    elegido = u;
    filas.querySelectorAll('tr').forEach((f) => {
        if (f.dataset.id === String(u.id)) {
            f.setAttribute('aria-current', 'true');
        } else {
            f.removeAttribute('aria-current');
        }
    });
    detalle.replaceChildren();
    const cabecera = crear('div', 'd-flex justify-content-between align-items-start gap-2');
    cabecera.append(crear('h2', 'h4 fw-semibold mb-1', u.nombre), estado(u.estado));
    detalle.append(cabecera, crear('p', 'ac-suave', `${ROL_SINGULAR[u.rol]}${subtitulo(u) ? ` · ${subtitulo(u)}` : ''}`));

    if (u.suspension) {
        const caja = crear('div', 'ac-alerta ac-alerta-error');
        const texto = crear('div');
        const motivo = MOTIVOS.find(([clave]) => clave === u.suspension.motivo)?.[1] ?? u.suspension.motivo;
        texto.append(crear('strong', 'd-block', `Suspendido: ${motivo}`), crear('span', '', u.suspension.detalle),
            crear('div', 'ac-suave', `${fechaHora(u.suspension.fechaInicio)} · ${u.suspension.administrador ?? ''}`));
        caja.append(icono('ban'), texto);
        detalle.append(caja);
    }

    const dl = crear('dl', 'mb-3');
    dato(dl, 'Celular', u.celular, u.distrito);
    dato(dl, 'Registrado', fechaHora(u.fechaRegistro), `Último ingreso: ${fechaHora(u.ultimoAcceso)}`);
    if (u.rol !== 'ADMINISTRADOR') {
        dato(dl, 'Ventas terminadas', numero(u.operacionesCompletadas, 0),
            u.ventasEnCurso.length > 0 ? textoVentas(u.ventasEnCurso) : 'Sin ventas en curso');
        dato(dl, 'Calificación', u.totalCalificaciones > 0 ? `★ ${numero(u.calificacion, 1)}` : 'Sin calificaciones',
            u.totalCalificaciones === 1 ? '1 calificación'
                : u.totalCalificaciones > 1 ? `${u.totalCalificaciones} calificaciones` : null);
        dato(dl, 'Reclamos fundados en su contra', numero(u.reclamosFundados, 0));
    }
    detalle.append(dl);

    if (u.rol === 'ADMINISTRADOR') {
        detalle.append(crear('p', 'ac-suave mb-0', 'Las cuentas de administrador no se suspenden ni se desactivan desde aquí.'));
    } else {
        if (u.estado === 'ACTIVO') {
            detalle.append(boton('ban', 'Suspender', 'btn-outline-primary', abrirSuspender));
        } else if (u.estado === 'SUSPENDIDO') {
            detalle.append(boton('refresh-cw', 'Reactivar', 'btn-primary', (b) => accion(b, 'reactivacion',
                `Cuenta de ${u.nombre} reactivada. Se le avisó.`)));
        }
        detalle.append(boton('settings', 'Contraseña temporal', 'btn-outline-primary', abrirContrasena));
        if (u.estado === 'ACTIVO') {
            detalle.append(boton('eye-off', 'Desactivar cuenta', 'btn-outline-primary', (b) => {
                if (window.confirm(`¿Desactivar la cuenta de ${u.nombre}? No podrá ingresar hasta que la vuelva a activar.`)) {
                    accion(b, 'desactivacion', `Cuenta de ${u.nombre} desactivada.`);
                }
            }));
        } else if (u.estado === 'INACTIVO') {
            detalle.append(boton('eye', 'Volver a activar', 'btn-primary', (b) => accion(b, 'activacion',
                `Cuenta de ${u.nombre} activada de nuevo.`)));
        }
    }
    detalle.hidden = false;
}

async function accion(b, ruta, exito) {
    marcarOcupado(b, true, 'Guardando…');
    try {
        const usuario = await api.post(`/admin/usuarios/${elegido.id}/${ruta}`);
        mostrarAlerta(aviso, exito, 'exito');
        mostrarDetalle(usuario);
        await cargar();
    } catch (e) {
        marcarOcupado(b, false);
        error(e);
    }
}

// ---------- Suspender (UC-11 parte 1, A1 y A3) ----------

function abrirSuspender() {
    limpiarErrores(formSuspender, document.getElementById('alerta-suspender'));
    document.getElementById('sub-suspender').textContent = `${elegido.nombre} · ${ROL_SINGULAR[elegido.rol]}`;
    const contenedor = document.getElementById('motivos-suspension');
    contenedor.replaceChildren(...MOTIVOS.map(([clave, texto]) => {
        const etiqueta = crear('label');
        const radio = crear('input');
        radio.type = 'radio';
        radio.name = 'motivo';
        radio.value = clave;
        const ayuda = clave === 'RECLAMOS_FUNDADOS' ? ` · tiene ${elegido.reclamosFundados}` : '';
        etiqueta.append(radio, crear('span', '', texto + ayuda));
        return etiqueta;
    }));
    formSuspender.elements.detalle.value = '';
    const ventas = document.getElementById('ventas-en-curso');
    const enCurso = elegido.ventasEnCurso;
    ventas.hidden = enCurso.length === 0;
    if (enCurso.length > 0) {
        const lista = crear('ul', 'mb-0 ps-3');
        lista.append(...enCurso.map((v) => crear('li', '', `${v.producto} · ${ESTADOS_VENTA[v.estado] ?? v.estado}`)));
        const texto = crear('div');
        texto.append(crear('strong', 'd-block', `Tiene ${textoVentas(enCurso)}.`),
            crear('span', '', 'Las ventas no cambian solas. Se avisará a la otra parte de las ventas acordadas o propuestas para que pueda cancelarlas si lo necesita.'),
            lista);
        ventas.replaceChildren(icono('triangle-alert'), texto);
    }
    document.getElementById('confirmar-suspender').disabled = false;
    dialogoSuspender.showModal();
}

formSuspender.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const alerta = document.getElementById('alerta-suspender');
    limpiarErrores(formSuspender, alerta);
    const confirmar = document.getElementById('confirmar-suspender');
    marcarOcupado(confirmar, true, 'Suspendiendo…');
    try {
        const usuario = await api.post(`/admin/usuarios/${elegido.id}/suspension`, {
            motivo: formSuspender.querySelector('input[name="motivo"]:checked')?.value ?? null,
            detalle: formSuspender.elements.detalle.value
        });
        dialogoSuspender.close();
        mostrarAlerta(aviso, `Cuenta de ${usuario.nombre} suspendida. Ya no puede ingresar y se le avisó.`, 'exito');
        mostrarDetalle(usuario);
        await cargar();
    } catch (e) {
        if (!irAIngresarSi401(e)) {
            mostrarErrores(formSuspender, alerta, e);
        }
    } finally {
        marcarOcupado(confirmar, false);
    }
});

// ---------- Contraseña temporal ----------

function abrirContrasena() {
    document.getElementById('paso-confirmar').hidden = false;
    document.getElementById('paso-mostrar').hidden = true;
    document.getElementById('contrasena-temporal').textContent = '';
    ocultarAlerta(document.getElementById('alerta-contrasena'));
    document.getElementById('texto-contrasena').textContent =
        `Se creará una contraseña nueva para ${elegido.nombre}. La actual dejará de funcionar.`;
    dialogoContrasena.showModal();
}

document.getElementById('generar-contrasena').addEventListener('click', async (evento) => {
    const b = evento.currentTarget;
    marcarOcupado(b, true, 'Generando…');
    try {
        const respuesta = await api.post(`/admin/usuarios/${elegido.id}/contrasena-temporal`);
        document.getElementById('contrasena-temporal').textContent = respuesta.contrasenaTemporal;
        document.getElementById('paso-confirmar').hidden = true;
        document.getElementById('paso-mostrar').hidden = false;
    } catch (e) {
        error(e, document.getElementById('alerta-contrasena'));
    } finally {
        marcarOcupado(b, false);
    }
});

// La contraseña temporal se borra de la pantalla al cerrar el diálogo
dialogoContrasena.addEventListener('close', () => {
    document.getElementById('contrasena-temporal').textContent = '';
});

// ---------- Nuevo administrador (UC-10 A5) ----------

async function abrirNuevoAdmin() {
    limpiarErrores(formAdmin, document.getElementById('alerta-admin'));
    formAdmin.reset();
    const select = formAdmin.elements.idDistrito;
    if (select.options.length === 0) {
        try {
            const distritos = await api.get('/catalogo/distritos');
            select.append(...distritos.map((d) => {
                const opcion = crear('option', '', d.nombre);
                opcion.value = String(d.id);
                return opcion;
            }));
        } catch (e) {
            error(e);
            return;
        }
    }
    const piura = [...select.options].find((o) => o.textContent === 'Piura');
    if (piura) {
        select.value = piura.value;
    }
    dialogoAdmin.showModal();
}

formAdmin.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const alerta = document.getElementById('alerta-admin');
    limpiarErrores(formAdmin, alerta);
    const crearBoton = document.getElementById('crear-admin');
    marcarOcupado(crearBoton, true, 'Creando…');
    try {
        const admin = await api.post('/admin/administradores', {
            nombreCompleto: formAdmin.elements.nombreCompleto.value,
            celular: formAdmin.elements.celular.value,
            idDistrito: Number(formAdmin.elements.idDistrito.value) || null,
            contrasena: formAdmin.elements.contrasena.value
        });
        dialogoAdmin.close();
        formAdmin.reset();
        mostrarAlerta(aviso, `Administrador ${admin.nombre} creado. Ya puede ingresar con su celular.`, 'exito');
        mostrarDetalle(admin);
        await cargar();
    } catch (e) {
        if (!irAIngresarSi401(e)) {
            mostrarErrores(formAdmin, alerta, e);
        }
    } finally {
        marcarOcupado(crearBoton, false);
    }
});

// ---------- Inicio ----------

document.querySelectorAll('[data-cerrar]').forEach((b) => b.addEventListener('click', () => b.closest('dialog').close()));
document.getElementById('nuevo-admin').addEventListener('click', abrirNuevoAdmin);
document.getElementById('anterior').addEventListener('click', () => { consulta.page -= 1; cargar(); });
document.getElementById('siguiente').addEventListener('click', () => { consulta.page += 1; cargar(); });
document.getElementById('estado').addEventListener('change', (e) => { consulta.estado = e.target.value; consulta.page = 0; cargar(); });
filtros.addEventListener('submit', (evento) => {
    evento.preventDefault();
    ocultarAlerta(aviso);
    consulta.q = filtros.elements.q.value.trim();
    consulta.page = 0;
    cargar();
});

if (iniciarAdministracion()) {
    dibujarRoles();
    cargar();
}
