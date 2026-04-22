package net.salesianos.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor multihilo que acepta conexiones de clientes y gestiona partidas.
 * Crea un nuevo ClientHandler por cada cliente conectado.
 */
public class Server {

	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
	private static final int MAX_PLAYERS_PER_ROOM = 4;
	private static final int MIN_PLAYERS_TO_START = 2;

	private final int port;
	private ServerSocket serverSocket;
	private final List<ClientHandler> connectedClients;
	private final List<GameRoom> gameRooms;
	private boolean running;

	public Server(int port) {
		this.port = port;
		this.connectedClients = Collections.synchronizedList(new ArrayList<>());
		this.gameRooms = Collections.synchronizedList(new ArrayList<>());
		this.running = false;
	}

	/**
	 * Inicia el servidor y comienza a aceptar conexiones.
	 */
	public void start() {
		try {
			serverSocket = new ServerSocket(port);
			running = true;
			LOGGER.log(Level.INFO, "Servidor iniciado en puerto " + port);

			acceptConnections();

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error al iniciar servidor: " + e.getMessage());
			running = false;
		} finally {
			stop();
		}
	}

	/**
	 * Bucle principal que acepta conexiones de clientes.
	 */
	private void acceptConnections() {
		while (running) {
			try {
				Socket clientSocket = serverSocket.accept();
				LOGGER.log(Level.INFO, "Cliente conectado desde " + clientSocket.getInetAddress().getHostAddress());

				ClientHandler handler = new ClientHandler(clientSocket, this);
				connectedClients.add(handler);

				Thread clientThread = new Thread(handler);
				clientThread.setName("ClientHandler-" + connectedClients.size());
				clientThread.start();

				// Asignar cliente a una sala disponible
				assignClientToRoom(handler);

			} catch (IOException e) {
				if (running) {
					LOGGER.log(Level.SEVERE, "Error aceptando conexión: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Asigna un cliente a una sala disponible o crea una nueva.
	 */
	private void assignClientToRoom(ClientHandler handler) {
		synchronized (gameRooms) {
			// Buscar una sala disponible (no llena, no iniciada)
			GameRoom availableRoom = null;
			for (GameRoom room : gameRooms) {
				if (room.getPlayerCount() < MAX_PLAYERS_PER_ROOM && !room.isGameStarted()) {
					availableRoom = room;
					break;
				}
			}

			// Si no hay sala disponible, crear una nueva
			if (availableRoom == null) {
				String roomId = "ROOM_" + System.currentTimeMillis();
				availableRoom = new GameRoom(roomId);
				gameRooms.add(availableRoom);
				LOGGER.log(Level.INFO, "Nueva sala creada: " + roomId);
			}

			// Agregar el cliente a la sala
			if (availableRoom.addPlayer(handler)) {
				LOGGER.log(Level.INFO, "Cliente " + handler.getPlayerName() + " agregado a sala");
			}
		}
	}

	/**
	 * Detiene un cliente del servidor.
	 */
	public void removeClient(ClientHandler handler) {
		connectedClients.remove(handler);
		LOGGER.log(Level.INFO, "Cliente desconectado. Clientes activos: " + connectedClients.size());
	}

	/**
	 * Obtiene la lista de clientes conectados.
	 */
	public List<ClientHandler> getConnectedClients() {
		return new ArrayList<>(connectedClients);
	}

	/**
	 * Obtiene la lista de salas activas.
	 */
	public List<GameRoom> getGameRooms() {
		return new ArrayList<>(gameRooms);
	}

	/**
	 * Detiene el servidor y cierra todas las conexiones.
	 */
	public void stop() {
		running = false;
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			for (ClientHandler handler : connectedClients) {
				handler.disconnect();
			}
			LOGGER.log(Level.INFO, "Servidor detenido");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error al detener servidor: " + e.getMessage());
		}
	}

	public int getPort() {
		return port;
	}

	public boolean isRunning() {
		return running;
	}

	public int getClientCount() {
		return connectedClients.size();
	}

	public int getRoomCount() {
		return gameRooms.size();
	}
}
