[← Volver al README](../README.md)
---

# Roles del Cliente y Servidor

## 1. ROL DEL SERVIDOR

El servidor es el **orquestador centralizado** de toda la lógica del juego. Es la única entidad autorizada para tomar decisiones sobre el estado del juego.

### Funciones Específicas

#### 1.1 Aceptar Conexiones de Clientes
- **Clase:** `Server.java`
- **Mecanismo:** Abre un `ServerSocket` en puerto configurable (ej: 8888)
- **Proceso:**
  ```java
  serverSocket = new ServerSocket(port);  // Escucha conexiones
  Socket clientSocket = serverSocket.accept();  // Bloquea hasta recibir cliente
  ```
- **Concurrencia:** Crea un `ClientHandler` **nuevo** en un **thread independiente** para cada cliente
- **Beneficio:** Múltiples clientes se conectan sin bloquearse entre sí

#### 1.2 Gestionar Salas de Juego (GameRooms)
- **Clase:** `Server.java` (mantiene lista) + `GameRoom.java` (lógica)
- **Funciones:**
  - Crear salas automáticamente cuando llega el primer cliente
  - Asignar clientes a salas disponibles (máx. 4 jugadores/sala)
  - Mantener lista sincronizada de salas activas
- **Código:**
  ```java
  // Server busca sala disponible o crea nueva
  GameRoom availableRoom = gameRooms.stream()
      .filter(r -> r.getPlayerCount() < MAX_PLAYERS && !r.isGameStarted())
      .findFirst()
      .orElseGet(() -> new GameRoom("ROOM_" + System.currentTimeMillis()));
  ```

#### 1.3 Validar Reglas del Juego
- **Clase:** `GameRoom.java`
- **Validaciones:**
  - ¿Es turno de este jugador? → Si no, rechaza con ERROR
  - ¿Tiene el jugador esta carta? → Si no, error
  - ¿Es válida la carta sobre la actual? → Usa lógica de `Card.canBePlayedOn()`
  - ¿Hay suficientes jugadores para iniciar? (mín 2, máx 4)
  - ¿Están todos los jugadores listos?

#### 1.4 Sincronizar Estado del Juego
- **Clase:** `GameRoomBroadcaster.java` (envía) + `GameRoom.java` (lógica)
- **Qué se sincroniza:**
  - Carta visible actual (`currentCard`)
  - Quién tiene turno (`currentPlayer`)
  - Dirección del juego (horario/antihorario)
  - Lista de jugadores y cuántas cartas tiene cada uno
  - **Información privada:** Solo el jugador destinatario recibe su mano completa
- **Frecuencia:** Después de CADA acción (jugar, robar, cambio de turno)
- **Código:**
  ```java
  broadcaster.broadcastStateUpdate(
      gameState.getCurrentCard().toString(),
      gameState.getCurrentPlayer().getName(),
      gameState.getDirection(),
      gameState.getPlayers(),
      playerMap, playerIdOrder, players
  );
  ```

#### 1.5 Detectar Desconexiones
- **Mecanismo:** Captura `EOFException` cuando cliente cierra conexión
- **Acciones:**
  - Remover jugador de la sala
  - Si la partida está en marcha y quedan < 2 jugadores → termina partida
  - Notificar a otros jugadores

#### 1.6 Gestionar Penalizaciones UNO
- **Clase:** `GameRoom.handleUnoButton()`
- **Lógica:**
  - Cuando jugador se queda con 1 carta → envía alerta a TODOS
  - Primer jugador en presionar botón:
    - Si es el dueño de la carta → se salva
    - Si es otro jugador → dueño recibe +2 cartas de penalización
  - Solo 1 "disputa UNO" por jugada (ya se resolvió)

#### 1.7 Broadcast de Mensajes
- **Clase:** `GameRoomBroadcaster.java`
- **Tipos de broadcast:**
  - `broadcastMessage()` → a TODOS
  - `broadcastMessageExcept()` → a todos MENOS uno
  - `sendToPlayer()` → a un jugador específico
  - `broadcastError()` → error a todos
  - `broadcastLobbyUpdate()` → actualización del lobby

