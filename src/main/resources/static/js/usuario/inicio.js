/**
 * Inicio del agricultor (diseño 06) y del comprador (diseño 14): saludo, pocas acciones grandes con su dato del día
 * y precios de hoy (RF-02). GET /api/v1/usuarios/me confirma que el token sigue válido y que el usuario está ACTIVO.
 * Agricultor: "Vender mi cosecha", "Mis ventas" (cosechas por vencer, GET /api/v1/publicaciones/mias) y "Mensajes"
 * (GET /api/v1/conversaciones/no-leidos). Comprador: "Buscar productos", "Lotes agrupados", la compra en curso
 * (GET /api/v1/ventas?grupo=EN_CURSO) y "Mis compras". Las iniciales llevan a "Mi perfil", donde está "Salir".
 */
import { api } from '../api.js';
import { requerirSesion } from '../comun/sesion.js';
import { mostrarAlerta } from '../comun/formulario.js';
import { mostrarTarjetas } from '../comun/precios.js';
import { ponerBarra } from '../comun/barra.js';
import { textoEstado } from '../venta/estados.js';

const sesion = requerirSesion();

// El administrador trabaja en su panel de escritorio (diseño 11a–11e)
if (sesion?.usuario.rol === 'ADMINISTRADOR') {
    window.location.replace('/pages/administracion/panel.html');
}

function saludo() {
    const hora = new Date().getHours();
    if (hora < 12) {
        return 'Buenos días';
    }
    return hora < 19 ? 'Buenas tardes' : 'Buenas noches';
}

function plural(n, singular, varios) {
    return n === 1 ? `1 ${singular}` : `${n} ${varios}`;
}

function iniciales(nombre) {
    return nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

/** Los datos de cada acción son opcionales: si fallan, la tarjeta queda con su texto general. */
async function datoMensajes() {
    try {
        const { total } = await api.get('/conversaciones/no-leidos');
        if (total > 0) {
            document.querySelectorAll('[data-dato-mensajes]').forEach((d) => {
                d.textContent = plural(total, 'mensaje nuevo', 'mensajes nuevos');
            });
        }
    } catch {
        // Sin el dato, la tarjeta sigue funcionando
    }
}

async function datoVentas() {
    try {
        const pagina = await api.get('/publicaciones/mias?grupo=ACTIVAS');
        const porVencer = pagina.content.filter((c) => c.estado === 'PUBLICADA' && c.porVencer).length;
        const aLaVenta = pagina.content.filter((c) => c.estado === 'PUBLICADA').length;
        const dato = document.getElementById('dato-ventas');
        if (porVencer > 0) {
            dato.textContent = plural(porVencer, 'venta por vencer', 'ventas por vencer');
        } else if (aLaVenta > 0) {
            dato.textContent = plural(aLaVenta, 'cosecha a la venta', 'cosechas a la venta');
        } else {
            dato.textContent = 'Todavía no tiene cosechas a la venta';
        }
    } catch {
        // Sin el dato, la tarjeta sigue funcionando
    }
}

async function datoCompras() {
    try {
        const pagina = await api.get('/ventas?grupo=EN_CURSO');
        const total = pagina.page.totalElements;
        document.getElementById('dato-compras').textContent = total > 0
            ? plural(total, 'compra en curso', 'compras en curso') : 'Sus compras y entregas';
        const compra = pagina.content[0];
        if (compra) {
            const producto = compra.variedad === 'Común' ? `${compra.cultivo} común` : `${compra.cultivo} ${compra.variedad}`;
            const unidad = Number(compra.cantidad) === 1 ? compra.unidad.toLowerCase() : compra.unidadPlural;
            const tarjeta = document.getElementById('compra-en-curso');
            tarjeta.href = `/pages/venta/venta.html?id=${compra.id}`;
            document.getElementById('compra-titulo').textContent = `${producto}: ${textoEstado(compra.estado).toLowerCase()}`;
            document.getElementById('compra-dato').textContent =
                `${Number(compra.cantidad).toLocaleString('es-PE')} ${unidad} · ≈${Number(compra.cantidadKg).toLocaleString('es-PE', { maximumFractionDigits: 0 })} kg · Ver entrega`;
            tarjeta.hidden = false;
        }
    } catch {
        // Sin el dato, la tarjeta sigue funcionando
    }
}

async function cargarPrecios(perfil) {
    let precios = await api.get(`/precios/vigentes?idDistrito=${perfil.idDistrito}`);
    const deSuZona = precios.length > 0;
    if (deSuZona) {
        document.getElementById('titulo-precios').textContent = `Precios de hoy en ${perfil.distrito}`;
    } else {
        precios = await api.get('/precios/vigentes');
    }
    // Diseños 06 y 14: tarjeta corta; la zona solo si se muestran varias zonas
    mostrarTarjetas(document.getElementById('precios'), precios, undefined, { compacta: true, conZona: !deSuZona });
}

async function cargar() {
    try {
        const perfil = await api.get('/usuarios/me');
        const nombre = perfil.nombreCompleto.split(' ').filter(Boolean);
        document.getElementById('avatar').textContent = iniciales(perfil.nombreCompleto);
        if (perfil.rol === 'AGRICULTOR') {
            // Diseño 06: "Buenos días, Juan Carlos" (con 3 palabras o más, las dos primeras)
            document.getElementById('saludo').textContent = `${saludo()}, ${nombre.slice(0, nombre.length >= 3 ? 2 : 1).join(' ')}`;
            document.getElementById('acciones-agricultor').hidden = false;
            datoVentas();
        } else {
            document.getElementById('saludo').textContent = `Hola, ${nombre[0]}`;
            const subtitulo = document.getElementById('subtitulo');
            subtitulo.textContent = [perfil.nombreNegocio, perfil.distrito].filter(Boolean).join(' · ');
            subtitulo.hidden = false;
            document.getElementById('acciones-comprador').hidden = false;
            datoCompras();
        }
        datoMensajes();
        await cargarPrecios(perfil);
    } catch (error) {
        if (error.estado === 401) {
            window.location.replace('/pages/usuario/ingresar.html');
            return;
        }
        mostrarAlerta(document.getElementById('alerta'), error.message, 'error');
    }
}

if (sesion && sesion.usuario.rol !== 'ADMINISTRADOR') {
    ponerBarra('inicio');
    cargar();
}
