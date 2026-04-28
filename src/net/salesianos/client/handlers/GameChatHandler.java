package net.salesianos.client.handlers;

import net.salesianos.client.Client;
import net.salesianos.protocol.Message;

import javax.swing.*;

/**
 * Gestiona lo relacionado con el chat.
 * Separa la lógica del chat de la lógica de GameFrame.
 */
public class GameChatHandler {

    private final JTextArea chatArea;
    private final JTextField chatInput;
    private final Client client;

    public interface ChatListener {
        void onChatMessageSent(String message);
    }

    private ChatListener listener;

    public GameChatHandler(JTextArea chatArea, JTextField chatInput, Client client) {
        this.chatArea = chatArea;
        this.chatInput = chatInput;
        this.client = client;
    }

    /**
     * Envíar mensaje del chat al servidor
     */
    public void sendMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            Message chatMessage = new Message(Message.MessageType.CHAT);
            chatMessage.put("playerName", client.getPlayerName());
            chatMessage.put("message", message);
            client.sendMessage(chatMessage);
            chatInput.setText("");

            if (listener != null) {
                listener.onChatMessageSent(message);
            }
        }
    }

    /**
     * Añadir el mensaje en el display
     */
    public void addMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * Añadir mensaje del sistema (e.g., "SISTEMA: ...").
     */
    public void addSystemMessage(String message) {
        addMessage("[SYSTEM] " + message);
    }

    /**
     * Limpia el área de chat.
     */
    public void clear() {
        chatArea.setText("");
    }

    public void setChatListener(ChatListener listener) {
        this.listener = listener;
    }
}

