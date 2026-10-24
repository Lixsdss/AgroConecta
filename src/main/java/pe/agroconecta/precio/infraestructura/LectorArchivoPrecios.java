package pe.agroconecta.precio.infraestructura;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import pe.agroconecta.compartido.error.SolicitudInvalidaException;

/**
 * Lee archivos de precios (UC-02 A2): Excel {@code .xlsx} con Apache POI o CSV
 * separado por coma o punto y coma.
 * Solo comprueba la forma del archivo (cabecera, cantidad de filas); los datos
 * de cada fila los valida el
 * servicio de carga.
 */
@Component
public class LectorArchivoPrecios {

    /** Columnas esperadas, en orden. */
    public static final List<String> COLUMNAS = List.of("cultivo", "zona", "fecha", "precio_kg");
    /** Máximo de filas de datos por archivo. */
    public static final int MAXIMO_FILAS = 500;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Lee las filas de datos del archivo, saltando las vacías.
     *
     * @param nombreArchivo nombre original (define el formato por su extensión)
     * @param contenido     bytes del archivo
     * @return filas de datos
     * @throws SolicitudInvalidaException si el archivo está vacío, no es .xlsx ni
     *                                    .csv, no tiene las columnas
     *                                    esperadas o supera el máximo de filas
     */
    public List<FilaArchivoPrecio> leer(String nombreArchivo, byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new SolicitudInvalidaException("El archivo está vacío.");
        }
        String extension = StringUtils.lowerCase(StringUtils.substringAfterLast(nombreArchivo, "."));
        List<List<String>> filas = switch (StringUtils.defaultString(extension)) {
            case "xlsx" -> leerExcel(contenido);
            case "csv" -> leerCsv(contenido);
            default -> throw new SolicitudInvalidaException("Suba un archivo Excel (.xlsx) o CSV (.csv).");
        };
        validarCabecera(filas.isEmpty() ? List.of() : filas.getFirst());
        List<FilaArchivoPrecio> datos = new ArrayList<>();
        for (int i = 1; i < filas.size(); i++) {
            List<String> f = filas.get(i);
            if (f.stream().allMatch(StringUtils::isBlank)) {
                continue;
            }
            datos.add(new FilaArchivoPrecio(i + 1, celda(f, 0), celda(f, 1), celda(f, 2), celda(f, 3)));
        }
        if (datos.size() > MAXIMO_FILAS) {
            throw new SolicitudInvalidaException("El archivo tiene más de " + MAXIMO_FILAS + " filas de precios.");
        }
        return ImmutableList.copyOf(datos);
    }

    private static void validarCabecera(List<String> cabecera) {
        List<String> normalizada = cabecera.stream()
                .map(c -> StringUtils.lowerCase(StringUtils.stripAccents(StringUtils.trimToEmpty(c)), Locale.ROOT))
                .filter(StringUtils::isNotEmpty).toList();
        if (!normalizada.equals(COLUMNAS)) {
            throw new SolicitudInvalidaException(
                    "La primera fila debe tener las columnas: " + String.join(", ", COLUMNAS) + ".");
        }
    }

    private static String celda(List<String> fila, int indice) {
        return indice < fila.size() ? StringUtils.trimToEmpty(fila.get(indice)) : "";
    }

    private static List<List<String>> leerExcel(byte[] contenido) {
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter formato = new DataFormatter(Locale.ROOT);
            List<List<String>> filas = new ArrayList<>();
            for (int r = 0; r <= hoja.getLastRowNum(); r++) {
                Row fila = hoja.getRow(r);
                List<String> valores = new ArrayList<>();
                for (int c = 0; c < COLUMNAS.size(); c++) {
                    valores.add(fila == null ? "" : texto(fila.getCell(c), formato));
                }
                filas.add(valores);
            }
            return filas;
        } catch (IOException | RuntimeException e) {
            throw new SolicitudInvalidaException("No se pudo leer el archivo Excel. Revise que sea un .xlsx válido.");
        }
    }

    private static String texto(Cell celda, DataFormatter formato) {
        if (celda == null) {
            return "";
        }
        if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            return celda.getLocalDateTimeCellValue().toLocalDate().format(ISO);
        }
        if (celda.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(celda.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return formato.formatCellValue(celda);
    }

    private static List<List<String>> leerCsv(byte[] contenido) {
        String texto = StringUtils.removeStart(new String(contenido, StandardCharsets.UTF_8), "﻿");
        List<String> lineas = Splitter.onPattern("\r?\n").splitToList(texto);
        String primera = lineas.isEmpty() ? "" : lineas.getFirst();
        char separador = StringUtils.countMatches(primera, ';') > StringUtils.countMatches(primera, ',') ? ';' : ',';
        List<List<String>> filas = new ArrayList<>();
        for (String linea : lineas) {
            filas.add(dividirCsv(linea, separador));
        }
        return filas;
    }

    /**
     * Divide una línea CSV respetando comillas dobles ("a;b" queda como un solo
     * valor).
     */
    private static List<String> dividirCsv(String linea, char separador) {
        List<String> valores = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == separador && !entreComillas) {
                valores.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        valores.add(actual.toString());
        return valores;
    }
}
