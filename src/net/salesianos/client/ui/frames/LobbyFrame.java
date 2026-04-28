package net.salesianos.client.ui.frames;

import net.salesianos.client.Client;
import net.salesianos.client.ui.components.GameButton;
import net.salesianos.client.ui.components.PlayerListRenderer;
import net.salesianos.protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.util.List;
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
    private GameButton readyButton;
    private GameButton cancelButton;
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
        setSize(500, 550);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
        setupClientListener();
    }

    private void initComponents() {
        getContentPane().setBackground(new Color(45, 45, 45));
        setLayout(new BorderLayout(20, 20));

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // --- PANEL SUPERIOR: INFORMACIÓN ---
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("SALA DE ESPERA", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        infoPanel.add(titleLabel);

        playersLabel = new JLabel("Jugadores: 1/4", SwingConstants.CENTER);
        playersLabel.setForeground(Color.WHITE);
        playersLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        infoPanel.add(playersLabel);

        waitingLabel = new JLabel("Esperando a más jugadores...", SwingConstants.CENTER);
        waitingLabel.setFont(new Font("Arial", Font.BOLD, 14));
        waitingLabel.setForeground(new Color(100, 200, 255));
        infoPanel.add(waitingLabel);

        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // --- PANEL CENTRAL: LISTA DE JUGADORES ---
        playerListModel = new DefaultListModel<>();
        playerListModel.addElement(client.getPlayerName() + " (TÚ)");

        playerList = new JList<>(playerListModel);
        playerList.setBackground(new Color(60, 60, 60));
        playerList.setSelectionBackground(new Color(80, 80, 80));

        // Aplicamos nuestro renderizador personalizado
        playerList.setCellRenderer(new PlayerListRenderer());

        JScrollPane scrollPane = new JScrollPane(playerList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(228, 30, 38), 2));
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- PANEL INFERIOR: BOTONES ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        readyButton = new GameButton("LISTO", null);
        readyButton.setPreferredSize(new Dimension(180, 50));
        readyButton.addActionListener(e -> markReady());
        buttonPanel.add(readyButton);

        cancelButton = new GameButton("SALIR", null);
        cancelButton.setPreferredSize(new Dimension(180, 50));
        cancelButton.addActionListener(e -> cancelLobby());
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
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

            if (connected < 2) {
                waitingLabel.setText("Esperando a más jugadores (mínimo 2)...");
                waitingLabel.setForeground(new Color(100, 200, 255));
                readyButton.setEnabled(false);
            } else if (connected == max) {
                waitingLabel.setText("¡Sala llena! Iniciando...");
                waitingLabel.setForeground(new Color(100, 255, 100));
                readyButton.setEnabled(false);
            } else {
                waitingLabel.setText("Esperando a que los jugadores estén listos...");
                waitingLabel.setForeground(new Color(255, 204, 0));
                if (readyButton.isEnabled() || !readyButton.getText().equals("ESPERANDO...")) {
                    readyButton.setEnabled(true);
                }
            }
        }
    }

    private void markReady() {
        readyButton.setEnabled(false);
        readyButton.setText("ESPERANDO...");

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