package net.salesianos.client.ui.components;

import java.awt.*;

public class ColorUtils {

    public static final Color[] PLAYER_COLORS = {
            new Color(0, 102, 204),   // Azul
            new Color(255, 204, 0),   // Amarillo
            new Color(228, 30, 38),   // Rojo
            new Color(0, 150, 0),     // Verde
    };

    public static Color getColorForPlayer(String playerName) {
        String cleanName = playerName.replace(" (TÚ)", "");
        int colorIndex = Math.abs(cleanName.hashCode()) % PLAYER_COLORS.length;
        return PLAYER_COLORS[colorIndex];
    }
}