package pe.agroconecta.administracion.dominio;

/**
 * Decisión del administrador sobre una publicación ({@code chk_revision_decision}, moderación posterior RF-09).
 */
public enum DecisionModeracion {
    /** La publicación está bien; sale de la cola. */
    CONFORME,
    /** Sigue visible, pero se avisa al agricultor con motivo y mensaje. */
    ADVERTENCIA,
    /** Se oculta (BLOQUEADA); exige motivo y mensaje. */
    BLOQUEO,
    /** Se vuelve a mostrar una publicación oculta. */
    DESBLOQUEO;

    /**
     * Indica si la decisión exige motivo de catálogo y mensaje al agricultor ({@code chk_revision_coherencia}).
     *
     * @return {@code true} en ADVERTENCIA y BLOQUEO
     */
    public boolean exigeMotivo() {
        return this == ADVERTENCIA || this == BLOQUEO;
    }
}
