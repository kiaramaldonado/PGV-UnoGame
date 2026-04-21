package net.salesianos.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

	private final List<Card> drawPile;
	private final List<Card> discardPile;

	public Deck() {
		this.drawPile = new ArrayList<>();
		this.discardPile = new ArrayList<>();
		generateDeck();
		shuffle();
	}

	public final void generateDeck() {
		drawPile.clear();
		discardPile.clear();

		for (Card.Color color : Card.Color.values()) {
			drawPile.add(new Card(color, Card.Value.ZERO));

			addTwoCopies(color, Card.Value.ONE);
			addTwoCopies(color, Card.Value.TWO);
			addTwoCopies(color, Card.Value.THREE);
			addTwoCopies(color, Card.Value.FOUR);
			addTwoCopies(color, Card.Value.FIVE);
			addTwoCopies(color, Card.Value.SIX);
			addTwoCopies(color, Card.Value.SEVEN);
			addTwoCopies(color, Card.Value.EIGHT);
			addTwoCopies(color, Card.Value.NINE);

			addTwoCopies(color, Card.Value.SALTAR);
			addTwoCopies(color, Card.Value.REVERSA);
			addTwoCopies(color, Card.Value.MAS_DOS);
		}
	}

	public void shuffle() {
		Collections.shuffle(drawPile);
	}

	public Card drawCard() {
		if (drawPile.isEmpty()) {
			recycleDiscardPile();
		}
		if (drawPile.isEmpty()) {
			throw new IllegalStateException("No hay cartas disponibles para robar");
		}
		return drawPile.remove(drawPile.size() - 1);
	}

	public void discard(Card card) {
		discardPile.add(card);
	}

	public void deal(Player player, int amount) {
		for (int i = 0; i < amount; i++) {
			player.drawCard(drawCard());
		}
	}

	public int getDrawPileSize() {
		return drawPile.size();
	}

	public int getDiscardPileSize() {
		return discardPile.size();
	}

	private void recycleDiscardPile() {
		if (discardPile.size() <= 1) {
			return;
		}

		Card topCard = discardPile.remove(discardPile.size() - 1);
		drawPile.addAll(discardPile);
		discardPile.clear();
		discardPile.add(topCard);
		shuffle();
	}

	private void addTwoCopies(Card.Color color, Card.Value value) {
		drawPile.add(new Card(color, value));
		drawPile.add(new Card(color, value));
	}
}
