# 🎴 UNO Game - Multi-jugador (TCP Sockets)

Este proyecto es una implementación completa y refactorizada del clásico juego **UNO**, diseñada para funcionar en un entorno multi-jugador distribuido utilizando una arquitectura **Cliente-Servidor** sobre sockets TCP en Java.

## Características Principales

- **Arquitectura Cliente-Servidor:** Servidor multihilo capaz de gestionar múltiples salas de juego simultáneas.
- **Protocolo de Comunicación Robusto:** Basado en la serialización de objetos (`Message`) para un intercambio de datos fluido y tipado.
- **Interfaz Gráfica Atractiva:** Desarrollada con `Swing`, utilizando componentes personalizados (`GameButton`, `CardButton`) y una estética moderna y oscura.
- **Sistema de Chat:** Comunicación en tiempo real entre los jugadores de una sala.
- **Lógica de Juego Completa:** Gestión de turnos, efectos de cartas (Saltar, Reversa, +2), mazo de robo/descarte y el botón "UNO" con sistema de penalizaciones.
- **Refactorización Limpia (SOLID):** Separación clara de responsabilidades en handlers especializados (`GameMessageHandler`, `GameRoomBroadcaster`, `GameStateUpdateHandler`, etc.).

## Tecnologías Utilizadas

- **Java SE:** Lenguaje principal.
- **Swing:** Para la interfaz de usuario.
- **Sockets TCP:** Comunicación de red.
- **Serialización de Objetos:** Para el protocolo de mensajes.

## Estructura del Proyecto

- `src/Main.java`: Punto de entrada único (permite elegir modo Servidor o Cliente).
- `src/net/salesianos/server`: Lógica del servidor, gestión de salas y broadcasting.
- `src/net/salesianos/client`: Lógica del cliente y comunicación con el servidor.
- `src/net/salesianos/client/ui`: Interfaces gráficas y manejadores de UI.
- `src/net/salesianos/model`: Modelos del dominio (Carta, Mazo, Jugador, Estado del Juego).
- `src/net/salesianos/protocol`: Definición del protocolo de comunicación.
- `src/net/salesianos/util`: Utilidades para I/O de sockets y parseo de datos.

## Cómo Ejecutar

1. **Compilar el proyecto:** Asegúrate de tener el JDK instalado.
2. **Ejecutar `Main.java`**:
   - Selecciona **"Crear Sala"** para iniciar el servidor. Configura el puerto (por defecto 8888).
   - Selecciona **"Jugar"** para abrir el cliente. Introduce tu nombre, la IP del servidor y el puerto.
3. **Lobby**: Una vez conectados al menos 2 jugadores y todos marquen "Listo", la partida comenzará automáticamente.

## Documentación Completa

1. **[Escenario Práctico](docs/01-scene.md)**
   - Justificación de la necesidad de comunicación cliente-servidor
   - Ciclo completo de comunicación (conexión, lobby, juego, finalización)
   - Por qué es imposible implementar UNO localmente sin red

2. **[Roles: Cliente y Servidor](docs/02-client-server-roles.md)**
   - Responsabilidades específicas del servidor (validación, sincronización, broadcasting)
   - Responsabilidades específicas del cliente (interfaz, envío de acciones)
   - Modelo de interacción completo

3. **[Clases y Librerías Java](docs/03-libraries.md)**
   - `java.net.Socket` y `java.net.ServerSocket` para comunicación
   - `java.io.ObjectInputStream/ObjectOutputStream` para serialización
   - `java.lang.Thread` para concurrencia
   - Clases personalizadas (`Message`, `SocketIOHandler`, etc.)
   - Tabla completa de librerías y sus propósitos

4. **[Servidor Multihilo](docs/04-threads.md)**
   - Arquitectura del servidor con múltiples threads
   - `ClientHandler`: gestión de un cliente en thread separado
   - Sincronización y thread-safety con `synchronized`
   - Garantías de concurrencia (exclusión mutua, visibilidad)
   - Flujo de 4 clientes conectados simultáneamente

5. **[Aplicación Cliente](docs/05-client.md)**
   - Conexión TCP mediante `Socket`
   - Thread receptor asíncrono (`startMessageReceiver()`)
   - Mecanismo de encolamiento de mensajes (sin pérdidas)
   - Envío sincronizado (`synchronized sendMessage()`)
   - Ciclo completo de una jugada

6. **[Sockets TCP: Intercambio Eficaz](docs/06-sockets.md)**
   - TCP vs UDP: por qué TCP para UNO
   - Arquitectura de `ServerSocket` y `Socket`
   - Flujos de entrada/salida (ObjectInputStream/ObjectOutputStream)
   - Protocolo `Message`: serialización tipada
   - Garantías de entrega y orden
   - Ciclo completo: envío, serialización, transmisión, recepción, deserialización

[  Hecho por Kiara Maldonado  ]
