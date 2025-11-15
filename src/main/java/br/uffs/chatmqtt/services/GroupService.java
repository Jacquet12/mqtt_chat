package br.uffs.chatmqtt.services;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.models.Group;
import br.uffs.chatmqtt.models.JoinRequest;
import org.eclipse.paho.client.mqttv3.*;

import java.util.*;

public class GroupService {

    private final String currentUser;
    private final MqttService mqtt;
    private final Map<String, Group> groups = new HashMap<>();
    private final Queue<JoinRequest> pendingRequests = new LinkedList<>();
    private final Scanner scanner = new Scanner(System.in);

    public GroupService(String currentUser, MqttService mqtt) throws MqttException {
        this.currentUser = currentUser;
        this.mqtt = mqtt;

        mqtt.subscribe("groups", (t, m) -> handleGroupUpdate(m));
        mqtt.addControlListener((t, m) -> handleControl(m));
    }

    public void createGroup(String groupId, String name) throws MqttException {
        Group g = new Group(groupId, name, currentUser);
        groups.put(groupId, g);

        String payload = "{\"id\":\"" + groupId + "\",\"name\":\"" + name + "\",\"owner\":\"" + currentUser + "\"}";
        mqtt.publish("groups", payload, true);

        mqtt.subscribe("group/" + groupId, (tp, msg) ->
                System.out.println("\n[Grupo " + name + "] " + new String(msg.getPayload())));
    }

    public void listGroups() {
        if (groups.isEmpty()) {
            System.out.println("Nenhum grupo.");
            return;
        }

        for (Group g : groups.values()) {
            System.out.println(g.getId() + " - " + g.getName() + " | Owner: " + g.getOwnerId() + " | " + g.getMembers());
        }
    }

    public void requestJoin(String groupId) throws MqttException {
        Group g = groups.get(groupId);
        if (g == null) {
            System.out.println("Grupo não encontrado.");
            return;
        }

        String leader = g.getOwnerId();
        String payload = "{\"type\":\"join_request\",\"group\":\"" + groupId + "\",\"from\":\"" + currentUser + "\"}";
        mqtt.publishControlMessage(leader, payload);

        System.out.println("Solicitação enviada.");
    }

    public void processRequests() throws MqttException {
        if (pendingRequests.isEmpty()) {
            System.out.println("Nenhuma solicitação.");
            return;
        }

        while (!pendingRequests.isEmpty()) {
            JoinRequest req = pendingRequests.poll();

            System.out.print("Aceitar " + req.getUserId() + " no grupo " + req.getGroupId() + "? (s/n): ");
            String r = scanner.nextLine().trim().toLowerCase();

            if (r.equals("s")) {
                String payload = "{\"type\":\"join_accept\",\"group\":\"" + req.getGroupId() + "\",\"user\":\"" + req.getUserId() + "\"}";
                mqtt.publishControlMessage(req.getUserId(), payload);

                groups.get(req.getGroupId()).addMember(req.getUserId());

                mqtt.subscribe("group/" + req.getGroupId(), (tp, msg) ->
                        System.out.println("\n[Grupo] " + new String(msg.getPayload())));

                System.out.println("Aceito.");
            } else {
                System.out.println("Rejeitado.");
            }
        }
    }

    public void sendGroupMessage(String groupId, String message) throws MqttException {
        Group g = groups.get(groupId);

        if (g == null) {
            System.out.println("Grupo desconhecido.");
            return;
        }

        String payload = currentUser + ": " + message;
        mqtt.publish("group/" + groupId, payload, false);

        System.out.println("Mensagem enviada.");
    }

    private void handleControl(MqttMessage msg) {
        String payload = new String(msg.getPayload());

        if (payload.contains("\"type\":\"join_request\"")) {
            String groupId = extract(payload, "group");
            String from = extract(payload, "from");

            pendingRequests.add(new JoinRequest(groupId, from));

            System.out.println("\n📥 Solicitação recebida!");
            System.out.println("➡️ " + from + " quer entrar no grupo " + groupId);
            System.out.println("👉 Use a opção [7] do menu para gerenciar.");
        }

        if (payload.contains("\"type\":\"join_accept\"")) {
            System.out.println("Entrada aceita no grupo " + extract(payload, "group"));
        }
    }

    private void handleGroupUpdate(MqttMessage msg) {
        String p = new String(msg.getPayload());
        String id = extract(p, "id");
        String name = extract(p, "name");
        String owner = extract(p, "owner");
        groups.putIfAbsent(id, new Group(id, name, owner));
    }

    private String extract(String json, String key) {
        try { return json.split("\"" + key + "\":\"")[1].split("\"")[0]; }
        catch (Exception e) { return ""; }
    }
}
