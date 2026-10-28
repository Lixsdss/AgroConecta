# ADR-003: JWT con Spring Security OAuth2 Resource Server

**Estado:** Aceptado  
**Fecha:** 27 de septiembre de 2026 (registra una decisión tomada en septiembre de 2026)

## Contexto

La API REST la consume un frontend estático (HTML, CSS y JavaScript) con tres roles: AGRICULTOR, COMPRADOR y ADMINISTRADOR. La API debe ser stateless (RNF-02), el inicio de sesión es con celular y contraseña, y en cada petición hay que comprobar que el usuario siga ACTIVO para que una suspensión surta efecto de inmediato (SRS §9.1). No hay presupuesto para un proveedor de identidad externo.

## Decisión

Usar **Spring Security con OAuth2 Resource Server** y JWT firmados con **HS256**:

- El módulo `usuario` emite el token al iniciar sesión con `NimbusJwtEncoder`: `sub` = id del usuario, claim `roles`.
- La clave se lee de la variable de entorno `JWT_SECRET` (mínimo 32 bytes); nunca se guarda en Git.
- El Resource Server valida la firma y la expiración con `NimbusJwtDecoder`; un convertidor transforma el claim `roles` en autoridades `ROLE_*` para `@PreAuthorize`.
- Un validador adicional en `compartido.seguridad` llama a la interfaz `VerificadorEstadoUsuario`, que implementa `usuario`, y rechaza con 401 a un usuario SUSPENDIDO o INACTIVO.
- Contraseñas con BCrypt. El frontend guarda el token en `sessionStorage`.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Filtro JWT manual con JJWT** | Obliga a escribir y mantener el filtro, el manejo de errores y la integración con Spring Security; es una fuente habitual de fallos de seguridad. Resource Server ya lo resuelve con código probado y Nimbus viene incluido. |
| **Sesión HTTP con cookie** | Rompe el diseño stateless y complica escalar a varias instancias; requiere protección CSRF adicional. |
| **Proveedor de identidad externo** (Keycloak, Auth0, etc.) | Agrega infraestructura o costo, y el login por celular sin email es un caso simple que no lo justifica. |

## Consecuencias

- **Positivas:** poco código propio de seguridad; configuración estándar y conocida; roles declarativos con `@PreAuthorize`; sin estado en el servidor.
- **Negativas:** con HS256 la misma clave firma y valida, así que debe protegerse bien y rotarse si se filtra. Un token no se puede revocar antes de expirar; se compensa con la verificación del estado del usuario en cada petición (una consulta por clave primaria).
- El detalle se implementa en el slice de RF-08.
