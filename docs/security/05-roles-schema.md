[← Volver al README](../../README.md)

# Esquema de Seguridad basado en Roles - UNO Game Escalado

## 1. Visión General

Para escalar la aplicación UNO Game a un proyecto más grande con múltiples usuarios, sedes administrativas y sistemas de monetización, se necesita un esquema robusto de roles y control de acceso basado en permisos (RBAC - Role Based Access Control).

## 2. Definición de Roles

![Roles y Permisos](/docs/assets/roles-schema.png)

### 2.1 Jugador (PLAYER)
**Descripción**: Usuario estándar que juega partidas.

**Permisos**:
- ✅ Crear/Unirse a salas de juego públicas
- ✅ Jugar cartas durante la partida
- ✅ Chat en-juego y lobby
- ✅ Ver estadísticas personales
- ✅ Ver perfil personal
- ❌ Crear salas privadas (limitado a premium)
- ❌ Moderar otros jugadores
- ❌ Acceder a paneles administrativos

### 2.2 Premium Player (PREMIUM_PLAYER)
**Descripción**: Jugador con suscripción activa.

**Permisos** (hereda de PLAYER):
- ✅ Crear salas privadas con contraseña
- ✅ Acceso a temas y avatares exclusivos
- ✅ Sin límite de partidas diarias
- ✅ Bonificación en moneda del juego
- ✅ Acceso a contenido exclusivo
- ❌ Moderar salas

### 2.3 Moderador (MODERATOR)
**Descripción**: Usuario con permiso para supervisar y mantener la comunidad.

**Permisos** (hereda de PLAYER):
- ✅ Todos los permisos de PREMIUM_PLAYER
- ✅ Silenciar/Kickear jugadores
- ✅ Cerrar salas problemáticas
- ✅ Ver logs de infracciones
- ✅ Emitir advertencias a jugadores
- ❌ Banear permanentemente
- ❌ Acceder a datos financieros
- ❌ Cambiar configuración del servidor

### 2.4 Administrador (ADMIN)
**Descripción**: Gestor del sistema con acceso casi total.

**Permisos**:
- ✅ Todos los permisos de MODERATOR
- ✅ Banear/Desbanear jugadores
- ✅ Acceso a panel de administración
- ✅ Ver estadísticas del servidor
- ✅ Editar restricciones de salas
- ✅ Enviar anuncios globales
- ✅ Gestionar reportes de jugadores
- ✅ Ver auditoría de eventos
- ❌ Acceder a datos financieros sensibles
- ❌ Cambiar configuración de seguridad crítica

### 2.5 Super Administrador (SUPER_ADMIN)
**Descripción**: Acceso máximo del sistema.

**Permisos**:
- ✅ Todos los permisos del sistema
- ✅ Gestionar otros administradores
- ✅ Acceder a datos financieros
- ✅ Editar configuración de seguridad
- ✅ Exportar/Importar datos
- ✅ Ejecutar comandos del servidor

### 2.6 Sistema (SYSTEM)
**Descripción**: Rol para procesos automatizados internos.

**Permisos**:
- ✅ Operaciones del servidor
- ✅ Notificaciones automáticas
- ✅ Mantenimiento programado
- ✅ Auditoría y logs

---

## 3. Matriz de Permisos

| Permiso | PLAYER | PREMIUM | MODERATOR | ADMIN | SUPER_ADMIN |
|---------|--------|---------|-----------|-------|------------|
| Jugar | ✅ | ✅ | ✅ | ✅ | ✅ |
| Chat | ✅ | ✅ | ✅ | ✅ | ✅ |
| Crear sala pública | ✅ | ✅ | ✅ | ✅ | ✅ |
| Crear sala privada | ❌ | ✅ | ✅ | ✅ | ✅ |
| Ver estadísticas | ✅ | ✅ | ✅ | ✅ | ✅ |
| Silenciar jugador | ❌ | ❌ | ✅ | ✅ | ✅ |
| Kickear jugador | ❌ | ❌ | ✅ | ✅ | ✅ |
| Banear jugador | ❌ | ❌ | ❌ | ✅ | ✅ |
| Panel admin | ❌ | ❌ | ❌ | ✅ | ✅ |
| Ver logs | ❌ | ❌ | ✅ | ✅ | ✅ |
| Gestionar admins | ❌ | ❌ | ❌ | ❌ | ✅ |
| Datos financieros | ❌ | ❌ | ❌ | ❌ | ✅ |
| Configuración servidor | ❌ | ❌ | ❌ | ✅ | ✅ |

---

## 4. Flujo de Autenticación y Autorización

### 4.1 Autenticación
El cliente envía usuario y contraseña al servidor de autenticación. El servidor verifica que las credenciales sean correctas y, si es así, construye un token JWT cuyo payload incluye el userId, el role asignado, la lista de permissions y la fecha de expiración. Antes de enviarlo, el servidor firma ese payload usando RSA-2048, lo que garantiza que nadie pueda falsificar ni modificar el token. El cliente recibe el JWT y lo guarda en sesión (normalmente en memoria o en una cookie HttpOnly).

