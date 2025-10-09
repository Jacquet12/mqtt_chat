package br.uffs.chatmqtt.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttService {
    private final String broker = "tcp://localhost:1883";
    private MqttClient client;
    private String currentClientId;

    public void connect(String clientId) throws MqttException {
        this.currentClientId = clientId;
        client = new MqttClient(broker, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);

        String willPayload = "{\"id\":\"" + clientId + "\",\"status\":\"OFFLINE\"}";
        options.setWill("USERS", willPayload.getBytes(), 1, true);

        client.connect(options);
        System.out.println("[" + clientId + "] conectado ao broker!");
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        if (client != null && client.isConnected()) {
            client.subscribe(topic, 1, listener);
        } else {
            throw new MqttException(new Throwable("Cliente não conectado ao broker!"));
        }
    }

    public void publish(String topic, String message, boolean retained) throws MqttException {
        if (client != null && client.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttMessage.setRetained(retained);
            client.publish(topic, mqttMessage);
        } else {
            throw new MqttException(new Throwable("Cliente não conectado ao broker!"));
        }
    }

    public void publishUserStatus(String userId, boolean online) throws MqttException {
        String payload = "{\"id\":\"" + userId + "\",\"status\":\"" + (online ? "ONLINE" : "OFFLINE") + "\"}";
        publish("USERS", payload, true);
    }

    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            System.out.println("Cliente desconectado do broker.");
        }
    }
}
