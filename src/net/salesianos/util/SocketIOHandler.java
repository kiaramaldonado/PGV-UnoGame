package net.salesianos.util;

import net.salesianos.protocol.Message;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles socket I/O operations (ObjectInputStream/ObjectOutputStream).
 * Eliminates code duplication between Client and ClientHandler.
 * Follows the Single Responsibility Principle.
 */
public class SocketIOHandler {

    private static final Logger LOGGER = Logger.getLogger(SocketIOHandler.class.getName());

    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    /**
     * Creates a SocketIOHandler from a socket's streams.
     * ObjectOutputStream MUST be created before ObjectInputStream.
     *
     * @param outputStream the socket's output stream
     * @param inputStream  the socket's input stream
     * @throws IOException if stream creation fails
     */
    public SocketIOHandler(OutputStream outputStream, InputStream inputStream) throws IOException {
        // ObjectOutputStream must be created first!
        this.out = new ObjectOutputStream(outputStream);
        this.out.flush();

        this.in = new ObjectInputStream(inputStream);
    }

    /**
     * Sends a message through the socket.
     *
     * @param message the message to send
     * @return true if successfully sent, false otherwise
     */
    public synchronized boolean sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Receives a message from the socket (blocking).
     *
     * @return the received message
     * @throws IOException            if connection is lost
     * @throws ClassNotFoundException if message type is unknown
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    /**
     * Closes all streams associated with this handler.
     */
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing streams: " + e.getMessage());
        }
    }
}

