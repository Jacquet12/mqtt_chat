package br.uffs.chatmqtt.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttService {
    private final String broker = "tcp://localhost:1883";
    private MqttClient client;

    public void connect(String clientId) throws MqttException {
        client = new MqttClient(broker, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);   // mantém mensagens para offline
        options.setAutomaticReconnect(true);

        client.connect(options);
        System.out.println("[" + clientId + "] conectado ao broker!");

        // Assina o tópico USERS para receber updates
        client.subscribe("USERS", (topic, msg) -> {
            System.out.println("Mensagem USERS -> " + new String(msg.getPayload()));
        });
    }

    public void publish(String topic, String message) throws MqttException {
        if (client != null && client.isConnected()) {
            client.publish(topic, new MqttMessage(message.getBytes()));
        }
    }

    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            System.out.println("Cliente desconectado do broker.");
        }
    }
}