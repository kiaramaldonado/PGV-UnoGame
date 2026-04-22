package net.salesianos.client;

import net.salesianos.protocol.Message;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente que se conecta al servidor UNO mediante sockets TCP.
 * Maneja envío/recepción de mensajes de forma asíncrona.
 */
public class Client {

	private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String playerName;
	private String serverHost;
	private int serverPort;
	private boolean connected;
	private MessageListener listener;
	private Thread receiverThread;

	public interface MessageListener {
		void onMessageReceived(Message message);
		void onDisconnected();
	}

	public Client(String playerName, String serverHost, int serverPort) {
		this.playerName = playerName;
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.connected = false;
	}

	/**
	 * Conecta al servidor.
	 */
	public boolean connect() {
		try {
			socket = new Socket(serverHost, serverPort);

			// ObjectOutputStream debe crearse antes que ObjectInputStream
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();

			in = new ObjectInputStream(socket.getInputStream());

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
			while (connected) {
				try {
					Message message = (Message) in.readObject();
					if (listener != null) {
						listener.onMessageReceived(message);
					}
				} catch (EOFException | ClassNotFoundException e) {
					// Conexión cerrada normalmente
					LOGGER.log(Level.INFO, "Conexión cerrada por servidor");
					break;
				} catch (IOException e) {
					if (connected) {
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
		if (!connected || out == null) {
			LOGGER.log(Level.WARNING, "No hay conexión al servidor");
			return false;
		}

		try {
			out.writeObject(message);
			out.flush();
			return true;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error enviando mensaje: " + e.getMessage());
			disconnect();
			return false;
		}
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
			if (in != null) in.close();
			if (out != null) out.close();
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
		this.listener = listener;
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
