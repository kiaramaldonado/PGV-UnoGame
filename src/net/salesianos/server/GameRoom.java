package net.salesianos.server;

import net.salesianos.model.Card;
import net.salesianos.model.GameState;
import net.salesianos.model.Player;
import net.salesianos.protocol.Message;
import net.salesianos.util.CardParser;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gestiona una partida del UNO con múltiples jugadores.
 * Sincroniza el estado del juego y notifica cambios a todos los clientes.
 */
public class GameRoom {

    private static final Logger LOGGER = Logger.getLogger(GameRoom.class.getName());
    private static final int MAX_PLAYERS = 4;
    private static final int MIN_PLAYERS = 2;
    private static final int INITIAL_CARDS = 7;

    private final String roomId;
    private final List<ClientHandler> players;
    private final Map<String, Player> playerMap;
    private final Set<String> readyPlayers;
    private final GameRoomBroadcaster broadcaster;
    private GameState gameState;
    private volatile int currentTurnIndex;
    private boolean gameStarted;
    private boolean gameEnded;
    private volatile String unoTargetPlayerId = null;

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.players = Collections.synchronizedList(new ArrayList<>());
        this.playerMap = Collections.synchronizedMap(new HashMap<>());
        this.readyPlayers = Collections.synchronizedSet(new HashSet<>());
        this.broadcaster = new GameRoomBroadcaster(this.players);
        this.gameStarted = false;
        this.gameEnded = false;
    }

    /**
     * Agrega un jugador a la sala.
     */
    public synchronized boolean addPlayer(ClientHandler handler) {
        if (gameStarted || players.size() >= MAX_PLAYERS) {
            return false;
        }

        String playerName = handler.getPlayerName();
        players.add(handler);
        playerMap.put(handler.getPlayerId(), new Player(playerName));
        handler.setGameRoom(this);

        LOGGER.log(Level.INFO, "🔵 Jugador " + playerName + " agregado a sala " + roomId + ". Total: " + players.size());
        LOGGER.log(Level.INFO, "🔵 Jugadores actuales: " + getPlayerNames());

        LOGGER.log(Level.INFO, "🔵 Enviando LOBBY_UPDATE inicial a " + playerName);
        broadcaster.sendLobbyUpdateToPlayer(handler, getPlayerNames(), readyPlayers.size(), MAX_PLAYERS, gameStarted);
        LOGGER.log(Level.INFO, "🔵 Haciendo broadcast a todos los jugadores");
        // Notificar a todos sobre el nuevo jugador
        broadcastLobbyUpdate();

        // Si tenemos mínimo 2 jugadores y máximo 4, podemos iniciar
        if (players.size() >= MIN_PLAYERS) {
            checkGameStart();
        }

        return true;
    }

    /**
     * Elimina un jugador de la sala.
     */
    public synchronized void removePlayer(ClientHandler handler) {
        players.remove(handler);
        playerMap.remove(handler.getPlayerId());

        // IMPORTANTE: Quitarlo de los jugadores listos si se desconecta
        readyPlayers.remove(handler.getPlayerId());

        LOGGER.log(Level.INFO, "Jugador " + handler.getPlayerName() + " salió de sala " + roomId);

        if (gameStarted && !gameEnded) {
            // Si hay menos de 2 jugadores, termina la partida
            if (players.size() < MIN_PLAYERS) {
                endGame();
            } else {
                broadcastStateUpdate();
            }
        } else {
            // Al actualizar el lobby, el método checkGameStart se llama si alguien se va y todos los que quedan estaban listos
            broadcastLobbyUpdate();
            checkGameStart();
        }
    }

    /**
     * Verifica si se debe iniciar el juego (máximo de jugadores alcanzado o todos listos).
     */
    private void checkGameStart() {
        // Si hay 4 jugadores, comienza directamente (ignora si le dieron a "Listo")
        if (players.size() == MAX_PLAYERS) {
            LOGGER.log(Level.INFO, "4 jugadores alcanzados. Iniciando partida automáticamente.");
            startGame();
        }
        // Si hay entre 2 y 3 jugadores, y TODOS han indicado que están listos
        else if (players.size() >= MIN_PLAYERS && readyPlayers.size() == players.size()) {
            LOGGER.log(Level.INFO, "Todos los jugadores (" + players.size() + ") están listos. Iniciando partida.");
            startGame();
        }
    }

    /**
     * Inicia la partida.
     */
    private synchronized void startGame() {
        if (gameStarted) {
            return;
        }

        gameStarted = true;
        gameEnded = false;

        List<Player> playerList = new ArrayList<>(playerMap.values());
        gameState = new GameState(playerList);
        gameState.startGame(INITIAL_CARDS);
        currentTurnIndex = 0;

        LOGGER.log(Level.INFO, "Partida iniciada en sala " + roomId + " con " + players.size() + " jugadores");

        // Notificar a todos que el juego comienza
        Message startMessage = new Message(Message.MessageType.START_GAME);
        startMessage.put("players", getPlayerNames());
        startMessage.put("currentCard", gameState.getCurrentCard().toString());
        startMessage.put("currentPlayer", gameState.getCurrentPlayer().getName());
        broadcaster.broadcastMessage(startMessage);

        // Pausa para permitir que los clientes abran GameFrame antes de recibir su mano
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        broadcastStateUpdate();
    }

    /**
     * Maneja cuando un jugador juega una carta.
     */
    public void handlePlayCard(ClientHandler handler, Message message) {
        synchronized (this) {
            if (!gameStarted || gameEnded) {
                return;
            }

            if (!isPlayerTurn(handler)) {
                broadcaster.sendErrorToPlayer(handler, "No es tu turno");
                return;
            }

            String cardStr = message.getString("card");
            Card card = CardParser.parseCard(cardStr);

            if (card == null) {
                broadcaster.sendErrorToPlayer(handler, "Carta inválida");
                return;
            }

            Player currentPlayer = gameState.getCurrentPlayer();
            if (!currentPlayer.hasCard(card)) {
                broadcaster.sendErrorToPlayer(handler, "No tienes esa carta");
                return;
            }

            if (!gameState.playCurrentPlayerCard(card)) {
                broadcaster.sendErrorToPlayer(handler, "No puedes jugar esa carta");
                return;
            }

            LOGGER.log(Level.INFO, currentPlayer.getName() + " jugó " + card);

            // Verificar si el jugador ganó
            if (currentPlayer.handSize() == 0) {
                winGame(currentPlayer);
                return;
            }

            // Lanzar aviso a todos si el jugador se queda con 1 carta
            if (currentPlayer.handSize() == 1) {
                unoTargetPlayerId = handler.getPlayerId();
                Message unoAlert = new Message(Message.MessageType.UNO_BUTTON);
                unoAlert.put("action", "SHOW");
                unoAlert.put("targetPlayer", currentPlayer.getName());
                broadcaster.broadcastMessage(unoAlert);
            }

            // Actualizar estado para todos
            broadcastStateUpdate();
        }
    }

    /**
     * Maneja cuando un jugador roba una carta.
     */
    public void handleDrawCard(ClientHandler handler, Message message) {
        synchronized (this) {
            if (!gameStarted || gameEnded) {
                return;
            }

            if (!isPlayerTurn(handler)) {
                broadcaster.sendErrorToPlayer(handler, "No es tu turno");
                return;
            }

            Player currentPlayer = gameState.getCurrentPlayer();
            Card drawnCard = currentPlayer.drawCard(gameState.getDeck());

            LOGGER.log(Level.INFO, currentPlayer.getName() + " robó una carta");

            // Avanzar turno
            gameState.nextTurn();
            broadcastStateUpdate();
        }
    }

    /**
     * Maneja cuando un jugador dice "UNO".
     */
    public void handleUnoButton(ClientHandler handler, Message message) {
        synchronized (this) {
            if (!gameStarted || gameEnded || unoTargetPlayerId == null) {
                return; // Ignorar si no hay una disputa activa
            }

            String clickerId = handler.getPlayerId();
            Player clicker = playerMap.get(clickerId);
            Player target = playerMap.get(unoTargetPlayerId);

            if (target == null || clicker == null) return;

            Message chatMsg = new Message(Message.MessageType.CHAT);
            chatMsg.put("playerName", "SISTEMA");

            if (clickerId.equals(unoTargetPlayerId)) {
                // El jugador que tiene 1 carta fue el primero en pulsar (Se salva)
                chatMsg.put("message", target.getName() + " dijo ¡UNO! a tiempo. ¡Se ha salvado!");
            } else {
                // Otro jugador fue más rápido (Penalización)
                chatMsg.put("message", "¡" + clicker.getName() + " fue más rápido! " + target.getName() + " recibe +2 cartas de penalización.");
                target.drawCards(gameState.getDeck(), 2);
            }

            // Mandar mensaje para ocultar el botón de UNO a todos
            Message hideAlert = new Message(Message.MessageType.UNO_BUTTON);
            hideAlert.put("action", "HIDE");
            broadcaster.broadcastMessage(hideAlert);

            // Anunciar el resultado en el chat
            broadcaster.broadcastMessage(chatMsg);

            // Reiniciar para que no se siga sumando penalizaciones
            unoTargetPlayerId = null;

            broadcastStateUpdate();
        }
    }

    /**
     * Transmite un mensaje a todos los jugadores.
     */
    public void broadcastMessage(Message message) {
        broadcaster.broadcastMessage(message);
    }

    /**
     * Transmite actualización de estado a todos los jugadores.
     */
    private void broadcastStateUpdate() {
        broadcaster.broadcastStateUpdate(
                gameState.getCurrentCard().toString(),
                gameState.getCurrentPlayer().getName(),
                gameState.getDirection(),
                gameState.getPlayers()
        );
    }

    /**
     * Transmite actualización de lobby.
     */
    private void broadcastLobbyUpdate() {
        broadcaster.broadcastLobbyUpdate(getPlayerNames(), readyPlayers.size(), MAX_PLAYERS, gameStarted);
    }

    /**
     * Termina la partida por victoria.
     */
    private void winGame(Player winner) {
        gameEnded = true;

        Message gameOverMessage = new Message(Message.MessageType.GAME_OVER);
        gameOverMessage.put("winnerId", winner.getName());
        gameOverMessage.put("winnerName", winner.getName());

        broadcaster.broadcastMessage(gameOverMessage);

        LOGGER.log(Level.INFO, winner.getName() + " ha ganado la partida en sala " + roomId);
    }

    /**
     * Termina la partida (desconexión de jugadores).
     */
    private void endGame() {
        gameEnded = true;
        broadcaster.broadcastError("La partida ha terminado por desconexión de jugadores");
    }

    /**
     * Verifica si es el turno del jugador.
     */
    private boolean isPlayerTurn(ClientHandler handler) {
        Player currentPlayer = gameState.getCurrentPlayer();
        Player clientPlayer = playerMap.get(handler.getPlayerId());
        return currentPlayer.equals(clientPlayer);
    }


    private List<String> getPlayerNames() {
        return gameState != null ?
                gameState.getPlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()) :
                players.stream()
                        .map(ClientHandler::getPlayerName)
                        .collect(Collectors.toList());
    }

    public String getRoomId() {
        return roomId;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public void handlePlayerReady(ClientHandler handler, Message message) {
        synchronized (this) {
            if (gameStarted) {
                return;
            }
            readyPlayers.add(handler.getPlayerId());
            LOGGER.log(Level.INFO, handler.getPlayerName() + " está listo. Ready: " + readyPlayers.size() + "/" + players.size());
            broadcastLobbyUpdate();
            checkGameStart();
        }
    }
}
