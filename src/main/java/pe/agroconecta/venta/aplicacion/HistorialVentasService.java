package pe.agroconecta.venta.aplicacion;

import static pe.agroconecta.venta.infraestructura.EspecificacionesVentas.acordadaEntre;
import static pe.agroconecta.venta.infraestructura.EspecificacionesVentas.contraparteContiene;
import static pe.agroconecta.venta.infraestructura.EspecificacionesVentas.deUsuario;
import static pe.agroconecta.venta.infraestructura.EspecificacionesVentas.estadoEn;
import static pe.agroconecta.venta.infraestructura.EspecificacionesVentas.idEn;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.venta.dominio.EstadoTransaccion;
import pe.agroconecta.venta.dominio.VentaHistorial;
import pe.agroconecta.venta.infraestructura.TransaccionRepository;
import pe.agroconecta.venta.infraestructura.VentaHistorialRepository;

/**
 * "Mis ventas" y "Mis compras" (UC-08): historial filtrado y paginado desde {@code v_historial_transacciones}, y su
 * exportación a Excel con Apache POI.
 */
@Service
@Transactional(readOnly = true)
public class HistorialVentasService {

    /** Ventas por página. */
    public static final int TAMANO_PAGINA = 20;
    /** Máximo de ventas en un Excel. */
    public static final int MAXIMO_EXCEL = 5000;

    static final String MENSAJE_GRUPO = "El grupo debe ser EN_CURSO o TERMINADAS.";
    static final String MENSAJE_FECHAS = "La fecha «desde» no puede ser posterior a la fecha «hasta».";
    static final String MENSAJE_PAGINA = "La página no puede ser negativa.";
    static final String MENSAJE_EXCEL_GRANDE =
            "Hay más de " + MAXIMO_EXCEL + " ventas en ese filtro. Acorte el rango de fechas.";

    private static final Sort RECIENTES = Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));

    private final VentaHistorialRepository historial;
    private final TransaccionRepository transacciones;
    private final CatalogoService catalogo;
    private final ExportadorExcelVentas excel;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param historial     vista del historial
     * @param transacciones ventas (filtro por conversación)
     * @param catalogo      unidades (nombre en singular)
     * @param excel         generador del Excel
     * @param reloj         reloj de la aplicación (nombre del archivo)
     */
    HistorialVentasService(VentaHistorialRepository historial, TransaccionRepository transacciones,
            CatalogoService catalogo, ExportadorExcelVentas excel, Clock reloj) {
        this.historial = historial;
        this.transacciones = transacciones;
        this.catalogo = catalogo;
        this.excel = excel;
        this.reloj = reloj;
    }

    /**
     * Ventas del usuario, la más reciente primero (20 por página).
     *
     * @param idUsuario usuario de la sesión
     * @param filtro    filtros opcionales
     * @param pagina    página desde 0
     * @return página de ventas
     * @throws SolicitudInvalidaException si el grupo, las fechas o la página no son válidos
     */
    public Page<VentaResumenDto> listar(long idUsuario, FiltroVentasDto filtro, int pagina) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(MENSAJE_PAGINA);
        }
        Map<String, String> singulares = singulares();
        return historial.findAll(condicion(idUsuario, filtro), PageRequest.of(pagina, TAMANO_PAGINA, RECIENTES))
                .map(v -> aDto(v, idUsuario, singulares));
    }

    /**
     * Excel con las ventas del filtro (UC-08 A3).
     *
     * @param idUsuario usuario de la sesión
     * @param filtro    filtros opcionales
     * @return archivo {@code AgroConecta_Historial_{fecha}.xlsx}
     * @throws SolicitudInvalidaException si el grupo o las fechas no son válidos
     * @throws ReglaNegocioException      si hay más de 5000 ventas
     */
    public ArchivoDto exportar(long idUsuario, FiltroVentasDto filtro) {
        Specification<VentaHistorial> condicion = condicion(idUsuario, filtro);
        if (historial.count(condicion) > MAXIMO_EXCEL) {
            throw new ReglaNegocioException(MENSAJE_EXCEL_GRANDE);
        }
        Map<String, String> singulares = singulares();
        byte[] contenido = excel.generar(historial.findAll(condicion, RECIENTES).stream()
                .map(v -> aDto(v, idUsuario, singulares))
                .toList());
        return new ArchivoDto("AgroConecta_Historial_%s.xlsx".formatted(LocalDate.now(reloj)), contenido);
    }

    private Specification<VentaHistorial> condicion(long idUsuario, FiltroVentasDto filtro) {
        Preconditions.checkNotNull(filtro, "filtro");
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw new SolicitudInvalidaException(MENSAJE_FECHAS);
        }
        return deUsuario(idUsuario)
                .and(estadoEn(grupo(filtro.grupo())))
                .and(acordadaEntre(filtro.desde(), filtro.hasta()))
                .and(contraparteContiene(idUsuario, StringUtils.trimToNull(filtro.contraparte())))
                .and(idEn(filtro.idConversacion() == null ? null
                        : transacciones.idsDeConversacion(filtro.idConversacion())));
    }

    private static Set<EstadoTransaccion> grupo(String grupo) {
        if (StringUtils.isBlank(grupo)) {
            return null;
        }
        return switch (grupo) {
            case "EN_CURSO" -> EstadoTransaccion.EN_CURSO;
            case "TERMINADAS" -> EstadoTransaccion.TERMINADAS;
            default -> throw new SolicitudInvalidaException(MENSAJE_GRUPO);
        };
    }

    /** Nombre en singular de cada unidad, por su nombre en plural (la vista solo trae el plural). */
    private Map<String, String> singulares() {
        return catalogo.listarUnidades().stream()
                .collect(Collectors.toMap(UnidadDto::nombrePlural, UnidadDto::nombre, (a, b) -> a));
    }

    private static VentaResumenDto aDto(VentaHistorial v, long idUsuario, Map<String, String> singulares) {
        boolean comprador = v.getIdComprador() == idUsuario;
        return new VentaResumenDto(v.getId(), comprador, comprador ? v.getAgricultor() : v.getComprador(),
                v.getCultivo(), v.getVariedad(), v.getCantidad(),
                singulares.getOrDefault(v.getUnidadPlural(), v.getUnidadPlural()), v.getUnidadPlural(),
                v.getCantidadKg(), v.getPrecioUnitario(), v.getPrecioKg(), v.getMontoTotal(), v.getFechaRecojo(),
                v.getEstado().name(), v.getFechaCreacion(), v.getFechaActualizacion());
    }
}