### Resumen de Responsabilidades del Servidor

| Responsabilidad | Clase | Thread-Safe |
|---|---|---|
| Aceptar conexiones TCP | `Server` | Sí (ServerSocket es thread-safe) |
| Gestionar clientes | `Server` + `ClientHandler` | Sí (ConcurrentHashMap, Collections.synchronized) |
| Validar reglas | `GameRoom` | Sí (synchronized blocks) |
| Sincronizar estado | `GameRoomBroadcaster` | Sí (broadcasta a todos atomáticamente) |
| Detectar desconexiones | `ClientHandler` | Sí (try-catch EOFException) |
| Gestionar castigos | `GameRoom.handleUnoButton()` | Sí (synchronized) |

---

## 2. ROL DEL CLIENTE

El cliente es la **interfaz del usuario** y **agente de comunicación**. Envía acciones del jugador al servidor y recibe/muestra actualizaciones de estado.

### Funciones Específicas

#### 2.1 Conectarse al Servidor
- **Clase:** `Client.java`
- **Proceso:**
  ```java
  socket = new Socket(serverHost, serverPort);
  ioHandler = new SocketIOHandler(socket.getOutputStream(), socket.getInputStream());
  ```
- **Parámetros:** Nombre de jugador, IP/host del servidor, puerto
- **Validación:** Si conexión falla → muestra error al usuario

#### 2.2 Enviar Mensaje de Login
- **Clase:** `Client.sendLoginMessage()`
- **Contenido:** Nombre del jugador
- **Servidor responde:** Con LOBBY_UPDATE (lista de jugadores conectados)

#### 2.3 Recibir Mensajes Asíncrónamente
- **Clase:** `Client.startMessageReceiver()`
- **Mecanismo:**
  - Thread receptor que escucha continuamente del servidor
  - No bloquea la interfaz gráfica (UI en thread EDT de Swing)
  - Encolamiento de mensajes si no hay listener (evita pérdida)
- **Código:**
  ```java
  receiverThread = new Thread(() -> {
      while (connected) {
          Message message = ioHandler.receiveMessage();  // Bloqueante
          if (listener != null) {
              listener.onMessageReceived(message);  // Procesa
          } else {
              messageQueue.add(message);  // Encola si no hay listener
          }
      }
  });
  ```

#### 2.4 Enviar Acciones al Servidor
- **Clase:** `Client.sendMessage()`
- **Tipos de acciones:**
  - `PLAYER_READY` → "Estoy listo" (lobby)
  - `PLAY_CARD` → "Juego esta carta"
  - `DRAW_CARD` → "Robo una carta"
  - `UNO_BUTTON` → "¡UNO!" (alguien con 1 carta)
  - `CHAT` → Mensaje de chat

#### 2.5 Mostrar Interfaz Gráfica
- **Clases Implicadas:**
  - `LoginFrame.java` → Conexión inicial
  - `LobbyFrame.java` → Lista de jugadores, botón "Listo"
  - `GameFrame.java` → Juego en vivo (mano, carta visible, turno)
- **Responsabilidades:**
  - `LoginFrame` → Recolecta credenciales y conecta
  - `LobbyFrame` → Muestra jugadores, activa "Listo", espera START_GAME
  - `GameFrame` → Renderiza mano, botones de cartas, chat, indicador de turno

#### 2.6 Procesar Actualizaciones de Estado
- **Clase:** `GameStateUpdateHandler.java`
- **Qué actualiza:**
  - Mano del jugador (su información privada)
  - Lista de rivales y cuántas cartas tienen
  - Carta visible en descarte
  - Indicador de quién tiene turno
  - Dirección del juego (flechas ↻/↺)
