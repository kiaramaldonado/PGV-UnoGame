package net.salesianos.client.ui;

import net.salesianos.client.Client;
import net.salesianos.client.ui.components.GameButton;
import net.salesianos.protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interfaz gráfica para conectarse al servidor.
 * Permite ingresar nombre, IP y puerto del servidor.
 */
public class LoginFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());

    private JTextField playerNameField;
    private JTextField serverHostField;
    private JTextField serverPortField;
    private GameButton connectButton;
    private JLabel statusLabel;
    private Client client;
    private LoginListener listener;

    public interface LoginListener {
        void onLoginSuccess(Client client);

        void onLoginFailed(String reason);
    }

    public LoginFrame() {
        setTitle("UNO - Conectar al Servidor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 400); // Un poco más alto para que respire bien el diseño
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
    }

    private void initComponents() {
        // Fondo principal
        getContentPane().setBackground(new Color(45, 45, 45));
        setLayout(new BorderLayout(10, 20));

        // --- PANEL SUPERIOR: TÍTULO ---
        JLabel titleLabel = new JLabel("UNIRSE A SALA");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        add(titleLabel, BorderLayout.NORTH);

        // --- PANEL CENTRAL: FORMULARIO ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false); // Transparente para ver el fondo oscuro

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        // Estilo común para las etiquetas
        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Color labelColor = Color.LIGHT_GRAY;

        // 1. Nombre de jugador
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Nombre:");
        nameLabel.setForeground(labelColor);
        nameLabel.setFont(labelFont);
        formPanel.add(nameLabel, gbc);

        playerNameField = new JTextField(15);
        playerNameField.setText("Jugador1");
        styleTextField(playerNameField);
        gbc.gridx = 1;
        formPanel.add(playerNameField, gbc);

        // 2. IP del servidor
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel ipLabel = new JLabel("IP del servidor:");
        ipLabel.setForeground(labelColor);
        ipLabel.setFont(labelFont);
        formPanel.add(ipLabel, gbc);

        serverHostField = new JTextField(15);
        serverHostField.setText("localhost");
        styleTextField(serverHostField);
        gbc.gridx = 1;
        formPanel.add(serverHostField, gbc);

        // 3. Puerto del servidor
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel portLabel = new JLabel("Puerto:");
        portLabel.setForeground(labelColor);
        portLabel.setFont(labelFont);
        formPanel.add(portLabel, gbc);

        serverPortField = new JTextField(15);
        serverPortField.setText("8888");
        styleTextField(serverPortField);
        gbc.gridx = 1;
        formPanel.add(serverPortField, gbc);

        add(formPanel, BorderLayout.CENTER);

        // --- PANEL INFERIOR: BOTÓN Y ESTADO ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Botón Conectar (Usando nuestro GameButton)
        connectButton = new GameButton("CONECTAR", null);
        connectButton.setPreferredSize(new Dimension(200, 50));
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.addActionListener(e -> attemptConnection());

        // Label de estado
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        bottomPanel.add(connectButton);
        bottomPanel.add(statusLabel);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Método auxiliar para estandarizar el diseño de los inputs de texto
     */
    private void styleTextField(JTextField field) {
        field.setBackground(new Color(60, 60, 60));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(228, 30, 38), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        field.setFont(new Font("Consolas", Font.BOLD, 16));
    }

    private void attemptConnection() {
        String playerName = playerNameField.getText().trim();
        String serverHost = serverHostField.getText().trim();
        String portStr = serverPortField.getText().trim();

        if (playerName.isEmpty()) {
            showError("El nombre del jugador es obligatorio");
            return;
        }

        if (serverHost.isEmpty()) {
            showError("La IP del servidor es obligatoria");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                showError("El puerto debe estar entre 1 y 65535");
                return;
            }
        } catch (NumberFormatException e) {
            showError("El puerto debe ser un número válido");
            return;
        }

        connectButton.setEnabled(false);
        statusLabel.setText("Conectando...");
        // Azul clarito para que contraste con el fondo oscuro
        statusLabel.setForeground(new Color(100, 200, 255));

        // Conectar en un hilo separado para no bloquear la UI
        new Thread(() -> connectToServer(playerName, serverHost, port)).start();
    }

    private void connectToServer(String playerName, String serverHost, int port) {
        try {
            client = new Client(playerName, serverHost, port);

            // NO registrar listener aquí - dejar que los mensajes se encolen
            // para que LobbyFrame los procese luego
            System.out.println("[LOGIN] Cliente creado para " + playerName);
            System.out.println("[LOGIN] NO se registra listener - los mensajes se encolaran");

            // Intentar conectar
            if (client.connect()) {
                // Esperar confirmación del servidor (en este caso es inmediata)
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Conectado correctamente");
                    statusLabel.setForeground(new Color(100, 255, 100)); // Verde claro

                    if (listener != null) {
                        listener.onLoginSuccess(client);
                    }
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    showError("No se pudo conectar al servidor");
                    connectButton.setEnabled(true);
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en conexión: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                showError("Error: " + e.getMessage());
                connectButton.setEnabled(true);
            });
        }
    }

    private void handleServerMessage(Message message) {
        // Por ahora no esperamos respuesta específica
        // En versiones futuras podría validar credenciales del servidor
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(new Color(255, 100, 100));
        connectButton.setEnabled(true);
    }

    public void setLoginListener(LoginListener listener) {
        this.listener = listener;
    }
}