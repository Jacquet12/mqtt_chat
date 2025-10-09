package br.uffs.chatmqtt.ui;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.services.UserService;
import br.uffs.chatmqtt.services.GroupService;
import br.uffs.chatmqtt.services.MessageService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite seu ID de usuário: ");
        String userId = scanner.nextLine().trim();

        MqttService mqtt = new MqttService();
        mqtt.connect(userId);

        UserService userService = new UserService(userId, mqtt);
        GroupService groupService = new GroupService(userId, mqtt);
        MessageService messageService = new MessageService(userId, mqtt);

        userService.goOnline();

        ConsoleUI console = new ConsoleUI(userService, groupService, messageService, mqtt);
        console.start();
    }
}