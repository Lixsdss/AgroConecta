/**
 * "Moderación de publicaciones" (diseño 11c y 11d, formato escritorio, RF-09 moderación posterior).
 * GET /api/v1/moderacion/publicaciones?vista=, GET /api/v1/moderacion/decisiones,
 * POST /api/v1/moderacion/publicaciones/{id}/decisiones y GET /api/v1/usuarios/me.
 */
import { api } from '../api.js';
import { requerirSesion, cerrarSesion } from '../comun/sesion.js';
import { mostrarAlerta, ocultarAlerta, limpiarErrores, mostrarErrores, marcarOcupado } from '../comun/formulario.js';
import { icono, ponerIconoYTexto } from '../comun/iconos.js';

/** Marcas de la cola: etiqueta, ícono, explicación y motivo sugerido (textos del diseño, adaptados a v4.1). */
const MARCAS = {
    PRIMERA_PUBLICACION: ['Primera publicación', 'user-plus',
        'Es la primera venta de esta cuenta. Revise que los datos tengan sentido.', 'CANTIDAD_INCORRECTA'],
    CANTIDAD_INUSUAL: ['Cantidad inusual', 'scale',
        'La cantidad es mucho mayor que el promedio de su zona.', 'CANTIDAD_INCORRECTA'],
    PRECIO_FUERA_RANGO: ['Precio fuera de rango', 'trending-up',
        'El precio por kilo se aleja más de 15 % de la referencia de hoy.', 'PRECIO_NO_COINCIDE'],
    CORREGIDA: ['Corregida por el agricultor', 'refresh-cw',
        'El agricultor corrigió una publicación oculta. Revise si ya se puede volver a mostrar.', 'DATOS_INCORRECTOS']
};

/** Motivos de catálogo (chk_revision_motivo) con el mensaje sugerido del diseño. */
const MOTIVOS = [
    ['PRECIO_NO_COINCIDE', 'El precio no coincide con el mercado',
        (p) => `El precio no coincide con el mercado de hoy. Revise el precio por ${p.unidad.toLowerCase()}.`],
    ['CANTIDAD_INCORRECTA', 'La cantidad no parece correcta',
        (p) => `La cantidad parece muy alta para su chacra. Revise cuántas ${p.unidadPlural} tiene.`],
    ['PESO_UNIDAD_INCORRECTO', 'El peso por unidad no parece correcto',
        (p) => `El peso por ${p.unidad.toLowerCase()} no parece correcto. Revise cuántos kilos tiene cada ${p.unidad.toLowerCase()}.`],
    ['DATOS_INCORRECTOS', 'Faltan datos o hay datos incorrectos',
        () => 'Faltan datos o hay datos incorrectos. Revise su publicación.'],
    ['OTRO', 'Otro motivo', () => '']
];

const DECISIONES = {
    CONFORME: 'Conforme', ADVERTENCIA: 'Advertida', BLOQUEO: 'Ocultada', DESBLOQUEO: 'Vuelta a mostrar'
};

const lista = document.getElementById('lista');
const detalle = document.getElementById('detalle');
const aviso = document.getElementById('aviso');
const dialogo = document.getElementById('dialogo');
const formMotivo = document.getElementById('form-motivo');
const alertaDialogo = document.getElementById('alerta-dialogo');

let vista = 'POR_REVISAR';
let elegida = null;
let decisionEnCurso = null;

// ---------- Formato de datos ----------

