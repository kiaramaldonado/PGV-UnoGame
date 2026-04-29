[← Volver al README](../../README.md)
---

# Sockets TCP: Intercambio de Información

## ¿Qué es un socket y por qué TCP?

Un socket es un **canal de comunicación entre dos programas** a través de la red. Hay dos extremos: el servidor espera conexiones con un `ServerSocket`, y el cliente se conecta con un `Socket`.

El protocolo elegido es **TCP**, porque garantiza tres cosas críticas para un juego de cartas: que los mensajes lleguen siempre, que lleguen en orden y que lleguen íntegros. UDP sería más rápido, pero puede perder o reordenar paquetes, lo que haría el estado del juego inconsistente entre jugadores.

---

## Cómo se establece la conexión

El servidor arranca y se queda escuchando en un puerto (8888). Cuando un cliente llama a `new Socket(host, 8888)`, ambos lados ejecutan el **3-way handshake** de TCP: el cliente envía un SYN, el servidor responde con SYN-ACK, el cliente confirma con ACK. A partir de ese momento la conexión está establecida y ambos pueden enviarse datos.

El servidor llama a `serverSocket.accept()`, que bloquea hasta que llega esa conexión. Al recibirla, devuelve un nuevo `Socket` dedicado a ese cliente, y vuelve a llamar a `accept()` para esperar el siguiente. Cada cliente tiene su propio socket independiente.

---

## Streams: cómo fluyen los datos

Una vez conectados, cada socket expone un `InputStream` (para leer) y un `OutputStream` (para escribir). El proyecto los envuelve en `ObjectInputStream` y `ObjectOutputStream`, que permiten enviar objetos Java enteros directamente, sin necesidad de convertirlos a texto manualmente.

**Hay un orden obligatorio al crear los streams**: primero se crea el `ObjectOutputStream` y se llama a `flush()`, y solo después el `ObjectInputStream`. Esto es porque `ObjectOutputStream` envía un header de sincronización al crearse, y `ObjectInputStream` espera leerlo antes de funcionar. Si se crean al revés, ambos lados se quedan esperando indefinidamente: un deadlock.

---

## La clase Message: el protocolo del juego

En vez de enviar texto plano (frágil y difícil de parsear), el proyecto define una clase `Message` con dos campos: un tipo (un `enum`) y un mapa de datos clave-valor. Al ser `Serializable`, puede enviarse directamente por el `ObjectOutputStream`.

El tipo define qué significa el mensaje, y el mapa lleva los datos necesarios para ese tipo. Por ejemplo:

| Tipo | Datos que lleva | Quién lo envía |
|---|---|---|
| `LOGIN` | nombre del jugador | Cliente |
| `LOBBY_UPDATE` | lista de jugadores en sala | Servidor |
| `START_GAME` | — | Servidor |
| `PLAY_CARD` | carta jugada | Cliente |
| `DRAW_CARD` | — | Cliente |
| `UPDATE_STATE` | mano, turno, carta actual | Servidor |
| `ERROR` | texto del error | Servidor |
| `GAME_OVER` | ganador | Servidor |

Usar un `enum` en vez de strings evita errores tipográficos y hace el código más seguro. Si llega un tipo desconocido, Java lanza `ClassNotFoundException`, que se captura y maneja limpiamente.

---

## Serialización: objetos convertidos a bytes y de vuelta

Cuando el servidor llama a `out.writeObject(message)`, Java convierte el objeto `Message` a una secuencia de bytes que incluye la clase, los campos y sus valores. Esos bytes viajan por TCP. En el otro extremo, `in.readObject()` los recibe y reconstruye el objeto exactamente igual.

Esta llamada es **bloqueante**: el receptor espera en `readObject()` hasta que llegue un mensaje completo. Por eso tanto el servidor (en `ClientHandler`) como el cliente (en el hilo receptor) ejecutan esto en hilos separados, sin bloquear la UI ni la aceptación de nuevas conexiones.

Después de cada `writeObject()` se llama a `flush()` para forzar que los bytes salgan del buffer del sistema operativo y viajen por la red inmediatamente. Sin este `flush()`, los datos podrían quedarse retenidos en el buffer y el receptor esperaría sin recibir nada.

---

## Qué garantiza TCP en la práctica

TCP maneja de forma transparente tres situaciones que de otro modo romperían el juego:

- Si un paquete llega corrupto, TCP lo detecta con su checksum y pide la retransmisión automáticamente.
- Si los paquetes llegan desordenados (por distintas rutas de red), TCP los reordena antes de entregarlos.
- Si el otro lado cierra la conexión, el `readObject()` lanza `EOFException`, que el código captura para desconectar limpiamente.

El resultado es que la capa de aplicación (el juego) nunca necesita preocuparse por estos problemas: recibe mensajes completos, en orden y sin corrupciones.

---

## Resumen

La comunicación se apoya en tres capas que trabajan juntas: **TCP** garantiza que los datos lleguen bien; **ObjectInputStream/ObjectOutputStream** convierte objetos Java en bytes y los reconstruye al otro lado; y la clase **Message** define un protocolo claro con tipos seguros. El conjunto permite que servidor y clientes se comuniquen de forma fiable, en orden y sin necesidad de parsear texto manualmente.

---
[← Volver al README](../../README.md)
