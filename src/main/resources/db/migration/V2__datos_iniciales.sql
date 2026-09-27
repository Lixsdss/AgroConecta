-- =====================================================================
-- AgroConecta - V2: datos iniciales de catalogos (modelo de datos v4)
-- Generado desde docs/04-diseno/base-datos/modelo-bd.sql, seccion 12.
-- modelo-bd.sql es la fuente de verdad; no editar esta migracion una vez aplicada.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 12. DATOS INICIALES DE CATALOGOS
-- ---------------------------------------------------------------------
INSERT INTO provincias (id_provincia, nombre) VALUES
    (1, 'Piura'), (2, 'Ayabaca'), (3, 'Huancabamba'), (4, 'Morropón'),
    (5, 'Paita'), (6, 'Sullana'), (7, 'Talara'), (8, 'Sechura');

-- Los 65 distritos de la region Piura (ubigeo INEI). Validar contra la lista oficial.
INSERT INTO distritos (id_provincia, nombre, ubigeo, es_zona_precio) VALUES
    (1, 'Piura', '200101', TRUE), (1, 'Castilla', '200104', FALSE), (1, 'Catacaos', '200105', FALSE),
    (1, 'Cura Mori', '200107', FALSE), (1, 'El Tallán', '200108', FALSE), (1, 'La Arena', '200109', FALSE),
    (1, 'La Unión', '200110', FALSE), (1, 'Las Lomas', '200111', FALSE), (1, 'Tambogrande', '200114', TRUE),
    (1, 'Veintiséis de Octubre', '200115', FALSE),
    (2, 'Ayabaca', '200201', FALSE), (2, 'Frías', '200202', FALSE), (2, 'Jililí', '200203', FALSE),
    (2, 'Lagunas', '200204', FALSE), (2, 'Montero', '200205', FALSE), (2, 'Pacaipampa', '200206', FALSE),
    (2, 'Paimas', '200207', FALSE), (2, 'Sapillica', '200208', FALSE), (2, 'Sícchez', '200209', FALSE),
    (2, 'Suyo', '200210', FALSE),
    (3, 'Huancabamba', '200301', FALSE), (3, 'Canchaque', '200302', FALSE),
    (3, 'El Carmen de la Frontera', '200303', FALSE), (3, 'Huarmaca', '200304', FALSE),
    (3, 'Lalaquiz', '200305', FALSE), (3, 'San Miguel de El Faique', '200306', FALSE),
    (3, 'Sondor', '200307', FALSE), (3, 'Sondorillo', '200308', FALSE),
    (4, 'Chulucanas', '200401', TRUE), (4, 'Buenos Aires', '200402', FALSE), (4, 'Chalaco', '200403', FALSE),
    (4, 'La Matanza', '200404', FALSE), (4, 'Morropón', '200405', FALSE), (4, 'Salitral', '200406', FALSE),
    (4, 'San Juan de Bigote', '200407', FALSE), (4, 'Santa Catalina de Mossa', '200408', FALSE),
    (4, 'Santo Domingo', '200409', FALSE), (4, 'Yamango', '200410', FALSE),
    (5, 'Paita', '200501', FALSE), (5, 'Amotape', '200502', FALSE), (5, 'Arenal', '200503', FALSE),
    (5, 'Colán', '200504', FALSE), (5, 'La Huaca', '200505', FALSE), (5, 'Tamarindo', '200506', FALSE),
    (5, 'Vichayal', '200507', FALSE),
    (6, 'Sullana', '200601', TRUE), (6, 'Bellavista', '200602', FALSE), (6, 'Ignacio Escudero', '200603', FALSE),
    (6, 'Lancones', '200604', FALSE), (6, 'Marcavelica', '200605', FALSE), (6, 'Miguel Checa', '200606', FALSE),
    (6, 'Querecotillo', '200607', FALSE), (6, 'Salitral', '200608', FALSE),
    (7, 'Pariñas', '200701', FALSE), (7, 'El Alto', '200702', FALSE), (7, 'La Brea', '200703', FALSE),
    (7, 'Lobitos', '200704', FALSE), (7, 'Los Órganos', '200705', FALSE), (7, 'Máncora', '200706', FALSE),
    (8, 'Sechura', '200801', FALSE), (8, 'Bellavista de la Unión', '200802', FALSE), (8, 'Bernal', '200803', FALSE),
    (8, 'Cristo Nos Valga', '200804', FALSE), (8, 'Vice', '200805', FALSE), (8, 'Rinconada Llicuar', '200806', FALSE);

-- Quintal peruano = 46 kg. Jaba y saco son de peso variable.
INSERT INTO unidades_medida (id_unidad, nombre, nombre_plural, factor_kg) VALUES
    (1, 'Kilo',     'kilos',     1.000),
    (2, 'Jaba',     'jabas',     NULL),
    (3, 'Saco',     'sacos',     NULL),
    (4, 'Quintal',  'quintales', 46.000),
    (5, 'Tonelada', 'toneladas', 1000.000);

-- Cultivos del MVP. Arroz: se comercializa en cascara, en sacos de 50 kg.
-- vida_util_dias: valores referenciales; validar con el equipo.
INSERT INTO cultivos (id_cultivo, nombre, vida_util_dias, id_unidad_local, peso_sugerido_kg) VALUES
    (1, 'Mango',   14,  2, 20.000),
    (2, 'Limón',   20,  3, 45.000),
    (3, 'Arroz',   180, 3, 50.000),
    (4, 'Plátano', 10,  5, NULL);

INSERT INTO variedades (id_cultivo, nombre) VALUES
    (1, 'Kent'), (1, 'Edward'), (1, 'Haden'), (1, 'Común'),
    (2, 'Sutil'), (2, 'Tahití'), (2, 'Común'),
    (3, 'NIR'), (3, 'IR-43'), (3, 'Común'),
    (4, 'Seda'), (4, 'Bellaco'), (4, 'Común');

-- El ADMINISTRADOR inicial se crea desde la aplicacion (Spring Security + BCrypt).
-- No se incluyen contrasenas ni hashes reales en este script.
