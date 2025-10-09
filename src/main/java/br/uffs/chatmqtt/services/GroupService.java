package br.uffs.chatmqtt.services;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.models.Group;
import br.uffs.chatmqtt.models.JoinRequest;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupService {
    private final MqttService mqttService;
    private final Map<String, Group> groups = new HashMap<>();
    private final Queue<JoinRequest> pendingRequests = new LinkedList<>();
    private final String currentUserId;

    // Regex patterns
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern OWNER_PATTERN = Pattern.compile("\"owner\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MEMBERS_PATTERN = Pattern.compile("\"members\"\\s*:\\s*\\[(.*?)\\]");

    private static final Pattern GROUPID_PATTERN = Pattern.compile("\"groupId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern USERID_PATTERN = Pattern.compile("\"userId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JOINREQ_PATTERN = Pattern.compile("\"joinRequest\"\\s*:\\s*(true|false)");

    public GroupService(String userId, MqttService mqttService) throws MqttException {
        this.currentUserId = userId.trim();
        this.mqttService = mqttService;

        mqttService.subscribe("GROUPS", (topic, msg) -> handleGroupMessage(msg));
        mqttService.subscribe(currentUserId + "_Control", (topic, msg) -> handleControlMessage(msg));
    }

    private void handleGroupMessage(MqttMessage msg) {
        String payload = new String(msg.getPayload());
        String id = extract(ID_PATTERN, payload);
        String name = extract(NAME_PATTERN, payload);
        String owner = extract(OWNER_PATTERN, payload);

        if (id == null || name == null || owner == null) return;

        Group group = new Group(id, name, owner);

        String membersStr = extract(MEMBERS_PATTERN, payload);
        if (membersStr != null) {
            String[] members = membersStr.replace("\"", "").split(",");
            for (String m : members) {
                String trimmed = m.trim();
                if (!trimmed.isEmpty()) group.addMember(trimmed);
            }
        }

        groups.put(id, group);
        System.out.println("📢 Grupo atualizado: " + name + " (líder: " + owner + ")");
    }

    private void handleControlMessage(MqttMessage msg) {
        String payload = new String(msg.getPayload());
        System.out.println("📩 Mensagem recebida no controle: " + payload);

        Matcher mJoin = JOINREQ_PATTERN.matcher(payload);
        if (!mJoin.find() || !"true".equalsIgnoreCase(mJoin.group(1))) {
            return;
        }

        String groupId = extract(GROUPID_PATTERN, payload);
        String requester = extract(USERID_PATTERN, payload);

        if (groupId == null || requester == null) {
            System.out.println("⚠️ Erro ao extrair dados do join request!");
            return;
        }

        System.out.println("📨 Solicitação de ingresso no grupo [" + groupId + "] recebida de [" + requester + "]");
        pendingRequests.add(new JoinRequest(groupId, requester));
        System.out.println("⚠️ Vá ao menu e escolha a opção [7] para gerenciar solicitações pendentes.");
    }

    private String extract(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    public void createGroup(String groupId, String name) throws MqttException {
        Group group = new Group(groupId, name, currentUserId);
        groups.put(groupId, group);
        publishGroup(group);
        System.out.println("🆕 Grupo criado: " + name + " (líder: " + currentUserId + ")");
    }

    public void requestJoin(String groupId) throws MqttException {
        Group group = groups.get(groupId);
        if (group == null) {
            System.out.println("⚠️ Grupo não encontrado!");
            return;
        }
        String leader = group.getOwnerId().trim();
        String topic = leader + "_Control";
        String payload = String.format(
                "{\"joinRequest\":true,\"groupId\":\"%s\",\"userId\":\"%s\"}",
                groupId, currentUserId
        );

        System.out.println("🚀 Publicando join em tópico: " + topic + " | Payload: " + payload);
        mqttService.publish(topic, payload, false);
        System.out.println("📨 Solicitação de ingresso enviada ao líder [" + leader + "]");
    }

    public void publishGroup(Group group) throws MqttException {
        String payload = String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"owner\":\"%s\",\"members\":[%s]}",
                group.getId(),
                group.getName(),
                group.getOwnerId(),
                String.join(",", group.getMembers().stream().map(m -> "\"" + m + "\"").toList())
        );
        System.out.println("📡 Publicando grupo no tópico GROUPS: " + payload);
        mqttService.publish("GROUPS", payload, true);
    }

    public void listGroups() {
        System.out.println("\n📋 ==== Lista de Grupos ====");
        if (groups.isEmpty()) {
            System.out.println("⚠️ Nenhum grupo encontrado!");
            return;
        }
        for (Group g : groups.values()) {
            System.out.println("👥 Grupo: " + g.getName() +
                    " | Líder: " + g.getOwnerId() +
                    " | Membros: " + String.join(", ", g.getMembers()));
        }
    }

    /** ============================
     *  GERENCIAR SOLICITAÇÕES
     *  ============================ */
    public void processRequests() {
        if (pendingRequests.isEmpty()) {
            System.out.println("✅ Nenhuma solicitação pendente.");
            return;
        }

        while (!pendingRequests.isEmpty()) {
            JoinRequest req = pendingRequests.poll();
            System.out.println("\n📨 Solicitação: " + req.getUserId() + " quer entrar no grupo [" + req.getGroupId() + "]");
            System.out.print("👉 Aceitar? (s/n): ");

            Scanner sc = new Scanner(System.in);
            String resposta = sc.nextLine().trim().toLowerCase();

            if (resposta.equals("s")) {
                Group g = groups.get(req.getGroupId());
                if (g != null && g.getOwnerId().equals(currentUserId)) {
                    try {
                        g.addMember(req.getUserId());
                        publishGroup(g);
                        System.out.println("✅ " + req.getUserId() + " foi adicionado ao grupo " + g.getName());
                    } catch (MqttException e) {
                        System.out.println("❌ Erro ao publicar grupo: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("❌ Solicitação de " + req.getUserId() + " rejeitada.");
            }
        }
    }
}