package net.salesianos.client.ui;

import net.salesianos.client.Client;
import net.salesianos.client.ui.components.CardButton;
import net.salesianos.client.ui.components.GameButton;
import net.salesianos.client.ui.components.UIUtils;
import net.salesianos.model.Card;
import net.salesianos.protocol.Message;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interfaz principal de juego.
 * Muestra la mano del jugador, la carta central, rivales y controles.
 */
public class GameFrame extends JFrame {

	private static final Logger LOGGER = Logger.getLogger(GameFrame.class.getName());

	private Client client;
	private GameListener listener;

	// Componentes UI
	private JPanel handPanel;
	private JPanel discardPileContainer;
	private JButton drawPileButton;
	private JLabel currentPlayerLabel;
	private JLabel directionLabel;
	private JDialog unoDialog;
	private JTextArea chatArea;
	private JTextField chatInput;
	private GameButton sendChatButton;
	private JList<String> playersList;
	private DefaultListModel<String> playersListModel;
	private List<CardButton> cardButtons;
	private CardButton selectedCard;

	// Estado del juego
	private List<String> playerHand;
	private String currentCard;
	private String currentPlayer;
	private int direction;

	public interface GameListener {
		void onGameEnd();
		void onDisconnected();
	}

	public GameFrame(Client client) {
		this.client = client;
		this.cardButtons = new ArrayList<>();
		this.playerHand = new ArrayList<>();

		setTitle("UNO - En juego");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 750); // Un poco más alto para dar espacio a las cartas
		setLocationRelativeTo(null);
		setResizable(false);

