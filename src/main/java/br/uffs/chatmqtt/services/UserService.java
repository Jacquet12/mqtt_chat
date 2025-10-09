package br.uffs.chatmqtt.services;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.models.User;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserService {
    private final MqttService mqttService;
    private final Map<String, User> users = new HashMap<>();
    private final String currentUserId;

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");

    public UserService(String userId, MqttService mqttService) throws MqttException {
        this.currentUserId = userId;
        this.mqttService = mqttService;

        mqttService.subscribe("USERS", (topic, msg) -> handleUsersMessage(msg));

        users.put(currentUserId, new User(currentUserId, true));
    }

    private void handleUsersMessage(MqttMessage msg) {
        String payload = new String(msg.getPayload());
        String id = extract(ID_PATTERN, payload);
        String status = extract(STATUS_PATTERN, payload);

        if (id == null || status == null) return;

        boolean online = "ONLINE".equalsIgnoreCase(status);
        users.put(id, new User(id, online));

        if (!id.equals(currentUserId)) {
            String origem = msg.isRetained() ? " (retido)" : "";
            if (online) {
                System.out.println(GREEN + "🟢 [" + id + "] está online" + origem + RESET);
            } else {
                System.out.println(RED + "🔴 [" + id + "] está offline" + origem + RESET);
            }
        }
    }

    private String extract(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    public void goOnline() throws MqttException {
        mqttService.publishUserStatus(currentUserId, true);
        users.put(currentUserId, new User(currentUserId, true));
        System.out.println(GREEN + "🟢 [" + currentUserId + "] agora está ONLINE ✅" + RESET);
    }

    public void goOffline() throws MqttException {
        mqttService.publishUserStatus(currentUserId, false);
        users.put(currentUserId, new User(currentUserId, false));
        System.out.println(RED + "🔴 [" + currentUserId + "] agora está OFFLINE ❌" + RESET);
    }

    public void listUsers() {
        System.out.println(CYAN + "\n📋 ==== Lista de Usuários ====" + RESET);
        boolean found = false;

        for (Map.Entry<String, User> entry : users.entrySet()) {
            String id = entry.getKey();
            User user = entry.getValue();

            if (id.equals(currentUserId)) continue;

            String status = user.isOnline()
                    ? GREEN + "🟢 ONLINE" + RESET
                    : RED + "🔴 OFFLINE" + RESET;

            System.out.println("👤 " + id + " -> " + status);
            found = true;
        }

        if (!found) {
            System.out.println(YELLOW + "⚠️  Nenhum outro usuário encontrado!" + RESET);
        }
    }
}
