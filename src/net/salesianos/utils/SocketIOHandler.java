package net.salesianos.utils;

import net.salesianos.protocol.Message;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maneja operaciones de I/O de socket (ObjectInputStream/ObjectOutputStream).
 * Elimina la duplicación de código entre Cliente y ClientHandler.
 * Sigue el Principio de Responsabilidad Única.
 */
public class SocketIOHandler {

    private static final Logger LOGGER = Logger.getLogger(SocketIOHandler.class.getName());

    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    /**
     * Crea un SocketIOHandler a partir de los streams de un socket.
     * ObjectOutputStream DEBE crearse antes que ObjectInputStream.
     *
     * @param outputStream el stream de salida del socket
     * @param inputStream  el stream de entrada del socket
     * @throws IOException si la creación del stream falla
     */
    public SocketIOHandler(OutputStream outputStream, InputStream inputStream) throws IOException {
        this.out = new ObjectOutputStream(outputStream);
        this.out.flush();

        this.in = new ObjectInputStream(inputStream);
    }

    /**
     * Envía un mensaje a través del socket.
     *
     * @param message el mensaje a enviar
     * @return verdadero si se envió exitosamente, falso en caso contrario
     */
    public synchronized boolean sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al enviar mensaje: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recibe un mensaje del socket (bloqueante).
     *
     * @return el mensaje recibido
     * @throws IOException            si la conexión se pierde
     * @throws ClassNotFoundException si el tipo de mensaje es desconocido
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    /**
     * Cierra todos los streams asociados con este manejador.
     */
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error cerrando streams: " + e.getMessage());
        }
    }
}

