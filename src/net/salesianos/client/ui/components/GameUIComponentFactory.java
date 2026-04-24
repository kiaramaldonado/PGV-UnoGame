package net.salesianos.client.ui.components;

import net.salesianos.model.Card;
import net.salesianos.util.CardParser;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles UI rendering for game components (cards, buttons, etc).
 * Separates UI rendering logic from GameFrame.
 * Follows Single Responsibility Principle.
 */
public class GameUIComponentFactory {

	private static final int CARD_WIDTH = 120;
	private static final int CARD_HEIGHT = 180;
	private static final int DRAW_PILE_WIDTH = 120;
	private static final int DRAW_PILE_HEIGHT = 180;

	private GameUIComponentFactory() {
		// Factory utility - no instantiation
	}

	/**
	 * Creates a button for the draw pile.
	 */
	public static JButton createDrawPileButton(Runnable onClickAction) {
		JButton btn = new JButton();
		btn.setPreferredSize(new Dimension(DRAW_PILE_WIDTH, DRAW_PILE_HEIGHT));
		btn.setContentAreaFilled(false);
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);

		try {
			URL cardBackUrl = GameUIComponentFactory.class.getResource("/net/salesianos/resources/assets/uno-card.png");
			if (cardBackUrl != null) {
				ImageIcon icon = new ImageIcon(cardBackUrl);
				Image img = icon.getImage().getScaledInstance(DRAW_PILE_WIDTH, DRAW_PILE_HEIGHT, Image.SCALE_SMOOTH);
				btn.setIcon(new ImageIcon(img));
			} else {
				btn.setBackground(new Color(228, 30, 38));
				btn.setOpaque(true);
				btn.setText("UNO");
				btn.setForeground(Color.WHITE);
				btn.setFont(new Font("Arial", Font.BOLD, 30));
			}
		} catch (Exception e) {
			System.out.println("Could not load draw pile image: " + e.getMessage());
		}

		if (onClickAction != null) {
			btn.addActionListener(e -> onClickAction.run());
		}

		return btn;
	}

	/**
	 * Creates card buttons from a list of card strings.
	 */
	public static List<CardButton> createCardButtons(List<String> cardStrings, CardButtonListener listener) {
		List<CardButton> buttons = new ArrayList<>();

		for (String cardStr : cardStrings) {
			Card card = CardParser.parseCard(cardStr);
			if (card != null) {
				final CardButton[] btnRef = new CardButton[1];
				btnRef[0] = new CardButton(card, e -> {
					if (listener != null) {
						listener.onCardSelected(btnRef[0]);
					}
				});
				buttons.add(btnRef[0]);
			}
		}

		return buttons;
	}

	/**
	 * Creates a card button for the current/discard pile card.
	 */
	public static CardButton createCenterCardButton(String cardStr) {
		Card card = CardParser.parseCard(cardStr);
		if (card != null) {
			CardButton btn = new CardButton(card, e -> {});
			btn.setCursor(Cursor.getDefaultCursor());
			return btn;
		}
		return null;
	}

	@FunctionalInterface
	public interface CardButtonListener {
		void onCardSelected(CardButton cardButton);
	}
}

