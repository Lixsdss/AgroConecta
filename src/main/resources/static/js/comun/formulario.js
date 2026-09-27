/**
 * Ayudas para formularios: errores por campo, mensaje general y botón ocupado.
 * Todo el texto se escribe con textContent (nunca innerHTML).
 */

/**
 * Borra los errores mostrados en el formulario.
 *
 * @param {HTMLFormElement} formulario formulario
 * @param {HTMLElement} alerta contenedor del mensaje general
 */
export function limpiarErrores(formulario, alerta) {
    formulario.querySelectorAll('.is-invalid').forEach((campo) => campo.classList.remove('is-invalid'));
    formulario.querySelectorAll('[data-error-de]').forEach((nodo) => { nodo.textContent = ''; });
    ocultarAlerta(alerta);
}

/**
 * Muestra un error de la API: los errores por campo junto a cada campo y el resto como mensaje general.
 *
 * @param {HTMLFormElement} formulario formulario
 * @param {HTMLElement} alerta contenedor del mensaje general
 * @param {{message: string, errores?: Array<{campo: string, mensaje: string}>}} error error de la API (ErrorApi)
 * @param {Object<string, string>} [alias] campos de la API que se muestran junto a otro campo del formulario
 */
export function mostrarErrores(formulario, alerta, error, alias = {}) {
    const sinCampo = [];
    for (const { campo, mensaje } of error.errores ?? []) {
        const nombre = alias[campo] ?? campo;
        const destino = formulario.querySelector(`[data-error-de="${nombre}"]`);
        const control = formulario.elements.namedItem(nombre);
        if (destino) {
            destino.textContent = mensaje;
            if (control instanceof HTMLElement) {
                control.classList.add('is-invalid');
            }
        } else {
            sinCampo.push(mensaje);
        }
    }
    if (!error.errores?.length) {
        mostrarAlerta(alerta, error.message, 'error');
    } else if (sinCampo.length > 0) {
        mostrarAlerta(alerta, sinCampo.join(' '), 'error');
    }
    const primero = formulario.querySelector('.is-invalid');
    (primero ?? alerta).focus?.();
}

/**
 * Muestra un mensaje general.
 *
 * @param {HTMLElement} alerta contenedor
 * @param {string} mensaje texto
 * @param {'error'|'info'|'exito'} tipo tipo de mensaje
 */
export function mostrarAlerta(alerta, mensaje, tipo = 'error') {
    alerta.textContent = mensaje;
    alerta.className = `ac-alerta ac-alerta-${tipo}`;
    alerta.hidden = false;
}

/**
 * Oculta el mensaje general.
 *
 * @param {HTMLElement} alerta contenedor
 */
export function ocultarAlerta(alerta) {
    alerta.textContent = '';
    alerta.hidden = true;
}

/**
 * Deshabilita el botón mientras se espera la respuesta y cambia su texto.
 *
 * @param {HTMLButtonElement} boton botón de envío
 * @param {boolean} ocupado true mientras se espera
 * @param {string} textoOcupado texto a mostrar mientras se espera
 */
export function marcarOcupado(boton, ocupado, textoOcupado = 'Espere…') {
    if (ocupado) {
        boton.dataset.textoOriginal = boton.textContent;
        boton.textContent = textoOcupado;
        boton.disabled = true;
    } else {
        boton.textContent = boton.dataset.textoOriginal ?? boton.textContent;
        boton.disabled = false;
    }
}

/**
 * Conecta un botón "Mostrar / Ocultar" con un campo de contraseña.
 *
 * @param {HTMLButtonElement} boton botón
 * @param {HTMLInputElement} campo campo de contraseña
 */
export function conectarMostrarContrasena(boton, campo) {
    boton.addEventListener('click', () => {
        const oculta = campo.type === 'password';
        campo.type = oculta ? 'text' : 'password';
        boton.textContent = oculta ? 'Ocultar' : 'Mostrar';
        boton.setAttribute('aria-pressed', String(oculta));
    });
}
