package net.salesianos.server;

import net.salesianos.protocol.Message;

import java.io.*;
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
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String playerName;
	private String playerId;
	private GameRoom gameRoom;
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
			// ObjectOutputStream debe crearse antes que ObjectInputStream
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();

			in = new ObjectInputStream(socket.getInputStream());

			LOGGER.log(Level.INFO, "ClientHandler iniciado para " + socket.getInetAddress());

			// Bucle de recepción de mensajes
			while (running) {
				try {
					Message message = (Message) in.readObject();
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
			case PLAY_CARD:
				if (gameRoom != null) {
					gameRoom.handlePlayCard(this, message);
				}
				break;
			case DRAW_CARD:
				if (gameRoom != null) {
					gameRoom.handleDrawCard(this, message);
				}
				break;
			case PLAYER_READY:
				if (gameRoom != null) {
					gameRoom.handlePlayerReady(this, message);
				}
				break;
			case UNO_BUTTON:
				if (gameRoom != null) {
					gameRoom.handleUnoButton(this, message);
				}
				break;
			case CHAT:
				if (gameRoom != null) {
					gameRoom.broadcastMessage(message);
				}
				break;
			default:
				LOGGER.log(Level.WARNING, "Tipo de mensaje no manejado: " + message.getType());
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
		if (out == null || !socket.isConnected()) {
			return false;
		}

		try {
			out.writeObject(message);
			out.flush();
			return true;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error enviando mensaje a " + playerName + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * Desconecta el cliente.
	 */
	public void disconnect() {
		running = false;

		try {
			if (in != null) in.close();
			if (out != null) out.close();
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
	}

	public GameRoom getGameRoom() {
		return gameRoom;
	}
}
