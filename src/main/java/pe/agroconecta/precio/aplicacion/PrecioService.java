package pe.agroconecta.precio.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.precio.dominio.PrecioMercado;
import pe.agroconecta.precio.dominio.PrecioVigente;
import pe.agroconecta.precio.infraestructura.PrecioMercadoRepository;
import pe.agroconecta.precio.infraestructura.PrecioVigenteRepository;

/**
 * Consultas públicas de precios de referencia (RF-02): tablero "Precios de hoy" e historial para el gráfico.
 */
@Service
@Transactional(readOnly = true)
public class PrecioService {

    /** Días máximos del historial. */
    static final int DIAS_MAXIMOS = 365;

    private final PrecioVigenteRepository vigentes;
    private final PrecioMercadoRepository precios;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param vigentes repositorio de la vista de precios vigentes
     * @param precios  repositorio de precios cargados
     * @param reloj    reloj de la aplicación
     */
    public PrecioService(PrecioVigenteRepository vigentes, PrecioMercadoRepository precios, Clock reloj) {
        this.vigentes = vigentes;
        this.precios = precios;
        this.reloj = reloj;
    }

    /**
     * Lista los precios vigentes, con filtros opcionales.
     *
     * @param idCultivo  id del cultivo, o {@code null}
     * @param idDistrito id de la zona, o {@code null}
     * @return precios vigentes ordenados por cultivo y zona
     */
    public List<PrecioVigenteDto> vigentes(Integer idCultivo, Integer idDistrito) {
        List<PrecioVigente> encontrados = (idCultivo == null
                ? vigentes.findAllByOrderByCultivoAscZonaAsc()
                : vigentes.findByIdCultivoOrderByZonaAsc(idCultivo)).stream()
                .filter(v -> idDistrito == null || idDistrito.equals(v.getIdDistrito()))
                .toList();
        Set<LocalDate> fechas = encontrados.stream().map(PrecioVigente::getFecha).collect(Collectors.toSet());
        Map<String, Long> ids = fechas.isEmpty() ? Map.of() : precios.findByFechaIn(fechas).stream()
                .collect(Collectors.toMap(p -> clave(p.getIdCultivo(), p.getIdDistrito(), p.getFecha()),
                        PrecioMercado::getId));
        return encontrados.stream()
                .map(v -> new PrecioVigenteDto(ids.get(clave(v.getIdCultivo(), v.getIdDistrito(), v.getFecha())),
                        v.getIdCultivo(), v.getCultivo(), v.getIdDistrito(), v.getZona(), v.getFecha(), v.getPrecioKg(),
                        v.getUnidadLocal(), v.getPrecioUnidadLocal(), v.getTendencia(), v.getFuente(),
                        v.getActualizado()))
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Serie de precios de un cultivo en una zona, del más antiguo al más reciente.
     *
     * @param idCultivo  id del cultivo
     * @param idDistrito id de la zona
     * @param dias       días hacia atrás (1 a 365)
     * @return puntos de la serie
     */
    public List<PuntoHistorialDto> historial(int idCultivo, int idDistrito, int dias) {
        Preconditions.checkArgument(dias >= 1 && dias <= DIAS_MAXIMOS, "dias fuera de rango");
        LocalDate desde = LocalDate.now(reloj).minusDays(dias - 1L);
        return precios.findByIdCultivoAndIdDistritoAndFechaGreaterThanEqualOrderByFechaAsc(idCultivo, idDistrito, desde)
                .stream().map(p -> new PuntoHistorialDto(p.getFecha(), p.getPrecioKg()))
                .collect(ImmutableList.toImmutableList());
    }

    private static String clave(Integer idCultivo, Integer idDistrito, LocalDate fecha) {
        return idCultivo + "|" + idDistrito + "|" + fecha;
    }
}
