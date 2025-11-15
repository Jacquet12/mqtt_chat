package br.uffs.chatmqtt.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import java.util.ArrayList;
import java.util.List;

public class MqttService {

    private MqttClient client;
    private final List<IMqttMessageListener> controlListeners = new ArrayList<>();

    public void connect(String clientId) throws MqttException {
        client = new MqttClient("tcp://localhost:1883", clientId.toLowerCase());
        MqttConnectOptions opt = new MqttConnectOptions();
        opt.setAutomaticReconnect(true);
        opt.setCleanSession(false);
        opt.setConnectionTimeout(10);
        client.connect(opt);

        String controlTopic = clientId.toLowerCase() + "_control";

        client.subscribe(controlTopic, (topic, msg) -> {
            for (IMqttMessageListener l : controlListeners) {
                l.messageArrived(topic, msg);
            }
        });
    }

    public void addControlListener(IMqttMessageListener listener) {
        controlListeners.add(listener);
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        client.subscribe(topic.toLowerCase(), listener);
    }

    public void publish(String topic, String message, boolean retained) throws MqttException {
        MqttMessage msg = new MqttMessage(message.getBytes());
        msg.setQos(1);
        msg.setRetained(retained);
        client.publish(topic.toLowerCase(), msg);
    }

    public void publishControlMessage(String userId, String payload) throws MqttException {
        publish(userId.toLowerCase() + "_control", payload, false);
    }

    public void publishUserStatus(String userId, boolean online) throws MqttException {
        String payload = "{\"id\":\"" + userId + "\",\"status\":\"" + (online ? "ONLINE" : "OFFLINE") + "\"}";
        publish("users", payload, true);
    }

    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) client.disconnect();
    }
}
