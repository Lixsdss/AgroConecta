package pe.agroconecta.precio.aplicacion;

import java.util.List;

/**
 * Resultado de cargar un archivo de precios: filas importadas y filas rechazadas con su motivo (UC-02 A2).
 *
 * @param importadas número de precios guardados
 * @param rechazadas filas que no se guardaron
 */
public record InformeImportacionDto(int importadas, List<FilaRechazadaDto> rechazadas) {

    /**
     * Fila del archivo que no se guardó.
     *
     * @param fila   número de fila en el archivo (la cabecera es la 1)
     * @param motivo motivo en español
     */
    public record FilaRechazadaDto(int fila, String motivo) {
    }
}
