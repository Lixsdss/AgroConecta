-- =====================================================================
-- AgroConecta - V3: "Mantener oculta" una publicación corregida (slice 11, RF-09)
-- Aprobado el 2026-10-09. Antes, una publicación BLOQUEADA que el agricultor corregía (marca CORREGIDA)
-- solo podía salir de la cola "Por revisar" con DESBLOQUEO (publicarla). Ahora el administrador también puede
-- registrar BLOQUEO sobre ella: sigue BLOQUEADA, trg_revisiones_ai borra la marca y avisa al agricultor.
-- El resto de las reglas de trg_revisiones_bi no cambia. modelo-bd.sql tiene la misma definición.
-- =====================================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TRIGGER IF EXISTS trg_revisiones_bi;

DELIMITER $$

CREATE TRIGGER trg_revisiones_bi BEFORE INSERT ON revisiones_publicacion
FOR EACH ROW
BEGIN
    DECLARE v_estado VARCHAR(12);
    DECLARE v_marca  VARCHAR(20);

    IF fn_rol_usuario(NEW.id_administrador) <> 'ADMINISTRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un ADMINISTRADOR revisa publicaciones';
    END IF;
    SELECT estado, marca_revision INTO v_estado, v_marca
    FROM publicaciones WHERE id_publicacion = NEW.id_publicacion;

    -- V3: BLOQUEO sobre una BLOQUEADA corregida ("Mantener oculta")
    IF NEW.decision IN ('CONFORME', 'ADVERTENCIA', 'BLOQUEO') AND v_estado <> 'PUBLICADA'
       AND NOT (NEW.decision = 'BLOQUEO' AND v_estado = 'BLOQUEADA' AND v_marca = 'CORREGIDA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se revisa, advierte o bloquea una publicacion PUBLICADA';
    END IF;
    IF NEW.decision = 'DESBLOQUEO' AND v_estado <> 'BLOQUEADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se desbloquea una publicacion BLOQUEADA';
    END IF;
END$$

DELIMITER ;
