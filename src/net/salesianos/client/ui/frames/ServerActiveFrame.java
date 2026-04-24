package net.salesianos.client.ui.frames;

import net.salesianos.client.ui.components.GameButton;
import net.salesianos.server.Server;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class ServerActiveFrame extends JFrame {

    private Server server;

    public ServerActiveFrame(Server server, int port) {
        this.server = server;
        setTitle("UNO - Sala Abierta");
        setSize(400, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setupUI(port);
    }

    private void setupUI(int port) {
        getContentPane().setBackground(new Color(45, 45, 45));
        setLayout(new BorderLayout());

        // --- PANEL CENTRAL: GIF Y TEXTO ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));

        // 1. Título
        JLabel titleLabel = new JLabel("ESPERANDO JUGADORES...");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. GIF Animado
        JLabel gifLabel = new JLabel();
        gifLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gifLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        try {
            URL gifUrl = getClass().getResource("/net/salesianos/resources/assets/man-playing.gif");
            if (gifUrl != null) {
                gifLabel.setIcon(new ImageIcon(gifUrl));
            } else {
                gifLabel.setText("(GIF no encontrado)");
                gifLabel.setForeground(Color.LIGHT_GRAY);
            }
        } catch (Exception e) {
            System.out.println("No se pudo cargar el GIF animado.");
        }

        // 3. Info del Puerto
        JLabel portLabel = new JLabel("Sala abierta en el puerto: " + port);
        portLabel.setForeground(new Color(255, 200, 0));
        portLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        portLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(titleLabel);
        centerPanel.add(gifLabel);
        centerPanel.add(portLabel);
        add(centerPanel, BorderLayout.CENTER);

        // --- PANEL INFERIOR: BOTÓN CERRAR ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        GameButton btnClose = new GameButton("CERRAR SALA", null);
        btnClose.setPreferredSize(new Dimension(200, 50));

        btnClose.addActionListener(e -> {
            // Aquí cerramos la aplicación entera, lo que destruye el servidor
            System.out.println("Cerrando sala...");
            System.exit(0);
        });

        bottomPanel.add(btnClose);
        add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // Centrar en pantalla
    }
}