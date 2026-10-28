/**
 * Pantalla "¿Quién es usted?" (diseño 03, UC-10 paso 1). Solo agricultores y compradores se registran solos.
 */
const opciones = document.querySelectorAll('[data-rol]');
let rolElegido = 'AGRICULTOR';

opciones.forEach((opcion) => {
    opcion.addEventListener('click', () => {
        rolElegido = opcion.dataset.rol;
        opciones.forEach((o) => o.setAttribute('aria-pressed', String(o === opcion)));
    });
});

document.getElementById('continuar').addEventListener('click', () => {
    window.location.assign(`/pages/usuario/registro.html?rol=${encodeURIComponent(rolElegido)}`);
});
