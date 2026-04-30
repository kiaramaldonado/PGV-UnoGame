[← Volver al README](../../README.md)

# Implementación de encriptación en el tráfico cliente-servidor

A continuación, se garantiza que toda la comunicación entre cliente y servidor viaje encriptada, usando una clave persistida en el archivo `.secret`.

## Cambios realizados

### 1) `SocketIOHandler`
- Se integra `SecureManager` como miembro privado.
- Se añaden dos constructores:
  - Sin clave: usa la clave persistida en `.secret` (si no existe, se genera y se guarda).
  - Con clave: permite pasar una clave personalizada.
- `sendMessage()` ahora serializa el `Message`, lo encripta y envía el payload cifrado.
- `receiveMessage()` ahora recibe el payload cifrado, lo desencripta y reconstruye el `Message`.
- Se añaden métodos privados para serializar/deserializar (`Message` ⇄ Base64).

### 2) `Client`
- Se añade `encryptionKey` como campo.
- Se incluyen dos constructores:
  - Constructor por defecto (clave nula): usa la clave persistida en `.secret`.
  - Constructor con clave personalizada.
- En `connect()` se inicializa `SocketIOHandler` con la clave configurada.

### 3) `ClientHandler`
- Se añade `encryptionKey` como campo.
- Se incluyen dos constructores:
  - Constructor por defecto (clave nula): usa la clave persistida en `.secret`.
  - Constructor con clave personalizada.
- En `run()` se inicializa `SocketIOHandler` con la clave configurada.

### 4) Persistencia de clave
- `FileManager` guarda y lee la clave desde el archivo `.secret`.
- Si no existe clave previa, se genera y se persiste automáticamente.

## Resultado
Con estos cambios, todo el tráfico de mensajes entre cliente y servidor queda encriptado de forma transparente, manteniendo la misma API de envío y recepción de `Message`.

[← Volver al README](../../README.md)