- **Mapeo de mensaje UPDATE_STATE:**
  ```
  Servidor envía → cliente.hand = [ROJO-5, AZUL-3, ...]  (su mano privada)
                    cliente.players = [{name: "jugador2", handSize: 6}, ...]
                    cliente.currentCard = "ROJO-5"
                    cliente.currentPlayer = "Mi nombre"
                    cliente.direction = 1  (horario)
  ```

#### 2.7 Reaccionar a Eventos

| Evento | Qué Hace | Clase |
|---|---|---|
| LOGIN exitoso | Abre LobbyFrame | `LoginFrame.LoginListener` |
| LOGIN fallido | Muestra error | `LoginFrame.LoginListener` |
| LOBBY_UPDATE | Actualiza lista de jugadores | `LobbyFrame` |
| START_GAME | Cierra lobby, abre GameFrame | `LobbyFrame.LobbyListener` |
| UPDATE_STATE | Renderiza nueva mano, jugadores, turno | `GameFrame` + `GameStateUpdateHandler` |
| UNO_BUTTON (SHOW) | Muestra botón flotante | `GameFrame` |
| UNO_BUTTON (HIDE) | Oculta botón flotante | `GameFrame` |
| GAME_OVER | Muestra ganador, cierra aplicación | `GameFrame` |
| ERROR | Muestra mensaje al usuario | `GameFrame` |
| Desconexión | Cierra ventana, sale de aplicación | `Client.MessageListener` |

#### 2.8 Detectar Desconexión
- **Mecanismo:** Captura `EOFException` o `IOException` en receptor
- **Acción:** Llama a `listener.onDisconnected()` → cierra aplicación

### Resumen de Responsabilidades del Cliente

| Responsabilidad | Clase | Síncrono/Asíncrono |
|---|---|---|
| Conectar al servidor | `Client` | Síncrono (bloqueante) |
| Enviar LOGIN | `Client.sendLoginMessage()` | A través de message sender |
| Recibir mensajes | `Client.startMessageReceiver()` | Asíncrono (thread separado) |
| Enviar acciones | `Client.sendMessage()` | Síncrono (sincronizado) |
| Mostrar UI | `LoginFrame/LobbyFrame/GameFrame` | Síncrono (EDT) |
| Procesar actualizaciones | `GameStateUpdateHandler` | Síncrono (desde receiver thread) |
| Reaccionar a eventos | Listeners de frames | Síncrono (EDT de Swing) |
| Detectar desconexión | `Client` (receiver thread) | Asíncrono |

---

## 3. COMUNICACIÓN CLIENTE-SERVIDOR

### Modelo de Interacción

```
CLIENTE                              SERVIDOR
  |                                    |
  |-----(1) Socket.connect()---------->|
  |<----(2) Acepta connection----------|
  |                                    |
  |-----(3) LOGIN message------------->|
  |                                    | (Crea ClientHandler)
  |<----(4) LOBBY_UPDATE---------------|
  |                                    |
  |<--(5) LOBBY_UPDATE (broadcast |<---|-(otros jugadores se unen)
  |         de otros clientes)         |
  |                                    |
  |-----(6) PLAYER_READY-------------->|
  |                                    |
  |<----(7) START_GAME (cuando    |----|-(todos listos)
  |         condiciones se cumplen)    |
  |                                    |
  |-----(8) PLAY_CARD----------------->|
  |                                    | (valida reglas)
  |<----(9) UPDATE_STATE---------------|-(broadcast a todos)
  |                                    |
  | (Repite 8-9 hasta GAME_OVER)       |
  |                                    |
  |<---(10) GAME_OVER------------------|
  |        (ganador anunciado)         |
  |                                    |
  |-----(11) Cierra socket------------>|
```

### Garantías de Confiabilidad

- **TCP garantiza:** Orden de mensajes, entrega confiable, sin duplicados
- **ObjectInputStream/ObjectOutputStream:** Serialización automática de objetos Java
- **Sincronización servidor:** `synchronized` blocks evitan race conditions
- **Encolamiento cliente:** Mensajes no se pierden antes de que listener esté listo

---
[← Volver al README](../README.md)

