package net.salesianos.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Player {

	private final String name;
	private final List<Card> hand;

	public Player(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("El nombre del jugador es obligatorio");
		}
		this.name = name;
		this.hand = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public List<Card> getHand() {
		return Collections.unmodifiableList(hand);
	}

	public void drawCard(Card card) {
		hand.add(Objects.requireNonNull(card, "La carta no puede ser null"));
	}

	public Card drawCard(Deck deck) {
		Card card = deck.drawCard();
		hand.add(card);
		return card;
	}

	public void drawCards(Deck deck, int amount) {
		for (int i = 0; i < amount; i++) {
			drawCard(deck);
		}
	}

	public boolean hasCard(Card card) {
		return hand.contains(card);
	}

	public boolean removeCard(Card card) {
		return hand.remove(card);
	}

	public Card playCard(Card card, Card currentCard) {
		if (card == null || !hasCard(card)) {
			return null;
		}
		if (!card.canBePlayedOn(currentCard)) {
			return null;
		}
		removeCard(card);
		return card;
	}

	public int handSize() {
		return hand.size();
	}
}
