package net.salesianos.client.handlers;

import net.salesianos.client.Client;
import net.salesianos.model.Card;
import net.salesianos.protocol.Message;
import net.salesianos.utils.CardParser;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gestiona las actualizaciones del estado del juego desde el servidor.
 */
public class GameStateUpdateHandler {

    private static final Logger LOGGER = Logger.getLogger(GameStateUpdateHandler.class.getName());

    private String currentCard;
    private String currentPlayer;
    private int direction;
    private List<String> playerHand;
    private List<Map<String, Object>> players;

    private final Client client;
    private final Listener listener;

    public interface Listener {
        void onStateUpdated(GameStateUpdateHandler state);

        void onPlayerHandUpdated(List<String> hand);

        void onPlayersListUpdated(List<Map<String, Object>> players);

        void onCurrentCardUpdated(String cardStr);

        void onCurrentPlayerChanged(String playerName, boolean isMyTurn);

        void onDirectionChanged(int direction);
    }

    public GameStateUpdateHandler(Client client, Listener listener) {
        this.client = client;
        this.listener = listener;
    }

    /**
     * Actualiza el estado desde un mensaje UPDATE_STATE del servidor.
     */
    public void updateFromMessage(Message message) {
        currentCard = message.getString("currentCard");
        currentPlayer = message.getString("currentPlayer");
        direction = message.getInteger("direction");

        @SuppressWarnings("unchecked")
        List<String> hand = (List<String>) message.get("hand");
        if (hand != null) {
            playerHand = hand;
            listener.onPlayerHandUpdated(hand);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playersList = (List<Map<String, Object>>) message.get("players");
        if (playersList != null) {
            players = playersList;
            listener.onPlayersListUpdated(playersList);
        }

        // Actualizar pantalla de carta actual
        listener.onCurrentCardUpdated(currentCard);

        // Actualizar jugador actual e indicador de turno
        boolean isMyTurn = currentPlayer.equals(client.getPlayerName());
        listener.onCurrentPlayerChanged(currentPlayer, isMyTurn);

        // Actualizar indicador de dirección
        listener.onDirectionChanged(direction);

        listener.onStateUpdated(this);
    }

    // Getters
    public String getCurrentCard() {
        return currentCard;
    }

    public Card getCurrentCardAsCard() {
        return CardParser.parseCard(currentCard);
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public int getDirection() {
        return direction;
    }

    public List<String> getPlayerHand() {
        return playerHand;
    }

    public List<Map<String, Object>> getPlayers() {
        return players;
    }
}

