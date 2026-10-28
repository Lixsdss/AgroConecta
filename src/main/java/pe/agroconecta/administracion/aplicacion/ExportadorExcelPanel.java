package pe.agroconecta.administracion.aplicacion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Genera el Excel del panel (UC-11 parte 4) con Apache POI: hojas Resumen, Ventas por mes, Embudo y Usuarios.
 */
@Component
class ExportadorExcelPanel {

    static final List<String> HOJAS = List.of("Resumen", "Ventas por mes", "Embudo", "Usuarios");

    private static final List<String> COLUMNAS_VENTAS = List.of("Mes", "Cultivo", "Ventas completadas",
            "Kilos vendidos", "Monto total (S/)");
    private static final List<String> COLUMNAS_EMBUDO = List.of("Publicación", "Producto", "Agricultor",
            "Clics de WhatsApp", "Contactos", "Ofertas", "Acuerdos", "Completadas");
    private static final List<String> COLUMNAS_USUARIOS = List.of("Nombre", "Rol", "Chacra o negocio", "Celular",
            "Distrito", "Estado", "Motivo de suspensión", "Operaciones completadas", "Calificación",
            "Calificaciones", "Reclamos fundados", "Fecha de registro");

    private static final Map<String, String> ROLES = Map.of("AGRICULTOR", "Agricultor", "COMPRADOR", "Comprador",
            "ADMINISTRADOR", "Administrador");
    private static final Map<String, String> ESTADOS = Map.of("ACTIVO", "Activo", "SUSPENDIDO", "Suspendido",
            "INACTIVO", "Desactivado");

    /** Estilos compartidos por las hojas. */
    private record Estilos(CellStyle titulo, CellStyle fechaHora, CellStyle decimal) {
    }

    /**
     * Arma el libro de Excel.
     *
     * @param tarjetas tarjetas del panel
     * @param meses    ventas por mes y cultivo
     * @param embudo   publicaciones con contactos
     * @param usuarios todos los usuarios
     * @return bytes del archivo {@code .xlsx}
     */
    byte[] generar(TarjetasPanelDto tarjetas, List<VentaMensualDto> meses, List<EmbudoDto> embudo,
            List<UsuarioAdminDto> usuarios) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos estilos = estilos(libro);
            resumen(libro.createSheet(HOJAS.get(0)), tarjetas, estilos);

            Sheet hoja = hoja(libro, HOJAS.get(1), COLUMNAS_VENTAS, estilos);
            int fila = 1;
            for (VentaMensualDto v : meses) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(v.mes());
                r.createCell(1).setCellValue(v.cultivo());
                r.createCell(2).setCellValue(v.ventasCompletadas());
                numero(r, 3, v.kilosVendidos(), estilos.decimal());
                numero(r, 4, v.montoTotal(), estilos.decimal());
            }

            hoja = hoja(libro, HOJAS.get(2), COLUMNAS_EMBUDO, estilos);
            fila = 1;
            for (EmbudoDto e : embudo) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(e.idPublicacion());
                texto(r, 1, e.producto());
                texto(r, 2, e.agricultor());
                r.createCell(3).setCellValue(e.clicsWhatsapp());
                r.createCell(4).setCellValue(e.contactos());
                r.createCell(5).setCellValue(e.ofertas());
                r.createCell(6).setCellValue(e.acuerdos());
                r.createCell(7).setCellValue(e.completadas());
            }

            hoja = hoja(libro, HOJAS.get(3), COLUMNAS_USUARIOS, estilos);
            fila = 1;
            for (UsuarioAdminDto u : usuarios) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(u.nombre());
                r.createCell(1).setCellValue(ROLES.getOrDefault(u.rol(), u.rol()));
                texto(r, 2, u.negocio() != null ? u.negocio() : u.chacra());
                r.createCell(3).setCellValue(u.celular());
                r.createCell(4).setCellValue(u.distrito());
                r.createCell(5).setCellValue(ESTADOS.getOrDefault(u.estado(), u.estado()));
                texto(r, 6, u.motivoSuspension());
                r.createCell(7).setCellValue(u.operacionesCompletadas());
                numero(r, 8, u.calificacion(), null);
                r.createCell(9).setCellValue(u.totalCalificaciones());
                r.createCell(10).setCellValue(u.reclamosFundados());
                fechaHora(r, 11, u.fechaRegistro(), estilos.fechaHora());
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del panel", e);
        }
    }

    private static void resumen(Sheet hoja, TarjetasPanelDto t, Estilos estilos) {
        Object[][] filas = {
            {"Indicador", "Valor"},
            {"Publicaciones activas", t.publicacionesActivas()},
            {"Publicadas en los últimos 7 días", t.publicadasUltimos7Dias()},
            {"Por revisar", t.porRevisar()},
            {"La más antigua por revisar", t.porRevisarMasAntigua()},
            {"Conformes hoy", t.conformesHoy()},
            {"Ocultadas hoy", t.bloqueadasHoy()},
            {"Precios cargados hoy", t.preciosCargadosHoy()},
            {"Precios esperados hoy", t.preciosEsperadosHoy()},
            {"Reclamos pendientes", t.reclamosPendientes()},
        };
        for (int i = 0; i < filas.length; i++) {
            Row r = hoja.createRow(i);
            Cell etiqueta = r.createCell(0);
            etiqueta.setCellValue((String) filas[i][0]);
            Object valor = filas[i][1];
            if (i == 0) {
                etiqueta.setCellStyle(estilos.titulo());
                Cell titulo = r.createCell(1);
                titulo.setCellValue((String) valor);
                titulo.setCellStyle(estilos.titulo());
            } else if (valor instanceof Long numero) {
                r.createCell(1).setCellValue(numero);
            } else {
                fechaHora(r, 1, (LocalDateTime) valor, estilos.fechaHora());
            }
        }
        hoja.setColumnWidth(0, 36 * 256);
        hoja.setColumnWidth(1, 18 * 256);
    }

    private static Estilos estilos(Workbook libro) {
        CreationHelper ayuda = libro.getCreationHelper();
        CellStyle titulo = libro.createCellStyle();
        Font negrita = libro.createFont();
        negrita.setBold(true);
        titulo.setFont(negrita);
        CellStyle fechaHora = libro.createCellStyle();
        fechaHora.setDataFormat(ayuda.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        CellStyle decimal = libro.createCellStyle();
        decimal.setDataFormat(ayuda.createDataFormat().getFormat("#,##0.00"));
        return new Estilos(titulo, fechaHora, decimal);
    }

    private static Sheet hoja(Workbook libro, String nombre, List<String> columnas, Estilos estilos) {
        Sheet hoja = libro.createSheet(nombre);
        Row cabecera = hoja.createRow(0);
        for (int i = 0; i < columnas.size(); i++) {
            Cell celda = cabecera.createCell(i);
            celda.setCellValue(columnas.get(i));
            celda.setCellStyle(estilos.titulo());
            // Ancho fijo: autoSizeColumn necesita las fuentes del sistema (falla en servidores sin ellas)
            hoja.setColumnWidth(i, Math.max(14, columnas.get(i).length() + 4) * 256);
        }
        hoja.createFreezePane(0, 1);
        return hoja;
    }

    private static void texto(Row fila, int columna, String valor) {
        if (valor != null) {
            fila.createCell(columna).setCellValue(valor);
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

    private static void fechaHora(Row fila, int columna, LocalDateTime valor, CellStyle estilo) {
        if (valor != null) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(valor);
            celda.setCellStyle(estilo);
        }
    }
}
