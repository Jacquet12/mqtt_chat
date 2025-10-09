package br.uffs.chatmqtt.services;

import br.uffs.chatmqtt.mqtt.MqttService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Scanner;

public class MessageService {
    private final MqttService mqttService;
    private final String currentUserId;
    private final Scanner scanner = new Scanner(System.in);

    public MessageService(String userId, MqttService mqttService) {
        this.currentUserId = userId.trim();
        this.mqttService = mqttService;

        try {
            // Escuta controle de solicitações privadas
            mqttService.subscribe(currentUserId + "_PrivateControl",
                    (topic, msg) -> handlePrivateControl(msg));
        } catch (MqttException e) {
            System.out.println("❌ Erro ao assinar tópico privado: " + e.getMessage());
        }
    }

    private void handlePrivateControl(MqttMessage msg) {
        String payload = new String(msg.getPayload());

        try {
            if (payload.contains("\"chatRequest\"")) {
                String requester = payload.split("\"from\":\"")[1].split("\"")[0];
                System.out.println("\n📩 " + requester + " quer iniciar uma conversa privada.");
                System.out.print("👉 Aceitar? (s/n): ");
                String resposta = scanner.nextLine().trim().toLowerCase();

                if (resposta.equals("s")) {
                    String chatId = makeChatId(currentUserId, requester);
                    mqttService.publish(requester + "_PrivateControl",
                            "{\"accepted\":true,\"chatId\":\"" + chatId + "\"}", false);
                    startChat(chatId, requester);
                } else {
                    mqttService.publish(requester + "_PrivateControl",
                            "{\"accepted\":false}", false);
                    System.out.println("❌ Conversa recusada.");
                }
            } else if (payload.contains("\"accepted\"")) {
                if (payload.contains("\"true\"")) {
                    String chatId = payload.split("\"chatId\":\"")[1].split("\"")[0];
                    System.out.println("✅ Sua solicitação foi aceita! Canal: " + chatId);
                    startChat(chatId, "Parceiro");
                } else {
                    System.out.println("❌ Sua solicitação de conversa foi recusada.");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erro ao processar controle privado: " + e.getMessage());
        }
    }

    public void requestChat(String targetUser) {
        try {
            String payload = String.format("{\"chatRequest\":true,\"from\":\"%s\"}", currentUserId);
            mqttService.publish(targetUser + "_PrivateControl", payload, false);
            System.out.println("📨 Solicitação enviada a [" + targetUser + "]");
        } catch (MqttException e) {
            System.out.println("❌ Erro ao solicitar conversa: " + e.getMessage());
        }
    }

    private void startChat(String chatId, String partner) {
        System.out.println("\n💬 Conversa iniciada com " + partner + " (chatId=" + chatId + ")");

        try {
            mqttService.subscribe(chatId, (topic, msg) -> {
                String message = new String(msg.getPayload());
                if (!message.startsWith(currentUserId + ":")) {
                    System.out.println("\n📥 " + message);
                }
            });
        } catch (MqttException e) {
            System.out.println("❌ Erro ao assinar chat: " + e.getMessage());
            return;
        }

        while (true) {
            String text = scanner.nextLine();
            if (text.equalsIgnoreCase("/sair")) {
                System.out.println("👋 Você saiu da conversa.");
                break;
            }
            try {
                mqttService.publish(chatId, currentUserId + ": " + text, false);
            } catch (MqttException e) {
                System.out.println("❌ Erro ao enviar mensagem: " + e.getMessage());
            }
        }
    }

    private String makeChatId(String user1, String user2) {
        return "PRIVATE/" + (user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1);
    }
}