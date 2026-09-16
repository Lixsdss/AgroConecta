AgroConecta - Resumen Ejecutivo APF1

# 1. Información del Proyecto

**Nombre: **AgroConecta: Plataforma Digital para la Comercialización Directa de Productos Agrícolas

**Curso: **Integrador I: Sistemas - Software (100000I58N)

**Ciclo: **VI - Agosto 2026

**Duración: **18 semanas (10 de agosto - 30 de noviembre de 2026)

**Equipo: **6 integrantes (4 programadores, 2 documentadores)

# 2. Problema Identificado

Los pequeños agricultores de Piura enfrentan:

Falta de acceso a información confiable y en tiempo real de precios de mercado

Dependencia excesiva de intermediarios que se apropian del 30%+ del valor final del producto

Presión para vender rápido (barato) debido a perecibilidad de productos

Limitadas alternativas de comercialización directa

# 3. Solución Propuesta

**AgroConecta **es una plataforma web que conecta directamente agricultores con compradores finales, permitiendo:

Publicación simple de cosechas disponibles (3 pasos máximo)

Visualización de precios de mercado en tiempo real por cultivo y zona

Contacto directo comprador-vendedor sin intermediarios

Sistema de calificación para generar confianza

# 4. Stack Tecnológico Seleccionado

**Backend: **Java + Spring Boot + JPA/Hibernate + Maven

**Frontend: **HTML5 + CSS3 + JavaScript + Bootstrap (Responsivo)

**Base de Datos: **MySQL (normalizado a 3NF)

**Servidor: **Apache Tomcat en entorno cloud (AWS/Azure/GCP)

**Control de Versiones: **Git & GitHub

**Porcentaje Java: **65%

# 5. Actores del Sistema

| Actor | Descripción |
| --- | --- |
| Agricultor | Pequeño productor que publica sus cosechas disponibles para la venta directa |
| Comprador | Mercados mayoristas, restaurantes, hoteles, procesadoras que buscan productos frescos |
| Administrador | Gestor de la plataforma (usuarios, publicaciones, reportes) |

# 6. Requisitos Funcionales (RF)

RF-01: Publicación simplificada en máximo 3 pasos (cultivo, cantidad, precio, ubicación)

RF-02: Consulta de precios de mercado con históricos por cultivo y zona

RF-03: Búsqueda y filtros multi-criterio (categoría, provincia/distrito, fecha)

RF-04: Contacto directo agricultor-comprador (mensajería integrada)

RF-05: Sistema de calificación (1-5 estrellas) y comentarios

RF-06: Historial de transacciones por usuario

RF-07: Alertas de vencimiento para productos próximos a perecer

RF-08: Gestión de roles (Agricultor, Comprador, Administrador)

RF-09: Panel administrativo (gestión de usuarios, moderación, reportes)

# 7. Requisitos No Funcionales (RNF)

| Requisito | Métrica |
| --- | --- |
| Rendimiento | Tiempo de carga < 3.0 seg en 4G |
| Escalabilidad | 500+ usuarios concurrentes |
| Usabilidad | Botones grandes, alto contraste, nav lineal |
| Mantenibilidad | SOLID + Javadoc completo |
| Integridad | Normalización 3NF + integridad referencial |
| Disponibilidad | 99.0% SLA en horario comercial |

# 8. Pantallas Principales (MVP)

Inicio de sesión y registro (agricultor/comprador)

Panel de publicación de cosechas

Panel de búsqueda y visualización de productos

Panel de mensajería y negociación

Panel de venta agrupada (consolidación de ofertas)

Panel de seguimiento de transporte

Perfil y historial de transacciones

Panel administrativo (moderación y reportes)

Centro de notificaciones

# 9. Cronograma de Hitos

| Hito | Semana | Fecha |
| --- | --- | --- |
| APF1: Planificación & Análisis | Semana 4 | Sept 3, 2026 |
| APF2: Diseño | Semana 8 | Oct 1, 2026 aprox |
| APF3: Desarrollo | Semana 12 | Oct 29, 2026 aprox |
| PROY: Despliegue Final | Semana 18 | Nov 27, 2026 |

# 10. Fases de Desarrollo

## Unidad 1 - Planificación & Análisis (Semanas 1-4)

Lean Canvas, Project Charter, WBS, Gantt, Requerimientos, Prototipos

## Unidad 2 - Diseño (Semanas 5-8)

BPM, Diseño BD (lógico/físico), UX/UI, Documentación técnica

## Unidad 3 - Desarrollo (Semanas 9-12)

Arquitectura MVC, Backend Spring Boot, Frontend HTML/CSS/JS

Checkpoints: 30%, 40% de avance completado

## Unidad 4 - Pruebas, Despliegue & Mantenimiento (Semanas 13-18)

Testing (unitarias, integración, seguridad), Despliegue, Monitoreo, Mantenimiento

Checkpoints: 60%, 70%, 80%, 90%, 100% de avance

# 11. Patrones & Principios de Arquitectura

MVC (Modelo-Vista-Controlador): Separación de datos, UI y lógica

SOLID: Principios de diseño orientado a objetos

DAO (Data Access Object): Abstracción del acceso a BD

REST API: Comunicación frontend-backend

TDD: Test-Driven Development

# 12. Próximos Pasos (APF2 - Diseño)

Elaborar diagramas BPM de procesos de negocio

Diseño lógico y físico de la base de datos

Prototipos interactivos de UI/UX (Figma/MockFlow)

Documentación técnica (Javadoc, Manual de Usuario)

Mapeo de reportes clave del sistema

Validar requisitos con stakeholders