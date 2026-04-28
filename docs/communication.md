# 🌐 Comunicación Cliente-Servidor - UNO Game

Este documento detalla el funcionamiento interno de la red y el intercambio de mensajes entre el cliente y el servidor en el juego UNO.

## Tipo de Comunicación: TCP Sockets

El proyecto utiliza **Sockets TCP (Transmission Control Protocol)** para la comunicación. Se eligió TCP sobre UDP por varias razones críticas para un juego de cartas:

1.  **Fiabilidad:** TCP garantiza que todos los paquetes lleguen a su destino en el orden correcto. En un juego de cartas, perder un mensaje de "Robar carta" o "Cambio de turno" rompería la lógica del juego.
2.  **Orientado a la Conexión:** Permite mantener una sesión activa entre el cliente y el servidor, facilitando la detección de desconexiones (mediante `EOFException`).
3.  **Flujo de Datos:** Facilita el uso de `ObjectOutputStream` y `ObjectInputStream` para enviar objetos Java completos.

## Protocolo de Mensajes (`Message`)

En lugar de enviar texto plano, el sistema utiliza una clase personalizada llamada `Message`. Esta clase contiene:
-   `type`: Un enum `MessageType` que identifica la acción (LOGIN, PLAY_CARD, UPDATE_STATE, etc.).
-   `data`: Un `Map<String, Object>` que almacena los parámetros necesarios para esa acción.

## Flujo de Comunicación por Acción

### 1. Conexión e Inicio (Handshake)
-   **Cliente → Servidor (`LOGIN`):** El cliente envía su nombre de usuario.
-   **Servidor → Cliente (`LOBBY_UPDATE`):** El servidor asigna al cliente una `GameRoom` y le envía la lista de jugadores conectados actualmente en esa sala.

### 2. Gestión del Lobby
-   **Cliente → Servidor (`PLAYER_READY`):** El cliente indica que está listo para empezar.
-   **Servidor → Todos (`LOBBY_UPDATE`):** Se actualiza el contador de jugadores listos para todos. Cuando todos están listos (mín. 2), el servidor envía `START_GAME`.

### 3. Sincronización del Estado del Juego (`UPDATE_STATE`)
Esta es la parte más importante. El servidor es la **única fuente de verdad**.
-   Cada vez que ocurre un cambio (se juega carta, se roba, cambia el turno), el servidor llama a `broadcastStateUpdate()`.
-   **Datos enviados:**
    -   `currentCard`: La carta que está arriba en el descarte.
    -   `currentPlayer`: Quién tiene el turno.
    -   `direction`: Sentido del juego (horario/antihorario).
    -   `players`: Una lista con los nombres de los rivales y cuántas cartas tienen (para que el cliente pueda dibujarlos).
    -   `hand` (**Personalizado**): Solo el jugador destinatario recibe el contenido real de su mano. El servidor filtra este mensaje para que ningún jugador sepa las cartas de los demás.

### 4. Acciones de Juego
-   **Robar Carta (`DRAW_CARD`):**
    1.  El cliente envía `DRAW_CARD`.
    2.  El servidor verifica si es el turno de ese jugador.
    3.  El servidor saca una carta del `Deck`, se la añade al `Player`.
    4.  El servidor avanza el turno y envía un `UPDATE_STATE` masivo.
-   **Jugar Carta (`PLAY_CARD`):**
    1.  El cliente envía `PLAY_CARD` con el string de la carta (ej: "ROJO-CINCO").
    2.  El servidor parsea la carta y valida: ¿Es su turno? ¿Tiene esa carta? ¿Es válida sobre la actual?
    3.  Si es válida, actualiza el estado y hace broadcast. Si no, envía un mensaje de `ERROR` solo a ese cliente.

### 5. Sistema "UNO" (`UNO_BUTTON`)
-   Cuando a un jugador le queda **1 carta**, el servidor envía un mensaje `UNO_BUTTON (action: SHOW)` a todos.
-   Esto hace que aparezca el botón flotante en todos los clientes.
-   El primero en pulsar envía `UNO_BUTTON` al servidor.
-   El servidor determina si el pulsador fue el poseedor de la carta (se salva) o un rival (penalización de +2 al poseedor).

## Implementación Técnica: Handlers

Para mantener el código limpio, se han separado las responsabilidades:
-   **`SocketIOHandler`**: Encapsula el envío y recepción de objetos, gestionando el `flush()` y el orden de creación de los streams (Output antes que Input para evitar deadlocks).
-   **`GameRoomBroadcaster`**: Se encarga de iterar sobre los clientes de una sala y enviar los mensajes correspondientes, filtrando información sensible si es necesario.
-   **`GameMessageHandler`**: En el servidor, recibe los mensajes del socket y los dirige al método correspondiente de `GameRoom`.

[ Volver al README ](../README.md)