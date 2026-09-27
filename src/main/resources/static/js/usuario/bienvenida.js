/**
 * Pantalla "Bienvenida" (diseño 05): tres tarjetas de una frase para el agricultor nuevo. Se puede saltar.
 */
import { requerirSesion } from '../comun/sesion.js';

const TARJETAS = ['Publique su cosecha', 'Reciba ofertas', 'Venda sin intermediarios'];

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
    paso.textContent = `${actual + 1} de ${TARJETAS.length}`;
    tarjeta.textContent = TARJETAS[actual];
    siguiente.textContent = actual === TARJETAS.length - 1 ? 'Empezar →' : 'Siguiente →';
});
