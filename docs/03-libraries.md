[← Volver al README](../README.md)
---

# Clases y Librerías Java de Comunicación

## 1. LIBRERÍAS DEL ECOSISTEMA JAVA ESTÁNDAR

### 1.1 Paquete `java.net` - Comunicación de Red

#### `java.net.ServerSocket`
- **Propósito:** Aceptar conexiones TCP entrantes
- **Uso en proyecto:** `Server.java`
- **Código:**
  ```java
  serverSocket = new ServerSocket(port);  // Puerto de escucha
  Socket clientSocket = serverSocket.accept();  // Bloquea hasta cliente
  ```
- **Por qué TCP:** Necesitamos garantía de entrega ordenada de mensajes (esencial en un juego)

#### `java.net.Socket`
- **Propósito:** Conexión bidireccional entre cliente y servidor
- **Uso en proyecto:** `Client.java` (lado cliente) y `ClientHandler.java` (lado servidor)
- **Cliente:**
  ```java
  socket = new Socket(serverHost, serverPort);
  ```
- **Servidor (recibido de accept()):**
  ```java
  Socket clientSocket = serverSocket.accept();
  new ClientHandler(clientSocket, server);
  ```
- **Propiedades importantes:**
  - `getInputStream()` para recibir datos
  - `getOutputStream()` para enviar datos
  - `isClosed()` para verificar estado
  - `close()` para desconectar

### 1.2 Paquete `java.io` - Serialización de Objetos

#### `java.io.ObjectOutputStream`
- **Propósito:** Enviar objetos Java serializados a través de un stream
- **Uso en proyecto:** `SocketIOHandler.java`
- **Crucial porque:**
  - Serializa objetos `Message` completos con tipos predefinidos
  - Más seguro que enviar strings planos (no hay parsing manual)
  - Soporta cualquier objeto que implemente `Serializable`
- **Código:**
  ```java
  ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
  out.writeObject(message);  // Serializa automáticamente
  out.flush();  // Força envío al socket
  ```
- **Importancia del orden:** DEBE crearse ANTES que `ObjectInputStream` para evitar deadlock

#### `java.io.ObjectInputStream`
- **Propósito:** Recibir objetos Java desde un stream
- **Uso en proyecto:** `SocketIOHandler.java`
- **Bloqueante:** `readObject()` espera hasta recibir un objeto completo
- **Excepciones:**
  - `EOFException` → cliente cerró conexión normalmente
  - `ClassNotFoundException` → tipo de objeto desconocido
- **Código:**
  ```java
  ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
  Message message = (Message) in.readObject();  // Bloqueante
  ```

#### `java.io.InputStream` / `java.io.OutputStream`
- **Propósito:** Abstracciones base para todos los streams
- **Uso en proyecto:** Parámetros de `SocketIOHandler` constructor
- **Beneficio:** Permite inyección de dependencia (testeable)

#### `java.io.Serializable`
- **Propósito:** Interfaz marcadora para objetos serializables
- **Uso en proyecto:** `Message.java` implementa `Serializable`
- **Ventaja:** Sin ella, `ObjectOutputStream` lanza `NotSerializableException`
- **Código:**
  ```java
  public class Message implements Serializable {
      private static final long serialVersionUID = 1L;
      // Esto permite que Java versionice la clase
      // Si cambias serializing incompatibles, antiguos clientes fallan gracefully
  }
  ```

#### `java.io.IOException`
- **Propósito:** Excepción para errores de I/O (conexión perdida, socket cerrado)
- **Uso en proyecto:** Capturada en `Client.java`, `ClientHandler.java`, `SocketIOHandler.java`

### 1.3 Paquete `java.util.concurrent` - Manejo de Threads

#### `java.util.concurrent.ConcurrentHashMap` / `ConcurrentLinkedQueue`
- **Propósito:** Estructuras de datos thread-safe sin usar `synchronized`
- **Uso en proyecto:**
  - `Client.messageQueue` → `ConcurrentLinkedQueue<Message>`
  - `Server.connectedClients` → `Collections.synchronizedList()` (más lento pero flexible)
- **Ventaja:** No requiere sincronización manual para operaciones comunes

