package net.salesianos.client.ui;

import net.salesianos.client.Client;
import net.salesianos.model.Card;
import net.salesianos.protocol.Message;

import javax.swing.*;
import java.awt.*;
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
	private JLabel currentCardLabel;
	private JLabel currentPlayerLabel;
	private JLabel deckSizeLabel;
	private JButton drawButton;
	private JButton unoButton;
	private JTextArea chatArea;
	private JTextField chatInput;
	private JButton sendChatButton;
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
		setSize(1200, 700);
		setLocationRelativeTo(null);
		setResizable(false);

		initComponents();
		setupClientListener();
	}

	private void initComponents() {
		setLayout(new BorderLayout());

		// Panel principal del juego
		JPanel gamePanel = new JPanel(new BorderLayout());
		gamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Panel superior: info y botones
		JPanel topPanel = new JPanel(new GridLayout(1, 2));

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
		currentCardLabel = new JLabel("Carta central: ?");
		currentCardLabel.setFont(new Font("Arial", Font.BOLD, 14));
		currentPlayerLabel = new JLabel("Turno: ?");
		currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 14));
		infoPanel.add(currentCardLabel);
		infoPanel.add(currentPlayerLabel);
		topPanel.add(infoPanel);

		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
		drawButton = new JButton("Robar carta");
		drawButton.addActionListener(e -> drawCard());
		unoButton = new JButton("¡UNO!");
		unoButton.addActionListener(e -> unoAction());
		actionPanel.add(drawButton);
		actionPanel.add(unoButton);
		topPanel.add(actionPanel);

		gamePanel.add(topPanel, BorderLayout.NORTH);

		// Panel central: mano de cartas
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createTitledBorder("Tu mano"));

		handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		handPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		JScrollPane handScroll = new JScrollPane(handPanel);
		handScroll.setPreferredSize(new Dimension(800, 120));
		centerPanel.add(handScroll, BorderLayout.CENTER);

		gamePanel.add(centerPanel, BorderLayout.CENTER);

		// Panel inferior: estados y mazo
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
		deckSizeLabel = new JLabel("Mazo: ?");
		deckSizeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		bottomPanel.add(deckSizeLabel);

		gamePanel.add(bottomPanel, BorderLayout.SOUTH);

		add(gamePanel, BorderLayout.CENTER);

		// Panel lateral: chat y jugadores
		JPanel sidePanel = new JPanel(new BorderLayout());
		sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		sidePanel.setPreferredSize(new Dimension(250, 0));

		// Chat
		JPanel chatPanel = new JPanel(new BorderLayout());
		chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));

		chatArea = new JTextArea();
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		chatArea.setWrapStyleWord(true);
		chatArea.setFont(new Font("Arial", Font.PLAIN, 11));

		JScrollPane chatScroll = new JScrollPane(chatArea);
		chatScroll.setPreferredSize(new Dimension(230, 200));
		chatPanel.add(chatScroll, BorderLayout.CENTER);

		JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
		chatInput = new JTextField();
		chatInput.addActionListener(e -> sendChat());
		sendChatButton = new JButton("Enviar");
		sendChatButton.addActionListener(e -> sendChat());
		inputPanel.add(chatInput, BorderLayout.CENTER);
		inputPanel.add(sendChatButton, BorderLayout.EAST);

		chatPanel.add(inputPanel, BorderLayout.SOUTH);
		sidePanel.add(chatPanel, BorderLayout.CENTER);

		// Lista de jugadores
		JPanel playersPanel = new JPanel(new BorderLayout());
		playersPanel.setBorder(BorderFactory.createTitledBorder("Jugadores"));

		playersListModel = new DefaultListModel<>();
		playersList = new JList<>(playersListModel);
		playersList.setFont(new Font("Arial", Font.PLAIN, 11));
		playersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane playersScroll = new JScrollPane(playersList);
		playersScroll.setPreferredSize(new Dimension(230, 150));
		playersPanel.add(playersScroll, BorderLayout.CENTER);

		sidePanel.add(playersPanel, BorderLayout.SOUTH);

		add(sidePanel, BorderLayout.EAST);
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
					String unoPlayer = message.getString("playerName");
					addChatMessage("¡" + unoPlayer + " dice UNO!");
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

		currentCardLabel.setText("Carta central: " + (currentCard != null ? currentCard : "?"));
		currentPlayerLabel.setText("Turno: " + currentPlayer);

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

		// Actualizar disponibilidad de botones
		boolean isMyTurn = currentPlayer.equals(client.getPlayerName());
		drawButton.setEnabled(isMyTurn);
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
			playersListModel.addElement(name + " (" + handSize + " cartas)");
		}
	}

	private void selectCard(CardButton cardButton) {
		if (selectedCard != null) {
			selectedCard.deselect();
		}
		selectedCard = cardButton;
		selectedCard.select();

		// Mostrar confirmación para jugar
		int response = JOptionPane.showConfirmDialog(
			this,
			"¿Jugar la carta " + selectedCard.getCard() + "?",
			"Confirmar",
			JOptionPane.YES_NO_OPTION
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

	private void unoAction() {
		Message unoMessage = new Message(Message.MessageType.UNO_BUTTON);
		client.sendMessage(unoMessage);
		unoButton.setEnabled(false);
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
		JOptionPane.showMessageDialog(this, "¡" + winner + " ha ganado!", "Juego terminado", JOptionPane.INFORMATION_MESSAGE);
		if (listener != null) {
			listener.onGameEnd();
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

	/**
	 * Componente visual para una carta.
	 */
	private static class CardButton extends JButton {

		private final Card card;
		private boolean selected;

		CardButton(Card card, java.awt.event.ActionListener action) {
			this.card = card;
			this.selected = false;

			setText(card.toString());
			setPreferredSize(new Dimension(80, 100));
			setFont(new Font("Arial", Font.BOLD, 12));

			// Color de fondo según el color de la carta
			setBackground(getCardColor());
			setForeground(Color.WHITE);
			setOpaque(true);
			setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			setFocusPainted(false);

			addActionListener(action);
		}

		private Color getCardColor() {
			return switch (card.getColor()) {
				case ROJO -> new Color(200, 50, 50);
				case AZUL -> new Color(50, 50, 200);
				case VERDE -> new Color(50, 150, 50);
				case AMARILLO -> new Color(200, 200, 50);
			};
		}

		void select() {
			selected = true;
			setBorder(BorderFactory.createLineBorder(Color.WHITE, 4));
		}

		void deselect() {
			selected = false;
			setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		}

		Card getCard() {
			return card;
		}
	}
}
