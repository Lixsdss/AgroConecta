/**
 * Gráfico de línea de una sola serie (precio por kilo en el tiempo), en SVG y sin librerías (RNF-01).
 * Sigue la guía de visualización del proyecto: línea de 2 px en el color de la marca, relleno al 10 %,
 * punto final de 8 px con anillo del color de fondo, ejes y cuadrícula de 1 px, etiqueta solo en el último
 * valor, línea guía con ventana de detalle al pasar el puntero o con las flechas del teclado, y una tabla
 * como vista alternativa. El texto usa los colores de texto, nunca el de la serie.
 */
const SVG = 'http://www.w3.org/2000/svg';
const ALTO = 200;
const MARGEN = { arriba: 16, derecha: 64, abajo: 28, izquierda: 60 };

function el(nombre, atributos = {}) {
    const nodo = document.createElementNS(SVG, nombre);
    for (const [clave, valor] of Object.entries(atributos)) {
        nodo.setAttribute(clave, String(valor));
    }
    return nodo;
}

function texto(contenido, atributos) {
    const nodo = el('text', atributos);
    nodo.textContent = contenido;
    return nodo;
}

/** Límites "redondos" del eje Y, en pasos de 0.10 soles. */
function limitesY(valores) {
    let min = Math.min(...valores);
    let max = Math.max(...valores);
    if (max - min < 0.2) {
        min -= 0.1;
        max += 0.1;
    }
    return { min: Math.max(0, Math.floor(min * 10) / 10), max: Math.ceil(max * 10) / 10 };
}

/**
 * Dibuja el gráfico dentro del contenedor (lo reemplaza).
 *
 * @param {HTMLElement} contenedor elemento donde va el gráfico
 * @param {Array<{fecha: string, precioKg: number}>} puntos serie ordenada por fecha
 * @param {{titulo: string, formatoValor: (v: number) => string, formatoFecha: (iso: string) => string}} opciones
 */
