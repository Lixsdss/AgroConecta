/**
 * "Precios de mercado" (diseño 11b, formato escritorio, RF-02 / UC-02).
 * GET /api/v1/catalogo/cultivos, /catalogo/distritos, /catalogo/unidades, GET /api/v1/precios/vigentes,
 * POST /api/v1/precios, PUT /api/v1/precios/{id} y POST /api/v1/precios/archivo.
 */
import { api } from '../api.js';
import { requerirSesion, cerrarSesion } from '../comun/sesion.js';
import { mostrarAlerta, ocultarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';
import { ponerIconoYTexto } from '../comun/iconos.js';
import { FUENTES, TENDENCIAS, crearChips, soles, textoActualizado } from '../comun/precios.js';

const formulario = document.getElementById('formulario');
const alerta = document.getElementById('alerta');
const guardar = document.getElementById('guardar');
const editarExistente = document.getElementById('editar-existente');
const cancelar = document.getElementById('cancelar');
const formArchivo = document.getElementById('form-archivo');
const alertaArchivo = document.getElementById('alerta-archivo');
const rechazadas = document.getElementById('rechazadas');
const campos = formulario.elements;

const FUENTE_INICIAL = 'MERCADO_MAYORISTA_PIURA';
const eleccion = { idCultivo: null, idDistrito: null, fuente: FUENTE_INICIAL };
let cultivos = [];
let zonas = [];
let unidades = [];
let vigentes = [];
/** Precio que se está editando (PUT), o null si el formulario crea uno nuevo (POST). */
let enEdicion = null;
/** Precio ya cargado que choca con el formulario (409), para ofrecer "Editar ese precio". */
let existente = null;

function hoy() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Kilos de la unidad local del cultivo: factor fijo de la unidad o peso sugerido (jaba, saco). */
function kilosUnidadLocal(cultivo) {
    const unidad = unidades.find((u) => u.id === cultivo.idUnidadLocal);
    return Number(unidad?.factorKg ?? cultivo.pesoSugeridoKg ?? 0);
}

function actualizarEquivalente() {
    const destino = document.getElementById('equivalente');
    const cultivo = cultivos.find((c) => String(c.id) === String(eleccion.idCultivo));
    const precio = Number(campos.precioKg.value);
    const kilos = cultivo ? kilosUnidadLocal(cultivo) : 0;
    destino.textContent = precio > 0 && kilos > 0
        ? `≈ ${soles(precio * kilos)} por ${cultivo.unidadLocal.toLowerCase()} de ${kilos} kg`
        : '';
}

function dibujarChips() {
    crearChips(document.getElementById('cultivos'), 'idCultivo',
        cultivos.map((c) => ({ valor: c.id, texto: c.nombre })), eleccion.idCultivo,
        (valor) => { eleccion.idCultivo = valor; actualizarEquivalente(); });
    crearChips(document.getElementById('zonas'), 'idDistrito',
        zonas.map((z) => ({ valor: z.id, texto: z.nombre })), eleccion.idDistrito,
        (valor) => { eleccion.idDistrito = valor; });
    crearChips(document.getElementById('fuentes'), 'fuente',
        Object.entries(FUENTES).map(([valor, texto]) => ({ valor, texto })), eleccion.fuente,
        (valor) => { eleccion.fuente = valor; });
    // En edición solo cambian el precio y la fuente (PUT /precios/{id})
    formulario.querySelectorAll('input[name="idCultivo"], input[name="idDistrito"]').forEach((radio) => {
        radio.disabled = enEdicion !== null;
    });
    campos.fecha.disabled = enEdicion !== null;
}

function modoNuevo() {
    enEdicion = null;
    existente = null;
    editarExistente.hidden = true;
    cancelar.hidden = true;
    document.getElementById('titulo-form').textContent = 'Cargar precio del día';
    campos.precioKg.value = '';
    campos.fecha.value = hoy();
    dibujarChips();
    actualizarEquivalente();
}

function modoEdicion(p) {
    enEdicion = p;
    existente = null;
    editarExistente.hidden = true;
    cancelar.hidden = false;
    eleccion.idCultivo = p.idCultivo;
    eleccion.idDistrito = p.idDistrito;
    eleccion.fuente = p.fuente;
    document.getElementById('titulo-form').textContent = `Editar precio de ${p.cultivo} en ${p.zona}`;
    campos.precioKg.value = p.precioKg;
    campos.fecha.value = p.fecha;
    limpiarErrores(formulario, alerta);
    dibujarChips();
    actualizarEquivalente();
    campos.precioKg.focus();
}

function dibujarTabla() {
    const cuerpo = document.getElementById('tabla');
    cuerpo.replaceChildren();
    for (const p of vigentes) {
        const fila = cuerpo.insertRow();
        fila.insertCell().textContent = p.cultivo;
        fila.insertCell().textContent = p.zona;
        fila.insertCell().textContent = soles(p.precioKg);
        fila.insertCell().textContent = p.precioUnidadLocal === null
            ? '—' : `${soles(p.precioUnidadLocal)} por ${p.unidadLocal.toLowerCase()}`;
        const [textoTendencia, iconoTendencia] = TENDENCIAS[p.tendencia] ?? TENDENCIAS.SIN_DATO;
        const tendencia = document.createElement('span');
        tendencia.className = `ac-tendencia ac-tendencia-${p.tendencia.toLowerCase()}`;
        ponerIconoYTexto(tendencia, iconoTendencia, textoTendencia);
        fila.insertCell().append(tendencia);
        fila.insertCell().textContent = textoActualizado(p.actualizado);
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'btn btn-link';
        boton.setAttribute('aria-label', `Editar precio de ${p.cultivo} en ${p.zona}`);
        ponerIconoYTexto(boton, 'sliders-horizontal', 'Editar');
        boton.addEventListener('click', () => modoEdicion(p));
        fila.insertCell().append(boton);
    }
    if (vigentes.length === 0) {
        const celda = cuerpo.insertRow().insertCell();
        celda.colSpan = 7;
        celda.className = 'ac-suave';
        celda.textContent = 'Todavía no hay precios cargados.';
    }
    const cargadosHoy = vigentes.filter((p) => p.fecha === hoy()).length;
    document.getElementById('avance').textContent =
        `${cargadosHoy} de ${cultivos.length * zonas.length} cargados hoy`;
}

async function cargarVigentes() {
    vigentes = await api.get('/precios/vigentes');
    dibujarTabla();
}

formulario.addEventListener('input', (evento) => {
    if (evento.target === campos.precioKg) {
        actualizarEquivalente();
    }
});

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario, alerta);
    editarExistente.hidden = true;
    const faltan = [['idCultivo', 'Elija el cultivo.'], ['idDistrito', 'Elija la zona.']]
        .filter(([campo]) => !eleccion[campo]);
    if (!(Number(campos.precioKg.value) > 0)) {
        faltan.push(['precioKg', 'Escriba el precio por kilo.']);
    }
    if (faltan.length > 0) {
        faltan.forEach(([campo, mensaje]) => {
            formulario.querySelector(`[data-error-de="${campo}"]`).textContent = mensaje;
        });
        return;
    }
    marcarOcupado(guardar, true, 'Guardando…');
    try {
        if (enEdicion) {
            await api.put(`/precios/${enEdicion.idPrecio}`, { precioKg: campos.precioKg.value, fuente: eleccion.fuente });
            mostrarAlerta(alerta, `Precio de ${enEdicion.cultivo} en ${enEdicion.zona} actualizado.`, 'exito');
        } else {
            const creado = await api.post('/precios', {
                idCultivo: Number(eleccion.idCultivo), idDistrito: Number(eleccion.idDistrito),
                fecha: campos.fecha.value || null, precioKg: campos.precioKg.value, fuente: eleccion.fuente
            });
            mostrarAlerta(alerta, `Precio de ${creado.cultivo} en ${creado.zona} guardado.`, 'exito');
        }
        const zonaElegida = eleccion.idDistrito;
        eleccion.idCultivo = null;
        eleccion.idDistrito = zonaElegida; // El administrador suele cargar varios cultivos de la misma zona
        modoNuevo();
        await cargarVigentes();
    } catch (error) {
        mostrarErrores(formulario, alerta, error);
        if (error.estado === 409) {
            existente = vigentes.find((p) => String(p.idCultivo) === String(eleccion.idCultivo)
                && String(p.idDistrito) === String(eleccion.idDistrito) && p.fecha === campos.fecha.value) ?? null;
            editarExistente.hidden = existente === null;
        }
    } finally {
        marcarOcupado(guardar, false);
    }
});

