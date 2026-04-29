[← Volver al README](../../README.md)
---

# Cliente: Comunicación Asíncrona con Servidor

El cliente es la **interfaz del jugador**: se conecta al servidor por TCP, envía sus acciones y recibe actualizaciones de estado en tiempo real sin bloquear la interfaz visual.

### Flujo de pantallas

```
LoginFrame → conectar al servidor
    ↓
LobbyFrame → esperar que otros jugadores entren
    ↓
GameFrame  → partida en vivo
    ↓
FIN        → al recibir GAME_OVER
```

---

## 1. Conexión al servidor

Al hacer clic en "Conectar", el cliente realiza tres pasos en orden:

1. **Abre un socket TCP** hacia la IP y puerto del servidor. Esta llamada es bloqueante: espera hasta que el servidor acepte o la conexión falle.
2. **Crea el `SocketIOHandler`**, que envuelve los streams del socket con `ObjectInputStream` / `ObjectOutputStream` para enviar y recibir objetos Java serializados.
3. **Lanza el hilo receptor** y envía inmediatamente un mensaje `LOGIN` con el nombre del jugador.

Si el servidor no está disponible, se lanza una `ConnectException` y se muestra el error al usuario. El resto del proceso no empieza.

---

## 2. Hilo receptor: recibir sin bloquear la UI

Este es el núcleo asíncrono del cliente. El problema es sencillo: **esperar un mensaje del servidor es una operación bloqueante**. Si se hiciera en el hilo principal (el EDT de Swing), la ventana se congelaría hasta recibir algo.

La solución es lanzar un **hilo dedicado solo a escuchar**. Este hilo corre en un bucle continuo: llama a `receiveMessage()`, que bloquea hasta que llega algo, y cuando llega, lo pasa al `listener` (la pantalla activa) para que actualice la UI.

```
Hilo receptor (background)          Hilo de UI (EDT)
───────────────────────────         ─────────────────
espera mensaje...
← recibe UPDATE_STATE
llama listener.onMessageReceived()  → GameFrame actualiza pantalla
espera mensaje...
← recibe UNO_BUTTON
llama listener.onMessageReceived()  → GameFrame muestra botón UNO
espera mensaje...
```

Si el servidor cierra la conexión, el hilo recibe una `EOFException` y llama a `disconnect()` para limpiar recursos y avisar al usuario.

El hilo receptor se marca como **daemon**, lo que significa que se cierra automáticamente cuando la aplicación termina, sin necesidad de pararle explícitamente.

---

## 3. Cola de mensajes: no perder nada durante las transiciones

Hay una ventana de tiempo peligrosa: justo después de conectar, el servidor puede enviar mensajes (`LOBBY_UPDATE`, por ejemplo) **antes de que la pantalla del lobby esté lista para procesarlos**. Si el `listener` es `null` en ese momento, el mensaje se perdería.

Para evitarlo, el hilo receptor usa esta lógica:

- Si el `listener` ya está asignado → procesa el mensaje directamente.
- Si el `listener` todavía es `null` → guarda el mensaje en una **cola en memoria**.

Cuando la pantalla del lobby se crea y se registra como `listener`, lo primero que hace el cliente es vaciar esa cola y procesar todos los mensajes acumulados. A partir de ese punto, los mensajes se entregan al instante.

```
Antes de que LobbyFrame exista:
  LOBBY_UPDATE recibido → encolado 1
  LOBBY_UPDATE recibido → encolado 2

LobbyFrame se registra como listener:
  → procesa 1 → actualiza lista de jugadores
  → procesa 2 → actualiza lista de jugadores

A partir de aquí, mensajes se entregan directamente.
```

---

## 4. Envío de mensajes

Enviar un mensaje es simple: el cliente construye un objeto `Message` con el tipo y los datos necesarios, y lo pasa a `sendMessage()`.

Lo importante aquí es que `sendMessage()` está marcado como `synchronized`. Esto se debe a que **varios hilos pueden querer enviar a la vez**: el hilo de UI cuando el usuario juega una carta, y el hilo receptor cuando detecta una desconexión. Sin sincronización, los dos podrían escribir en el socket simultáneamente y corromper los datos. Con `synchronized`, solo uno escribe a la vez.

---

## 5. Tipos de mensajes

| Mensaje | Dirección | Qué hace |
|---|---|---|
| `LOGIN` | Cliente → Servidor | Registra al jugador y le asigna una sala |
| `LOBBY_UPDATE` | Servidor → Cliente | Actualiza la lista de jugadores en sala |
| `START_GAME` | Servidor → Cliente | Cierra el lobby y abre la partida |
| `PLAYER_READY` | Cliente → Servidor | Indica que el jugador está listo |
| `PLAY_CARD` | Cliente → Servidor | El jugador juega una carta |
| `DRAW_CARD` | Cliente → Servidor | El jugador roba una carta |
| `UPDATE_STATE` | Servidor → Cliente | Estado completo del juego (mano, turno, carta actual) |
| `UNO_BUTTON` | Ambos | Mostrar u ocultar el botón de UNO |
| `CHAT` | Ambos | Mensaje de chat en partida |
| `ERROR` | Servidor → Cliente | Acción inválida (ej: "no es tu turno") |
| `GAME_OVER` | Servidor → Cliente | Fin de partida con el ganador |

---

## 6. Ciclo de una jugada completa

Para ver cómo encaja todo, aquí el recorrido de un clic en una carta hasta que la UI se actualiza:

```
1. El usuario hace clic en "ROJO-5" en GameFrame
2. El EDT construye un mensaje PLAY_CARD y llama a sendMessage()
3. El mensaje viaja por TCP al servidor
4. El servidor valida la jugada y actualiza el estado del juego
5. El servidor envía UPDATE_STATE a los 4 clientes
6. El hilo receptor de cada cliente recibe el UPDATE_STATE
7. Llama a listener.onMessageReceived() → GameFrame redibuja la pantalla
8. Cada jugador ve la carta jugada, el nuevo turno y su mano actualizada
```

Todo esto ocurre en menos de un segundo. La UI nunca se congela porque los pasos 6–8 ocurren en el hilo receptor, no en el EDT.

---

## 7. Desconexión

La desconexión puede ocurrir por tres razones:

- **El usuario cierra la ventana** → se llama a `disconnect()` manualmente.
- **El servidor se apaga** → el hilo receptor recibe `EOFException` y llama a `disconnect()`.
- **Error de red** → el hilo receptor recibe `IOException` y hace lo mismo.

En todos los casos, `disconnect()` marca la conexión como cerrada, cierra el socket y notifica al `listener` con `onDisconnected()`, para que la UI pueda mostrar un aviso y cerrar la aplicación de forma limpia.

---

## Resumen

El cliente combina tres mecanismos para comunicarse de forma fiable y sin bloqueos:

- Un **hilo receptor en background** que escucha mensajes sin congelar la UI.
- Una **cola de mensajes** que evita perder eventos durante las transiciones entre pantallas.
- Un **`sendMessage()` sincronizado** que protege el socket cuando varios hilos quieren escribir a la vez.

El resultado es una interfaz siempre responsive que se mantiene sincronizada con el servidor en tiempo real.

---
[← Volver al README](../../README.md)