const numero = (v, d = 2) => Number(v).toLocaleString('es-PE', { maximumFractionDigits: d });
const soles = (v) => `S/ ${Number(v).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

function haceCuanto(isoLocal) {
    const minutos = Math.max(0, Math.round((Date.now() - new Date(isoLocal).getTime()) / 60000));
    if (minutos < 60) {
        return `hace ${minutos} min`;
    }
    const horas = Math.round(minutos / 60);
    return horas < 24 ? `hace ${horas} h` : `hace ${Math.round(horas / 24)} días`;
}

function fecha(iso) {
    return new Date(`${iso}T00:00:00`).toLocaleDateString('es-PE', { day: 'numeric', month: 'long' });
}

function cantidadTexto(p) {
    const plural = Number(p.cantidad) === 1 ? p.unidad.toLowerCase() : p.unidadPlural;
    return `${numero(p.cantidad)} ${plural}`;
}

function crear(etiqueta, clase, texto) {
    const el = document.createElement(etiqueta);
    if (clase) {
        el.className = clase;
    }
    if (texto !== undefined) {
        el.textContent = texto;
    }
    return el;
}

function etiquetaMarca(marca) {
    const [texto, nombreIcono] = MARCAS[marca] ?? ['Sin marca', 'info'];
    const span = crear('span', 'ac-etiqueta-marca');
    span.append(icono(nombreIcono), crear('span', '', texto));
    return span;
}

// ---------- Lista ----------

async function cargar() {
    detalle.hidden = true;
    lista.replaceChildren();
    try {
        if (vista === 'DECISIONES') {
            mostrarDecisiones(await api.get('/moderacion/decisiones'));
        } else {
            const pagina = await api.get(`/moderacion/publicaciones?vista=${vista}&size=50`);
            mostrarPublicaciones(pagina.content);
            if (vista === 'POR_REVISAR') {
                const insignia = document.getElementById('insignia');
                insignia.textContent = String(pagina.page.totalElements);
                insignia.hidden = pagina.page.totalElements === 0;
                document.getElementById('tab-POR_REVISAR').textContent = `Por revisar (${pagina.page.totalElements})`;
            }
        }
    } catch (error) {
        if (error.estado === 401) {
            window.location.replace('/pages/usuario/ingresar.html');
            return;
        }
        mostrarAlerta(aviso, error.message, 'error');
    }
}

function vacio(titulo, texto) {
    const caja = crear('div', 'ac-vacio');
    caja.append(icono('circle-check', 'ac-icono-grande'), crear('p', 'fw-semibold mt-2 mb-1', titulo),
        crear('p', 'ac-suave mb-0', texto));
    lista.replaceChildren(caja);
}

function mostrarPublicaciones(publicaciones) {
    if (publicaciones.length === 0) {
        if (vista === 'OCULTAS') {
            vacio('No hay publicaciones ocultas', 'Aparecerán aquí cuando oculte una publicación.');
        } else {
            vacio('Nada por revisar', 'Las nuevas publicaciones marcadas por el sistema aparecerán aquí.');
        }
        return;
    }
    for (const p of publicaciones) {
        const item = crear('button', 'ac-item-cola');
        item.type = 'button';
        const titulo = crear('div', 'd-flex justify-content-between align-items-center gap-2 mb-1');
        titulo.append(crear('strong', '', `${p.cultivo} ${p.variedad} · ${cantidadTexto(p)} (≈${numero(p.cantidadKg)} kg)`));
        if (p.marcaRevision) {
            titulo.append(etiquetaMarca(p.marcaRevision));
        }
        const agricultor = p.agricultor?.nombreCompleto ?? 'Agricultor';
        item.append(titulo,
            crear('div', 'ac-suave', `${agricultor} · ${p.distrito} · ${soles(p.precioUnitario)} por ${p.unidad.toLowerCase()} (≈${soles(p.precioKg)} por kilo) · ${haceCuanto(p.fechaPublicacion)}`));
        item.addEventListener('click', () => {
            lista.querySelectorAll('.ac-item-cola').forEach((i) => i.removeAttribute('aria-current'));
            item.setAttribute('aria-current', 'true');
            mostrarDetalle(p);
        });
        lista.append(item);
    }
}

function mostrarDecisiones(decisiones) {
    if (decisiones.length === 0) {
        vacio('Todavía no hay decisiones hoy', 'Aparecerán aquí cuando revise una publicación.');
        return;
    }
    for (const d of decisiones) {
        const item = crear('div', 'ac-item-cola');
        const hora = new Date(d.fecha).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
        item.append(crear('strong', 'd-block', `${DECISIONES[d.decision]} · ${d.producto}`),
            crear('div', 'ac-suave', `${hora} · ${d.administrador ?? ''}`));
        if (d.mensaje) {
            item.append(crear('div', 'mt-1', `Mensaje enviado: “${d.mensaje}”`));
        }
        lista.append(item);
    }
}

// ---------- Detalle ----------

function dato(dl, etiqueta, valor, extra) {
    const fila = crear('div', 'ac-dato');
    const dd = crear('dd', '', valor);
    if (extra) {
        dd.append(crear('div', 'ac-suave fw-normal', extra));
    }
    fila.append(crear('dt', '', etiqueta), dd);
    dl.append(fila);
}

function botonAccion(nombreIcono, texto, clase, decision) {
    const boton = crear('button', `btn ${clase} w-100 mb-2`);
    boton.type = 'button';
    ponerIconoYTexto(boton, nombreIcono, texto);
    boton.addEventListener('click', () => (decision === 'CONFORME' || decision === 'DESBLOQUEO')
        ? decidir(decision, boton)
        : abrirDialogo(decision));
    return boton;
}

function mostrarDetalle(p) {
    elegida = p;
    detalle.replaceChildren();
    detalle.append(crear('p', 'ac-suave mb-1', `Publicación enviada ${haceCuanto(p.fechaPublicacion)}`),
        crear('h2', 'h4 fw-semibold', `${p.cultivo} ${p.variedad} · ${cantidadTexto(p)}`));
    if (p.marcaRevision) {
        const marca = crear('div', 'ac-alerta ac-alerta-info');
        const [etiqueta, nombreIcono, explicacion] = MARCAS[p.marcaRevision];
        const texto = crear('div');
        texto.append(crear('strong', 'd-block', `Marcado por el sistema: ${etiqueta}`), crear('span', '', explicacion));
        marca.append(icono(nombreIcono), texto);
        detalle.append(marca);
    }
    const dl = crear('dl', 'mb-3');
    const a = p.agricultor;
    if (a) {
        const registro = new Date(a.fechaRegistro).toLocaleDateString('es-PE');
        dato(dl, 'Agricultor', a.nombreCompleto, `${a.celular} · ${a.distrito} · registrado el ${registro}`);
    }
    dato(dl, 'Cantidad', `${cantidadTexto(p)} · ≈${numero(p.cantidadKg)} kg`,
        `Cada ${p.unidad.toLowerCase()} pesa ${numero(p.pesoUnidadKg, 3)} kilos`);
    dato(dl, 'Precio', `${soles(p.precioUnitario)} por ${p.unidad.toLowerCase()}`, `≈ ${soles(p.precioKg)} por kilo`);
    dato(dl, 'Cosecha', fecha(p.fechaCosecha), `${p.distrito}, ${p.provincia}`);
    dato(dl, 'Vence', fecha(p.fechaLimite));
    detalle.append(dl);

    if (p.estado === 'BLOQUEADA') {
        detalle.append(crear('p', 'ac-alerta ac-alerta-error', 'Esta publicación está oculta: los compradores no la ven.'),
            botonAccion('eye', 'Volver a mostrar', 'btn-primary', 'DESBLOQUEO'));
    } else {
        if (p.marcaRevision) {
            detalle.append(botonAccion('circle-check', 'Conforme', 'btn-primary', 'CONFORME'));
        }
        detalle.append(botonAccion('triangle-alert', 'Advertir', 'btn-outline-primary', 'ADVERTENCIA'),
            botonAccion('eye-off', 'Ocultar', 'btn-outline-primary', 'BLOQUEO'));
    }
    detalle.hidden = false;
}

// ---------- Decisiones ----------

async function decidir(decision, boton, motivo = null, mensaje = null) {
    marcarOcupado(boton, true, 'Guardando…');
    try {
        await api.post(`/moderacion/publicaciones/${elegida.id}/decisiones`, { decision, motivo, mensaje });
        const nombre = elegida.agricultor?.nombreCompleto ?? 'el agricultor';
        const textos = {
            CONFORME: 'Publicación marcada como conforme.',
            ADVERTENCIA: `Advertencia enviada a ${nombre}.`,
            BLOQUEO: `Publicación ocultada. Se avisó a ${nombre}.`,
            DESBLOQUEO: `Publicación visible de nuevo. Se avisó a ${nombre}.`
        };
        dialogo.close();
        mostrarAlerta(aviso, textos[decision], 'exito');
        await cargar();
        return true;
    } catch (error) {
        marcarOcupado(boton, false);
        if (dialogo.open) {
            mostrarErrores(formMotivo, alertaDialogo, error, { motivoPresente: 'motivo', mensajePresente: 'mensaje' });
        } else {
            mostrarAlerta(aviso, error.message, 'error');
        }
        return false;
    }
}

function actualizarVista() {
    const producto = `${elegida.cultivo} ${elegida.variedad}`.toLowerCase();
    document.getElementById('vista-titulo').textContent = decisionEnCurso === 'BLOQUEO'
        ? `Su ${producto} fue ocultado.`
        : `Aviso sobre su ${producto}.`;
    const motivo = formMotivo.querySelector('input[name="motivo"]:checked');
    const etiqueta = MOTIVOS.find(([clave]) => clave === motivo?.value)?.[1] ?? '';
    document.getElementById('vista-mensaje').textContent =
        `Motivo: ${etiqueta}. ${formMotivo.elements.mensaje.value}`.trim();
}

function abrirDialogo(decision) {
    decisionEnCurso = decision;
    limpiarErrores(formMotivo, alertaDialogo);
    const confirmar = document.getElementById('confirmar');
    document.getElementById('dialogo-titulo').textContent = decision === 'BLOQUEO' ? 'Ocultar publicación' : 'Advertir al agricultor';
    confirmar.textContent = decision === 'BLOQUEO' ? 'Ocultar y avisar' : 'Advertir y avisar';
    confirmar.disabled = false;
    document.getElementById('dialogo-sub').textContent =
        `${elegida.cultivo} ${elegida.variedad} · ${cantidadTexto(elegida)} (≈${numero(elegida.cantidadKg)} kg) · ${elegida.agricultor?.nombreCompleto ?? ''}`;

    const sugerido = MARCAS[elegida.marcaRevision]?.[3] ?? 'DATOS_INCORRECTOS';
    const contenedor = document.getElementById('motivos');
    contenedor.replaceChildren();
    for (const [clave, etiqueta, plantilla] of MOTIVOS) {
        const label = crear('label');
        const radio = crear('input');
        radio.type = 'radio';
        radio.name = 'motivo';
        radio.value = clave;
        radio.checked = clave === sugerido;
        radio.addEventListener('change', () => {
            formMotivo.elements.mensaje.value = plantilla(elegida);
            actualizarVista();
        });
        label.append(radio, crear('span', '', etiqueta));
        contenedor.append(label);
    }
    formMotivo.elements.mensaje.value = MOTIVOS.find(([c]) => c === sugerido)[2](elegida);
    actualizarVista();
    dialogo.showModal();
}

formMotivo.elements.mensaje.addEventListener('input', actualizarVista);
document.getElementById('cancelar').addEventListener('click', () => dialogo.close());

formMotivo.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formMotivo, alertaDialogo);
    const motivo = formMotivo.querySelector('input[name="motivo"]:checked')?.value ?? null;
    await decidir(decisionEnCurso, document.getElementById('confirmar'), motivo, formMotivo.elements.mensaje.value);
});

// ---------- Inicio ----------

document.querySelectorAll('[role="tab"]').forEach((pestana) => {
    pestana.addEventListener('click', () => {
        vista = pestana.dataset.vista;
        document.querySelectorAll('[role="tab"]').forEach((p) => p.setAttribute('aria-selected', String(p === pestana)));
        ocultarAlerta(aviso);
        cargar();
    });
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
    await cargar();
}

iniciar();