editarExistente.addEventListener('click', () => {
    if (existente) {
        modoEdicion(existente);
    }
});

cancelar.addEventListener('click', () => {
    limpiarErrores(formulario, alerta);
    modoNuevo();
});

formArchivo.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formArchivo, alertaArchivo);
    rechazadas.hidden = true;
    const archivo = formArchivo.elements.archivo.files[0];
    if (!archivo) {
        formArchivo.querySelector('[data-error-de="archivo"]').textContent = 'Elija un archivo.';
        return;
    }
    const datos = new FormData();
    datos.append('archivo', archivo);
    datos.append('fuente', formArchivo.elements.fuente.value);
    const subir = document.getElementById('subir');
    marcarOcupado(subir, true, 'Subiendo…');
    try {
        const informe = await api.post('/precios/archivo', datos);
        const rechazos = informe.rechazadas;
        mostrarAlerta(alertaArchivo, rechazos.length === 0
            ? `Se guardaron ${informe.importadas} precios.`
            : `Se guardaron ${informe.importadas} precios. ${rechazos.length} filas no se guardaron:`,
        rechazos.length === 0 ? 'exito' : 'info');
        rechazadas.replaceChildren(...rechazos.map((r) => {
            const item = document.createElement('li');
            item.textContent = `Fila ${r.fila}: ${r.motivo}`;
            return item;
        }));
        rechazadas.hidden = rechazos.length === 0;
        formArchivo.reset();
        await cargarVigentes();
    } catch (error) {
        mostrarErrores(formArchivo, alertaArchivo, error);
    } finally {
        marcarOcupado(subir, false);
    }
});

document.getElementById('salir').addEventListener('click', () => {
    cerrarSesion();
    window.location.assign('/pages/usuario/ingresar.html');
});

async function iniciar() {
    const sesion = requerirSesion();
    if (!sesion) {
        return;
    }
    if (sesion.usuario.rol !== 'ADMINISTRADOR') {
        window.location.replace('/pages/usuario/inicio.html');
        return;
    }
    const nombre = sesion.usuario.nombreCompleto;
    document.getElementById('nombre-admin').textContent = nombre;
    document.getElementById('iniciales').textContent =
        nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');

    const selectFuente = formArchivo.elements.fuente;
    Object.entries(FUENTES).forEach(([valor, texto]) => selectFuente.append(new Option(texto, valor)));
    campos.fecha.max = hoy();
    try {
        [cultivos, zonas, unidades] = await Promise.all([
            api.get('/catalogo/cultivos'), api.get('/catalogo/distritos'), api.get('/catalogo/unidades')]);
        zonas = zonas.filter((d) => d.esZonaPrecio);
        modoNuevo();
        await cargarVigentes();
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    }
}

iniciar();
