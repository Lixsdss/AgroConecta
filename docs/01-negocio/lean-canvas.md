# Lean Canvas — AgroConecta Piura

## 1. Problema

- Los pequeños agricultores de Piura no tienen acceso a información confiable y en tiempo real de precios de mercado, lo que les impide negociar en condiciones justas.
- Los intermediarios se apropian de hasta un 30 % o más del valor de la cosecha, aprovechando esa asimetría de información.
- La falta de canales directos de venta obliga a vender rápido (y barato) para evitar el deterioro post-cosecha de productos perecibles.
- **Alternativas actuales:** venta a intermediarios o acopiadores de confianza, ferias locales, coordinación informal por WhatsApp o llamadas.

## 2. Segmentos de clientes

- **Oferta:** pequeños y medianos agricultores de Piura. Early adopters: productores de 1–2 cultivos de alta rotación (limón, mango, plátano) organizados en asociaciones o cooperativas.
- **Demanda:** mercados mayoristas, restaurantes, hoteles y procesadoras. Early adopters: compradores medianos con volumen recurrente que hoy dependen de acopiadores y buscan trazabilidad de origen.

## 3. Propuesta de valor única

- **Agricultor:** vender la cosecha hasta 30 % más cara, conectado el mismo día con compradores reales, sin depender del intermediario.
- **Comprador:** comprar directo al productor piurano, con precios transparentes y trazabilidad de origen, sin sobrecostos de intermediación.

## 4. Solución (MVP)

- Publicación simple de cosecha disponible (cantidad, cultivo, precio referencial, ubicación), pensada para uso desde el celular.
- Tablero de precios de mercado por cultivo y zona.
- Contacto directo comprador–vendedor (chat o WhatsApp integrado).
- Sistema básico de calificación para generar confianza.

## 5. Canales

- WhatsApp Business como canal principal de comunicación y soporte.
- Promotores locales y técnicos de campo que capacitan y enrolan agricultores.
- Asociaciones y cooperativas agrarias como canal de onboarding grupal (no son actores del sistema).
- Ferias agropecuarias y agencias agrarias locales (Agro Rural, Juntas de Usuarios de Riego).
- Sitio web y Facebook como canal secundario para compradores urbanos.
- Recomendación entre agricultores (boca a boca).

## 6. Fuentes de ingresos

- Gratis para el agricultor en el MVP.
- A futuro (RF-12, fuera del alcance del curso): suscripción mensual para compradores frecuentes y comisión por transacción cobrada al comprador.
- Posibles convenios o cofinanciamiento con programas gubernamentales o de cooperación (subsidio, no ingreso recurrente).

## 7. Estructura de costos

- **Fijos:** dominio y hosting, soporte y mantenimiento, promotores locales, recolección y actualización de datos de precios.
- **Variables:** marketing digital, costo de adquisición en campo.

## 8. Métricas clave

- Valor total transaccionado (GMV) en soles.
- Publicaciones activas por semana.
- Conversión: publicaciones → transacciones concretadas.
- Transacciones completadas.
- Ratio oferta/demanda: agricultores activos vs. compradores activos.
- Retención y recompra de compradores.
- Tiempo promedio entre publicación y primer contacto.
- Calificación promedio de usuarios.

## 9. Ventaja competitiva

- Base de datos propia de precios construida con transacciones reales.
- Efectos de red entre agricultores y compradores.
- Alianzas con asociaciones y cooperativas locales de Piura.
- Conocimiento del territorio y confianza construida por los promotores locales.

---

**Nota para el desarrollo:** este documento da contexto de negocio. Los requisitos que se implementan están en `docs/02-requisitos/SRS-AgroConecta.md`. La pasarela de pagos y la comisión por transacción no forman parte del sistema (ver ADR-002).