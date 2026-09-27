# ADR-004: Esquema definido por SQL con Flyway; Hibernate solo valida

**Estado:** Aceptado  
**Fecha:** 27 de septiembre de 2026 (registra una decisión tomada en septiembre de 2026)

## Contexto

Buena parte de las reglas de negocio de AgroConecta viven en la base de datos: `modelo-bd.sql` v4.1 tiene 20 tablas, 40 restricciones `CHECK`, columnas generadas, 21 triggers, 1 función, 1 procedimiento, 10 vistas y un evento diario. El esquema es además un entregable del curso (diseño de base de datos, APF2) que el equipo revisa y documenta. La base de datos es MySQL en producción y MariaDB en desarrollo y CI.

## Decisión

- El esquema se define **en SQL**. `docs/04-diseno/base-datos/modelo-bd.sql` es la fuente de verdad.
- Se aplica con **Flyway** (`flyway-mysql`): `V1__esquema_inicial.sql` (secciones 2 a 11) y `V2__datos_iniciales.sql` (sección 12). Cada cambio posterior va en una nueva migración `Vn` y se refleja en `modelo-bd.sql`; una migración aplicada nunca se edita.
- Hibernate **solo valida** (`spring.jpa.hibernate.ddl-auto=validate` en todos los perfiles); nunca crea ni modifica tablas.
- Las pruebas de integración corren contra una base real (`agroconecta_test`), nunca contra H2.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Generar el esquema desde las entidades** (`ddl-auto=update` o `create`) | Hibernate no genera triggers, rutinas, eventos, vistas ni columnas generadas; el esquema quedaría incompleto y cambiaría de forma implícita con cada entidad. |
| **Liquibase** | Es equivalente en capacidad, pero sus changelogs (XML/YAML) se alejan del SQL que el equipo diseña y entrega; Flyway ejecuta ese mismo SQL. |
| **Scripts SQL aplicados a mano** | Sin control de versión del esquema aplicado ni forma confiable de repetirlo en CI y producción. |
| **H2 para las pruebas** | No soporta los triggers, eventos ni la sintaxis de MySQL del esquema; las pruebas no probarían las reglas reales. |

## Consecuencias

- **Positivas:** el esquema entregado es exactamente el que corre; las reglas de la base de datos se prueban en CI; Hibernate detecta al arrancar cualquier diferencia entre entidades y tablas.
- **Negativas:** cada cambio de esquema requiere una migración nueva y actualizar `modelo-bd.sql`; el usuario de la base de datos necesita privilegios TRIGGER, EVENT y de rutinas, y el servidor, `event_scheduler=ON`; las entidades deben mapear columnas generadas como solo lectura y los `CHECK` como enums.
- Mientras el proyecto está en diseño y V1 no se aplicó en ninguna base compartida, V1 y V2 se regeneran desde `modelo-bd.sql` en lugar de agregar migraciones (decisión tomada para v4.1).
