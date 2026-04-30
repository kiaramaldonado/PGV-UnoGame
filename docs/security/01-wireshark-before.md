[← Volver al README](../../README.md)

# Análisis de Tráfico sin Cifrar con Wireshark

## Descripción
En esta actividad se analiza el tráfico de red entre cliente y servidor **antes de aplicar cifrado**. El objetivo es:
1. Capturar los paquetes intercambiados entre cliente y servidor
2. Identificar la información sensible que se transmite en texto plano
3. Documentar las vulnerabilidades encontradas
4. Establecer una línea base para comparar después de implementar cifrado

Para ello, se utilizará la herramienta **Wireshark** para monitorizar el tráfico TCP en el puerto utilizado por la aplicación (8888).
He creado un filtro para ello, y procedí a escuchar el tráfico en lo0 (loopback) mientras ejecutaba el servidor y los clientes en la misma máquina.
Haciendo click derecho en cada paquete, seleccioné "Follow TCP Stream" para ver el contenido de los mensajes intercambiados, siendo el resultado las capturas adjuntas:

## Vulnerabilidades Identificadas
### Información Transmitida sin Cifrar
El análisis del código revela que la aplicación transmite los siguientes datos sin encriptar:

#### 1. **Datos de Autenticación**
- **Nombre del jugador** (`playerName`): Transmitido en el primer mensaje LOGIN
- **ID de jugador**: Enviado en cada acción del juego
- Sin protección: credenciales en texto plano

![Datos de Autenticación](/docs/assets/wireshark-login-raw.png)

#### 2. **Información del Juego**
- **Nombres de otros jugadores**: En mensajes `LOBBY_UPDATE`
- **Estado del juego actual**: `UPDATE_STATE` contiene cartas, turno actual, etc.
- **Acciones del juego**: 
  - Qué carta juega cada jugador
  - Cuándo roba cartas
  - Quién dice "UNO"
  - Cuándo gana/pierde

Por ejemplo, en un mensaje `UPDATE_STATE` se pueden leer las cartas que tiene cada jugador, lo cual es información crítica para la estrategia del juego.
Aquí muestro un ejemplo de cómo se pueden leer las cartas de un jugador en texto plano, tras haber jugado una carta:

Se muestra en el mensaje sin encriptar tanto la carta jugada como las cartas que quedan en la mano de cada jugador, lo cual es una vulnerabilidad grave, ya que un atacante podría ver toda la información del juego en tiempo real.

![Estado del Juego](/docs/assets/wireshark-player-deck-raw.png)
![Acciones del Juego](/docs/assets/uno-player-deck.png)

#### 3. **Mensajes de Chat**
- **Contenido de chat**: Intercambiado en mensajes `CHAT`
- Sin cifrado: cualquier mensaje enviado es visible para un atacante
- Puede incluir información personal o estratégica

![Captura del Chat en el Uno](/docs/assets/uno-chat.png)
![Chat sin cifrar](/docs/assets/wireshark-chat-raw.png)


#### 4. **Información de Errores**
- **Mensajes de error del servidor**: Pueden revelar detalles de la lógica interna
- **Estados de conexión**: Desconexiones, reconexiones

![Errores sin cifrar](/docs/assets/wireshark-error-raw.png)

### Tipos de Mensajes Vulnerables

```
Según Message.java, los siguientes MessageTypes transmiten datos sensibles:

- LOGIN: {playerName, playerId}
- PLAY_CARD: {card, playerId}
- DRAW_CARD: {playerId}
- PLAYER_READY: {playerId}
- START_GAME: {players, currentCard}
- UPDATE_STATE: {currentCard, currentPlayer, players, hands}
- DRAW_PENALTY: {playerId, cardCount}
- GAME_OVER: {winnerId, winnerName}
- CHAT: {playerName, message}
- LOBBY_UPDATE: {players, readyCount}
- UNO_BUTTON: {playerId}
- DISCONNECT: {playerId, reason}
```

## Conclusiones de la Actividad 1

Esta actividad demuestra que:

1.  **La información se transmite en texto plano** - Vulnerable a ataques 
2.  **Los datos sensibles son legibles** - Nombres, acciones, mensajes expuestos
3.  **No hay mecanismo de verificación de integridad** - Los paquetes pueden ser modificados

## Referencias

- [Wireshark Documentation](https://www.wireshark.org/docs/)

[← Volver al README](../../README.md)
