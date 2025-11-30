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

    public ChatService(String currentUser, MqttService mqtt, ConsoleUI console) {
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

        String payload = "{\"type\":\"chat_request\",\"from\":\"" + currentUser + "\"}";
        mqtt.publishControlMessage(target, payload);

        System.out.println("Solicitação enviada para " + target);
    }

    private void handleControl(MqttMessage msg) {
        String payload = new String(msg.getPayload());

        if (payload.contains("\"type\":\"chat_request\"")) {
            String from = extract(payload, "from");
            pendingRequests.add(from);

            System.out.println("\n📥 Nova solicitação de conversa privada de " + from);
        }

        if (payload.contains("\"type\":\"chat_accept\"")) {
            String chatId = extract(payload, "chatId");
            String partner = extract(payload, "from");

            ChatSession session = new ChatSession(chatId, currentUser, partner);
            sessions.put(chatId, session);

            subscribePrivateChat(chatId);

            System.out.println("\n✔ Conversa com " + partner + " criada");
            System.out.println("➡ Entre na opção [12] para conversar.");
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

                ChatSession session = new ChatSession(chatId, currentUser, from);
                sessions.put(chatId, session);

                subscribePrivateChat(chatId);

                System.out.println("\n✔ Conversa criada com " + from);
                System.out.println("➡ Use a opção [12] para conversar.");

                pendingRequests.remove(from);
            } else {
                System.out.println("Recusada.");
                pendingRequests.remove(from);
            }
        }
    }

    private void subscribePrivateChat(String chatId) {
        try {
            mqtt.subscribe("chat/" + chatId, (tp, msg) -> {
                String m = new String(msg.getPayload());
                if (!m.startsWith(currentUser + ":")) {
                    System.out.println("\n" + m);
                }
            });
        } catch (Exception ignored) {}
    }

    public void listSessions() {
        if (sessions.isEmpty()) {
            System.out.println("Nenhuma conversa privada existente.");
            return;
        }

        System.out.println("\nConversas privadas disponíveis:");
        for (ChatSession s : sessions.values()) {
            String partner = s.getUserA().equals(currentUser) ? s.getUserB() : s.getUserA();
            System.out.println("- Chat com " + partner + " (ID: " + s.getChatId() + ")");
        }
    }

    public void openPrivateChat(ConsoleUI consoleUI) throws MqttException {
        if (sessions.isEmpty()) {
            System.out.println("Nenhuma conversa privada disponível.");
            return;
        }

        List<ChatSession> list = new ArrayList<>(sessions.values());

        System.out.println("\nEscolha o chat:");
        for (int i = 0; i < list.size(); i++) {
            ChatSession s = list.get(i);
            String partner = s.getUserA().equals(currentUser) ? s.getUserB() : s.getUserA();
            System.out.println((i + 1) + " - Conversa com " + partner);
        }

        System.out.print("Opção: ");
        String opt = scanner.nextLine().trim();

        if (!opt.matches("\\d+")) {
            System.out.println("Opção inválida.");
            return;
        }

        int idx = Integer.parseInt(opt) - 1;
        if (idx < 0 || idx >= list.size()) {
            System.out.println("Opção inválida.");
            return;
        }

        ChatSession session = list.get(idx);
        String chatId = session.getChatId();
        String partner = session.getUserA().equals(currentUser) ? session.getUserB() : session.getUserA();

        startChat(chatId, partner);
    }

    private void startChat(String chatId, String partner) {
        consoleUI.ativarChat();

        String topic = "chat/" + chatId;

        System.out.println("\n💬 Conversa privada com " + partner);
        System.out.println("Digite /sair para encerrar.");

        while (true) {
            String text = scanner.nextLine().trim();

            if (text.equalsIgnoreCase("/sair")) {
                consoleUI.desativarChat();
                break;
            }

            String msg = currentUser + ": " + text;

            try {
                mqtt.publish(topic, msg, false);
            } catch (Exception ignored) {}
        }
    }

    public void listRequests() {
        System.out.println("Solicitações pendentes: " + pendingRequests);
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