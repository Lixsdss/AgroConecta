# ADR-002: Sin pasarela de pagos

**Estado:** Aceptado  
**Fecha:** 27 de septiembre de 2026 (registra una decisión tomada en septiembre de 2026)

## Contexto

AgroConecta conecta a pequeños agricultores de Piura con compradores. En el mercado local el pago se hace en efectivo, por transferencia o con billeteras móviles al momento del recojo o de la entrega, y a menudo en parte por adelantado según la confianza entre las partes. El curso exige un MVP funcional con costo cero, y cobrar dentro de la plataforma implicaría manejar dinero de terceros, comisiones y responsabilidades legales.

## Decisión

La plataforma **no procesa pagos**. Una transacción (`transacciones`) solo registra el **acuerdo comercial**: unidad, cantidad, precio, fecha de recojo y el avance de la entrega. El pago ocurre fuera de la plataforma, directamente entre agricultor y comprador. No se integra ninguna pasarela ni se agregan librerías de pago.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Pasarela de pagos** (tarjeta o billetera) | Tiene costos por transacción o de afiliación, exige cumplir normas de seguridad de pagos y trámites con la entidad financiera; excede el alcance y rompe el costo cero. |
| **Registro manual del pago** (el comprador marca "pagué") | Agrega estados y conflictos (pago parcial, disputa) sin que la plataforma pueda verificarlos. Los incumplimientos se atienden con reclamos (motivo `INCUMPLIO_PAGO`, RF-09). |
| **Custodia de fondos (escrow)** | Requiere ser una entidad regulada o un tercero con costo; fuera de alcance. |

## Consecuencias

- **Positivas:** alcance acotado, sin costos ni obligaciones de manejo de dinero; el modelo de datos es más simple (no hay tablas de pagos ni comisiones).
- **Negativas:** la plataforma no garantiza el cobro. Se mitiga con la reputación (RF-05), los reclamos y las suspensiones justificadas (RF-09).
- Las suscripciones y comisiones (RF-12) quedan como extensión futura y necesitarían revisar esta decisión.