![Flujo de Login](/docs/assets/login-schema.png)

### 4.3 Autorización
A partir de ese momento, el cliente adjunta el JWT en la cabecera Authorization de cada petición. El servidor de autorización hace dos comprobaciones en orden: primero verifica la firma RSA para asegurarse de que el token no fue alterado, y luego comprueba si el rol del token tiene el permiso necesario para esa acción concreta.
El resultado es binario: si pasa ambas verificaciones, la acción se ejecuta y queda registrada en el log de auditoría. Si falla cualquiera de las dos (token expirado, firma inválida, o permiso insuficiente), el servidor responde con un error 403 Forbidden y el intento queda registrado igualmente, lo que permite detectar patrones de acceso no autorizado.

![Flujo de Autorización](/docs/assets/authorization-schema.png)

---

## 5. Estructura de Datos: UserRole

```java
public class UserRole {
    private String userId;
    private Role role;  // PLAYER, PREMIUM_PLAYER, MODERATOR, ADMIN, SUPER_ADMIN
    private List<String> permissions;  // Lista dinámmica de permisos
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;  // Para roles temporales
    private boolean active;
    
    // Métodos
    public boolean hasPermission(String permission) { ... }
    public boolean canManage(UserRole otherRole) { ... }
    public void revoke() { ... }
}

public enum Role {
    PLAYER("base", 0),
    PREMIUM_PLAYER("premium", 1),
    MODERATOR("moderator", 2),
    ADMIN("admin", 3),
    SUPER_ADMIN("super_admin", 4),
    SYSTEM("system", 5);
    
    private String code;
    private int level;  // Para comparación jerárquica
}
```

---

## 6. Token JWT con Payload

```json
{
  "sub": "user_12345",
  "username": "jugador_nombre",
  "role": "PREMIUM_PLAYER",
  "permissions": ["play", "create_private_room", "chat"],
  "iat": 1683698400,
  "exp": 1683784800,
  "iss": "uno-game-server",
  "aud": "uno-game-client"
}
```

---

## 7. Auditoría y Logging

### 7.1 Eventos a registrar
- ✅ Login/Logout
- ✅ Cambios de rol
- ✅ Acciones prohibidas (intentos fallidos)
- ✅ Moderación (bans, kicks, silencias)
- ✅ Cambios críticos (configuración, usuarios)
- ✅ Transacciones (compra de premium, recompensas)

### 7.2 Estructura de Audit Log
```java
public class AuditLog {
    private String id;
    private String userId;
    private String action;  // LOGIN, BAN_USER, KICK_USER, etc.
    private String resource;  // Usuario afectado, sala, etc.
    private String result;  // SUCCESS, FAILED
    private String reason;
    private LocalDateTime timestamp;
    private String ipAddress;
}
```

---

## 8. Restricciones y Limitaciones

### 8.1 Rate Limiting
- **PLAYER**: 30 mensajes/minuto, 10 salas/día
- **PREMIUM_PLAYER**: 60 mensajes/minuto, sin limite salas
- **MODERATOR**: 120 mensajes/minuto, funciones ilimitadas
- **ADMIN**: Sin limitaciones

### 8.2 Ventanas de Suspensión
- **Silencio**: 5-60 minutos (configurable)
- **Kick**: Inmediato + reentrada permitida
- **Ban temporal**: 1-30 días
- **Ban permanente**: Solo SUPER_ADMIN

---

## 9. Validación en Protocolo de Mensajes

### 9.1 Extensión de Message.java
```java
public enum MessageType {
    // ... tipos existentes ...
    AUTH_LOGIN,          // Autenticación
    AUTH_LOGOUT,         // Cierre de sesión
    PERMISSION_DENIED,   // Rechazo por permisos
    ROLE_CHANGE,         // Cambio de rol
    AUDIT_LOG,          // Registro de auditoría
    ADMIN_ACTION,       // Acción administrativa
}
```

### 9.2 Validación en cada operación
```java
if (!validateToken(message.getToken())) {
    return ERROR("Invalid token");
}

UserRole userRole = getRoleFromToken(message.getToken());

if (!userRole.hasPermission(requiredPermission)) {
    logAuditEvent(userId, "PERMISSION_DENIED", action);
    return PERMISSION_DENIED(action);
}

executeAction(action);
```

---

## 10. Seguridad Adicional

### 10.1 Contraseñas
- **Algoritmo**: BCrypt con salt (costo 12)
- **Requisito mínimo**: 8 caracteres, mayúscula, minúscula, número, símbolo

### 10.2 Comunicación
- **Protocolo**: TLS 1.3 obligatorio
- **Encriptación de datos**: AES-256-GCM
- **Firma**: RSA-2048

### 10.3 Sesiones
- **Timeout**: 30 minutos para PLAYER, 24 horas para ADMIN
- **Refresh token**: Válido 7 días
- **IP validation**: Detectar cambios anómalos

[← Volver al README](../../README.md)

