package br.uffs.chatmqtt.services;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.models.ChatSession;
import br.uffs.chatmqtt.ui.ConsoleUI;
import org.eclipse.paho.client.mqttv3.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatService {

    private final MqttService mqtt;
    private final String currentUser;
    private final Scanner scanner = new Scanner(System.in);
    private final ConsoleUI consoleUI;

    private final Map<String, ChatSession> sessions = new HashMap<>();
    private final List<String> pendingRequests = new ArrayList<>();

    public ChatService(String currentUser, MqttService mqtt, ConsoleUI console) throws MqttException {
        this.currentUser = currentUser;
        this.mqtt = mqtt;
        this.consoleUI = console;

        mqtt.addControlListener((topic, msg) -> handleControl(msg));
    }

    public void requestChat(String target) throws MqttException {
        if (target.equalsIgnoreCase(currentUser)) {
            System.out.println("Não pode falar consigo mesmo.");
            return;
        }

        String payload =
                "{\"type\":\"chat_request\",\"from\":\"" + currentUser + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";

        mqtt.publishControlMessage(target, payload);
        System.out.println("Solicitação enviada para " + target);
    }

    private void handleControl(MqttMessage msg) {
        String payload = new String(msg.getPayload());

        if (payload.contains("\"type\":\"chat_request\"")) {
            String from = extract(payload, "from");
            pendingRequests.add(from);
            System.out.println("Solicitação de conversa recebida de " + from);
        }

        if (payload.contains("\"type\":\"chat_accept\"")) {
            String id = extract(payload, "chatId");
            String partner = extract(payload, "from");
            startChat(id, partner);
        }
    }

    public void processRequests() throws MqttException {
        if (pendingRequests.isEmpty()) {
            System.out.println("Nenhuma solicitação pendente.");
            return;
        }

        for (String from : new ArrayList<>(pendingRequests)) {
            System.out.print("Aceitar conversa com " + from + "? (s/n): ");
            String r = scanner.nextLine().trim().toLowerCase();

            if (r.equals("s")) {

                String chatId = makeChatId(currentUser, from);

                String payload =
                        "{\"type\":\"chat_accept\",\"from\":\"" + currentUser + "\",\"chatId\":\"" + chatId + "\"}";

                mqtt.publishControlMessage(from, payload);

                pendingRequests.remove(from);

                startChat(chatId, from);
            } else {
                pendingRequests.remove(from);
                System.out.println("Recusada.");
            }
        }
    }

    private void startChat(String chatId, String partner) {
        consoleUI.ativarChat();

        String topic = "chat/" + chatId;

        try {
            mqtt.subscribe(topic, (tp, msg) -> {
                String m = new String(msg.getPayload());
                if (!m.startsWith(currentUser + ":")) {
                    System.out.println("\n" + m);
                }
            });
        } catch (Exception e) {
            consoleUI.desativarChat();
            return;
        }

        System.out.println("\nChat com " + partner);
        System.out.println("Digite /sair para encerrar.");

        ChatSession session = new ChatSession(chatId, currentUser, partner);
        sessions.put(chatId, session);

        while (true) {
            String text = scanner.nextLine().trim();

            if (text.equalsIgnoreCase("/sair")) {
                System.out.println("Saindo da conversa...");
                consoleUI.desativarChat();
                break;
            }

            String msg = currentUser + ": " + text;

            try {
                mqtt.publish(topic, msg, false);
            } catch (Exception ignored) {}

            session.addMessage(msg);
        }
    }

    public void listRequests() {
        System.out.println("Pendentes: " + pendingRequests);
    }

    private String makeChatId(String a, String b) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        return a + "_" + b + "_" + ts;
    }

    private String extract(String json, String key) {
        try { return json.split("\"" + key + "\":\"")[1].split("\"")[0]; }
        catch (Exception e) { return ""; }
    }
}