		initComponents();
		setupClientListener();
	}

	private void initComponents() {
		// Fondo general
		getContentPane().setBackground(new Color(45, 45, 45));
		setLayout(new BorderLayout(10, 10));

		// ==========================================
		// PANEL PRINCIPAL (Mesa y Mano)
		// ==========================================
		JPanel mainGamePanel = new JPanel(new BorderLayout(10, 10));
		mainGamePanel.setOpaque(false);
		mainGamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));

		// --- PANEL SUPERIOR: INFO DE TURNO ---
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
		topPanel.setOpaque(false);

		currentPlayerLabel = new JLabel("Esperando turno...");
		currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 24));
		currentPlayerLabel.setForeground(Color.WHITE);

		directionLabel = new JLabel("⇄"); // Indicador de dirección
		directionLabel.setFont(new Font("Arial", Font.BOLD, 28));
		directionLabel.setForeground(Color.LIGHT_GRAY);

		topPanel.add(currentPlayerLabel);
		topPanel.add(directionLabel);
		mainGamePanel.add(topPanel, BorderLayout.NORTH);

		// --- PANEL CENTRAL: LA MESA (Mazo y Descarte) ---
		JPanel tablePanel = new JPanel(new GridBagLayout());
		tablePanel.setOpaque(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 20, 0, 20); // Espacio entre el mazo y la pila

		// 1. Mazo para robar (Carta boca abajo)
		drawPileButton = createDrawPileButton();
		gbc.gridx = 0;
		tablePanel.add(drawPileButton, gbc);

		// 2. Pila de descartes (Carta actual)
		discardPileContainer = new JPanel(new BorderLayout());
		discardPileContainer.setOpaque(false);
		discardPileContainer.setPreferredSize(new Dimension(120, 180));

		// Placeholder inicial
		JLabel placeholderLabel = new JLabel("?", SwingConstants.CENTER);
		placeholderLabel.setFont(new Font("Arial", Font.BOLD, 40));
		placeholderLabel.setForeground(Color.GRAY);
		placeholderLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
		discardPileContainer.add(placeholderLabel, BorderLayout.CENTER);

		gbc.gridx = 1;
		tablePanel.add(discardPileContainer, gbc);

		mainGamePanel.add(tablePanel, BorderLayout.CENTER);

		// --- PANEL INFERIOR: MANO DEL JUGADOR ---
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setOpaque(false);

		TitledBorder handBorder = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.GRAY), "Tu Mano");
		handBorder.setTitleColor(Color.WHITE);
		handBorder.setTitleFont(new Font("Arial", Font.BOLD, 14));
		bottomPanel.setBorder(handBorder);

		handPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, -10, 10)); // Margen negativo para que se solapen un poco como un abanico
		handPanel.setBackground(new Color(55, 55, 55));

		JScrollPane handScroll = new JScrollPane(handPanel);
		handScroll.setPreferredSize(new Dimension(800, 220));
		handScroll.setBorder(null);
		handScroll.getViewport().setBackground(new Color(55, 55, 55));
		handScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		handScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		bottomPanel.add(handScroll, BorderLayout.CENTER);
		mainGamePanel.add(bottomPanel, BorderLayout.SOUTH);

		add(mainGamePanel, BorderLayout.CENTER);

		// ==========================================
		// PANEL LATERAL (Chat y Jugadores)
		// ==========================================
		JPanel sidePanel = new JPanel(new BorderLayout(0, 10));
		sidePanel.setOpaque(false);
		sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
		sidePanel.setPreferredSize(new Dimension(300, 0));

		// --- LISTA DE JUGADORES ---
		JPanel playersPanel = new JPanel(new BorderLayout());
		playersPanel.setOpaque(false);
		TitledBorder playersBorder = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.GRAY), "Jugadores");
		playersBorder.setTitleColor(Color.WHITE);
		playersPanel.setBorder(playersBorder);

		playersListModel = new DefaultListModel<>();
		playersList = new JList<>(playersListModel);
		playersList.setBackground(new Color(60, 60, 60));
		playersList.setForeground(Color.WHITE);
		playersList.setFont(new Font("Arial", Font.BOLD, 14));
		playersList.setSelectionBackground(new Color(80, 80, 80));

		JScrollPane playersScroll = new JScrollPane(playersList);
		playersScroll.setPreferredSize(new Dimension(280, 150));
		playersScroll.setBorder(null);
		playersPanel.add(playersScroll, BorderLayout.CENTER);

		sidePanel.add(playersPanel, BorderLayout.NORTH);

		// --- CHAT ---
		JPanel chatPanel = new JPanel(new BorderLayout(0, 5));
		chatPanel.setOpaque(false);
		TitledBorder chatBorder = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.GRAY), "Chat de Sala");
		chatBorder.setTitleColor(Color.WHITE);
		chatPanel.setBorder(chatBorder);

		chatArea = new JTextArea();
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		chatArea.setWrapStyleWord(true);
		chatArea.setBackground(new Color(30, 30, 30));
		chatArea.setForeground(Color.WHITE);
		chatArea.setFont(new Font("Consolas", Font.PLAIN, 13));
		chatArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JScrollPane chatScroll = new JScrollPane(chatArea);
		chatScroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		chatPanel.add(chatScroll, BorderLayout.CENTER);

		JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
		inputPanel.setOpaque(false);

		chatInput = new JTextField();
		chatInput.setBackground(new Color(60, 60, 60));
		chatInput.setForeground(Color.WHITE);
		chatInput.setCaretColor(Color.WHITE);
		chatInput.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)
		));
		chatInput.addActionListener(e -> sendChat());

		sendChatButton = new GameButton("ENVIAR", null);
		sendChatButton.setPreferredSize(new Dimension(80, 30));
		sendChatButton.setFont(new Font("Arial", Font.BOLD, 10));
		sendChatButton.addActionListener(e -> sendChat());

		inputPanel.add(chatInput, BorderLayout.CENTER);
		inputPanel.add(sendChatButton, BorderLayout.EAST);

		chatPanel.add(inputPanel, BorderLayout.SOUTH);
		sidePanel.add(chatPanel, BorderLayout.CENTER);

		add(sidePanel, BorderLayout.EAST);
	}

	/**
	 * Crea el mazo de robar usando la imagen real del reverso de la carta.
	 */
	private JButton createDrawPileButton() {
		JButton btn = new JButton();
		btn.setPreferredSize(new Dimension(120, 180));

		btn.setContentAreaFilled(false);
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);

		try {
			URL cardBackUrl = getClass().getResource("/assets/uno-card.png");
			if (cardBackUrl != null) {
				ImageIcon icon = new ImageIcon(cardBackUrl);
				Image img = icon.getImage().getScaledInstance(120, 180, Image.SCALE_SMOOTH);
				btn.setIcon(new ImageIcon(img));
			} else {
				btn.setBackground(new Color(228, 30, 38));
				btn.setOpaque(true);
				btn.setText("UNO");
				btn.setForeground(Color.WHITE);
				btn.setFont(new Font("Arial", Font.BOLD, 30));
			}
		} catch (Exception e) {
			System.out.println("No se pudo cargar la imagen del mazo: " + e.getMessage());
		}

		btn.addActionListener(e -> drawCard());
		return btn;
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
					JOptionPane.showMessageDialog(GameFrame.this,
							"Desconectado del servidor",
							"Error de conexión",
							JOptionPane.ERROR_MESSAGE);
					if (listener != null) {
						listener.onDisconnected();
					}
				});
			}
		});
	}

	private void handleServerMessage(Message message) {
		SwingUtilities.invokeLater(() -> {
			switch (message.getType()) {
				case UPDATE_STATE:
					updateGameState(message);
					break;
				case GAME_OVER:
					handleGameOver(message);
					break;
				case ERROR:
					String errorMsg = message.getString("errorMessage");
					JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
					break;
				case CHAT:
					String playerName = message.getString("playerName");
					String chatMsg = message.getString("message");
					addChatMessage(playerName + ": " + chatMsg);
					break;
				case UNO_BUTTON:
					String action = message.getString("action");
					if ("SHOW".equals(action)) {
						String target = message.getString("targetPlayer");
						showUnoDialog(target);
					} else if ("HIDE".equals(action)) {
						hideUnoDialog();
					} else {
						String unoPlayer = message.getString("playerName");
						if (unoPlayer != null) {
							addChatMessage("¡" + unoPlayer + " dice UNO!");
						}
					}
					break;
				default:
					break;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void updateGameState(Message message) {
		currentCard = message.getString("currentCard");
		currentPlayer = message.getString("currentPlayer");
		direction = message.getInteger("direction");

		Card centerCard = parseCard(currentCard);
		discardPileContainer.removeAll();
		if (centerCard != null) {
			CardButton centerBtn = new CardButton(centerCard, e -> {});
			centerBtn.setCursor(Cursor.getDefaultCursor());
			discardPileContainer.add(centerBtn, BorderLayout.CENTER);
		}
		discardPileContainer.revalidate();
		discardPileContainer.repaint();

		// Actualizar UI del turno actual
		boolean isMyTurn = currentPlayer.equals(client.getPlayerName());
		if (isMyTurn) {
			currentPlayerLabel.setText("¡ES TU TURNO!");
			currentPlayerLabel.setForeground(new Color(100, 255, 100));
		} else {
			currentPlayerLabel.setText("Turno de: " + currentPlayer);
			currentPlayerLabel.setForeground(Color.WHITE);
		}

		directionLabel.setText(direction == 1 ? "⟳" : "⟲");

		// Actualizar la mano del jugador
		List<String> hand = (List<String>) message.get("hand");
		if (hand != null) {
			updatePlayerHand(hand);
		}

		// Actualizar lista de jugadores
		List<Map<String, Object>> players = (List<Map<String, Object>>) message.get("players");
		if (players != null) {
			updatePlayersList(players);
		}

		// Actualizar disponibilidad del mazo de robar
		drawPileButton.setEnabled(isMyTurn);
		if(isMyTurn) {
			drawPileButton.setBorder(BorderFactory.createLineBorder(new Color(100, 255, 100), 4, true)); // Brillo verde si es tu turno
		} else {
			drawPileButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 4, true));
		}
	}

	@SuppressWarnings("unchecked")
	private void updatePlayerHand(List<String> hand) {
		handPanel.removeAll();
		cardButtons.clear();
		selectedCard = null;

		playerHand = hand;

		for (String cardStr : hand) {
			Card card = parseCard(cardStr);
			if (card != null) {
				CardButton[] cardButtonRef = new CardButton[1];
				cardButtonRef[0] = new CardButton(card, e -> selectCard(cardButtonRef[0]));
				cardButtons.add(cardButtonRef[0]);
				handPanel.add(cardButtonRef[0]);
			}
		}

		handPanel.revalidate();
		handPanel.repaint();
	}

	private void updatePlayersList(List<Map<String, Object>> players) {
		playersListModel.clear();
		for (Map<String, Object> playerInfo : players) {
			String name = (String) playerInfo.get("name");
			Integer handSize = (Integer) playerInfo.get("handSize");
			// Pequeño indicador si es su turno
			String turnIndicator = name.equals(currentPlayer) ? " ⬅" : "";
			playersListModel.addElement(name + ": " + handSize + " cartas" + turnIndicator);
		}
	}

	private void selectCard(CardButton cardButton) {
		// Solo puedes seleccionar si es tu turno
		if (!currentPlayer.equals(client.getPlayerName())) {
			return;
		}

		if (selectedCard != null) {
			selectedCard.deselect();
		}
		selectedCard = cardButton;
		selectedCard.select();

		// Diálogo tematizado
		int response = JOptionPane.showOptionDialog(
				this,
				"¿Quieres jugar esta carta?",
				"Confirmar jugada",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				new String[]{"Jugar", "Cancelar"},
				"Jugar"
		);

		if (response == JOptionPane.YES_OPTION) {
			playCard(selectedCard.getCard());
		} else {
			selectedCard.deselect();
			selectedCard = null;
		}
	}

	private void playCard(Card card) {
		Message playMessage = new Message(Message.MessageType.PLAY_CARD);
		playMessage.put("card", card.toString());
		client.sendMessage(playMessage);
	}

	private void drawCard() {
		Message drawMessage = new Message(Message.MessageType.DRAW_CARD);
		client.sendMessage(drawMessage);
	}

	private void sendChat() {
		String message = chatInput.getText().trim();
		if (!message.isEmpty()) {
			Message chatMessage = new Message(Message.MessageType.CHAT);
			chatMessage.put("playerName", client.getPlayerName());
			chatMessage.put("message", message);
			client.sendMessage(chatMessage);
			chatInput.setText("");
		}
	}

	private void addChatMessage(String message) {
		chatArea.append(message + "\n");
		chatArea.setCaretPosition(chatArea.getDocument().getLength());
	}

	private void handleGameOver(Message message) {
		String winner = message.getString("winnerName");
		JOptionPane.showMessageDialog(this, "¡" + winner + " ha ganado la partida!", "🏆 Fin del juego", JOptionPane.INFORMATION_MESSAGE);
		if (listener != null) {
			listener.onGameEnd();
		}
	}

	private void showUnoDialog(String targetName) {
		if (unoDialog != null) {
			unoDialog.dispose();
		}

		unoDialog = new JDialog(this, "¡Atención!", false);
		unoDialog.setSize(350, 180);
		unoDialog.setLocationRelativeTo(this);
		unoDialog.getContentPane().setBackground(new Color(45, 45, 45));
		unoDialog.setLayout(new BorderLayout(10, 20));

		JLabel label = new JLabel("¡" + targetName + " tiene 1 carta!", SwingConstants.CENTER);
		label.setFont(new Font("Arial", Font.BOLD, 18));
		label.setForeground(Color.WHITE);
		label.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		JPanel btnPanel = new JPanel(new FlowLayout());
		btnPanel.setOpaque(false);

		GameButton btn = new GameButton("¡GRITAR UNO!", null);
		btn.setPreferredSize(new Dimension(200, 50));
		btn.addActionListener(e -> {
			client.sendMessage(new Message(Message.MessageType.UNO_BUTTON));
			hideUnoDialog();
		});

		btnPanel.add(btn);
		unoDialog.add(label, BorderLayout.NORTH);
		unoDialog.add(btnPanel, BorderLayout.CENTER);
		unoDialog.setAlwaysOnTop(true);
		unoDialog.setVisible(true);
	}

	private void hideUnoDialog() {
		if (unoDialog != null) {
			unoDialog.dispose();
			unoDialog = null;
		}
	}

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
			LOGGER.log(Level.WARNING, "Carta inválida: " + cardStr);
			return null;
		}
	}

	public void setGameListener(GameListener listener) {
		this.listener = listener;
	}
}