package net.salesianos.model;

import java.util.Objects;

public class Card {

    public enum Color {
        ROJO,
        AZUL,
        VERDE,
        AMARILLO
    }

    public enum Value {
        ZERO(true),
        ONE(true),
        TWO(true),
        THREE(true),
        FOUR(true),
        FIVE(true),
        SIX(true),
        SEVEN(true),
        EIGHT(true),
        NINE(true),
        SALTAR(false),
        REVERSA(false),
        MAS_DOS(false);

        private final boolean number;

        Value(boolean number) {
            this.number = number;
        }

        public boolean isNumber() {
            return number;
        }
    }

    private final Color color;
    private final Value value;

    public Card(Color color, Value value) {
        this.color = Objects.requireNonNull(color, "El color no puede ser null");
        this.value = Objects.requireNonNull(value, "El valor no puede ser null");
    }

    public Color getColor() {
        return color;
    }

    public Value getValue() {
        return value;
    }

    public boolean canBePlayedOn(Card currentCard) {
        if (currentCard == null) {
            return true;
        }
        return this.color == currentCard.color || this.value == currentCard.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Card)) {
            return false;
        }
        Card other = (Card) obj;
        return color == other.color && value == other.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, value);
    }

    @Override
    public String toString() {
        return color + "-" + value;
    }
}
