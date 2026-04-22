package net.salesianos.client.ui;

import net.salesianos.client.Client;
import net.salesianos.protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Sala de espera donde los jugadores esperan a que se llene la partida.
 * Muestra lista de jugadores conectados y cuenta atrás.
 */
public class LobbyFrame extends JFrame {

	private static final Logger LOGGER = Logger.getLogger(LobbyFrame.class.getName());

	private Client client;
	private JLabel playersLabel;
	private JLabel waitingLabel;
	private JButton readyButton;
	private JButton cancelButton;
	private DefaultListModel<String> playerListModel;
	private JList<String> playerList;
	private LobbyListener listener;
	private int playersConnected;
	private int maxPlayers;

	public interface LobbyListener {
		void onGameStart();
		void onCancelLobby();
	}

	public LobbyFrame(Client client) {
		this.client = client;

		setTitle("UNO - Sala de Espera");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500, 400);
		setLocationRelativeTo(null);
		setResizable(false);

		initComponents();
		setupClientListener();
	}

	private void initComponents() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Panel superior con información
		JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 10));

		playersLabel = new JLabel("Jugadores: 1/4");
		playersLabel.setFont(new Font("Arial", Font.BOLD, 16));
		infoPanel.add(playersLabel);

		waitingLabel = new JLabel("Esperando a que se conecten más jugadores...");
		waitingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		waitingLabel.setForeground(Color.BLUE);
		infoPanel.add(waitingLabel);

		panel.add(infoPanel, BorderLayout.NORTH);

		// Panel central con lista de jugadores
		playerListModel = new DefaultListModel<>();
		playerListModel.addElement(client.getPlayerName() + " (TÚ)");

		playerList = new JList<>(playerListModel);
		playerList.setFont(new Font("Arial", Font.PLAIN, 12));
		playerList.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		JScrollPane scrollPane = new JScrollPane(playerList);
		scrollPane.setPreferredSize(new Dimension(400, 150));
		panel.add(scrollPane, BorderLayout.CENTER);

		// Panel inferior con botones
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

		readyButton = new JButton("Listo para jugar");
		readyButton.setFont(new Font("Arial", Font.PLAIN, 12));
		readyButton.addActionListener(e -> markReady());
		buttonPanel.add(readyButton);

		cancelButton = new JButton("Cancelar");
		cancelButton.setFont(new Font("Arial", Font.PLAIN, 12));
		cancelButton.addActionListener(e -> cancelLobby());
		buttonPanel.add(cancelButton);

		panel.add(buttonPanel, BorderLayout.SOUTH);

		add(panel);
	}

	private void setupClientListener() {
		client.setMessageListener(new Client.MessageListener() {
			@Override
			public void onMessageReceived(Message message) {
				handleServerMessage(message);
			}

			@Override
			public void onDisconnected() {
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(LobbyFrame.this,
						"Desconectado del servidor",
						"Error de conexión",
						JOptionPane.ERROR_MESSAGE);
					if (listener != null) {
						listener.onCancelLobby();
					}
				});
			}
		});
	}

	private void handleServerMessage(Message message) {
		SwingUtilities.invokeLater(() -> {
			switch (message.getType()) {
				case LOBBY_UPDATE:
					handleLobbyUpdate(message);
					break;
				case START_GAME:
					if (listener != null) {
						listener.onGameStart();
					}
					break;
				case ERROR:
					String errorMsg = message.getString("errorMessage");
					JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
					break;
				default:
					break;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void handleLobbyUpdate(Message message) {
		System.out.println("[LOBBY] handleLobbyUpdate recibido en " + client.getPlayerName());

		List<String> players = (List<String>) message.get("players");
		Integer connected = message.getInteger("playersConnected");
		Integer ready = message.getInteger("readyCount");
		Integer max = message.getInteger("maxPlayers");

		if (players != null) {
			playerListModel.clear();
			playerListModel.addElement(client.getPlayerName() + " (TÚ)");
			for (String player : players) {
				if (!player.equals(client.getPlayerName())) {
					playerListModel.addElement(player);
				}
			}
		}

		if (connected != null && max != null) {
			playersLabel.setText("Jugadores: " + connected + "/" + max);
			playersConnected = connected;
			maxPlayers = max;

			// LÓGICA DE ACTIVACIÓN DEL JUEGO Y BOTONES
			if (connected < 2) {
				waitingLabel.setText("Esperando a más jugadores (mínimo 2)...");
				waitingLabel.setForeground(Color.BLUE);
				readyButton.setEnabled(false);
				readyButton.setText("Faltan jugadores...");
			} else if (connected == max) {
				waitingLabel.setText("¡Sala llena! Iniciando...");
				waitingLabel.setForeground(new Color(0, 150, 0));
				readyButton.setEnabled(false);
			} else {
				waitingLabel.setText("Esperando a que los jugadores estén listos...");
				waitingLabel.setForeground(Color.BLUE);
				// Solo habilitamos el botón si el usuario aún no le ha dado a "Listo"
				if (!readyButton.getText().equals("Esperando a otros jugadores...")) {
					readyButton.setEnabled(true);
					readyButton.setText("Listo para jugar");
				}
			}
		}
	}

	private void markReady() {
		readyButton.setEnabled(false);
		readyButton.setText("Esperando a otros jugadores...");

		// Enviar el mensaje al servidor indicando que este jugador está listo
		Message readyMsg = new Message(Message.MessageType.PLAYER_READY);
		client.sendMessage(readyMsg);
	}

	private void cancelLobby() {
		client.disconnect();
		if (listener != null) {
			listener.onCancelLobby();
		}
	}

	public void setLobbyListener(LobbyListener listener) {
		this.listener = listener;
	}
}
