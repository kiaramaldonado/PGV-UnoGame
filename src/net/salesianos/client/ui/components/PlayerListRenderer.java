package net.salesianos.client.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Gestiona la demostración de jugadores.
 * */
public class PlayerListRenderer extends DefaultListCellRenderer {

    // Obtener la lista de jugadores
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        String playerName = value.toString();

        if (playerName.endsWith(" (TÚ)")) {
            label.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 16));
        }

        Color playerColor = ColorUtils.getColorForPlayer(playerName);

        label.setIcon(createCircleIcon(playerColor, 18));
        label.setIconTextGap(15);

        return label;
    }

    // Método auxiliar para dibujar el círculo de color
    private Icon createCircleIcon(Color color, int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x, y, size, size);

                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(x, y, size, size);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }
}