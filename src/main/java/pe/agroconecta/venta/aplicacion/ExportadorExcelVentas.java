package pe.agroconecta.venta.aplicacion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Genera el Excel del historial de ventas (UC-08 A3) con Apache POI: una fila por venta y las columnas del listado.
 */
@Component
class ExportadorExcelVentas {

    static final List<String> COLUMNAS = List.of("Fecha del acuerdo", "Cultivo", "Variedad", "Cantidad", "Unidad",
            "Kilos", "Precio por unidad (S/)", "Precio por kilo (S/)", "Total (S/)", "Contraparte", "Estado", "Recojo",
            "Última actualización");

    /** Texto de cada estado en la hoja (el mismo que en la pantalla). */
    static final Map<String, String> ESTADOS = Map.of(
            "PROPUESTA", "Propuesta", "ACORDADA", "Venta acordada", "RECOGIDO", "Ya recogieron",
            "EN_CAMINO", "En camino", "ENTREGADO", "Ya entregado", "COMPLETADA", "Venta terminada",
            "RECHAZADA", "Rechazada", "CANCELADA", "Cancelada");

    /**
     * Arma el libro de Excel.
     *
     * @param ventas filas del historial
     * @return bytes del archivo {@code .xlsx}
     */
    byte[] generar(List<VentaResumenDto> ventas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Historial");
            CreationHelper ayuda = libro.getCreationHelper();
            CellStyle titulo = libro.createCellStyle();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            titulo.setFont(negrita);
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(ayuda.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle fechaHora = libro.createCellStyle();
            fechaHora.setDataFormat(ayuda.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            CellStyle soles = libro.createCellStyle();
            soles.setDataFormat(ayuda.createDataFormat().getFormat("#,##0.00"));

            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < COLUMNAS.size(); i++) {
                Cell celda = cabecera.createCell(i);
                celda.setCellValue(COLUMNAS.get(i));
                celda.setCellStyle(titulo);
            }
            int fila = 1;
            for (VentaResumenDto v : ventas) {
                Row r = hoja.createRow(fila++);
                fechaHora(r, 0, v.fechaCreacion(), fechaHora);
                r.createCell(1).setCellValue(v.cultivo());
                r.createCell(2).setCellValue(v.variedad());
                numero(r, 3, v.cantidad(), null);
                r.createCell(4).setCellValue(v.cantidad().compareTo(BigDecimal.ONE) == 0 ? v.unidad() : v.unidadPlural());
                numero(r, 5, v.cantidadKg(), soles);
                numero(r, 6, v.precioUnitario(), soles);
                numero(r, 7, v.precioKg(), soles);
                numero(r, 8, v.montoTotal(), soles);
                r.createCell(9).setCellValue(v.contraparte());
                r.createCell(10).setCellValue(ESTADOS.getOrDefault(v.estado(), v.estado()));
                fecha(r, 11, v.fechaRecojo(), fecha);
                fechaHora(r, 12, v.fechaActualizacion(), fechaHora);
            }
            hoja.createFreezePane(0, 1);
            // Ancho fijo por columna: autoSizeColumn necesita las fuentes del sistema (falla en servidores sin ellas)
            for (int i = 0; i < COLUMNAS.size(); i++) {
                hoja.setColumnWidth(i, Math.max(14, COLUMNAS.get(i).length() + 4) * 256);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del historial", e);
        }
    }

    private static void numero(Row fila, int columna, BigDecimal valor, CellStyle estilo) {
        if (valor != null) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(valor.doubleValue());
            if (estilo != null) {
                celda.setCellStyle(estilo);
            }
        }
    }

    private static void fecha(Row fila, int columna, LocalDate valor, CellStyle estilo) {
        if (valor != null) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(valor);
            celda.setCellStyle(estilo);
        }
    }

    private static void fechaHora(Row fila, int columna, LocalDateTime valor, CellStyle estilo) {
        if (valor != null) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(valor);
            celda.setCellStyle(estilo);
        }
    }
}
