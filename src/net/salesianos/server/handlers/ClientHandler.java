package net.salesianos.server.handlers;

import net.salesianos.protocol.Message;
import net.salesianos.server.GameRoom;
import net.salesianos.server.Server;
import net.salesianos.utils.SocketIOHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maneja la comunicación con un cliente específico.
 * Se ejecuta en su propio hilo dentro del servidor.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final Server server;
    private SocketIOHandler ioHandler;
    private String playerName;
    private String playerId;
    private GameRoom gameRoom;
    private GameMessageHandler gameMessageHandler;
    private boolean running;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.running = true;
        this.playerId = generatePlayerId();
    }

    /**
     * Genera un ID único para el cliente.
     */
    private String generatePlayerId() {
        return "PLAYER_" + System.currentTimeMillis() + "_" + hashCode();
    }

    /**
     * Inicia el manejo de comunicación con el cliente.
     */
    @Override
    public void run() {
        try {
            // Use centralized SocketIOHandler
            ioHandler = new SocketIOHandler(socket.getOutputStream(), socket.getInputStream());

            LOGGER.log(Level.INFO, "ClientHandler iniciado para " + socket.getInetAddress());

            // Bucle de recepción de mensajes
            while (running) {
                try {
                    Message message = ioHandler.receiveMessage();
                    handleMessage(message);
                } catch (EOFException e) {
                    LOGGER.log(Level.INFO, "Cliente desconectado normalmente");
                    break;
                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Tipo de mensaje desconocido: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error en ClientHandler: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Procesa un mensaje recibido del cliente.
     */
    private void handleMessage(Message message) {
        LOGGER.log(Level.INFO, "Mensaje recibido: " + message.getType() + " de " + playerName);

        switch (message.getType()) {
            case LOGIN:
                handleLogin(message);
                break;
            default:
                // Delegate game messages to specialized handler
                if (gameMessageHandler != null) {
                    gameMessageHandler.handle(message);
                } else {
                    LOGGER.log(Level.WARNING, "Game message handler not initialized: " + message.getType());
                }
                break;
        }
    }

    /**
     * Maneja el login del cliente.
     */
    private void handleLogin(Message message) {
        this.playerName = message.getString("playerName");
        LOGGER.log(Level.INFO, "Jugador autenticado: " + playerName + " (" + playerId + ")");

        server.assignClientToRoom(this);
    }

    /**
     * Envía un mensaje al cliente.
     */
    public synchronized boolean sendMessage(Message message) {
        if (ioHandler == null || !socket.isConnected()) {
            return false;
        }

        return ioHandler.sendMessage(message);
    }

    /**
     * Desconecta el cliente.
     */
    public void disconnect() {
        running = false;

        try {
            if (ioHandler != null) ioHandler.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al cerrar socket: " + e.getMessage());
        }

        if (gameRoom != null) {
            gameRoom.removePlayer(this);
        }

        server.removeClient(this);
        LOGGER.log(Level.INFO, "ClientHandler cerrado para " + playerName);
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setGameRoom(GameRoom gameRoom) {
        this.gameRoom = gameRoom;
        // Initialize game message handler when assigned to a room
        this.gameMessageHandler = new GameMessageHandler(gameRoom, this);
    }

    public GameRoom getGameRoom() {
        return gameRoom;
    }
}
