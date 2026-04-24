package net.salesianos.client.ui;

import net.salesianos.client.Client;
import net.salesianos.protocol.Message;

import javax.swing.*;

/**
 * Handles chat-related functionality.
 * Separates chat logic from GameFrame.
 * Follows Single Responsibility Principle.
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
     * Sends a chat message to the server.
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
     * Adds a message to the chat display.
     */
    public void addMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * Adds a system message (e.g., "SISTEMA: ...").
     */
    public void addSystemMessage(String message) {
        addMessage("[SYSTEM] " + message);
    }

    /**
     * Clears the chat area.
     */
    public void clear() {
        chatArea.setText("");
    }

    public void setChatListener(ChatListener listener) {
        this.listener = listener;
    }
}

