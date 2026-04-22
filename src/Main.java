import net.salesianos.client.Client;
import net.salesianos.client.ui.GameFrame;
import net.salesianos.client.ui.LobbyFrame;
import net.salesianos.client.ui.LoginFrame;
import net.salesianos.server.Server;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.util.Scanner;

/**
 * Punto de entrada de la aplicación.
 * Permite elegir entre ejecutar como cliente o servidor.
 */
public class Main {

	private static final int DEFAULT_PORT = 8888;

	public static void main(String[] args) {
		String mode = askMode();

		if ("server".equalsIgnoreCase(mode)) {
			startServer();
		} else if ("client".equalsIgnoreCase(mode)) {
			startClient();
		} else {
			System.out.println("Opción inválida");
		}
	}

	private static String askMode() {
		if (isHeadless()) {
			// Si no hay GUI (línea de comandos)
			Scanner scanner = new Scanner(System.in);
			System.out.println("=== UNO Game ===");
			System.out.println("1. Servidor");
			System.out.println("2. Cliente");
			System.out.print("Selecciona una opción: ");
			String input = scanner.nextLine().trim();
			scanner.close();

			return switch (input) {
				case "1" -> "server";
				case "2" -> "client";
				default -> "";
			};
		} else {
			// Mostrar diálogo en GUI
			String[] options = {"Servidor", "Cliente"};
			int choice = JOptionPane.showOptionDialog(
				null,
				"¿Deseas ejecutar como servidor o cliente?",
				"UNO - Seleccionar modo",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[1]
			);

			return choice == 0 ? "server" : "client";
		}
	}

	private static void startServer() {
		String portStr = JOptionPane.showInputDialog(
			null,
			"Ingresa el puerto para el servidor:",
			DEFAULT_PORT
		);

		int port = DEFAULT_PORT;
		if (portStr != null && !portStr.trim().isEmpty()) {
			try {
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(
					null,
					"Puerto inválido. Usando puerto " + DEFAULT_PORT,
					"Error",
					JOptionPane.ERROR_MESSAGE
				);
			}
		}

		Server server = new Server(port);
		System.out.println("Iniciando servidor en puerto " + port + "...");
		server.start();
	}

	private static void startClient() {
		// Mostrar LoginFrame
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