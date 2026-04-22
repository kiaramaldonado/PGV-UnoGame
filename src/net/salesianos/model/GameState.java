package net.salesianos.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GameState {

	private final List<Player> players;
	private final Deck deck;
	private int currentTurnIndex;
	private int direction;
	private Card currentCard;
	private int pendingTurnAdvance;

	public GameState(List<Player> players) {
		if (players == null || players.size() < 2 || players.size() > 4) {
			throw new IllegalArgumentException("El juego requiere entre 2 y 4 jugadores");
		}
		this.players = new ArrayList<>(players);
		this.deck = new Deck();
		this.currentTurnIndex = 0;
		this.direction = 1;
		this.pendingTurnAdvance = 1;
	}

	public void startGame(int initialCards) {
		for (Player player : players) {
			deck.deal(player, initialCards);
		}

		currentCard = deck.drawCard();
		deck.discard(currentCard);
	}

	public List<Player> getPlayers() {
		return Collections.unmodifiableList(players);
	}

	public Player getCurrentPlayer() {
		return players.get(currentTurnIndex);
	}

	public Card getCurrentCard() {
		return currentCard;
	}

	public int getDirection() {
		return direction;
	}

	public Deck getDeck() {
		return deck;
	}

	public void nextTurn() {
		nextTurn(1);
	}

	public boolean isValidCard(Card card) {
		Objects.requireNonNull(card, "La carta no puede ser null");
		return card.canBePlayedOn(currentCard);
	}

	public boolean playCurrentPlayerCard(Card card) {
		Player currentPlayer = getCurrentPlayer();

		if (!currentPlayer.hasCard(card) || !isValidCard(card)) {
			return false;
		}

		currentPlayer.removeCard(card);
		currentCard = card;
		deck.discard(card);

		pendingTurnAdvance = 1;
		applyCardEffect(card);
		nextTurn(pendingTurnAdvance);

		return true;
	}

	public void applyCardEffect(Card card) {
		switch (card.getValue()) {
			case SALTAR:
				pendingTurnAdvance = 2;
				break;
			case REVERSA:
				direction *= -1;
				if (players.size() == 2) {
					pendingTurnAdvance = 2;
				}
				break;
			case MAS_DOS:
				Player penalized = players.get(peekIndex(1));
				penalized.drawCards(deck, 2);
				pendingTurnAdvance = 2;
				break;
			default:
				pendingTurnAdvance = 1;
				break;
		}
	}

	private void nextTurn(int steps) {
		currentTurnIndex = peekIndex(steps);
	}

	private int peekIndex(int steps) {
		int size = players.size();
		int raw = currentTurnIndex + (direction * steps);
		int mod = raw % size;
		return mod < 0 ? mod + size : mod;
	}
}
