package net.salesianos.server.handlers;

import net.salesianos.protocol.Message;
import net.salesianos.server.GameRoom;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestiona los mensajes para realizar acciones
 */
public class GameMessageHandler {

    private static final Logger LOGGER = Logger.getLogger(GameMessageHandler.class.getName());

    private final GameRoom gameRoom;
    private final ClientHandler handler;

    public GameMessageHandler(GameRoom gameRoom, ClientHandler handler) {
        this.gameRoom = gameRoom;
        this.handler = handler;
    }

    /**
     * Conecta el mensaje a la acción que le pertenece
     *
     * @param message el mensaje a procesar
     */
    public void handle(Message message) {
        if (gameRoom == null) {
            return;
        }

        switch (message.getType()) {
            case PLAY_CARD:
                gameRoom.handlePlayCard(handler, message);
                break;
            case DRAW_CARD:
                gameRoom.handleDrawCard(handler, message);
                break;
            case PLAYER_READY:
                gameRoom.handlePlayerReady(handler, message);
                break;
            case UNO_BUTTON:
                gameRoom.handleUnoButton(handler, message);
                break;
            case CHAT:
                gameRoom.broadcastMessage(message);
                break;
            default:
                LOGGER.log(Level.WARNING, "Tipo de mensaje no manejado: " + message.getType());
        }
    }
}

