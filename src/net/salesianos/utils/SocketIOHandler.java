package net.salesianos.utils;

import net.salesianos.protocol.Message;

import java.io.*;
import java.util.Base64;
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
    private final SecureManager secureManager;

    /**
     * Crea un SocketIOHandler a partir de los streams de un socket.
     * ObjectOutputStream DEBE crearse antes que ObjectInputStream.
     *
     * @param outputStream el stream de salida del socket
     * @param inputStream  el stream de entrada del socket
     * @throws IOException si la creación del stream falla
     */
    public SocketIOHandler(OutputStream outputStream, InputStream inputStream) throws IOException {
        this(outputStream, inputStream, null);
    }

    public SocketIOHandler(OutputStream outputStream, InputStream inputStream, String secretKey) throws IOException {
        this.out = new ObjectOutputStream(outputStream);
        this.out.flush();

        this.in = new ObjectInputStream(inputStream);

        if (secretKey != null) {
            this.secureManager = new SecureManager(secretKey);
        } else {
            byte[] savedKey = FileManager.readSecretKey();
            if (savedKey == null) {
                SecureManager manager = new SecureManager();
                manager.generateAndSaveKey();
            }
            this.secureManager = new SecureManager();
        }
    }

    /**
     * Envía un mensaje a través del socket.
     *
     * @param message el mensaje a enviar
     * @return verdadero si se envió exitosamente, falso en caso contrario
     */
    public synchronized boolean sendMessage(Message message) {
        try {
            String payload = serializeMessage(message);
            String encryptedPayload = secureManager.encriptString(payload);
            if (encryptedPayload == null) {
                return false;
            }
            out.writeObject(encryptedPayload);
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
        Object raw = in.readObject();
        if (!(raw instanceof String)) {
            throw new IOException("Formato de mensaje inválido");
        }
        String decryptedPayload = secureManager.decriptString((String) raw);
        if (decryptedPayload == null) {
            throw new IOException("Error al desencriptar mensaje");
        }
        return deserializeMessage(decryptedPayload);
    }

    private String serializeMessage(Message message) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(buffer)) {
            objectStream.writeObject(message);
            objectStream.flush();
            return Base64.getEncoder().encodeToString(buffer.toByteArray());
        }
    }

    private Message deserializeMessage(String payload) throws IOException, ClassNotFoundException {
        byte[] raw = Base64.getDecoder().decode(payload);
        try (ByteArrayInputStream buffer = new ByteArrayInputStream(raw);
             ObjectInputStream objectStream = new ObjectInputStream(buffer)) {
            return (Message) objectStream.readObject();
        }
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
