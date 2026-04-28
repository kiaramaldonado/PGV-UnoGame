package net.salesianos.server;

import net.salesianos.model.Card;
import net.salesianos.model.Player;
import net.salesianos.protocol.Message;
import net.salesianos.server.handlers.ClientHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gestiona el envío de los mensajes a todos los jugadores
 */
public class GameRoomBroadcaster {

    private static final Logger LOGGER = Logger.getLogger(GameRoomBroadcaster.class.getName());

    private final List<ClientHandler> players;

    public GameRoomBroadcaster(List<ClientHandler> players) {
        this.players = players;
    }

    /**
     * Broadcast un mensaje a todos los jugadores
     */
    public void broadcastMessage(Message message) {
        for (ClientHandler handler : players) {
            handler.sendMessage(message);
        }
    }

    /**
     * Envía el mensaje a alguien en concreto
     */
    public void sendToPlayer(ClientHandler handler, Message message) {
        handler.sendMessage(message);
    }

    /**
     * Broadcasts el mensaje a todos los jugadores menos uno
     */
    public void broadcastMessageExcept(Message message, ClientHandler excludeHandler) {
        for (ClientHandler handler : players) {
            if (!handler.equals(excludeHandler)) {
                handler.sendMessage(message);
            }
        }
    }

    /**
     * Broadcasts un mensaje de error a todos los jugadores
     */
    public void broadcastError(String errorMessage) {
        Message error = new Message(Message.MessageType.ERROR);
        error.put("errorMessage", errorMessage);
        broadcastMessage(error);
    }

    /**
     * Envía un error a un jugador en específico
     */
    public void sendErrorToPlayer(ClientHandler handler, String errorMessage) {
        Message error = new Message(Message.MessageType.ERROR);
        error.put("errorMessage", errorMessage);
        sendToPlayer(handler, error);
    }

    /**
     * Transmite actualización de lobby a un jugador específico.
     */
    public void sendLobbyUpdateToPlayer(ClientHandler handler, List<String> playerNames, int readyCount, int maxPlayers, boolean gameStarted) {
        Message lobbyMessage = createLobbyUpdateMessage(playerNames, readyCount, maxPlayers, gameStarted);
        handler.sendMessage(lobbyMessage);
    }

    /**
     * Transmite actualización de lobby a todos los jugadores.
     */
    public void broadcastLobbyUpdate(List<String> playerNames, int readyCount, int maxPlayers, boolean gameStarted) {
        Message lobbyMessage = createLobbyUpdateMessage(playerNames, readyCount, maxPlayers, gameStarted);
        broadcastMessage(lobbyMessage);
    }

    private Message createLobbyUpdateMessage(List<String> playerNames, int readyCount, int maxPlayers, boolean gameStarted) {
        Message lobbyMessage = new Message(Message.MessageType.LOBBY_UPDATE);
        lobbyMessage.put("players", playerNames);
        lobbyMessage.put("playersConnected", players.size());
        lobbyMessage.put("readyCount", readyCount);
        lobbyMessage.put("maxPlayers", maxPlayers);
        lobbyMessage.put("gameStarted", gameStarted);
        return lobbyMessage;
    }

     /**
      * Transmite actualización de estado a todos los jugadores.
      */
     public void broadcastStateUpdate(String currentCard, String currentPlayer, int direction, List<Player> gameStatePlayers,
                                     Map<String, Player> playerMap, List<String> playerIdOrder, List<ClientHandler> handlers) {
         List<Map<String, Object>> playersInfo = getPlayersInfo(gameStatePlayers);

         for (ClientHandler handler : handlers) {
             String playerId = handler.getPlayerId();
             Player player = playerMap.get(playerId);

             if (player == null) {
                 LOGGER.log(Level.WARNING, "Player not found for handler: " + playerId);
                 continue;
             }

            Message playerMessage = new Message(Message.MessageType.UPDATE_STATE);
            playerMessage.put("currentCard", currentCard);
            playerMessage.put("currentPlayer", currentPlayer);
            playerMessage.put("direction", direction);
            playerMessage.put("hand", getCardStrings(player.getHand()));
            playerMessage.put("players", playersInfo);

            handler.sendMessage(playerMessage);
        }
    }

    private List<Map<String, Object>> getPlayersInfo(List<Player> players) {
        return players.stream()
                .map(p -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", p.getName());
                    info.put("handSize", p.handSize());
                    return info;
                })
                .collect(Collectors.toList());
    }

    private List<String> getCardStrings(List<Card> cards) {
        return cards.stream()
                .map(Card::toString)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el número de jugadores.
     */
    public int getPlayerCount() {
        return players.size();
    }
}

