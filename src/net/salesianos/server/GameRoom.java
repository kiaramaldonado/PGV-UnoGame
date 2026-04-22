package net.salesianos.server;

import net.salesianos.model.*;
import net.salesianos.protocol.Message;

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
	private GameState gameState;
	private volatile int currentTurnIndex;
	private boolean gameStarted;
	private boolean gameEnded;

	public GameRoom(String roomId) {
		this.roomId = roomId;
		this.players = Collections.synchronizedList(new ArrayList<>());
		this.playerMap = Collections.synchronizedMap(new HashMap<>());
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

		LOGGER.log(Level.INFO, "Jugador " + playerName + " agregado a sala " + roomId + ". Total: " + players.size());

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

		LOGGER.log(Level.INFO, "Jugador " + handler.getPlayerName() + " removido de sala " + roomId);

		if (gameStarted && !gameEnded) {
			// Si hay menos de 2 jugadores, termina la partida
			if (players.size() < MIN_PLAYERS) {
				endGame();
			} else {
				broadcastStateUpdate();
			}
		} else {
			broadcastLobbyUpdate();
		}
	}

	/**
	 * Verifica si se debe iniciar el juego (máximo de jugadores alcanzado o timer).
	 */
	private void checkGameStart() {
		if (players.size() >= MAX_PLAYERS) {
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
		broadcastMessage(startMessage);

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
				sendError(handler, "No es tu turno");
				return;
			}

			String cardStr = message.getString("card");
			Card card = parseCard(cardStr);

			if (card == null) {
				sendError(handler, "Carta inválida");
				return;
			}

			Player currentPlayer = gameState.getCurrentPlayer();
			if (!currentPlayer.hasCard(card)) {
				sendError(handler, "No tienes esa carta");
				return;
			}

			if (!gameState.playCurrentPlayerCard(card)) {
				sendError(handler, "No puedes jugar esa carta");
				return;
			}

			LOGGER.log(Level.INFO, currentPlayer.getName() + " jugó " + card);

			// Verificar si el jugador ganó
			if (currentPlayer.handSize() == 0) {
				winGame(currentPlayer);
				return;
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
				sendError(handler, "No es tu turno");
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
			if (!gameStarted || gameEnded) {
				return;
			}

			Player player = playerMap.get(handler.getPlayerId());
			if (player != null && player.handSize() == 1) {
				Message unoMessage = new Message(Message.MessageType.UNO_BUTTON);
				unoMessage.put("playerName", player.getName());
				broadcastMessage(unoMessage);
			}
		}
	}

	/**
	 * Transmite un mensaje a todos los jugadores.
	 */
	public void broadcastMessage(Message message) {
		for (ClientHandler handler : players) {
			handler.sendMessage(message);
		}
	}

	/**
	 * Transmite actualización de estado a todos los jugadores.
	 */
	private void broadcastStateUpdate() {
		Message updateMessage = new Message(Message.MessageType.UPDATE_STATE);
		updateMessage.put("currentCard", gameState.getCurrentCard().toString());
		updateMessage.put("currentPlayer", gameState.getCurrentPlayer().getName());
		updateMessage.put("direction", gameState.getDirection());
		updateMessage.put("players", getPlayerInfo());

		for (int i = 0; i < players.size(); i++) {
			ClientHandler handler = players.get(i);
			Player player = gameState.getPlayers().get(i);

			Message playerMessage = new Message(Message.MessageType.UPDATE_STATE);
			playerMessage.put("currentCard", gameState.getCurrentCard().toString());
			playerMessage.put("currentPlayer", gameState.getCurrentPlayer().getName());
			playerMessage.put("direction", gameState.getDirection());
			playerMessage.put("hand", getCardStrings(player.getHand()));
			playerMessage.put("players", getPlayerInfo());

			handler.sendMessage(playerMessage);
		}
	}

	/**
	 * Transmite actualización de lobby.
	 */
	private void broadcastLobbyUpdate() {
		Message lobbyMessage = new Message(Message.MessageType.LOBBY_UPDATE);
		lobbyMessage.put("players", getPlayerNames());
		lobbyMessage.put("playersConnected", players.size());
		lobbyMessage.put("maxPlayers", MAX_PLAYERS);
		lobbyMessage.put("gameStarted", gameStarted);

		broadcastMessage(lobbyMessage);
	}

	/**
	 * Termina la partida por victoria.
	 */
	private void winGame(Player winner) {
		gameEnded = true;

		Message gameOverMessage = new Message(Message.MessageType.GAME_OVER);
		gameOverMessage.put("winnerId", winner.getName());
		gameOverMessage.put("winnerName", winner.getName());

		broadcastMessage(gameOverMessage);

		LOGGER.log(Level.INFO, winner.getName() + " ha ganado la partida en sala " + roomId);
	}

	/**
	 * Termina la partida (desconexión de jugadores).
	 */
	private void endGame() {
		gameEnded = true;

		Message errorMessage = new Message(Message.MessageType.ERROR);
		errorMessage.put("errorMessage", "La partida ha terminado por desconexión de jugadores");

		broadcastMessage(errorMessage);
	}

	/**
	 * Verifica si es el turno del jugador.
	 */
	private boolean isPlayerTurn(ClientHandler handler) {
		Player currentPlayer = gameState.getCurrentPlayer();
		Player clientPlayer = playerMap.get(handler.getPlayerId());
		return currentPlayer.equals(clientPlayer);
	}

	/**
	 * Envía un error específico a un cliente.
	 */
	private void sendError(ClientHandler handler, String errorMessage) {
		Message error = new Message(Message.MessageType.ERROR);
		error.put("errorMessage", errorMessage);
		handler.sendMessage(error);
	}

	/**
	 * Convierte una carta a string.
	 */
	private Card parseCard(String cardStr) {
		if (cardStr == null || !cardStr.contains("-")) {
			return null;
		}

		String[] parts = cardStr.split("-");
		if (parts.length != 2) {
			return null;
		}

		try {
			Card.Color color = Card.Color.valueOf(parts[0]);
			Card.Value value = Card.Value.valueOf(parts[1]);
			return new Card(color, value);
		} catch (IllegalArgumentException e) {
			return null;
		}
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

	private List<Map<String, Object>> getPlayerInfo() {
		return gameState.getPlayers().stream()
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
}
