/**
 * Inicio público (diseño 01, RF-02 / UC-02): precios vigentes por cultivo y zona (ambos opcionales) y, con un
 * cultivo y una zona elegidos, la serie de 30 días.
 * GET /api/v1/catalogo/cultivos, /catalogo/distritos, /precios/vigentes y /precios/historial (públicos).
 */
import { api } from './api.js';
import { mostrarAlerta } from './comun/formulario.js';
import { crearChips, mostrarTarjetas, soles, fechaCorta } from './comun/precios.js';
import { dibujarLinea } from './comun/grafico.js';

const TODOS = 'todos';
const alerta = document.getElementById('alerta');
const tarjetas = document.getElementById('tarjetas');
const grafico = document.getElementById('grafico');

const filtro = { cultivo: TODOS, zona: null };
let cultivos = [];
let zonas = [];

/** Texto cuando no hay precios (UC-02 A1 y A3). */
function mensajeVacio() {
    if (filtro.zona !== TODOS) {
        return 'Sin resultados en esta zona. Pruebe con «Todas».';
    }
    return filtro.cultivo === TODOS
        ? 'Todavía no hay precios cargados.'
        : 'No hay precios de referencia para este cultivo.';
}

async function mostrarPrecios() {
    const parametros = new URLSearchParams();
    if (filtro.cultivo !== TODOS) {
        parametros.set('idCultivo', filtro.cultivo);
    }
    if (filtro.zona !== TODOS) {
        parametros.set('idDistrito', filtro.zona);
    }
    try {
        const precios = await api.get(`/precios/vigentes?${parametros}`);
        mostrarTarjetas(tarjetas, precios, mensajeVacio());
        // La serie de 30 días es de un cultivo en una zona
        grafico.hidden = filtro.cultivo === TODOS || filtro.zona === TODOS || precios.length === 0;
        if (!grafico.hidden) {
            const serie = await api.get(`/precios/historial?idCultivo=${filtro.cultivo}&idDistrito=${filtro.zona}&dias=30`);
            const cultivo = cultivos.find((c) => String(c.id) === filtro.cultivo)?.nombre ?? '';
            const zona = zonas.find((z) => String(z.id) === filtro.zona)?.nombre ?? '';
            dibujarLinea(grafico, serie, {
                titulo: `Precio por kilo de ${cultivo} en ${zona}, últimos 30 días`,
                formatoValor: soles,
                formatoFecha: fechaCorta
            });
        }
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
    }
}

async function iniciar() {
    try {
        [cultivos, zonas] = await Promise.all([api.get('/catalogo/cultivos'), api.get('/catalogo/distritos')]);
    } catch (error) {
        mostrarAlerta(alerta, error.message, 'error');
        return;
    }
    zonas = zonas.filter((d) => d.esZonaPrecio);
    // Piura (Mercado Mayorista) es la zona de partida; si no está, la primera zona
    filtro.zona = String((zonas.find((z) => z.nombre === 'Piura') ?? zonas[0])?.id ?? '');

    crearChips(document.getElementById('cultivos'), 'cultivo',
        [{ valor: TODOS, texto: 'Todos' }, ...cultivos.map((c) => ({ valor: c.id, texto: c.nombre }))],
        filtro.cultivo, (valor) => { filtro.cultivo = valor; mostrarPrecios(); });
    crearChips(document.getElementById('zonas'), 'zona',
        [{ valor: TODOS, texto: 'Todas' }, ...zonas.map((z) => ({ valor: z.id, texto: z.nombre }))],
        filtro.zona, (valor) => { filtro.zona = valor; mostrarPrecios(); });
    mostrarPrecios();
}

iniciar();
