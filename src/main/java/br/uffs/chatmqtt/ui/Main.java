package br.uffs.chatmqtt.ui;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.services.ChatService;
import br.uffs.chatmqtt.services.GroupService;
import br.uffs.chatmqtt.services.UserService;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            Scanner sc = new Scanner(System.in);

            System.out.print("Digite seu ID de usuário: ");
            String user = sc.nextLine().trim().toLowerCase();

            MqttService mqtt = new MqttService();
            mqtt.connect(user);

            ConsoleUI console = new ConsoleUI();

            UserService userService = new UserService(user, mqtt);
            GroupService groupService = new GroupService(user, mqtt);
            ChatService chatService = new ChatService(user, mqtt, console);

            console.setServices(userService, groupService, mqtt, chatService);

            userService.goOnline();
            console.start();

        } catch (Exception e) {
            System.out.println("Erro fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
