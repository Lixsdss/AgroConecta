-- =====================================================================
-- AgroConecta - V1: esquema inicial (modelo de datos v4)
-- Generado desde docs/04-diseno/base-datos/modelo-bd.sql, secciones 2 a 11
-- (catalogos, actores, oferta y precios, interaccion comercial, seguimiento,
-- administracion, indices, funciones, procedimiento, triggers, evento y vistas).
-- Sin DROP/CREATE DATABASE ni USE: Flyway trabaja sobre la BD del datasource.
-- modelo-bd.sql es la fuente de verdad; no editar esta migracion una vez aplicada.
-- =====================================================================

-- Fija la collation de la sesion: los triggers y funciones guardan la de su
-- creacion; si difiere de la de las tablas, las comparaciones de texto fallan.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. CATALOGOS
--    Listas fijas definidas por reglas de negocio (roles, estados, motivos,
--    fuentes) van como CHECK, no como tablas: menos relaciones en el modelo.
--    Solo son tablas los catalogos que el administrador amplia o que tienen
--    atributos propios (distritos, cultivos, variedades, unidades).
-- ---------------------------------------------------------------------
CREATE TABLE provincias (
    id_provincia INT         NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(60) NOT NULL,
    PRIMARY KEY (id_provincia),
    UNIQUE KEY uk_provincia_nombre (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE distritos (
    id_distrito    INT         NOT NULL AUTO_INCREMENT,
    id_provincia   INT         NOT NULL,
    nombre         VARCHAR(60) NOT NULL,
    ubigeo         CHAR(6)     NOT NULL,
    es_zona_precio BOOLEAN     NOT NULL DEFAULT FALSE COMMENT 'Tiene precio de referencia diario (RF-02)',
    PRIMARY KEY (id_distrito),
    UNIQUE KEY uk_distrito_provincia_nombre (id_provincia, nombre),
    UNIQUE KEY uk_distrito_ubigeo (ubigeo),
    CONSTRAINT chk_distrito_ubigeo CHECK (ubigeo REGEXP '^20[0-9]{4}$'),  -- solo region Piura
    CONSTRAINT fk_distrito_provincia FOREIGN KEY (id_provincia)
        REFERENCES provincias (id_provincia) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE unidades_medida (
    id_unidad     INT           NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(30)   NOT NULL,
    nombre_plural VARCHAR(30)   NOT NULL,
    factor_kg     DECIMAL(10,3) NULL COMMENT 'NULL = peso variable (jaba, saco): lo indica el agricultor',
    PRIMARY KEY (id_unidad),
    UNIQUE KEY uk_unidad_nombre (nombre),
    CONSTRAINT chk_unidad_factor CHECK (factor_kg IS NULL OR factor_kg > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cultivos (
    id_cultivo       INT           NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(60)   NOT NULL,
    vida_util_dias   SMALLINT      NOT NULL COMMENT 'Sugiere la fecha de vencimiento (RF-07)',
    id_unidad_local  INT           NOT NULL COMMENT 'Unidad en que se comercializa en Piura (RF-02)',
    peso_sugerido_kg DECIMAL(10,3) NULL COMMENT 'Peso tipico si la unidad local es variable',
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_cultivo),
    UNIQUE KEY uk_cultivo_nombre (nombre),
    CONSTRAINT chk_cultivo_vida_util CHECK (vida_util_dias > 0),
    CONSTRAINT chk_cultivo_peso CHECK (peso_sugerido_kg IS NULL OR peso_sugerido_kg > 0),
    CONSTRAINT fk_cultivo_unidad_local FOREIGN KEY (id_unidad_local)
        REFERENCES unidades_medida (id_unidad) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF-10: la venta agrupada consolida por cultivo, variedad y zona
CREATE TABLE variedades (
    id_variedad INT         NOT NULL AUTO_INCREMENT,
    id_cultivo  INT         NOT NULL,
    nombre      VARCHAR(60) NOT NULL,
    PRIMARY KEY (id_variedad),
    UNIQUE KEY uk_variedad_cultivo_nombre (id_cultivo, nombre),
    CONSTRAINT fk_variedad_cultivo FOREIGN KEY (id_cultivo)
        REFERENCES cultivos (id_cultivo) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. ACTORES (Agricultor, Comprador, Administrador) - RF-08
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario      BIGINT       NOT NULL AUTO_INCREMENT,
    rol             VARCHAR(13)  NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    celular         CHAR(9)      NOT NULL COMMENT 'Usuario de inicio de sesion',
    password_hash   VARCHAR(255) NOT NULL COMMENT 'Hash BCrypt. Nunca texto plano',
    id_distrito     INT          NOT NULL,
    estado          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO'
        COMMENT 'Lo actualiza la tabla suspensiones (redundancia controlada para el login)',
    fecha_registro  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso   DATETIME     NULL,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuario_celular (celular),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('AGRICULTOR', 'COMPRADOR', 'ADMINISTRADOR')),
    CONSTRAINT chk_usuario_celular CHECK (celular REGEXP '^9[0-9]{8}$'),
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'INACTIVO')),
    CONSTRAINT fk_usuario_distrito FOREIGN KEY (id_distrito)
        REFERENCES distritos (id_distrito) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE agricultores (
    id_usuario    BIGINT      NOT NULL,
    nombre_chacra VARCHAR(80) NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_agricultor_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE compradores (
    id_usuario     BIGINT       NOT NULL,
    tipo_comprador VARCHAR(20)  NOT NULL,
    nombre_negocio VARCHAR(120) NOT NULL COMMENT 'Ej.: Restaurante El Chalan; el contacto es usuarios.nombre_completo',
    ruc            CHAR(11)     NULL,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_comprador_ruc (ruc),
    CONSTRAINT chk_comprador_tipo CHECK (tipo_comprador IN
        ('MERCADO_MAYORISTA', 'RESTAURANTE', 'HOTEL', 'PROCESADORA', 'MOLINO', 'OTRO')),
    CONSTRAINT chk_comprador_ruc CHECK (ruc IS NULL OR ruc REGEXP '^[12][0-9]{10}$'),
    CONSTRAINT fk_comprador_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. OFERTA Y PRECIOS - RF-01, RF-02, RF-07, RF-09
-- ---------------------------------------------------------------------
CREATE TABLE publicaciones (
    id_publicacion      BIGINT        NOT NULL AUTO_INCREMENT,
    id_agricultor       BIGINT        NOT NULL,
    id_variedad         INT           NOT NULL COMMENT 'El cultivo se obtiene de la variedad (3FN)',
    id_distrito         INT           NOT NULL COMMENT 'Donde esta la cosecha; puede diferir del distrito del agricultor',
    id_unidad           INT           NOT NULL COMMENT 'Unidad elegida por el agricultor',
    cantidad            DECIMAL(12,2) NOT NULL,
    peso_unidad_kg      DECIMAL(10,3) NOT NULL COMMENT 'Unidad fija: factor del catalogo. Variable: lo indica el agricultor',
    precio_unitario     DECIMAL(10,2) NOT NULL COMMENT 'Soles por unidad elegida',
    cantidad_kg         DECIMAL(14,2) GENERATED ALWAYS AS (ROUND(cantidad * peso_unidad_kg, 2)) STORED,
    precio_kg           DECIMAL(10,4) GENERATED ALWAYS AS (ROUND(precio_unitario / NULLIF(peso_unidad_kg, 0), 4)) STORED,
    fecha_cosecha       DATE          NOT NULL,
    fecha_limite        DATE          NOT NULL COMMENT 'Vencimiento (RF-07). Si llega NULL se calcula',
    estado              VARCHAR(12)   NOT NULL DEFAULT 'EN_REVISION',
    marca_revision      VARCHAR(20)   NULL COMMENT 'Alerta del sistema para el moderador',
    fecha_publicacion   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_publicacion),
    CONSTRAINT chk_publicacion_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_publicacion_peso CHECK (peso_unidad_kg > 0),
    CONSTRAINT chk_publicacion_precio CHECK (precio_unitario > 0),
    CONSTRAINT chk_publicacion_fechas CHECK (fecha_limite >= fecha_cosecha),
    CONSTRAINT chk_publicacion_estado CHECK (estado IN
        ('EN_REVISION', 'PUBLICADA', 'RECHAZADA', 'VENDIDA', 'VENCIDA', 'RETIRADA')),
    CONSTRAINT chk_publicacion_marca CHECK (marca_revision IS NULL OR marca_revision IN
        ('PRIMERA_PUBLICACION', 'PRECIO_FUERA_RANGO', 'CANTIDAD_INUSUAL')),
    CONSTRAINT fk_publicacion_agricultor FOREIGN KEY (id_agricultor)
        REFERENCES agricultores (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_publicacion_variedad FOREIGN KEY (id_variedad)
        REFERENCES variedades (id_variedad) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_publicacion_distrito FOREIGN KEY (id_distrito)
        REFERENCES distritos (id_distrito) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_publicacion_unidad FOREIGN KEY (id_unidad)
        REFERENCES unidades_medida (id_unidad) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF-09: cada decision del moderador (una publicacion corregida puede revisarse varias veces)
CREATE TABLE revisiones_publicacion (
    id_revision      BIGINT       NOT NULL AUTO_INCREMENT,
    id_publicacion   BIGINT       NOT NULL,
    id_administrador BIGINT       NOT NULL,
    decision         VARCHAR(10)  NOT NULL,
    motivo_rechazo   VARCHAR(25)  NULL,
    mensaje          VARCHAR(300) NULL COMMENT 'Texto que ve el agricultor en su celular',
    fecha            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_revision),
    CONSTRAINT chk_revision_decision CHECK (decision IN ('APROBADA', 'RECHAZADA')),
    CONSTRAINT chk_revision_motivo CHECK (motivo_rechazo IS NULL OR motivo_rechazo IN
        ('PRECIO_NO_COINCIDE', 'CANTIDAD_INCORRECTA', 'PESO_UNIDAD_INCORRECTO', 'DATOS_INCORRECTOS', 'OTRO')),
    CONSTRAINT chk_revision_coherencia CHECK (
        (decision = 'APROBADA' AND motivo_rechazo IS NULL)
        OR (decision = 'RECHAZADA' AND motivo_rechazo IS NOT NULL AND mensaje IS NOT NULL)
    ),
    CONSTRAINT fk_revision_publicacion FOREIGN KEY (id_publicacion)
        REFERENCES publicaciones (id_publicacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_revision_administrador FOREIGN KEY (id_administrador)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF-02: precio de referencia del mercado real, siempre en soles por kilo.
-- El precio en unidad local (jaba, saco) y la tendencia se calculan (v_precios_vigentes).
-- El rango de la plataforma sale de ventas completadas (v_rango_precio_plataforma).
CREATE TABLE precios_mercado (
    id_precio           BIGINT        NOT NULL AUTO_INCREMENT,
    id_cultivo          INT           NOT NULL,
    id_distrito         INT           NOT NULL COMMENT 'Zona de precio',
    fecha               DATE          NOT NULL,
    precio_kg           DECIMAL(10,2) NOT NULL,
    fuente              VARCHAR(25)   NOT NULL,
    metodo              VARCHAR(10)   NOT NULL DEFAULT 'FORMULARIO',
    id_administrador    BIGINT        NOT NULL,
    fecha_registro      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_precio),
    UNIQUE KEY uk_precio_cultivo_zona_fecha (id_cultivo, id_distrito, fecha),
    CONSTRAINT chk_precio_valor CHECK (precio_kg > 0),
    CONSTRAINT chk_precio_fuente CHECK (fuente IN
        ('MERCADO_MAYORISTA_PIURA', 'MERCADO_SULLANA', 'ENCUESTA_ACOPIADORES')),
    CONSTRAINT chk_precio_metodo CHECK (metodo IN ('FORMULARIO', 'ARCHIVO')),
    CONSTRAINT fk_precio_cultivo FOREIGN KEY (id_cultivo)
        REFERENCES cultivos (id_cultivo) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_precio_distrito FOREIGN KEY (id_distrito)
        REFERENCES distritos (id_distrito) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_precio_administrador FOREIGN KEY (id_administrador)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5. INTERACCION COMERCIAL - RF-04, RF-05, RF-06, RF-10
-- ---------------------------------------------------------------------
-- Una conversacion = una publicacion + un comprador. Es el eje comercial:
-- mensajes, clics de WhatsApp y ventas cuelgan de ella.
CREATE TABLE conversaciones (
    id_conversacion BIGINT      NOT NULL AUTO_INCREMENT,
    id_publicacion  BIGINT      NOT NULL,
    id_comprador    BIGINT      NOT NULL,
    canal_inicio    VARCHAR(18) NOT NULL DEFAULT 'CHAT',
    clics_whatsapp  INT         NOT NULL DEFAULT 0 COMMENT 'RF-04: eventos de interes por WhatsApp',
    fecha_inicio    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_conversacion),
    UNIQUE KEY uk_conversacion_pub_comprador (id_publicacion, id_comprador),
    CONSTRAINT chk_conversacion_canal CHECK (canal_inicio IN ('CHAT', 'WHATSAPP', 'PROPUESTA_CONJUNTA')),
    CONSTRAINT chk_conversacion_clics CHECK (clics_whatsapp >= 0),
    CONSTRAINT fk_conversacion_publicacion FOREIGN KEY (id_publicacion)
        REFERENCES publicaciones (id_publicacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_conversacion_comprador FOREIGN KEY (id_comprador)
        REFERENCES compradores (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- La oferta del chat se expresa en la unidad de la publicacion
CREATE TABLE mensajes (
    id_mensaje             BIGINT        NOT NULL AUTO_INCREMENT,
    id_conversacion        BIGINT        NOT NULL,
    id_remitente           BIGINT        NOT NULL,
    tipo                   VARCHAR(6)    NOT NULL DEFAULT 'TEXTO',
    contenido              VARCHAR(1000) NULL,
    oferta_cantidad        DECIMAL(12,2) NULL,
    oferta_precio_unitario DECIMAL(10,2) NULL,
    leido                  BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_envio            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_mensaje),
    CONSTRAINT chk_mensaje_tipo CHECK (
        (tipo = 'TEXTO' AND contenido IS NOT NULL
            AND oferta_cantidad IS NULL AND oferta_precio_unitario IS NULL)
        OR (tipo = 'OFERTA' AND oferta_cantidad IS NOT NULL AND oferta_precio_unitario IS NOT NULL
            AND oferta_cantidad > 0 AND oferta_precio_unitario > 0)
    ),
    CONSTRAINT fk_mensaje_conversacion FOREIGN KEY (id_conversacion)
        REFERENCES conversaciones (id_conversacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_mensaje_remitente FOREIGN KEY (id_remitente)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF-10: propuesta conjunta del comprador a un lote (cultivo + variedad + zona), en kg.
-- Cada agricultor la recibe como una transaccion PROPUESTA en su propia unidad.
CREATE TABLE propuestas_grupales (
    id_propuesta_grupal BIGINT        NOT NULL AUTO_INCREMENT,
    id_comprador        BIGINT        NOT NULL,
    id_variedad         INT           NOT NULL,
    id_distrito         INT           NOT NULL,
    cantidad_kg         DECIMAL(12,2) NOT NULL,
    precio_kg           DECIMAL(10,2) NOT NULL,
    fecha_recojo        DATE          NOT NULL COMMENT 'Tambien es el limite para responder',
    estado              VARCHAR(10)   NOT NULL DEFAULT 'ABIERTA',
    fecha_creacion      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_propuesta_grupal),
    CONSTRAINT chk_propuesta_cantidad CHECK (cantidad_kg > 0),
    CONSTRAINT chk_propuesta_precio CHECK (precio_kg > 0),
    CONSTRAINT chk_propuesta_fecha CHECK (fecha_recojo >= DATE(fecha_creacion)),
    CONSTRAINT chk_propuesta_estado CHECK (estado IN ('ABIERTA', 'CERRADA', 'CANCELADA')),
    CONSTRAINT fk_propuesta_comprador FOREIGN KEY (id_comprador)
        REFERENCES compradores (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_propuesta_variedad FOREIGN KEY (id_variedad)
        REFERENCES variedades (id_variedad) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_propuesta_distrito FOREIGN KEY (id_distrito)
        REFERENCES distritos (id_distrito) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Acuerdo comercial. No registra pagos (se hacen fuera de la plataforma).
-- Acuerdo del chat:   ACORDADA -> RECOGIDO -> EN_CAMINO -> ENTREGADO -> COMPLETADA
-- Venta agrupada:     PROPUESTA -> ACORDADA (sigue igual) | RECHAZADA | CANCELADA
-- ACORDADA puede pasar a CANCELADA. La unidad acordada puede diferir de la publicacion.
CREATE TABLE transacciones (
    id_transaccion      BIGINT        NOT NULL AUTO_INCREMENT,
    id_conversacion     BIGINT        NOT NULL COMMENT 'Da la publicacion (agricultor) y el comprador',
    id_propuesta_grupal BIGINT        NULL COMMENT 'Solo si nace de una venta agrupada (RF-10)',
    id_unidad           INT           NOT NULL COMMENT 'Unidad acordada',
    cantidad            DECIMAL(12,2) NOT NULL,
    peso_unidad_kg      DECIMAL(10,3) NOT NULL,
    precio_unitario     DECIMAL(10,2) NOT NULL,
    cantidad_kg         DECIMAL(14,2) GENERATED ALWAYS AS (ROUND(cantidad * peso_unidad_kg, 2)) STORED,
    monto_total         DECIMAL(14,2) GENERATED ALWAYS AS (ROUND(cantidad * precio_unitario, 2)) STORED,
    fecha_recojo        DATE          NOT NULL,
    estado              VARCHAR(12)   NOT NULL,
    id_actualizado_por  BIGINT        NULL COMMENT 'Autor del ultimo cambio; NULL = proceso automatico',
    fecha_creacion      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_transaccion),
    UNIQUE KEY uk_transaccion_propuesta_conv (id_propuesta_grupal, id_conversacion),
    CONSTRAINT chk_transaccion_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_transaccion_peso CHECK (peso_unidad_kg > 0),
    CONSTRAINT chk_transaccion_precio CHECK (precio_unitario > 0),
    CONSTRAINT chk_transaccion_estado CHECK (estado IN ('PROPUESTA', 'ACORDADA', 'RECOGIDO',
        'EN_CAMINO', 'ENTREGADO', 'COMPLETADA', 'RECHAZADA', 'CANCELADA')),
    CONSTRAINT fk_transaccion_conversacion FOREIGN KEY (id_conversacion)
        REFERENCES conversaciones (id_conversacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaccion_propuesta FOREIGN KEY (id_propuesta_grupal)
        REFERENCES propuestas_grupales (id_propuesta_grupal) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaccion_unidad FOREIGN KEY (id_unidad)
        REFERENCES unidades_medida (id_unidad) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaccion_actualizado_por FOREIGN KEY (id_actualizado_por)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE calificaciones (
    id_calificacion BIGINT       NOT NULL AUTO_INCREMENT,
    id_transaccion  BIGINT       NOT NULL,
    id_evaluador    BIGINT       NOT NULL COMMENT 'El evaluado es la contraparte (se deriva, 3FN)',
    puntuacion      TINYINT      NOT NULL,
    comentario      VARCHAR(500) NULL,
    fecha           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_calificacion),
    UNIQUE KEY uk_calificacion_trans_evaluador (id_transaccion, id_evaluador),
    CONSTRAINT chk_calificacion_puntuacion CHECK (puntuacion BETWEEN 1 AND 5),
    CONSTRAINT fk_calificacion_transaccion FOREIGN KEY (id_transaccion)
        REFERENCES transacciones (id_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_calificacion_evaluador FOREIGN KEY (id_evaluador)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 6. SEGUIMIENTO DE LA VENTA - RF-06, RF-11
--    Linea de tiempo: cada cambio de estado de la transaccion con su fecha y autor.
--    La llenan los triggers; la aplicacion nunca inserta aqui.
-- ---------------------------------------------------------------------
CREATE TABLE transacciones_historial (
    id_historial   BIGINT      NOT NULL AUTO_INCREMENT,
    id_transaccion BIGINT      NOT NULL,
    estado         VARCHAR(12) NOT NULL,
    id_usuario     BIGINT      NULL COMMENT 'NULL = proceso automatico',
    fecha          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_historial),
    CONSTRAINT chk_historial_estado CHECK (estado IN ('PROPUESTA', 'ACORDADA', 'RECOGIDO',
        'EN_CAMINO', 'ENTREGADO', 'COMPLETADA', 'RECHAZADA', 'CANCELADA')),
    CONSTRAINT fk_historial_transaccion FOREIGN KEY (id_transaccion)
        REFERENCES transacciones (id_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 7. ADMINISTRACION - RF-07, RF-09
-- ---------------------------------------------------------------------
-- Reclamo de una parte contra la otra sobre una venta acordada.
-- El reclamado es la contraparte (se deriva). El administrador lo resuelve.
CREATE TABLE reclamos (
    id_reclamo       BIGINT       NOT NULL AUTO_INCREMENT,
    id_transaccion   BIGINT       NOT NULL,
    id_reclamante    BIGINT       NOT NULL,
    motivo           VARCHAR(25)  NOT NULL,
    descripcion      VARCHAR(300) NOT NULL,
    estado           VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE',
    id_administrador BIGINT       NULL,
    respuesta        VARCHAR(300) NULL,
    fecha_registro   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion DATETIME     NULL,
    PRIMARY KEY (id_reclamo),
    UNIQUE KEY uk_reclamo_trans_reclamante (id_transaccion, id_reclamante),
    CONSTRAINT chk_reclamo_motivo CHECK (motivo IN ('NO_SE_PRESENTO', 'PRODUCTO_NO_COINCIDE',
        'CANTIDAD_INCOMPLETA', 'INCUMPLIO_PAGO', 'MAL_TRATO')),
    CONSTRAINT chk_reclamo_estado CHECK (estado IN ('PENDIENTE', 'FUNDADO', 'INFUNDADO')),
    CONSTRAINT chk_reclamo_resolucion CHECK (
        (estado = 'PENDIENTE' AND id_administrador IS NULL AND fecha_resolucion IS NULL)
        OR (estado IN ('FUNDADO', 'INFUNDADO') AND id_administrador IS NOT NULL
            AND respuesta IS NOT NULL AND fecha_resolucion IS NOT NULL)
    ),
    CONSTRAINT fk_reclamo_transaccion FOREIGN KEY (id_transaccion)
        REFERENCES transacciones (id_transaccion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reclamo_reclamante FOREIGN KEY (id_reclamante)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reclamo_administrador FOREIGN KEY (id_administrador)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Suspension justificada: motivo de catalogo + detalle obligatorio.
-- RECLAMOS_FUNDADOS exige al menos 3 reclamos fundados contra el usuario.
CREATE TABLE suspensiones (
    id_suspension    BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario       BIGINT       NOT NULL,
    id_administrador BIGINT       NOT NULL,
    motivo           VARCHAR(25)  NOT NULL,
    detalle          VARCHAR(300) NOT NULL,
    fecha_inicio     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin        DATETIME     NULL COMMENT 'Fecha de reactivacion',
    PRIMARY KEY (id_suspension),
    CONSTRAINT chk_suspension_motivo CHECK (motivo IN ('RECLAMOS_FUNDADOS', 'DATOS_FALSOS',
        'PUBLICACIONES_ENGANOSAS', 'USO_INDEBIDO')),
    CONSTRAINT chk_suspension_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT fk_suspension_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_suspension_administrador FOREIGN KEY (id_administrador)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- id_referencia sin FK a proposito: segun el tipo apunta a una publicacion,
-- transaccion, conversacion o propuesta. La notificacion es un aviso, no un
-- dato de negocio; poner 4 FK opcionales solo agregaria lineas al modelo.
CREATE TABLE notificaciones (
    id_notificacion BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario      BIGINT       NOT NULL,
    tipo            VARCHAR(22)  NOT NULL,
    titulo          VARCHAR(120) NOT NULL,
    contenido       VARCHAR(300) NOT NULL,
    id_referencia   BIGINT       NULL,
    leida           BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_notificacion),
    CONSTRAINT chk_notificacion_tipo CHECK (tipo IN ('POR_VENCER', 'OFERTA', 'MENSAJE',
        'PUBLICACION_APROBADA', 'PUBLICACION_RECHAZADA', 'PROPUESTA_CONJUNTA', 'VENTA',
        'CALIFICACION', 'RECLAMO', 'CUENTA')),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 8. INDICES ADICIONALES (InnoDB ya indexa cada FK)
-- ---------------------------------------------------------------------
CREATE INDEX idx_publicacion_busqueda   ON publicaciones (estado, id_variedad, id_distrito, fecha_cosecha); -- RF-03
CREATE INDEX idx_publicacion_precio_kg  ON publicaciones (estado, precio_kg);                  -- RF-03 orden por precio/kg
CREATE INDEX idx_publicacion_limite     ON publicaciones (estado, fecha_limite);               -- RF-07
CREATE INDEX idx_publicacion_agricultor ON publicaciones (id_agricultor, estado);              -- Mis ventas
CREATE INDEX idx_revision_fecha         ON revisiones_publicacion (decision, fecha);           -- RF-09
CREATE INDEX idx_precio_fecha           ON precios_mercado (fecha);                            -- RF-09 precios de hoy
CREATE INDEX idx_mensaje_conversacion   ON mensajes (id_conversacion, fecha_envio);            -- RF-04
CREATE INDEX idx_transaccion_estado     ON transacciones (id_conversacion, estado);            -- disponibilidad
CREATE INDEX idx_historial_transaccion  ON transacciones_historial (id_transaccion, fecha);     -- RF-11
CREATE INDEX idx_historial_estado_fecha ON transacciones_historial (estado, fecha);            -- RF-09 metricas
CREATE INDEX idx_reclamo_estado         ON reclamos (estado, fecha_registro);                  -- RF-09
CREATE INDEX idx_notificacion_usuario   ON notificaciones (id_usuario, leida, fecha_creacion); -- RF-07
CREATE INDEX idx_propuesta_estado       ON propuestas_grupales (estado, fecha_recojo);         -- RF-10

-- ---------------------------------------------------------------------
-- 9. FUNCIONES, PROCEDIMIENTO Y TRIGGERS
--    Reglas que cruzan tablas. Spring Boot tambien las valida (mensajes amigables);
--    la BD es la ultima barrera.
-- ---------------------------------------------------------------------
DELIMITER $$

CREATE FUNCTION fn_rol_usuario(p_usuario BIGINT)
RETURNS VARCHAR(13)
READS SQL DATA
BEGIN
    RETURN (SELECT rol FROM usuarios WHERE id_usuario = p_usuario);
END$$

-- Marca la publicacion VENDIDA cuando lo acordado cubre todo su volumen,
-- o la reabre si se cancela un acuerdo y vuelve a haber saldo.
CREATE PROCEDURE sp_sincronizar_publicacion(IN p_publicacion BIGINT)
MODIFIES SQL DATA
BEGIN
    DECLARE v_estado    VARCHAR(12);
    DECLARE v_kg        DECIMAL(14,2);
    DECLARE v_limite    DATE;
    DECLARE v_reservado DECIMAL(14,2);

    SELECT estado, cantidad_kg, fecha_limite INTO v_estado, v_kg, v_limite
    FROM publicaciones WHERE id_publicacion = p_publicacion;

    SELECT COALESCE(SUM(t.cantidad_kg), 0) INTO v_reservado
    FROM transacciones t
    JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
    WHERE c.id_publicacion = p_publicacion
      AND t.estado IN ('ACORDADA', 'RECOGIDO', 'EN_CAMINO', 'ENTREGADO', 'COMPLETADA');

    IF v_estado = 'PUBLICADA' AND v_reservado >= v_kg THEN
        UPDATE publicaciones SET estado = 'VENDIDA' WHERE id_publicacion = p_publicacion;
    ELSEIF v_estado = 'VENDIDA' AND v_reservado < v_kg THEN
        UPDATE publicaciones
        SET estado = IF(v_limite >= CURRENT_DATE, 'PUBLICADA', 'VENCIDA')
        WHERE id_publicacion = p_publicacion;
    END IF;
END$$

-- ----- Actores -----
CREATE TRIGGER trg_agricultores_bi BEFORE INSERT ON agricultores
FOR EACH ROW
BEGIN
    IF fn_rol_usuario(NEW.id_usuario) <> 'AGRICULTOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no tiene rol AGRICULTOR';
    END IF;
END$$

CREATE TRIGGER trg_compradores_bi BEFORE INSERT ON compradores
FOR EACH ROW
BEGIN
    IF fn_rol_usuario(NEW.id_usuario) <> 'COMPRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no tiene rol COMPRADOR';
    END IF;
END$$

-- ----- Publicacion (RF-01, RF-07) -----
CREATE TRIGGER trg_publicaciones_bi BEFORE INSERT ON publicaciones
FOR EACH ROW
BEGIN
    DECLARE v_factor DECIMAL(10,3);
    DECLARE v_dias   SMALLINT;

    IF NEW.estado <> 'EN_REVISION' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Toda publicacion inicia EN_REVISION';
    END IF;

    -- Unidad de peso fijo (kilo, quintal, tonelada): el peso lo pone el catalogo
    SELECT factor_kg INTO v_factor FROM unidades_medida WHERE id_unidad = NEW.id_unidad;
    IF v_factor IS NOT NULL THEN
        SET NEW.peso_unidad_kg = v_factor;
    ELSEIF NEW.peso_unidad_kg IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Indique cuantos kilos pesa cada jaba o saco';
    END IF;

    IF NEW.fecha_limite IS NULL THEN
        SELECT c.vida_util_dias INTO v_dias
        FROM variedades v JOIN cultivos c ON c.id_cultivo = v.id_cultivo
        WHERE v.id_variedad = NEW.id_variedad;
        SET NEW.fecha_limite = DATE_ADD(NEW.fecha_cosecha, INTERVAL v_dias DAY);
    END IF;

    IF NEW.fecha_limite < CURRENT_DATE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La fecha de vencimiento no puede ser anterior a hoy';
    END IF;

    -- RF-09: alerta para el moderador (las demas marcas las pone el service)
    IF NEW.marca_revision IS NULL
       AND NOT EXISTS (SELECT 1 FROM publicaciones WHERE id_agricultor = NEW.id_agricultor) THEN
        SET NEW.marca_revision = 'PRIMERA_PUBLICACION';
    END IF;
END$$

CREATE TRIGGER trg_publicaciones_bu BEFORE UPDATE ON publicaciones
FOR EACH ROW
BEGIN
    DECLARE v_factor    DECIMAL(10,3);
    DECLARE v_reservado DECIMAL(14,2);

    IF NEW.id_agricultor <> OLD.id_agricultor THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede cambiar el agricultor de una publicacion';
    END IF;

    SELECT factor_kg INTO v_factor FROM unidades_medida WHERE id_unidad = NEW.id_unidad;
    IF v_factor IS NOT NULL THEN
        SET NEW.peso_unidad_kg = v_factor;
    END IF;

    -- RF-07 "Extender": una publicacion VENCIDA vuelve a PUBLICADA con fecha valida
    IF OLD.estado = 'VENCIDA' AND NEW.estado = 'VENCIDA'
       AND NEW.fecha_limite > OLD.fecha_limite AND NEW.fecha_limite >= CURRENT_DATE THEN
        SET NEW.estado = 'PUBLICADA';
    END IF;

    IF NEW.fecha_limite <> OLD.fecha_limite AND NEW.fecha_limite < CURRENT_DATE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La nueva fecha de vencimiento no puede ser anterior a hoy';
    END IF;

    IF NEW.estado <> OLD.estado AND NOT (
           (OLD.estado = 'EN_REVISION' AND NEW.estado IN ('PUBLICADA', 'RECHAZADA', 'RETIRADA'))
        OR (OLD.estado = 'RECHAZADA'   AND NEW.estado IN ('EN_REVISION', 'RETIRADA'))
        OR (OLD.estado = 'PUBLICADA'   AND NEW.estado IN ('VENDIDA', 'VENCIDA', 'RETIRADA'))
        OR (OLD.estado = 'VENDIDA'     AND NEW.estado IN ('PUBLICADA', 'VENCIDA'))
        OR (OLD.estado = 'VENCIDA'     AND NEW.estado IN ('PUBLICADA', 'RETIRADA'))
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cambio de estado de publicacion no permitido';
    END IF;

    IF NEW.estado = 'PUBLICADA' AND NEW.fecha_limite < CURRENT_DATE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se publica una cosecha vencida; extienda la fecha';
    END IF;

    -- No se puede bajar la cantidad por debajo de lo ya acordado
    IF NEW.cantidad <> OLD.cantidad OR NEW.peso_unidad_kg <> OLD.peso_unidad_kg THEN
        SELECT COALESCE(SUM(t.cantidad_kg), 0) INTO v_reservado
        FROM transacciones t
        JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
        WHERE c.id_publicacion = NEW.id_publicacion
          AND t.estado IN ('ACORDADA', 'RECOGIDO', 'EN_CAMINO', 'ENTREGADO', 'COMPLETADA');
        IF NEW.cantidad * NEW.peso_unidad_kg < v_reservado THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cantidad no puede ser menor a lo ya acordado';
        END IF;
    END IF;
END$$

-- ----- Moderacion (RF-09) -----
CREATE TRIGGER trg_revisiones_bi BEFORE INSERT ON revisiones_publicacion
FOR EACH ROW
BEGIN
    IF fn_rol_usuario(NEW.id_administrador) <> 'ADMINISTRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un ADMINISTRADOR revisa publicaciones';
    END IF;
    IF (SELECT estado FROM publicaciones WHERE id_publicacion = NEW.id_publicacion) <> 'EN_REVISION' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La publicacion no esta EN_REVISION';
    END IF;
END$$

-- Aplica la decision y avisa al agricultor
CREATE TRIGGER trg_revisiones_ai AFTER INSERT ON revisiones_publicacion
FOR EACH ROW
BEGIN
    DECLARE v_agricultor BIGINT;
    DECLARE v_producto   VARCHAR(130);

    SELECT p.id_agricultor, LOWER(CONCAT(c.nombre, ' ', v.nombre))
      INTO v_agricultor, v_producto
    FROM publicaciones p
    JOIN variedades v ON v.id_variedad = p.id_variedad
    JOIN cultivos c   ON c.id_cultivo = v.id_cultivo
    WHERE p.id_publicacion = NEW.id_publicacion;

    UPDATE publicaciones
    SET estado = IF(NEW.decision = 'APROBADA', 'PUBLICADA', 'RECHAZADA')
    WHERE id_publicacion = NEW.id_publicacion;

    INSERT INTO notificaciones (id_usuario, tipo, titulo, contenido, id_referencia)
    VALUES (v_agricultor,
            IF(NEW.decision = 'APROBADA', 'PUBLICACION_APROBADA', 'PUBLICACION_RECHAZADA'),
            IF(NEW.decision = 'APROBADA',
               CONCAT('Su ', v_producto, ' fue aprobado. Ya está publicado.'),
               CONCAT('Su ', v_producto, ' no fue publicado.')),
            COALESCE(NEW.mensaje, 'Los compradores ya pueden ver su cosecha.'),
            NEW.id_publicacion);
END$$

-- ----- Precios de mercado (RF-02) -----
CREATE TRIGGER trg_precios_mercado_bi BEFORE INSERT ON precios_mercado
FOR EACH ROW
BEGIN
    IF fn_rol_usuario(NEW.id_administrador) <> 'ADMINISTRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un ADMINISTRADOR carga precios';
    END IF;
    IF NOT (SELECT es_zona_precio FROM distritos WHERE id_distrito = NEW.id_distrito) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El distrito no es una zona de precio';
    END IF;
END$$

CREATE TRIGGER trg_precios_mercado_bu BEFORE UPDATE ON precios_mercado
FOR EACH ROW
BEGIN
    IF fn_rol_usuario(NEW.id_administrador) <> 'ADMINISTRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un ADMINISTRADOR edita precios';
    END IF;
END$$

-- ----- Mensajeria (RF-04) -----
CREATE TRIGGER trg_conversaciones_bi BEFORE INSERT ON conversaciones
FOR EACH ROW
BEGIN
    IF (SELECT estado FROM publicaciones WHERE id_publicacion = NEW.id_publicacion) <> 'PUBLICADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se contacta una publicacion PUBLICADA';
    END IF;
END$$

CREATE TRIGGER trg_mensajes_bi BEFORE INSERT ON mensajes
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM conversaciones c
        JOIN publicaciones p ON p.id_publicacion = c.id_publicacion
        WHERE c.id_conversacion = NEW.id_conversacion
          AND NEW.id_remitente IN (c.id_comprador, p.id_agricultor)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El remitente no participa en la conversacion';
    END IF;
END$$

-- ----- Transaccion (RF-06, RF-10, RF-11) -----
CREATE TRIGGER trg_transacciones_bi BEFORE INSERT ON transacciones
FOR EACH ROW
BEGIN
    DECLARE v_id_pub       BIGINT;
    DECLARE v_estado_pub   VARCHAR(12);
    DECLARE v_pub_kg       DECIMAL(14,2);
    DECLARE v_pub_unidad   INT;
    DECLARE v_pub_peso     DECIMAL(10,3);
    DECLARE v_agricultor   BIGINT;
    DECLARE v_comprador    BIGINT;
    DECLARE v_variedad     INT;
    DECLARE v_distrito     INT;
    DECLARE v_factor       DECIMAL(10,3);
    DECLARE v_reservado    DECIMAL(14,2);
    DECLARE v_pg_comprador BIGINT;
    DECLARE v_pg_variedad  INT;
    DECLARE v_pg_distrito  INT;
    DECLARE v_pg_estado    VARCHAR(10);

    IF NEW.estado NOT IN ('PROPUESTA', 'ACORDADA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una venta inicia como PROPUESTA o ACORDADA';
    END IF;
    IF (NEW.estado = 'PROPUESTA') <> (NEW.id_propuesta_grupal IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'PROPUESTA es solo para venta agrupada; el acuerdo del chat inicia ACORDADA';
    END IF;

    SELECT p.id_publicacion, p.estado, p.cantidad_kg, p.id_unidad, p.peso_unidad_kg,
           p.id_agricultor, p.id_variedad, p.id_distrito, c.id_comprador
      INTO v_id_pub, v_estado_pub, v_pub_kg, v_pub_unidad, v_pub_peso,
           v_agricultor, v_variedad, v_distrito, v_comprador
    FROM conversaciones c
    JOIN publicaciones p ON p.id_publicacion = c.id_publicacion
    WHERE c.id_conversacion = NEW.id_conversacion;

    IF v_estado_pub <> 'PUBLICADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La publicacion no esta PUBLICADA';
    END IF;

    IF NEW.id_actualizado_por IS NULL OR NEW.id_actualizado_por NOT IN (v_comprador, v_agricultor) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Quien registra la venta debe ser parte de ella';
    END IF;

    -- Peso de la unidad acordada
    SELECT factor_kg INTO v_factor FROM unidades_medida WHERE id_unidad = NEW.id_unidad;
    IF v_factor IS NOT NULL THEN
        SET NEW.peso_unidad_kg = v_factor;
    ELSEIF NEW.peso_unidad_kg IS NULL AND NEW.id_unidad = v_pub_unidad THEN
        SET NEW.peso_unidad_kg = v_pub_peso;
    ELSEIF NEW.peso_unidad_kg IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Indique cuantos kilos pesa cada unidad acordada';
    END IF;

    -- Disponibilidad en kg (las unidades pueden diferir)
    SELECT COALESCE(SUM(t.cantidad_kg), 0) INTO v_reservado
    FROM transacciones t
    JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
    WHERE c.id_publicacion = v_id_pub
      AND t.estado IN ('ACORDADA', 'RECOGIDO', 'EN_CAMINO', 'ENTREGADO', 'COMPLETADA');

    IF NEW.cantidad * NEW.peso_unidad_kg > v_pub_kg - v_reservado THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cantidad supera lo disponible en la publicacion';
    END IF;

    -- RF-10: coherencia con la propuesta conjunta
    IF NEW.id_propuesta_grupal IS NOT NULL THEN
        SELECT id_comprador, id_variedad, id_distrito, estado
          INTO v_pg_comprador, v_pg_variedad, v_pg_distrito, v_pg_estado
        FROM propuestas_grupales
        WHERE id_propuesta_grupal = NEW.id_propuesta_grupal;

        IF v_pg_estado <> 'ABIERTA' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La propuesta conjunta no esta ABIERTA';
        END IF;
        IF v_pg_comprador <> v_comprador OR v_pg_variedad <> v_variedad OR v_pg_distrito <> v_distrito THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La publicacion no pertenece al lote de la propuesta (comprador, variedad o zona)';
        END IF;
    END IF;
END$$

-- id_actualizado_por: el service debe fijarlo en CADA update (autor del cambio)
CREATE TRIGGER trg_transacciones_bu BEFORE UPDATE ON transacciones
FOR EACH ROW
BEGIN
    DECLARE v_id_pub     BIGINT;
    DECLARE v_agricultor BIGINT;
    DECLARE v_comprador  BIGINT;
    DECLARE v_estado_pub VARCHAR(12);
    DECLARE v_pub_kg     DECIMAL(14,2);
    DECLARE v_reservado  DECIMAL(14,2);

    IF OLD.estado IN ('COMPLETADA', 'RECHAZADA', 'CANCELADA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una venta finalizada no se modifica';
    END IF;

    IF NEW.id_conversacion <> OLD.id_conversacion
       OR NOT (NEW.id_propuesta_grupal <=> OLD.id_propuesta_grupal) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se pueden cambiar las partes de la venta';
    END IF;

    IF NEW.id_unidad <> OLD.id_unidad OR NEW.cantidad <> OLD.cantidad
       OR NEW.peso_unidad_kg <> OLD.peso_unidad_kg OR NEW.precio_unitario <> OLD.precio_unitario THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Los terminos no se modifican; cancele y registre un nuevo acuerdo';
    END IF;

    IF NEW.estado <> OLD.estado THEN
        SELECT p.id_publicacion, p.id_agricultor, c.id_comprador
          INTO v_id_pub, v_agricultor, v_comprador
        FROM conversaciones c
        JOIN publicaciones p ON p.id_publicacion = c.id_publicacion
        WHERE c.id_conversacion = NEW.id_conversacion;

        IF NEW.id_actualizado_por IS NULL THEN
            IF NOT (OLD.estado = 'PROPUESTA' AND NEW.estado = 'CANCELADA') THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Sin usuario solo se cancelan propuestas vencidas (proceso automatico)';
            END IF;
        ELSEIF NEW.id_actualizado_por NOT IN (v_comprador, v_agricultor) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no es parte de la venta';
        ELSEIF NOT (
               (OLD.estado = 'PROPUESTA' AND NEW.estado IN ('ACORDADA', 'RECHAZADA')
                    AND NEW.id_actualizado_por = v_agricultor)
            OR (OLD.estado = 'PROPUESTA' AND NEW.estado = 'CANCELADA'
                    AND NEW.id_actualizado_por = v_comprador)
            OR (OLD.estado = 'ACORDADA'  AND NEW.estado IN ('RECOGIDO', 'CANCELADA'))
            OR (OLD.estado = 'RECOGIDO'  AND NEW.estado = 'EN_CAMINO')
            OR (OLD.estado = 'EN_CAMINO' AND NEW.estado = 'ENTREGADO'
                    AND NEW.id_actualizado_por = v_comprador)
            OR (OLD.estado = 'ENTREGADO' AND NEW.estado = 'COMPLETADA'
                    AND NEW.id_actualizado_por = v_comprador)
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cambio de estado no permitido para este usuario';
        END IF;

        -- El agricultor acepta su parte de la propuesta conjunta: se verifica saldo
        IF OLD.estado = 'PROPUESTA' AND NEW.estado = 'ACORDADA' THEN
            SELECT estado, cantidad_kg INTO v_estado_pub, v_pub_kg
            FROM publicaciones WHERE id_publicacion = v_id_pub;

            IF v_estado_pub <> 'PUBLICADA' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La publicacion no esta PUBLICADA';
            END IF;

            SELECT COALESCE(SUM(t.cantidad_kg), 0) INTO v_reservado
            FROM transacciones t
            JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
            WHERE c.id_publicacion = v_id_pub
              AND t.id_transaccion <> OLD.id_transaccion
              AND t.estado IN ('ACORDADA', 'RECOGIDO', 'EN_CAMINO', 'ENTREGADO', 'COMPLETADA');

            IF v_reservado + OLD.cantidad_kg > v_pub_kg THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cantidad supera lo disponible en la publicacion';
            END IF;
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_transacciones_ai AFTER INSERT ON transacciones
FOR EACH ROW
BEGIN
    DECLARE v_id_pub BIGINT;

    INSERT INTO transacciones_historial (id_transaccion, estado, id_usuario)
    VALUES (NEW.id_transaccion, NEW.estado, NEW.id_actualizado_por);

    IF NEW.estado = 'ACORDADA' THEN
        SELECT id_publicacion INTO v_id_pub FROM conversaciones WHERE id_conversacion = NEW.id_conversacion;
        CALL sp_sincronizar_publicacion(v_id_pub);
    END IF;
END$$

CREATE TRIGGER trg_transacciones_au AFTER UPDATE ON transacciones
FOR EACH ROW
BEGIN
    DECLARE v_id_pub BIGINT;

    IF NEW.estado <> OLD.estado THEN
        INSERT INTO transacciones_historial (id_transaccion, estado, id_usuario)
        VALUES (NEW.id_transaccion, NEW.estado, NEW.id_actualizado_por);

        SELECT id_publicacion INTO v_id_pub FROM conversaciones WHERE id_conversacion = NEW.id_conversacion;
        CALL sp_sincronizar_publicacion(v_id_pub);
    END IF;
END$$

-- ----- Calificacion (RF-05): solo ventas COMPLETADAS y solo las partes -----
CREATE TRIGGER trg_calificaciones_bi BEFORE INSERT ON calificaciones
FOR EACH ROW
BEGIN
    DECLARE v_estado     VARCHAR(12);
    DECLARE v_comprador  BIGINT;
    DECLARE v_agricultor BIGINT;

    SELECT t.estado, c.id_comprador, p.id_agricultor
      INTO v_estado, v_comprador, v_agricultor
    FROM transacciones t
    JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
    JOIN publicaciones p  ON p.id_publicacion = c.id_publicacion
    WHERE t.id_transaccion = NEW.id_transaccion;

    IF v_estado <> 'COMPLETADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se califican ventas COMPLETADAS';
    END IF;
    IF NEW.id_evaluador NOT IN (v_comprador, v_agricultor) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El evaluador no participa en la venta';
    END IF;
END$$

-- ----- Reclamos (RF-09) -----
CREATE TRIGGER trg_reclamos_bi BEFORE INSERT ON reclamos
FOR EACH ROW
BEGIN
    DECLARE v_estado     VARCHAR(12);
    DECLARE v_comprador  BIGINT;
    DECLARE v_agricultor BIGINT;

    SELECT t.estado, c.id_comprador, p.id_agricultor
      INTO v_estado, v_comprador, v_agricultor
    FROM transacciones t
    JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
    JOIN publicaciones p  ON p.id_publicacion = c.id_publicacion
    WHERE t.id_transaccion = NEW.id_transaccion;

    IF v_estado IN ('PROPUESTA', 'RECHAZADA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se reclama sobre una venta acordada';
    END IF;
    IF NEW.id_reclamante NOT IN (v_comprador, v_agricultor) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El reclamante no participa en la venta';
    END IF;
    IF NEW.estado <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Todo reclamo inicia PENDIENTE';
    END IF;
END$$

CREATE TRIGGER trg_reclamos_bu BEFORE UPDATE ON reclamos
FOR EACH ROW
BEGIN
    IF OLD.estado <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un reclamo resuelto no se modifica';
    END IF;
    IF NEW.id_transaccion <> OLD.id_transaccion OR NEW.id_reclamante <> OLD.id_reclamante
       OR NEW.motivo <> OLD.motivo OR NEW.descripcion <> OLD.descripcion THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se registra la resolucion del reclamo';
    END IF;
    IF NEW.estado <> 'PENDIENTE' THEN
        IF NEW.id_administrador IS NULL OR fn_rol_usuario(NEW.id_administrador) <> 'ADMINISTRADOR' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un ADMINISTRADOR resuelve reclamos';
        END IF;
        SET NEW.fecha_resolucion = NOW();
    END IF;
END$$

-- ----- Suspensiones (RF-09) -----
CREATE TRIGGER trg_suspensiones_bi BEFORE INSERT ON suspensiones
FOR EACH ROW
BEGIN
    DECLARE v_fundados INT;

    IF fn_rol_usuario(NEW.id_administrador) <> 'ADMINISTRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un ADMINISTRADOR suspende usuarios';
    END IF;
    IF fn_rol_usuario(NEW.id_usuario) = 'ADMINISTRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se suspende a un administrador';
    END IF;
    IF NEW.fecha_fin IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La suspension inicia sin fecha de fin';
    END IF;
    IF EXISTS (SELECT 1 FROM suspensiones WHERE id_usuario = NEW.id_usuario AND fecha_fin IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario ya esta suspendido';
    END IF;

    -- Motivo justificado: 3 o mas reclamos fundados en su contra
    IF NEW.motivo = 'RECLAMOS_FUNDADOS' THEN
        SELECT COUNT(*) INTO v_fundados
        FROM reclamos r
        JOIN transacciones t  ON t.id_transaccion = r.id_transaccion
        JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
        JOIN publicaciones p  ON p.id_publicacion = c.id_publicacion
        WHERE r.estado = 'FUNDADO'
          AND r.id_reclamante <> NEW.id_usuario
          AND NEW.id_usuario IN (c.id_comprador, p.id_agricultor);
        IF v_fundados < 3 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Se requieren al menos 3 reclamos fundados';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_suspensiones_ai AFTER INSERT ON suspensiones
FOR EACH ROW
BEGIN
    UPDATE usuarios SET estado = 'SUSPENDIDO' WHERE id_usuario = NEW.id_usuario;
END$$

-- Reactivar = registrar fecha_fin; nada mas cambia
CREATE TRIGGER trg_suspensiones_bu BEFORE UPDATE ON suspensiones
FOR EACH ROW
BEGIN
    IF OLD.fecha_fin IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La suspension ya fue levantada';
    END IF;
    IF NEW.id_usuario <> OLD.id_usuario OR NEW.id_administrador <> OLD.id_administrador
       OR NEW.motivo <> OLD.motivo OR NEW.detalle <> OLD.detalle THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se registra la fecha de reactivacion';
    END IF;
END$$

CREATE TRIGGER trg_suspensiones_au AFTER UPDATE ON suspensiones
FOR EACH ROW
BEGIN
    IF OLD.fecha_fin IS NULL AND NEW.fecha_fin IS NOT NULL THEN
        UPDATE usuarios SET estado = 'ACTIVO' WHERE id_usuario = NEW.id_usuario;
    END IF;
END$$

-- ---------------------------------------------------------------------
-- 10. EVENTO PROGRAMADO DIARIO (06:00)
--     Requiere event_scheduler=ON en el servidor (ver CLAUDE.md).
-- ---------------------------------------------------------------------
CREATE EVENT ev_tareas_diarias
ON SCHEDULE EVERY 1 DAY
STARTS (CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 6 HOUR)
DO
BEGIN
    -- RF-07: avisos a 3 dias y a 24 horas del vencimiento (uno por dia)
    INSERT INTO notificaciones (id_usuario, tipo, titulo, contenido, id_referencia)
    SELECT p.id_agricultor,
           'POR_VENCER',
           CONCAT('Su ', LOWER(c.nombre),
                  IF(p.fecha_limite = CURRENT_DATE + INTERVAL 1 DAY, ' vence mañana.', ' vence en 3 días.'),
                  ' ¿Qué desea hacer?'),
           CONCAT(TRIM(TRAILING '.00' FROM CAST(p.cantidad AS CHAR)), ' ',
                  LOWER(um.nombre_plural), ' · ', d.nombre),
           p.id_publicacion
    FROM publicaciones p
    JOIN variedades v       ON v.id_variedad = p.id_variedad
    JOIN cultivos c         ON c.id_cultivo = v.id_cultivo
    JOIN unidades_medida um ON um.id_unidad = p.id_unidad
    JOIN distritos d        ON d.id_distrito = p.id_distrito
    WHERE p.estado = 'PUBLICADA'
      AND p.fecha_limite IN (CURRENT_DATE + INTERVAL 3 DAY, CURRENT_DATE + INTERVAL 1 DAY)
      AND NOT EXISTS (
          SELECT 1 FROM notificaciones n
          WHERE n.tipo = 'POR_VENCER'
            AND n.id_referencia = p.id_publicacion
            AND DATE(n.fecha_creacion) = CURRENT_DATE
      );

    -- RF-07: publicaciones que pasaron su fecha de vencimiento
    UPDATE publicaciones
    SET estado = 'VENCIDA'
    WHERE estado = 'PUBLICADA' AND fecha_limite < CURRENT_DATE;

    -- RF-10: propuestas conjuntas cuya fecha de recojo paso sin respuesta
    UPDATE transacciones t
    JOIN propuestas_grupales pg ON pg.id_propuesta_grupal = t.id_propuesta_grupal
    SET t.estado = 'CANCELADA', t.id_actualizado_por = NULL
    WHERE t.estado = 'PROPUESTA'
      AND pg.estado = 'ABIERTA'
      AND pg.fecha_recojo < CURRENT_DATE;

    UPDATE propuestas_grupales
    SET estado = 'CERRADA'
    WHERE estado = 'ABIERTA' AND fecha_recojo < CURRENT_DATE;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------
-- 11. VISTAS: datos derivados que no se almacenan (3FN)
-- ---------------------------------------------------------------------
-- RF-05: reputacion; el evaluado es la contraparte del evaluador
CREATE VIEW v_reputacion_usuario AS
SELECT x.id_evaluado                AS id_usuario,
       COUNT(*)                     AS total_calificaciones,
       ROUND(AVG(x.puntuacion), 1)  AS promedio_puntuacion
FROM (
    SELECT CASE WHEN cal.id_evaluador = c.id_comprador
                THEN p.id_agricultor ELSE c.id_comprador END AS id_evaluado,
           cal.puntuacion
    FROM calificaciones cal
    JOIN transacciones t  ON t.id_transaccion = cal.id_transaccion
    JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
    JOIN publicaciones p  ON p.id_publicacion = c.id_publicacion
) x
GROUP BY x.id_evaluado;

-- RF-03 y RF-07: tarjetas de busqueda y "Mis ventas"
CREATE VIEW v_publicaciones_disponibles AS
SELECT p.id_publicacion,
       p.id_agricultor,
       ua.nombre_completo                     AS agricultor,
       rep.promedio_puntuacion                AS calificacion_agricultor,
       c.id_cultivo,
       c.nombre                               AS cultivo,
       v.id_variedad,
       v.nombre                               AS variedad,
       d.id_distrito,
       d.nombre                               AS distrito,
       pr.id_provincia,
       pr.nombre                              AS provincia,
       um.id_unidad,
       um.nombre_plural                       AS unidad,
       p.cantidad,
       p.peso_unidad_kg,
       p.precio_unitario,
       p.precio_kg,
       p.cantidad_kg,
       p.cantidad_kg - COALESCE(r.reservado_kg, 0) AS disponible_kg,
       p.fecha_cosecha,
       p.fecha_limite,
       DATEDIFF(p.fecha_limite, CURRENT_DATE) AS dias_restantes,
       CASE WHEN p.estado = 'PUBLICADA' AND DATEDIFF(p.fecha_limite, CURRENT_DATE) <= 3
            THEN TRUE ELSE FALSE END          AS por_vencer,
       p.estado,
       p.fecha_publicacion
FROM publicaciones p
JOIN usuarios ua        ON ua.id_usuario = p.id_agricultor
JOIN variedades v       ON v.id_variedad = p.id_variedad
JOIN cultivos c         ON c.id_cultivo = v.id_cultivo
JOIN distritos d        ON d.id_distrito = p.id_distrito
JOIN provincias pr      ON pr.id_provincia = d.id_provincia
JOIN unidades_medida um ON um.id_unidad = p.id_unidad
LEFT JOIN v_reputacion_usuario rep ON rep.id_usuario = p.id_agricultor
LEFT JOIN (
    SELECT cv.id_publicacion, SUM(t.cantidad_kg) AS reservado_kg
    FROM transacciones t
    JOIN conversaciones cv ON cv.id_conversacion = t.id_conversacion
    WHERE t.estado IN ('ACORDADA', 'RECOGIDO', 'EN_CAMINO', 'ENTREGADO', 'COMPLETADA')
    GROUP BY cv.id_publicacion
) r ON r.id_publicacion = p.id_publicacion;

-- RF-10: lotes agrupados (mismo cultivo, variedad y zona; 2 o mas agricultores), en kg
CREATE VIEW v_lotes_agrupados AS
SELECT id_cultivo, cultivo,
       id_variedad, variedad,
       id_distrito, distrito,
       COUNT(DISTINCT id_agricultor)                              AS total_agricultores,
       COUNT(*)                                                   AS total_publicaciones,
       SUM(disponible_kg)                                         AS volumen_kg,
       ROUND(SUM(disponible_kg * precio_kg) / SUM(disponible_kg), 2) AS precio_promedio_kg,
       MIN(fecha_limite)                                          AS fecha_limite_proxima
FROM v_publicaciones_disponibles
WHERE estado = 'PUBLICADA' AND disponible_kg > 0
GROUP BY id_cultivo, cultivo, id_variedad, variedad, id_distrito, distrito
HAVING COUNT(DISTINCT id_agricultor) >= 2;

-- RF-02: rango que se muestra al agricultor ("pagan entre ... y ...")
-- Ventas completadas en la plataforma en los ultimos 30 dias, en soles por kilo
CREATE VIEW v_rango_precio_plataforma AS
SELECT v.id_cultivo,
       p.id_distrito,
       ROUND(MIN(t.precio_unitario / t.peso_unidad_kg), 2) AS precio_kg_minimo,
       ROUND(MAX(t.precio_unitario / t.peso_unidad_kg), 2) AS precio_kg_maximo,
       ROUND(SUM(t.monto_total) / SUM(t.cantidad_kg), 2)   AS precio_kg_promedio,
       COUNT(*)                                            AS total_ventas
FROM transacciones t
JOIN transacciones_historial h ON h.id_transaccion = t.id_transaccion AND h.estado = 'COMPLETADA'
JOIN conversaciones c          ON c.id_conversacion = t.id_conversacion
JOIN publicaciones p           ON p.id_publicacion = c.id_publicacion
JOIN variedades v              ON v.id_variedad = p.id_variedad
WHERE t.estado = 'COMPLETADA'
  AND h.fecha >= CURRENT_DATE - INTERVAL 30 DAY
GROUP BY v.id_cultivo, p.id_distrito;

-- RF-02: precio vigente del mercado real por cultivo y zona, en kilo y en unidad local,
-- con tendencia frente a la carga anterior ("Precios de hoy")
CREATE VIEW v_precios_vigentes AS
SELECT x.id_cultivo,
       c.nombre                                                         AS cultivo,
       x.id_distrito,
       d.nombre                                                         AS zona,
       x.fecha,
       x.precio_kg,
       um.nombre                                                        AS unidad_local,
       ROUND(x.precio_kg * COALESCE(um.factor_kg, c.peso_sugerido_kg), 2) AS precio_unidad_local,
       CASE WHEN x.precio_anterior IS NULL THEN 'SIN_DATO'
            WHEN x.precio_kg > x.precio_anterior THEN 'SUBE'
            WHEN x.precio_kg < x.precio_anterior THEN 'BAJA'
            ELSE 'IGUAL' END                                            AS tendencia,
       x.fuente,
       COALESCE(x.fecha_actualizacion, x.fecha_registro)                AS actualizado
FROM (
    SELECT pm.*,
           LAG(pm.precio_kg) OVER (PARTITION BY pm.id_cultivo, pm.id_distrito ORDER BY pm.fecha)        AS precio_anterior,
           ROW_NUMBER()      OVER (PARTITION BY pm.id_cultivo, pm.id_distrito ORDER BY pm.fecha DESC)   AS orden
    FROM precios_mercado pm
) x
JOIN cultivos c         ON c.id_cultivo = x.id_cultivo
JOIN distritos d        ON d.id_distrito = x.id_distrito
JOIN unidades_medida um ON um.id_unidad = c.id_unidad_local
WHERE x.orden = 1;

-- RF-06 y RF-11: historial de ventas con montos (la linea de tiempo sale de transacciones_historial)
CREATE VIEW v_historial_transacciones AS
SELECT t.id_transaccion,
       c.id_comprador,
       cb.nombre_negocio                                  AS comprador,
       p.id_agricultor,
       ua.nombre_completo                                 AS agricultor,
       t.id_propuesta_grupal,
       cu.nombre                                          AS cultivo,
       v.nombre                                           AS variedad,
       t.cantidad,
       um.nombre_plural                                   AS unidad,
       t.cantidad_kg,
       t.precio_unitario,
       ROUND(t.precio_unitario / t.peso_unidad_kg, 2)     AS precio_kg,
       t.monto_total,
       t.fecha_recojo,
       t.estado,
       t.fecha_creacion,
       t.fecha_actualizacion
FROM transacciones t
JOIN conversaciones c   ON c.id_conversacion = t.id_conversacion
JOIN compradores cb     ON cb.id_usuario = c.id_comprador
JOIN publicaciones p    ON p.id_publicacion = c.id_publicacion
JOIN usuarios ua        ON ua.id_usuario = p.id_agricultor
JOIN variedades v       ON v.id_variedad = p.id_variedad
JOIN cultivos cu        ON cu.id_cultivo = v.id_cultivo
JOIN unidades_medida um ON um.id_unidad = t.id_unidad;

-- RF-09: pantalla Usuarios y perfil (ventas/compras, calificacion, reclamos, suspension)
CREATE VIEW v_resumen_usuario AS
SELECT u.id_usuario,
       u.rol,
       u.nombre_completo,
       a.nombre_chacra,
       cb.nombre_negocio,
       cb.tipo_comprador,
       u.celular,
       d.nombre AS distrito,
       u.estado,
       s.motivo AS motivo_suspension,
       (SELECT COUNT(*)
          FROM transacciones t
          JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
          JOIN publicaciones p  ON p.id_publicacion = c.id_publicacion
         WHERE t.estado = 'COMPLETADA'
           AND u.id_usuario IN (c.id_comprador, p.id_agricultor)) AS operaciones_completadas,
       rep.promedio_puntuacion,
       COALESCE(rep.total_calificaciones, 0) AS total_calificaciones,
       (SELECT COUNT(*)
          FROM reclamos r
          JOIN transacciones t  ON t.id_transaccion = r.id_transaccion
          JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
          JOIN publicaciones p  ON p.id_publicacion = c.id_publicacion
         WHERE r.estado = 'FUNDADO'
           AND r.id_reclamante <> u.id_usuario
           AND u.id_usuario IN (c.id_comprador, p.id_agricultor)) AS reclamos_fundados,
       u.fecha_registro
FROM usuarios u
JOIN distritos d          ON d.id_distrito = u.id_distrito
LEFT JOIN agricultores a  ON a.id_usuario = u.id_usuario
LEFT JOIN compradores cb  ON cb.id_usuario = u.id_usuario
LEFT JOIN suspensiones s  ON s.id_usuario = u.id_usuario AND s.fecha_fin IS NULL
LEFT JOIN v_reputacion_usuario rep ON rep.id_usuario = u.id_usuario;

-- RF-04 y RF-09: embudo por publicacion (contactos -> ofertas -> acuerdos -> completadas)
CREATE VIEW v_conversion_publicacion AS
SELECT p.id_publicacion,
       p.id_agricultor,
       (SELECT COALESCE(SUM(c.clics_whatsapp), 0) FROM conversaciones c
         WHERE c.id_publicacion = p.id_publicacion)                               AS clics_whatsapp,
       (SELECT COUNT(*) FROM conversaciones c
         WHERE c.id_publicacion = p.id_publicacion)                               AS contactos,
       (SELECT COUNT(*) FROM mensajes m
          JOIN conversaciones c ON c.id_conversacion = m.id_conversacion
         WHERE c.id_publicacion = p.id_publicacion AND m.tipo = 'OFERTA')         AS ofertas,
       (SELECT COUNT(*) FROM transacciones t
          JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
         WHERE c.id_publicacion = p.id_publicacion
           AND t.estado IN ('ACORDADA', 'RECOGIDO', 'EN_CAMINO', 'ENTREGADO', 'COMPLETADA')) AS acuerdos,
       (SELECT COUNT(*) FROM transacciones t
          JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
         WHERE c.id_publicacion = p.id_publicacion AND t.estado = 'COMPLETADA')   AS completadas
FROM publicaciones p;

-- RF-09: ventas terminadas y kilos vendidos por mes y cultivo (panel y comparativo mensual)
CREATE VIEW v_ventas_mensuales_cultivo AS
SELECT DATE_FORMAT(h.fecha, '%Y-%m') AS mes,
       cu.id_cultivo,
       cu.nombre                     AS cultivo,
       COUNT(*)                      AS ventas_completadas,
       SUM(t.cantidad_kg)            AS kilos_vendidos,
       SUM(t.monto_total)            AS monto_total
FROM transacciones_historial h
JOIN transacciones t  ON t.id_transaccion = h.id_transaccion
JOIN conversaciones c ON c.id_conversacion = t.id_conversacion
JOIN publicaciones p  ON p.id_publicacion = c.id_publicacion
JOIN variedades v     ON v.id_variedad = p.id_variedad
JOIN cultivos cu      ON cu.id_cultivo = v.id_cultivo
WHERE h.estado = 'COMPLETADA'
GROUP BY DATE_FORMAT(h.fecha, '%Y-%m'), cu.id_cultivo, cu.nombre;

-- RF-09: tarjetas del panel del administrador (una sola fila)
CREATE VIEW v_panel_admin AS
SELECT (SELECT COUNT(*) FROM publicaciones WHERE estado = 'PUBLICADA')                    AS publicaciones_activas,
       (SELECT COUNT(*) FROM publicaciones
         WHERE estado = 'PUBLICADA' AND fecha_publicacion >= CURRENT_DATE - INTERVAL 7 DAY) AS publicadas_ultimos_7_dias,
       (SELECT COUNT(*) FROM publicaciones WHERE estado = 'EN_REVISION')                  AS en_revision,
       (SELECT MIN(fecha_publicacion) FROM publicaciones WHERE estado = 'EN_REVISION')    AS revision_mas_antigua,
       (SELECT COUNT(*) FROM revisiones_publicacion
         WHERE decision = 'APROBADA' AND DATE(fecha) = CURRENT_DATE)                      AS aprobadas_hoy,
       (SELECT COUNT(*) FROM revisiones_publicacion
         WHERE decision = 'RECHAZADA' AND DATE(fecha) = CURRENT_DATE)                     AS rechazadas_hoy,
       (SELECT COUNT(*) FROM precios_mercado WHERE fecha = CURRENT_DATE)                  AS precios_cargados_hoy,
       (SELECT COUNT(*) FROM cultivos WHERE activo)
         * (SELECT COUNT(*) FROM distritos WHERE es_zona_precio)                          AS precios_esperados_hoy,
       (SELECT COUNT(*) FROM reclamos WHERE estado = 'PENDIENTE')                         AS reclamos_pendientes;
