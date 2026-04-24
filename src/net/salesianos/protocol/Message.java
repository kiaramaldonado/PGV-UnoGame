package net.salesianos.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Clase para serializar/deserializar mensajes entre cliente y servidor.
 * Utiliza tipos de mensajes predefinidos y un mapa genérico de datos.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum MessageType {
        LOGIN,           // Jugador se conecta: {playerName, playerId}
        PLAY_CARD,       // Jugador juega carta: {card, playerId}
        DRAW_CARD,       // Jugador roba carta: {playerId}
        PLAYER_READY,    // Jugador dice que está listo: {playerId}
        START_GAME,      // Servidor inicia partida: {players, currentCard}
        UPDATE_STATE,    // Servidor envía estado: {currentCard, currentPlayer, players, hands}
        DRAW_PENALTY,    // Servidor penaliza: {playerId, cardCount}
        GAME_OVER,       // Juego terminado: {winnerId, winnerName}
        CHAT,            // Mensaje de chat: {playerName, message}
        LOBBY_UPDATE,    // Actualización de lobby: {players, readyCount}
        UNO_BUTTON,      // Jugador dice UNO: {playerId}
        ERROR,           // Error del servidor: {errorMessage}
        DISCONNECT       // Desconexión: {playerId, reason}
    }

    private MessageType type;
    private Map<String, Object> data;

    public Message(MessageType type) {
        this.type = Objects.requireNonNull(type, "MessageType no puede ser null");
        this.data = new HashMap<>();
    }

    public Message(MessageType type, Map<String, Object> data) {
        this.type = Objects.requireNonNull(type, "MessageType no puede ser null");
        this.data = new HashMap<>(Objects.requireNonNull(data, "Data no puede ser null"));
    }

    public MessageType getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public Integer getInteger(String key) {
        Object value = data.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}
