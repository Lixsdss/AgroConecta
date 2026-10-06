package pe.agroconecta.publicacion.aplicacion;

import org.springframework.data.domain.Sort;

/**
 * Orden de los resultados de la búsqueda (RF-03). Cada orden termina en el id para que la paginación no repita
 * ni salte resultados.
 */
public enum OrdenBusqueda {

    /** Menor precio por kilo primero; empate: la publicación más reciente. */
    PRECIO_KG(Sort.by(Sort.Order.asc("precioKg"), Sort.Order.desc("fechaPublicacion"), Sort.Order.desc("id"))),

    /** Cosecha más reciente primero; empate: menor precio por kilo. */
    FECHA_COSECHA(Sort.by(Sort.Order.desc("fechaCosecha"), Sort.Order.asc("precioKg"), Sort.Order.desc("id"))),

    /**
     * Mejor calificado primero; empate: menor precio por kilo. En MySQL y MariaDB el orden descendente deja al
     * final los valores {@code NULL}, es decir, a los agricultores sin calificaciones.
     */
    CALIFICACION(Sort.by(Sort.Order.desc("calificacionAgricultor"), Sort.Order.asc("precioKg"),
            Sort.Order.desc("id")));

    private final Sort orden;

    OrdenBusqueda(Sort orden) {
        this.orden = orden;
    }

    /** @return orden de Spring Data sobre los atributos de la vista */
    public Sort comoSort() {
        return orden;
    }
}
