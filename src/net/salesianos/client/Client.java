package net.salesianos.client;

import net.salesianos.protocol.Message;
import net.salesianos.utils.SocketIOHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente que se conecta al servidor UNO mediante sockets TCP.
 * Maneja envío/recepción de mensajes de forma asíncrona.
 */
public class Client {

    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    private Socket socket;
    private SocketIOHandler ioHandler;
    private String playerName;
    private String serverHost;
    private int serverPort;
    private boolean connected;
    private MessageListener listener;
    private Thread receiverThread;
    private final Queue<Message> messageQueue;

    public interface MessageListener {
        void onMessageReceived(Message message);

        void onDisconnected();
    }

    public Client(String playerName, String serverHost, int serverPort) {
        this.playerName = playerName;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.connected = false;
        this.messageQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Conecta al servidor.
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);

            // Usar SocketIOHandler centralizado
            ioHandler = new SocketIOHandler(socket.getOutputStream(), socket.getInputStream());


            connected = true;
            LOGGER.log(Level.INFO, "Conectado al servidor " + serverHost + ":" + serverPort);

            // Iniciar hilo receptor de mensajes
            startMessageReceiver();

            // Enviar mensaje de login
            sendLoginMessage();

            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error conectando al servidor: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    /**
     * Inicia el hilo que recibe mensajes del servidor.
     */
    private void startMessageReceiver() {
        receiverThread = new Thread(() -> {
            System.out.println("[CLIENT] Receiver thread iniciado para " + playerName);
            LOGGER.log(Level.INFO, "[CLIENT] Receiver thread iniciado para " + playerName);

            while (connected) {
                try {
                    System.out.println("[CLIENT] " + playerName + " - Esperando mensaje...");
                    Message message = ioHandler.receiveMessage();
                    System.out.println("[CLIENT] " + playerName + " - Mensaje recibido: " + message.getType());
                    LOGGER.log(Level.INFO, "[CLIENT] " + playerName + " - Mensaje recibido: " + message.getType());

                    if (listener != null) {
                        System.out.println("[CLIENT] " + playerName + " - Listener ACTIVO, procesando inmediatamente");
                        listener.onMessageReceived(message);
                    } else {
                        // Guardar en cola si no hay listener aún
                        messageQueue.add(message);
                        System.out.println("[CLIENT] " + playerName + " - Listener NULL, ENCOLANDO: " + message.getType());
                        LOGGER.log(Level.INFO, "[CLIENT] " + playerName + " - Mensaje encolado: " + message.getType());
                    }
                } catch (EOFException | ClassNotFoundException e) {
                    // Conexión cerrada normalmente
                    System.out.println("[CLIENT] " + playerName + " - Conexión cerrada");
                    LOGGER.log(Level.INFO, "Conexión cerrada por servidor");
                    break;
                } catch (IOException e) {
                    if (connected) {
                        System.out.println("[CLIENT] " + playerName + " - Error IO: " + e.getMessage());
                        LOGGER.log(Level.SEVERE, "Error recibiendo mensaje: " + e.getMessage());
                    }
                    break;
                }
            }

            disconnect();
        });

        receiverThread.setName("MessageReceiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    /**
     * Envía un mensaje al servidor.
     */
    public synchronized boolean sendMessage(Message message) {
        if (!connected || ioHandler == null) {
            LOGGER.log(Level.WARNING, "No hay conexión al servidor");
            return false;
        }

        return ioHandler.sendMessage(message);
    }

    /**
     * Envía el mensaje de login al servidor.
     */
    private void sendLoginMessage() {
        Message loginMessage = new Message(Message.MessageType.LOGIN);
        loginMessage.put("playerName", playerName);
        sendMessage(loginMessage);
    }

    /**
     * Desconecta del servidor.
     */
    public synchronized void disconnect() {
        if (!connected) {
            return;
        }

        connected = false;

        try {
            if (ioHandler != null) ioHandler.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            LOGGER.log(Level.INFO, "Desconectado del servidor");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al desconectar: " + e.getMessage());
        }

        if (listener != null) {
            listener.onDisconnected();
        }
    }

    public void setMessageListener(MessageListener listener) {
        System.out.println("[CLIENT] " + playerName + " - setMessageListener() llamado");
        LOGGER.log(Level.INFO, "[CLIENT] " + playerName + " - setMessageListener() llamado");
        System.out.println("[CLIENT] " + playerName + " - Mensajes en cola: " + messageQueue.size());

        this.listener = listener;

        // Procesar mensajes que llegaron antes de establecer el listener
        while (!messageQueue.isEmpty()) {
            Message message = messageQueue.poll();
            System.out.println("[CLIENT] " + playerName + " - Procesando mensaje ENCOLADO: " + message.getType());
            LOGGER.log(Level.INFO, "[CLIENT] " + playerName + " - Procesando mensaje encolado: " + message.getType());
            javax.swing.SwingUtilities.invokeLater(() -> listener.onMessageReceived(message));
        }

        System.out.println("[CLIENT] " + playerName + " - setMessageListener() completado. Cola vacía");
    }

    public boolean isConnected() {
        return connected;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}
