package net.salesianos.client.ui.frames;

import net.salesianos.client.ui.components.GameButton;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MainMenuDialog extends JDialog {

    private String selectedMode = "";

    public MainMenuDialog() {
        super((Frame) null, "UNO - Menú Principal", true); // true = Modal (bloquea hasta que se cierra)
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 20));
        getContentPane().setBackground(new Color(45, 45, 45));

        // --- 1. CONFIGURAR EL LOGO CENTRADO ---
        JLabel logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));

        try {
            URL logoUrl = getClass().getResource("/net/salesianos/resources/assets/UNO-logo.png");

            if (logoUrl != null) {
                ImageIcon originalIcon = new ImageIcon(logoUrl);
                // Escalar la imagen si es necesario
                Image scaledImage = originalIcon.getImage().getScaledInstance(200, -1, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                logoLabel.setText("¡Bienvenido a UNO!");
                logoLabel.setForeground(Color.WHITE);
                logoLabel.setFont(new Font("Arial", Font.BOLD, 24));
            }
        } catch (Exception e) {
            System.out.println("No se pudo cargar el logo: " + e.getMessage());
            e.printStackTrace();
        }

        add(logoLabel, BorderLayout.NORTH);

        // --- 2. CONFIGURAR LOS BOTONES ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        buttonPanel.setOpaque(false);

        GameButton btnServer = new GameButton("CREAR SALA", null);
        GameButton btnClient = new GameButton("JUGAR", null);

        btnServer.addActionListener(e -> {
            selectedMode = "server";
            dispose();
        });

        btnClient.addActionListener(e -> {
            selectedMode = "client";
            dispose();
        });

        buttonPanel.add(btnServer);
        buttonPanel.add(btnClient);
        add(buttonPanel, BorderLayout.CENTER);

        JPanel bottomPadding = new JPanel();
        bottomPadding.setOpaque(false);
        bottomPadding.setPreferredSize(new Dimension(10, 20));
        add(bottomPadding, BorderLayout.SOUTH);

        // --- 3. PROPIEDADES DE LA VENTANA ---
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public String getSelectedMode() {
        return selectedMode;
    }
}