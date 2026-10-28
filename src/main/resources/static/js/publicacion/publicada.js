/**
 * "Cosecha publicada" (diseño 11, UC-01 paso 7): confirma con los kilos totales y la fecha límite resultante.
 */
import { exigirAgricultor, leerPublicada, numero, soles, fechaCorta } from './borrador.js';

if (exigirAgricultor()) {
    const p = leerPublicada();
    if (!p) {
        window.location.replace('/pages/usuario/inicio.html');
    } else {
        const plural = Number(p.cantidad) === 1 ? p.unidad.toLowerCase() : p.unidadPlural;
        document.getElementById('resumen').textContent =
            `${p.cultivo} ${p.variedad}: ${numero(p.cantidad)} ${plural} (≈ ${numero(p.cantidadKg)} kilos) `
            + `a ${soles(p.precioKg)} por kilo.`;
        document.getElementById('vence').textContent =
            `Los compradores ya pueden verla. Vence el ${fechaCorta(p.fechaLimite)}.`;
    }
}
