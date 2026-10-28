package pe.agroconecta.venta.aplicacion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Venta vista por la administración (reclamos, RF-09): partes, términos, estado y línea de tiempo.
 *
 * @param id               id de la venta
 * @param idComprador      comprador
 * @param comprador        nombre del negocio del comprador
 * @param idAgricultor     agricultor
 * @param agricultor       nombre del agricultor
 * @param cultivo          cultivo
 * @param variedad         variedad
 * @param cantidad         cantidad acordada
 * @param unidad           unidad en singular
 * @param unidadPlural     unidad en plural
 * @param cantidadKg       kilos
 * @param montoTotal       total en soles
 * @param estado           estado de la venta
 * @param llegoAAcordada   {@code true} si su historial pasó por ACORDADA (requisito para reclamar)
 * @param lineaDeTiempo    pasos de la venta, del primero al último
 */
public record VentaParaAdministracionDto(Long id, Long idComprador, String comprador, Long idAgricultor,
        String agricultor, String cultivo, String variedad, BigDecimal cantidad, String unidad, String unidadPlural,
        BigDecimal cantidadKg, BigDecimal montoTotal, String estado, boolean llegoAAcordada,
        List<PasoDto> lineaDeTiempo) {

    /**
     * Indica si el usuario es una de las partes.
     *
     * @param idUsuario usuario
     * @return {@code true} si es el comprador o el agricultor
     */
    public boolean participa(long idUsuario) {
        return idUsuario == idComprador || idUsuario == idAgricultor;
    }

    /**
     * Devuelve "Mango Kent" (sin repetir "Común").
     *
     * @return "Mango Kent" (sin repetir "Común")
     */
    public String producto() {
        return "Común".equals(variedad) ? cultivo + " común" : cultivo + " " + variedad;
    }
}
