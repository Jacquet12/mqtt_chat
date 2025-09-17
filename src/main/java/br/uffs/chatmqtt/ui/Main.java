package br.uffs.chatmqtt.ui;

import br.uffs.chatmqtt.mqtt.MqttService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite seu ID de usuário: ");
        String userId = scanner.nextLine();

        MqttService mqtt = new MqttService();
        mqtt.connect(userId);

        // Marca como online
        mqtt.publish("USERS", userId + ":ONLINE");

        System.out.println("Você está online! Pressione ENTER para sair...");
        scanner.nextLine();

        // Ao sair
        mqtt.publish("USERS", userId + ":OFFLINE");
        mqtt.disconnect();
    }
}