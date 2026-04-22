package net.salesianos.client.ui;

import net.salesianos.client.Client;
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
	private JButton connectButton;
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
		setSize(400, 300);
		setLocationRelativeTo(null);
		setResizable(false);

		initComponents();
	}

	private void initComponents() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);

		// Nombre de jugador
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(new JLabel("Nombre de jugador:"), gbc);

		playerNameField = new JTextField(15);
		playerNameField.setText("Jugador1");
		gbc.gridx = 1;
		panel.add(playerNameField, gbc);

		// IP del servidor
		gbc.gridx = 0;
		gbc.gridy = 1;
		panel.add(new JLabel("IP del servidor:"), gbc);

		serverHostField = new JTextField(15);
		serverHostField.setText("localhost");
		gbc.gridx = 1;
		panel.add(serverHostField, gbc);

		// Puerto del servidor
		gbc.gridx = 0;
		gbc.gridy = 2;
		panel.add(new JLabel("Puerto:"), gbc);

		serverPortField = new JTextField(15);
		serverPortField.setText("8888");
		gbc.gridx = 1;
		panel.add(serverPortField, gbc);

		// Botón Conectar
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		connectButton = new JButton("Conectar");
		connectButton.addActionListener(e -> attemptConnection());
		panel.add(connectButton, gbc);

		// Label de estado
		gbc.gridy = 4;
		statusLabel = new JLabel(" ");
		statusLabel.setForeground(Color.RED);
		panel.add(statusLabel, gbc);

		add(panel);
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
		statusLabel.setForeground(Color.BLUE);

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
					statusLabel.setForeground(Color.GREEN);

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
		statusLabel.setForeground(Color.RED);
		connectButton.setEnabled(true);
	}

	public void setLoginListener(LoginListener listener) {
		this.listener = listener;
	}
}
