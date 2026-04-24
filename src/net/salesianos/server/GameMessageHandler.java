package net.salesianos.server;

import net.salesianos.protocol.Message;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles message routing for game actions.
 * Separates message dispatching logic from ClientHandler.
 * Follows Single Responsibility Principle.
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
     * Routes a game message to the appropriate handler method.
     *
     * @param message the message to process
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
                LOGGER.log(Level.WARNING, "Unhandled message type: " + message.getType());
        }
    }
}

