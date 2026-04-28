package net.salesianos.utils;

import net.salesianos.model.Card;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad centralizada para parsear cadenas de Cartas
 * Elimina duplicidad de código entre GameFrame y GameRoom.
 */
public class CardParser {

    private static final Logger LOGGER = Logger.getLogger(CardParser.class.getName());

    private CardParser() {
    }

    /**
     * Parsea una cadena de carta en formato "COLOR-VALOR" a un objeto Card.
     *
     * @param cardStr la cadena de carta
     * @return el objeto Card, o null si es inválido
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

