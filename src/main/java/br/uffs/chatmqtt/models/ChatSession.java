package br.uffs.chatmqtt.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChatSession {
    private final String chatId;
    private final String userA;
    private final String userB;
    private final List<String> messages = new ArrayList<>();
    private final LocalDateTime createdAt;

    public ChatSession(String chatId, String userA, String userB) {
        this.chatId = chatId;
        this.userA = userA;
        this.userB = userB;
        this.createdAt = LocalDateTime.now();
    }

    public String getChatId() { return chatId; }
    public String getUserA() { return userA; }
    public String getUserB() { return userB; }
    public List<String> getMessages() { return messages; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void addMessage(String message) {
        messages.add(message);
    }

    public void printHistory() {
        System.out.println("\n🕓 Histórico da conversa (" + chatId + "):");
        if (messages.isEmpty()) {
            System.out.println("   ⚠️ Nenhuma mensagem ainda.");
        } else {
            for (String m : messages) {
                System.out.println("   " + m);
            }
        }
    }

    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}