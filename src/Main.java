import net.salesianos.client.Client;
import net.salesianos.client.ui.*;
import net.salesianos.client.ui.components.GameButton;
import net.salesianos.server.Server;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Scanner;

/**
 * Punto de entrada de la aplicación.
 * Permite elegir entre ejecutar como creador de sala (servidor) o jugador (cliente).
 */
public class Main {

	private static final int DEFAULT_PORT = 8888;

	public static void main(String[] args) {
		String mode = askMode();

		if ("server".equalsIgnoreCase(mode)) {
			startServer();
		} else if ("client".equalsIgnoreCase(mode)) {
			startClient();
		} else if (!mode.isEmpty()) {
			System.out.println("Opción cancelada o inválida");
		}
	}

	private static String askMode() {
		if (isHeadless()) {
			// Si no hay GUI (línea de comandos)
			Scanner scanner = new Scanner(System.in);
			System.out.println("=== UNO Game ===");
			System.out.println("1. 🌐 Crear Sala (Abrir puerto)");
			System.out.println("2. 🎮 Jugar (Unirse a sala)");
			System.out.print("Selecciona una opción: ");
			String input = scanner.nextLine().trim();
			scanner.close();

			return switch (input) {
				case "1" -> "server";
				case "2" -> "client";
				default -> "";
			};
		} else {
			MainMenuDialog menuDialog = new MainMenuDialog();
			menuDialog.setVisible(true);

			return menuDialog.getSelectedMode();
		}
	}

	private static void startServer() {
		// 1. Pedir la configuración (nuestro diálogo personalizado)
		ServerConfigDialog configDialog = new ServerConfigDialog(null, DEFAULT_PORT);
		configDialog.setVisible(true);

		String portStr = configDialog.getPort();

		// Si canceló, salimos silenciosamente
		if (portStr == null) {
			return;
		}

		int port = DEFAULT_PORT;
		try {
			port = Integer.parseInt(portStr.trim());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null,
					"Puerto inválido. Abriendo sala en el puerto por defecto: " + DEFAULT_PORT,
					"Aviso",
					JOptionPane.WARNING_MESSAGE);
		}

		// 2. Instanciar el servidor
		Server server = new Server(port);

		// 3. Mostrar la nueva ventana visual de "Sala Abierta"
		ServerActiveFrame activeFrame = new ServerActiveFrame(server, port);
		activeFrame.setVisible(true);

		// 4. Arrancar el servidor en un HILO NUEVO
		// Esto es VITAL para que el GIF gire y el botón "CERRAR" reaccione
		int finalPort = port;
		Thread serverThread = new Thread(() -> {
			System.out.println("Sala creada y esperando jugadores en el puerto " + finalPort + "...");
			server.start();
		});

		// El hilo se cerrará automáticamente si se cierra la app
		serverThread.setDaemon(true);
		serverThread.start();
	}

	private static void startClient() {
		LoginFrame loginFrame = new LoginFrame();
		loginFrame.setVisible(true);

		loginFrame.setLoginListener(new LoginFrame.LoginListener() {
			@Override
			public void onLoginSuccess(Client client) {
				loginFrame.dispose();
				showLobby(client);
			}

			@Override
			public void onLoginFailed(String reason) {
				JOptionPane.showMessageDialog(
						loginFrame,
						"Error: " + reason,
						"Conexión fallida",
						JOptionPane.ERROR_MESSAGE
				);
			}
		});
	}

	private static void showLobby(Client client) {
		LobbyFrame lobbyFrame = new LobbyFrame(client);
		lobbyFrame.setVisible(true);

		lobbyFrame.setLobbyListener(new LobbyFrame.LobbyListener() {
			@Override
			public void onGameStart() {
				lobbyFrame.dispose();
				showGame(client);
			}

			@Override
			public void onCancelLobby() {
				System.exit(0);
			}
		});
	}

	private static void showGame(Client client) {
		GameFrame gameFrame = new GameFrame(client);
		gameFrame.setVisible(true);

		gameFrame.setGameListener(new GameFrame.GameListener() {
			@Override
			public void onGameEnd() {
				gameFrame.dispose();
				System.exit(0);
			}

			@Override
			public void onDisconnected() {
				gameFrame.dispose();
				System.exit(0);
			}
		});
	}

	private static boolean isHeadless() {
		return GraphicsEnvironment.isHeadless();
	}
}