package net.salesianos.client.ui.components;

import net.salesianos.model.Card;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Componente visual reutilizable para una carta de UNO.
 */
public class CardButton extends JButton {

    private final Card card;
    private boolean selected;

    public CardButton(Card card, ActionListener action) {
        this.card = card;
        this.selected = false;

        setText(getCardSymbol(card));
        setPreferredSize(new Dimension(80, 100));
        setFont(new Font("Arial", Font.BOLD, 30));

        // Color de fondo según el color de la carta
        setBackground(getCardColorForUI(card));
        setForeground(Color.WHITE);
        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        setFocusPainted(false);

        addActionListener(action);
    }

    public void select() {
        selected = true;
        setBorder(BorderFactory.createLineBorder(Color.WHITE, 4));
    }

    public void deselect() {
        selected = false;
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    }

    public Card getCard() {
        return card;
    }

    // Métodos estáticos de utilidad visual trasladados aquí
    public static String getCardSymbol(Card card) {
        if (card == null) return "?";
        return switch (card.getValue()) {
            case ZERO -> "0";
            case ONE -> "1";
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case SALTAR -> "⊘";
            case REVERSA -> "⇄";
            case MAS_DOS -> "+2";
        };
    }

    public static Color getCardColorForUI(Card card) {
        if (card == null) return Color.BLACK;
        return switch (card.getColor()) {
            case ROJO -> new Color(200, 50, 50);
            case AZUL -> new Color(50, 50, 200);
            case VERDE -> new Color(50, 150, 50);
            case AMARILLO -> new Color(200, 200, 50);
        };
    }
}