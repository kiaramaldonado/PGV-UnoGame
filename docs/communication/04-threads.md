[← Volver al README](../../README.md)
---

# Servidor Multihilo: Gestión Concurrente de Clientes

## ¿Por qué multihilo?

Sin multihilo, el servidor atiende clientes **uno a uno**: mientras procesa al Cliente 1, los demás esperan bloqueados. Con multihilo, cada cliente recibe su propio hilo y todos se procesan **en paralelo**.

```
SIN multihilo          CON multihilo
────────────────       ─────────────────────────────
Server procesa C1  →   Server acepta → Thread 1: C1 ┐
C2, C3, C4 esperan     Server acepta → Thread 2: C2 ├ simultáneos
                       Server acepta → Thread 3: C3 ┘
```

---

## Arquitectura: cómo funciona

El servidor tiene **dos responsabilidades**: aceptar conexiones (hilo principal) y procesar cada cliente (hilos independientes).

```java
// Bucle principal: acepta conexiones sin parar
private void acceptConnections() {
    while (running) {
        Socket clientSocket = serverSocket.accept(); // bloqueante, pero solo para este hilo
        
        ClientHandler handler = new ClientHandler(clientSocket, this);
        connectedClients.add(handler);
        
        Thread clientThread = new Thread(handler);  // nuevo hilo por cliente
        clientThread.start();                        // empieza ClientHandler.run()
    }
}
```

> `serverSocket.accept()` bloquea **solo el hilo principal** mientras espera. Los hilos de otros clientes siguen ejecutándose sin interrupción.

---

## ClientHandler: ciclo de vida de un cliente

Cada `ClientHandler` corre en su propio hilo e implementa `Runnable`. Su vida tiene 4 fases:

```
1. INICIO      → Crea SocketIOHandler (streams de entrada/salida)
2. RECEPCIÓN   → Bucle: espera mensajes del cliente
3. PROCESADO   → Redirige cada mensaje según su tipo
4. CIERRE      → Limpia recursos si hay error o desconexión
```

```java
@Override
public void run() {
    try {
        ioHandler = new SocketIOHandler(socket.getOutputStream(), socket.getInputStream());
        
        while (running) {
            Message msg = ioHandler.receiveMessage(); // bloqueante: espera datos del cliente
            handleMessage(msg);                       // procesa sin afectar a otros hilos
        }
        
    } catch (EOFException e) {
        // El cliente cerró la conexión normalmente → salir del bucle
    } catch (IOException e) {
        // Error de red → salir
    } finally {
        disconnect(); // siempre limpiar, pase lo que pase
    }
}

private void handleMessage(Message msg) {
    switch (msg.getType()) {
        case LOGIN  -> handleLogin(msg);         // autenticación + asignación a sala
        default     -> gameMessageHandler.handle(msg); // PLAY_CARD, DRAW_CARD, etc.
    }
}
```

> Cada hilo tiene su propio `ioHandler`, por lo que **no comparten estado** entre sí. El único recurso compartido es `GameRoom`.

---

## Sincronización: evitar condiciones de carrera

Una **condición de carrera** ocurre cuando dos hilos leen y modifican el mismo dato a la vez, produciendo resultados incoherentes. Por ejemplo:

```
Thread 1 (Cliente A): "¿Es mi turno?" → SÍ → juega carta
Thread 2 (Cliente B): "¿Es mi turno?" → SÍ → juega carta  ← ¡dos turnos a la vez!
```

La solución es `synchronized`: garantiza que **solo un hilo a la vez** ejecuta ese método.

```java
// GameRoom.java
public synchronized void handlePlayCard(ClientHandler handler, Message msg) {
    if (!isPlayerTurn(handler)) {
        broadcaster.sendErrorToPlayer(handler, "No es tu turno");
        return; // el otro hilo esperó y llegó tarde
    }
    gameState.playCurrentPlayerCard(card);
    broadcastStateUpdate(); // notifica a TODOS los clientes
}
```

Otros mecanismos usados en el proyecto:

| Mecanismo | Dónde | Para qué |
|---|---|---|
| `Collections.synchronizedList()` | `Server.java` | Lista de clientes/salas segura entre hilos |
| `synchronized` en métodos | `GameRoom.java`, `SocketIOHandler.java` | Turno, acceso a estado de partida, envío de mensajes |
| `volatile` | `GameRoom.java` | Variables leídas frecuentemente sin necesitar lock completo |

---

## Garantías del sistema

| Propiedad | Cómo se logra |
|---|---|
| **Exclusión mutua** en turnos | `synchronized` en métodos de `GameRoom` |
| **Visibilidad** de cambios entre hilos | Memory barrier implícito en `synchronized` |
| **Mensajes en orden** | TCP garantiza entrega ordenada |
| **Detección de desconexión** | `EOFException` capturada → `disconnect()` limpia recursos |
| **Broadcasting coherente** | `GameRoomBroadcaster` envía estado completo a todos tras cada acción |

---

## Resumen

El servidor usa un **hilo por cliente** con `ClientHandler`. Cada hilo es independiente excepto al acceder a `GameRoom`, donde `synchronized` evita conflictos. El resultado es un sistema que soporta múltiples partidas simultáneas, detecta desconexiones de forma segura y escala añadiendo más `GameRoom`s sin modificar la lógica de red.

---
[← Volver al README](../../README.md)
