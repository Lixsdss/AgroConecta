/**
 * "Confirmar publicación" (diseño 10, UC-01 pasos 3 a 5): resumen y POST /api/v1/publicaciones.
 */
import { api } from '../api.js';
import { mostrarAlerta, marcarOcupado } from '../comun/formulario.js';
import {
    exigirAgricultor, leerBorrador, borrarBorrador, volverSiFalta, guardarPublicada, numero, soles, fechaCorta, RUTA
} from './borrador.js';

const alerta = document.getElementById('alerta');
const publicar = document.getElementById('publicar');

function escribir(id, texto) {
    document.getElementById(id).textContent = texto;
}

function mostrarResumen(b) {
    const cantidad = Number(b.cantidad);
    const kilosUnidad = Number(b.factorKg ?? b.pesoUnidadKg);
    const plural = cantidad === 1 ? b.unidad.toLowerCase() : b.unidadPlural;
    escribir('producto', `${b.cultivo} ${b.variedad}`);
    escribir('cantidad', `${numero(cantidad)} ${plural} de ${numero(kilosUnidad, 3)} kilos (≈ ${numero(cantidad * kilosUnidad)} kilos)`);
    escribir('precio', `${soles(b.precioUnitario)} por ${b.unidad.toLowerCase()}`);
    escribir('precio-kilo', `≈ ${soles(Number(b.precioUnitario) / kilosUnidad)}`);
    escribir('cosecha', fechaCorta(b.fechaCosecha));
    escribir('lugar', `${b.distrito}, ${b.provincia}`);
}

function iniciar() {
    if (!exigirAgricultor()) {
        return;
    }
    const b = leerBorrador();
    if (volverSiFalta(b, [['idVariedad', 'paso1.html'], ['idUnidad', 'paso2.html'], ['cantidad', 'paso2.html'],
        ['fechaCosecha', 'paso2.html'], ['precioUnitario', 'paso3.html'], ['idDistrito', 'paso3.html']])) {
        return;
    }
    mostrarResumen(b);
}

publicar.addEventListener('click', async () => {
    const b = leerBorrador();
    marcarOcupado(publicar, true, 'Publicando…');
    try {
        const creada = await api.post('/publicaciones', {
            idVariedad: b.idVariedad,
            idUnidad: b.idUnidad,
            cantidad: Number(b.cantidad),
            pesoUnidadKg: b.pesoUnidadKg ? Number(b.pesoUnidadKg) : null,
            precioUnitario: Number(b.precioUnitario),
            idDistrito: b.idDistrito,
            fechaCosecha: b.fechaCosecha
        });
        guardarPublicada(creada);
        borrarBorrador();
        window.location.assign(`${RUTA}/publicada.html`);
    } catch (error) {
        const detalle = error.errores?.map((e) => e.mensaje).join(' ');
        mostrarAlerta(alerta, detalle || error.message, 'error');
        marcarOcupado(publicar, false);
    }
});

iniciar();