#### `java.util.concurrent.locks` (No usado pero importante contexto)
- El proyecto usa `synchronized` en lugar de `ReentrantLock` por simplicidad
- Suficiente para este caso de uso

### 1.4 Paquete `java.lang` - Threading Base

#### `java.lang.Thread`
- **Propósito:** Ejecutar código concurrentemente
- **Uso en proyecto:**
  - `Server.acceptConnections()` → bucle en thread principal
  - `ClientHandler.run()` → cada cliente en thread propio
  - `Client.startMessageReceiver()` → receptor de mensajes en thread independiente
  - `Main.startServer()` → servidor en thread daemon
- **Código servidor (acepta múltiples clientes):**
  ```java
  while (running) {
      Socket clientSocket = serverSocket.accept();  // Bloqueante
      Thread clientThread = new Thread(new ClientHandler(clientSocket, this));
      clientThread.start();  // Comienza ClientHandler.run()
  }
  ```
- **Código cliente (recibe asíncrónamente):**
  ```java
  receiverThread = new Thread(() -> {
      while (connected) {
          Message message = ioHandler.receiveMessage();  // Bloqueante
          if (listener != null) {
              listener.onMessageReceived(message);
          }
      }
  });
  receiverThread.setDaemon(true);  // Se cierra con la app
  receiverThread.start();
  ```

#### `java.lang.Runnable`
- **Propósito:** Interfaz para código ejecutable en threads
- **Uso en proyecto:**
  - `ClientHandler implements Runnable` → su método `run()` se ejecuta en thread
  - Thread lambdas (Java 8+) → `new Thread(() -> { ... })`

#### `java.lang.synchronized`
- **Propósito:** Mutuamente exclusión para evitar race conditions
- **Uso en proyecto:**
  - `GameRoom` métodos critiales (`addPlayer()`, `handlePlayCard()`, etc.)
  - `Client.sendMessage()` → solo un hilo a la vez
  - `ClientHandler.sendMessage()` → solo un hilo a la vez
- **Ejemplo:**
  ```java
  public synchronized void handlePlayCard(ClientHandler handler, Message message) {
      // Solo un thread ejecuta esto a la vez
      if (!isPlayerTurn(handler)) {
          return;
      }
      // ...
  }
  ```

#### Excepciones de Threading

- `InterruptedException` → thread fue interrumpido
  ```java
  try {
      Thread.sleep(800);  // Pausa antes de UPDATE_STATE masivo
  } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
  }
  ```

### 1.5 Paquete `java.util` - Utilidades Generales

#### `java.util.Collection` (List, Map, Set)
- `List<ClientHandler>` → almacena todos los clientes en una sala
- `Map<String, Player>` → mapea playerId → Player
- `Set<String>` → jugadores listos (readyPlayers)

#### `java.util.Collections`
- `Collections.synchronizedList()` → crea lista thread-safe
  ```java
  private final List<ClientHandler> players = Collections.synchronizedList(new ArrayList<>());
  ```

#### `java.util.logging` (Logger)
- **Propósito:** Registrar eventos del sistema (sin salida a consola ruidosa)
- **Uso en proyecto:** `Logger.getLogger()` en todas las clases de comunicación
- **Niveles:** `SEVERE`, `WARNING`, `INFO`, `FINE`

---

## 2. CLASES PERSONALIZADAS DE COMUNICACIÓN

### 2.1 `net.salesianos.protocol.Message`

#### Propósito y Diseño
- Es el **protocolo definido** entre cliente y servidor
- Reemplaza strings planos o JSON (más tipos seguros)
- Serializable → puede enviarse vía `ObjectOutputStream`

#### Estructura
```java
public class Message implements Serializable {
    public enum MessageType {
        LOGIN, PLAY_CARD, DRAW_CARD, UPDATE_STATE, 
        GAME_OVER, CHAT, ERROR, ...
    }
    
    private MessageType type;
    private Map<String, Object> data;  // Datos genéricos
}
```

#### Métodos Clave
- `getType()` → qué tipo de mensaje es
- `get(key)` / `getString(key)` / `getInteger(key)` → acceso a datos tipado
- `put(key, value)` → añade datos al mensaje
- `containsKey(key)` → verifica si clave existe

