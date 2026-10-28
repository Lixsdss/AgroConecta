package pe.agroconecta.administracion.aplicacion;

import pe.agroconecta.administracion.dominio.DecisionModeracion;

/**
 * Resultado de registrar una decisión de moderación.
 *
 * @param idRevision        id de la revisión registrada
 * @param idPublicacion     id de la publicación
 * @param decision          decisión
 * @param estadoPublicacion estado de la publicación después de la decisión
 */
public record DecisionRegistradaDto(Long idRevision, Long idPublicacion, DecisionModeracion decision,
        String estadoPublicacion) {
}
