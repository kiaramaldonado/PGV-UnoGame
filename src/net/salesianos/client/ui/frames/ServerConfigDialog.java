package net.salesianos.client.ui.frames;

import net.salesianos.client.ui.components.GameButton;

import javax.swing.*;
import java.awt.*;

public class ServerConfigDialog extends JDialog {

    private String port = null;
    private JTextField portField;

    public ServerConfigDialog(Frame owner, int defaultPort) {
        super(owner, "Configurar Sala", true);
        setupUI(defaultPort);
    }

    private void setupUI(int defaultPort) {
        setLayout(new BorderLayout(10, 20));
        getContentPane().setBackground(new Color(45, 45, 45));

        // --- Panel Superior: Título ---
        JLabel titleLabel = new JLabel("CONFIGURACIÓN DE SALA");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        add(titleLabel, BorderLayout.NORTH);

        // --- Panel Central: Input ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JLabel label = new JLabel("Puerto del servidor: ");
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(new Font("Arial", Font.PLAIN, 14));

        portField = new JTextField(String.valueOf(defaultPort), 8);
        portField.setBackground(new Color(60, 60, 60));
        portField.setForeground(Color.WHITE);
        portField.setCaretColor(Color.WHITE);
        portField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(228, 30, 38), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        portField.setFont(new Font("Consolas", Font.BOLD, 16));

        centerPanel.add(label);
        centerPanel.add(portField);
        add(centerPanel, BorderLayout.CENTER);

        // --- Panel Inferior: Botones ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.setOpaque(false);

        GameButton btnConfirm = new GameButton("ABRIR SALA", null);
        // Podemos ajustar el tamaño para que no sean tan grandes como los del menú principal
        btnConfirm.setPreferredSize(new Dimension(160, 50));

        GameButton btnCancel = new GameButton("CANCELAR", null);
        btnCancel.setPreferredSize(new Dimension(160, 50));
        // Opcional: Podrías añadir un color diferente al botón cancelar en GameButton

        btnConfirm.addActionListener(e -> {
            port = portField.getText();
            dispose();
        });

        btnCancel.addActionListener(e -> {
            port = null;
            dispose();
        });

        buttonPanel.add(btnConfirm);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    public String getPort() {
        return port;
    }
}