#### Ejemplos de Uso
```java
// Cliente envía carta
Message playCardMsg = new Message(Message.MessageType.PLAY_CARD);
playCardMsg.put("card", "ROJO-5");
client.sendMessage(playCardMsg);

// Servidor recibe y parsea
String cardStr = message.getString("card");
```

### 2.2 `net.salesianos.utils.SocketIOHandler`

#### Propósito
- **Encapsula** toda la lógica de I/O de sockets
- Sigue **Single Responsibility Principle** (SRP)
- Evita duplicación código entre `Client` y `ClientHandler`

#### Responsabilidades
1. Crear streams (ObjectOutputStream ANTES que ObjectInputStream)
2. Enviar mensajes (`sendMessage`)
3. Recibir mensajes (`receiveMessage`)
4. Cerrar streams gracefully

#### Implementación Clave
```java
public SocketIOHandler(OutputStream outputStream, InputStream inputStream) 
        throws IOException {
    // ORDEN IMPORTANTE: Output PRIMERO
    this.out = new ObjectOutputStream(outputStream);
    this.out.flush();  // Señal de sincronización
    
    // Input SEGUNDO
    this.in = new ObjectInputStream(inputStream);
}

public synchronized boolean sendMessage(Message message) {
    out.writeObject(message);
    out.flush();  // Força transmisión
    return true;
}

public Message receiveMessage() throws IOException, ClassNotFoundException {
    return (Message) in.readObject();  // Bloqueante
}
```

#### Por Qué Existe
- Sin esta clase: código duplicado en `Client` y `ClientHandler`
- Con esta clase: cambios centralizados en I/O afectan a ambos

### 2.3 `net.salesianos.server.Server`

#### Propósito
- Punto de entrada del servidor
- Acepta conexiones TCP
- Gestiona ciclo de vida del servidor

#### Comunicación Específica
- Escucha `ServerSocket` en puerto configurable
- Crea thread (`ClientHandler`) por cliente
- Asigna clientes a `GameRoom` disponibles

### 2.4 `net.salesianos.server.handlers.ClientHandler`

#### Propósito
- Maneja comunicación con **UN cliente específico**
- Itera leyendo mensajes (`while (running) { receiveMessage() }`)
- Cada instancia en su propio thread

#### Comunicación Específica
```java
while (running) {
    Message message = ioHandler.receiveMessage();  // Bloqueante
    handleMessage(message);  // Procesa
}
```

#### Detección de Desconexión
```java
catch (EOFException e) {  // Cliente cerró
    break;
} catch (ClassNotFoundException e) {  // Tipo desconocido
    LOGGER.log(Level.SEVERE, "Tipo de mensaje desconocido");
}
```

### 2.5 `net.salesianos.server.handlers.GameMessageHandler`

#### Propósito
- **Router** de mensajes de juego según tipo
- Separa lógica de dispatching de `ClientHandler`

#### Comunicación Específica
```java
public void handle(Message message) {
    switch (message.getType()) {
        case PLAY_CARD:
            gameRoom.handlePlayCard(handler, message);
            break;
        case DRAW_CARD:
            gameRoom.handleDrawCard(handler, message);
            break;
        // ...
    }
}
```

### 2.6 `net.salesianos.server.GameRoomBroadcaster`

#### Propósito
- Centraliza envío de mensajes a múltiples clientes
- Sigue SRP: SOLO responsable de broadcast

#### Comunicación Específica
```java
public void broadcastMessage(Message message) {
    for (ClientHandler handler : players) {
        handler.sendMessage(message);  // Envía a cada jugador
    }
}

public void broadcastStateUpdate(...) {
    // Envía UPDATE_STATE personalizado a CADA cliente
    // Su mano privada solo a él, rivales ven solo cantidad
}
```

### 2.7 `net.salesianos.client.Client`

#### Propósito
- Interfaz de comunicación del lado cliente
- Maneja threading de receptor
- Proporciona métodos simples: `connect()`, `sendMessage()`, `disconnect()`

