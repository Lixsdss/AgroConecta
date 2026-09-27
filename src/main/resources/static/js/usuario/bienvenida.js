/**
 * Pantalla "Bienvenida" (diseño 05): tres tarjetas de una frase para el agricultor nuevo. Se puede saltar.
 */
import { requerirSesion } from '../comun/sesion.js';
import { icono } from '../comun/iconos.js';

/** Textos e íconos de las tres tarjetas, según el diseño. */
const TARJETAS = [
    ['Publique su cosecha', 'sprout'],
    ['Reciba ofertas', 'hand-coins'],
    ['Venda sin intermediarios', 'handshake']
];

requerirSesion();

const paso = document.getElementById('paso');
const tarjeta = document.getElementById('tarjeta');
const siguiente = document.getElementById('siguiente');
let actual = 0;

siguiente.addEventListener('click', () => {
    if (actual === TARJETAS.length - 1) {
        window.location.assign('/pages/usuario/inicio.html');
        return;
    }
    actual += 1;
    const [texto, nombreIcono] = TARJETAS[actual];
    paso.textContent = `${actual + 1} de ${TARJETAS.length}`;
    const span = document.createElement('span');
    span.textContent = texto;
    tarjeta.replaceChildren(icono(nombreIcono), span);
    const etiqueta = document.createElement('span');
    etiqueta.textContent = actual === TARJETAS.length - 1 ? 'Empezar' : 'Siguiente';
    siguiente.replaceChildren(etiqueta, icono('arrow-right'));
});
