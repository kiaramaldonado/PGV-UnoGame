package net.salesianos.util;

import net.salesianos.model.Card;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized utility for parsing Card strings.
 * Eliminates code duplication between GameFrame and GameRoom.
 */
public class CardParser {

    private static final Logger LOGGER = Logger.getLogger(CardParser.class.getName());

    private CardParser() {
        // Utility class - no instantiation
    }

    /**
     * Parses a card string in format "COLOR-VALUE" to a Card object.
     *
     * @param cardStr the card string (e.g., "ROJO-FIVE")
     * @return the Card object, or null if invalid
     */
    public static Card parseCard(String cardStr) {
        if (cardStr == null || !cardStr.contains("-")) {
            return null;
        }

        String[] parts = cardStr.split("-");
        if (parts.length != 2) {
            return null;
        }

        try {
            Card.Color color = Card.Color.valueOf(parts[0]);
            Card.Value value = Card.Value.valueOf(parts[1]);
            return new Card(color, value);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid card: " + cardStr);
            return null;
        }
    }
}