export function dibujarLinea(contenedor, puntos, opciones) {
    contenedor.replaceChildren();
    const titulo = document.createElement('p');
    titulo.className = 'fw-semibold mb-1';
    titulo.textContent = opciones.titulo;
    contenedor.append(titulo);

    if (puntos.length === 0) {
        const vacio = document.createElement('p');
        vacio.className = 'ac-suave';
        vacio.textContent = 'Todavía no hay precios cargados para este cultivo y zona.';
        contenedor.append(vacio);
        return;
    }

    // El ancho es el del contenedor, para que el texto y la línea salgan a su tamaño real en el celular
    const ANCHO = Math.max(280, Math.round(contenedor.clientWidth || 320));
    const valores = puntos.map((p) => Number(p.precioKg));
    const { min, max } = limitesY(valores);
    const anchoUtil = ANCHO - MARGEN.izquierda - MARGEN.derecha;
    const altoUtil = ALTO - MARGEN.arriba - MARGEN.abajo;
    const x = (i) => MARGEN.izquierda + (puntos.length === 1 ? anchoUtil : (i / (puntos.length - 1)) * anchoUtil);
    const y = (v) => MARGEN.arriba + altoUtil - ((v - min) / (max - min)) * altoUtil;

    const svg = el('svg', {
        viewBox: `0 0 ${ANCHO} ${ALTO}`, class: 'ac-grafico', role: 'img', tabindex: 0,
        'aria-label': `${opciones.titulo}. De ${opciones.formatoValor(valores[0])} a `
            + `${opciones.formatoValor(valores.at(-1))}. Use las flechas para recorrer los días.`
    });

    // Cuadrícula y eje Y: tres líneas (mínimo, medio, máximo)
    for (const v of [min, (min + max) / 2, max]) {
        svg.append(el('line', { x1: MARGEN.izquierda, x2: ANCHO - MARGEN.derecha, y1: y(v), y2: y(v), class: 'ac-grafico-rejilla' }));
        svg.append(texto(opciones.formatoValor(v), { x: MARGEN.izquierda - 8, y: y(v) + 4, 'text-anchor': 'end', class: 'ac-grafico-eje' }));
    }
    // Eje X: primer y último día
    svg.append(texto(opciones.formatoFecha(puntos[0].fecha), { x: MARGEN.izquierda, y: ALTO - 6, class: 'ac-grafico-eje' }));
    svg.append(texto(opciones.formatoFecha(puntos.at(-1).fecha), { x: ANCHO - MARGEN.derecha, y: ALTO - 6, 'text-anchor': 'end', class: 'ac-grafico-eje' }));

    // Área y línea
    const recorrido = puntos.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(valores[i]).toFixed(1)}`).join(' ');
    const base = MARGEN.arriba + altoUtil;
    svg.append(el('path', { d: `${recorrido} L${x(puntos.length - 1).toFixed(1)},${base} L${x(0).toFixed(1)},${base} Z`, class: 'ac-grafico-area' }));
    svg.append(el('path', { d: recorrido, class: 'ac-grafico-linea' }));

    // Punto y etiqueta del último valor
    const ultimo = puntos.length - 1;
    svg.append(el('circle', { cx: x(ultimo), cy: y(valores[ultimo]), r: 4, class: 'ac-grafico-punto' }));
    svg.append(texto(opciones.formatoValor(valores[ultimo]), { x: x(ultimo) + 8, y: y(valores[ultimo]) + 4, class: 'ac-grafico-valor' }));

    // Línea guía y ventana de detalle
    const guia = el('line', { y1: MARGEN.arriba, y2: base, class: 'ac-grafico-guia', visibility: 'hidden' });
    const marca = el('circle', { r: 4, class: 'ac-grafico-punto', visibility: 'hidden' });
    svg.append(guia, marca);
    const detalle = document.createElement('div');
    detalle.className = 'ac-grafico-detalle';
    detalle.hidden = true;
    detalle.setAttribute('role', 'status');

    let actual = ultimo;
    function mostrar(i) {
        actual = i;
        const cx = x(i);
        guia.setAttribute('x1', cx);
        guia.setAttribute('x2', cx);
        marca.setAttribute('cx', cx);
        marca.setAttribute('cy', y(valores[i]));
        guia.setAttribute('visibility', 'visible');
        marca.setAttribute('visibility', 'visible');
        detalle.textContent = `${opciones.formatoFecha(puntos[i].fecha)}: ${opciones.formatoValor(valores[i])} por kilo`;
        detalle.style.left = `${(cx / ANCHO) * 100}%`;
        detalle.hidden = false;
    }
    function ocultar() {
        guia.setAttribute('visibility', 'hidden');
        marca.setAttribute('visibility', 'hidden');
        detalle.hidden = true;
    }
    svg.addEventListener('pointermove', (evento) => {
        const caja = svg.getBoundingClientRect();
        const px = ((evento.clientX - caja.left) / caja.width) * ANCHO;
        let cercano = 0;
        puntos.forEach((_, i) => {
            if (Math.abs(x(i) - px) < Math.abs(x(cercano) - px)) {
                cercano = i;
            }
        });
        mostrar(cercano);
    });
    svg.addEventListener('pointerleave', ocultar);
    svg.addEventListener('focus', () => mostrar(actual));
    svg.addEventListener('blur', ocultar);
    svg.addEventListener('keydown', (evento) => {
        if (evento.key === 'ArrowLeft' || evento.key === 'ArrowRight') {
            evento.preventDefault();
            mostrar(Math.min(ultimo, Math.max(0, actual + (evento.key === 'ArrowRight' ? 1 : -1))));
        }
    });

    const marco = document.createElement('div');
    marco.className = 'ac-grafico-marco';
    marco.append(svg, detalle);
    contenedor.append(marco, tablaAlternativa(puntos, opciones));
}

/** Vista de tabla: los mismos datos sin depender del gráfico. */
function tablaAlternativa(puntos, opciones) {
    const detalles = document.createElement('details');
    detalles.className = 'mt-2';
    const resumen = document.createElement('summary');
    resumen.textContent = 'Ver como tabla';
    const tabla = document.createElement('table');
    tabla.className = 'table table-sm mt-2';
    const cabecera = tabla.createTHead().insertRow();
    for (const titulo of ['Fecha', 'Precio por kilo']) {
        const th = document.createElement('th');
        th.scope = 'col';
        th.textContent = titulo;
        cabecera.append(th);
    }
    const cuerpo = tabla.createTBody();
    for (const p of puntos) {
        const fila = cuerpo.insertRow();
        fila.insertCell().textContent = opciones.formatoFecha(p.fecha);
        fila.insertCell().textContent = opciones.formatoValor(Number(p.precioKg));
    }
    detalles.append(resumen, tabla);
    return detalles;
}
