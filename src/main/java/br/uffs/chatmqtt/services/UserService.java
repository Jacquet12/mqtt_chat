package br.uffs.chatmqtt.services;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.models.User;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    private final MqttService mqttService;
    private final Map<String, User> users = new HashMap<>();
    private final String currentUserId;

    public UserService(String userId, MqttService mqttService) {
        this.currentUserId = userId;
        this.mqttService = mqttService;
    }

    public void goOnline() throws MqttException {
        mqttService.publish("USERS", currentUserId + ":ONLINE");
        users.put(currentUserId, new User(currentUserId, true));
        System.out.println("[" + currentUserId + "] agora está ONLINE");
    }

    public void goOffline() throws MqttException {
        mqttService.publish("USERS", currentUserId + ":OFFLINE");
        users.put(currentUserId, new User(currentUserId, false));
        System.out.println("[" + currentUserId + "] agora está OFFLINE");
    }

    public void updateUserStatus(String userId, boolean online) {
        users.put(userId, new User(userId, online));
    }

    public void listUsers() {
        System.out.println("==== Usuários ====");
        users.forEach((id, user) ->
                System.out.println(id + " -> " + (user.isOnline() ? "ONLINE" : "OFFLINE")));
    }
}