#### Comunicación Específica
```java
public boolean connect() {
    socket = new Socket(serverHost, serverPort);
    ioHandler = new SocketIOHandler(...);
    startMessageReceiver();  // Thread asíncrono
    sendLoginMessage();      // Envía LOGIN
}

private void startMessageReceiver() {
    receiverThread = new Thread(() -> {
        while (connected) {
            Message msg = ioHandler.receiveMessage();  // Bloqueante
            listener.onMessageReceived(msg);           // Callback
        }
    });
}
```

#### Encolamiento de Mensajes
- Si llega mensaje antes de que listener esté ready → se encola
- Cuando `setMessageListener()` se llama → procesa cola

### 2.8 `net.salesianos.client.handlers.GameStateUpdateHandler`

#### Propósito
- Procesa `UPDATE_STATE` del servidor
- Separa lógica de actualización de UI (`GameFrame`)

#### Comunicación Específica
```java
public void updateFromMessage(Message message) {
    currentCard = message.getString("currentCard");
    currentPlayer = message.getString("currentPlayer");
    
    @SuppressWarnings("unchecked")
    List<String> hand = (List<String>) message.get("hand");
    // Su mano privada
    
    listener.onPlayerHandUpdated(hand);
    listener.onCurrentPlayerChanged(currentPlayer, isMyTurn);
    // ...
}
```

---

## 3. RESUMEN TABLA COMPLETA

| Librería/Clase | Propósito | Usado Por | Crítico Para Comunicación |
|---|---|---|---|
| `java.net.ServerSocket` | Escuchar conexiones | `Server` | ✅ SÍ |
| `java.net.Socket` | Conexión TCP | `Client`, `ClientHandler` | ✅ SÍ |
| `java.io.ObjectOutputStream` | Serializar envíos | `SocketIOHandler` | ✅ SÍ |
| `java.io.ObjectInputStream` | Deserializar recepciones | `SocketIOHandler` | ✅ SÍ |
| `java.io.Serializable` | Interfaz marcadora | `Message` | ✅ SÍ |
| `java.io.EOFException` | Fin de stream | `Client`, `ClientHandler` | ✅ SÍ |
| `java.lang.Thread` | Concurrencia | `Server`, `Client`, `ClientHandler` | ✅ SÍ |
| `java.lang.Runnable` | Ejecutable en thread | `ClientHandler` | ✅ SÍ |
| `java.lang.synchronized` | Mutuamente exclusión | `GameRoom`, varios | ✅ SÍ |
| `java.util.Collections.synchronized*` | Thread-safe collections | `Server`, `GameRoom` | ✅ SÍ |
| `java.util.concurrent.ConcurrentLinkedQueue` | Queue thread-safe | `Client.messageQueue` | ✅ SÍ |
| `java.util.logging.Logger` | Logging | Todas las clases | ❌ NO (informativo) |
| `Message` | Protocolo de mensajes | Cliente ↔ Servidor | ✅ SÍ |
| `SocketIOHandler` | Encapsulación I/O | `Client`, `ClientHandler` | ✅ SÍ |
| `Server` | Servidor TCP | Aplicación servidor | ✅ SÍ |
| `ClientHandler` | Gestor de cliente | Aplicación servidor | ✅ SÍ |
| `GameMessageHandler` | Router de mensajes | `ClientHandler` | ✅ SÍ |
| `GameRoomBroadcaster` | Broadcast de mensajes | `GameRoom` | ✅ SÍ |
| `Client` | Cliente TCP | Aplicación cliente | ✅ SÍ |
| `GameStateUpdateHandler` | Procesador de estado | `GameFrame` | ✅ SÍ |

---

## Conclusión

La arquitectura de comunicación se construye en capas:

1. **Capa de Red:** `Socket`, `ServerSocket` (java.net)
2. **Capa de Serialización:** `ObjectInputStream/OutputStream` (java.io)
3. **Capa de Protocolo:** `Message` (definición propia)
4. **Capa de Encapsulación:** `SocketIOHandler` (utilidad)
5. **Capa de Aplicación:** `Client`, `Server`, `GameRoom` (lógica)
6. **Capa de Concurrencia:** `Thread`, `synchronized` (java.lang)

Cada capa es **responsable de una cosa** (SRP), lo que facilita testing, debugging y mantenimiento.

---
[← Volver al README](../README.md)

