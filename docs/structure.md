# 🏗️ Estructura del Proyecto UNO Game

## Carpetas y Responsabilidades

### 📂 `net/salesianos/server/` - LADO SERVIDOR
**Responsabilidad:** Gestionar la comunicación con clientes, validar jugadas y sincronizar estado del juego.

#### Clases:
- **`Server.java`** - Aceptar conexiones TCP entrantes en el puerto 8888 y crear ClientHandlers
- **`GameRoom.java`** - Orquestador del juego: gestiona jugadores, valida jugadas, sincroniza estado, aplica reglas
- **`GameRoomBroadcaster.java`** - Enviar mensajes a todos/algunos clientes
- **`handlers/ClientHandler.java`** - Manejar comunicación con UN cliente específico en su propio hilo

---

### 📂 `net/salesianos/client/` - LADO CLIENTE
**Responsabilidad:** Conectar al servidor, recibir estado del juego y permitir interacción del usuario.

#### Clases:
- **`Client.java`** - Conectar al servidor, gestionar thread receptor de mensajes, enviar acciones del jugador
- **`handlers/GameStateUpdateHandler.java`** - Procesar actualizaciones de estado del servidor
- **`handlers/GameChatHandler.java`** - Procesar mensajes de chat

---

### 📂 `net/salesianos/model/` - MODELOS DE DATOS
**Responsabilidad:** Representar la lógica pura del juego sin networking.

#### Clases:
- **`Card.java`** - Representar una carta (color + valor), validar si es jugable
- **`Deck.java`** - Gestionar mazo y descartes, repartir y robar cartas
- **`Player.java`** - Representar un jugador, gestionar su mano de cartas
- **`GameState.java`** - Lógica pura: turnos, validación de jugadas, efectos de cartas

---

### 📂 `net/salesianos/protocol/` - COMUNICACIÓN
**Responsabilidad:** Definir el protocolo de mensajes entre cliente y servidor.

#### Clases:
- **`Message.java`** - Clase serializable con tipos de mensaje y mapa genérico de datos

---

### 📂 `net/salesianos/client/ui/` - INTERFAZ GRÁFICA

#### `frames/` - Ventanas principales:
- **`LoginFrame.java`** - Entrada de usuario (nombre, host, puerto)
- **`LobbyFrame.java`** - Mostrar jugadores conectados, botón "Listo"
- **`GameFrame.java`** - Ventana de juego (mano, carta visible, turno, chat)
- **`MainMenuDialog.java`** - Menú inicial (Servidor/Cliente)
- **`ServerConfigDialog.java`** - Configurar puerto del servidor
- **`ServerActiveFrame.java`** - Ventana del servidor (muestra estado)

#### `components/` - Componentes reutilizables:
- **`CardButton.java`** - Botón visual de una carta
- **`GameUIComponentFactory.java`** - Factory para crear componentes UI
- **`PlayerListRenderer.java`** - Renderizar lista de jugadores
- **`GameButton.java`** - Botón genérico del juego
- **`ColorUtils.java`** - Utilidades de colores para UI

---

### 📂 `net/salesianos/utils/` - UTILIDADES

#### Clases:
- **`SocketIOHandler.java`** - Centralizar envío/recepción de objetos Message por Socket
- **`CardParser.java`** - Convertir strings como "ROJO-5" a objetos Card

---

## Flujo de Datos

```
CLIENTE                          SERVIDOR
  ↓                                ↓
LoginFrame ----LOGIN---→ Server ----accept()-→ ClientHandler
  ↓                                ↓
LobbyFrame ----PLAYER_READY---→ GameRoom (checkGameStart)
  ↓                                ↓
GameFrame ←----START_GAME----- startGame()
  ↓                                ↓
[Usuario juega] ----PLAY_CARD---→ GameRoom.handlePlayCard()
  ↓                                ↓
[Recibe UPDATE_STATE] ←------ [Valida + Broadcasting]
  ↓                                ↓
[UI actualizada]              [Estado sincronizado]
```

---

## Responsabilidades Resumidas

| Componente | Responsabilidad |
|-----------|-----------------|
| **Server** | Escuchar conexiones TCP |
| **ClientHandler** | Recibir mensajes de UN cliente |
| **GameRoom** | Validar reglas, sincronizar juego |
| **GameRoomBroadcaster** | Enviar mensajes a clientes |
| **Client** | Conectar y recibir del servidor |
| **GameState** | Lógica pura de juego (sin network) |
| **Card/Deck/Player** | Modelos de datos del UNO |
| **GameFrame** | UI principal del juego |
| **LobbyFrame** | UI pre-juego |
| **SocketIOHandler** | Serializar/deserializar objetos |
| **Message** | Protocolo de comunicación |

[ Volver al README ](../README.md)