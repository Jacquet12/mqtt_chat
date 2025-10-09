package br.uffs.chatmqtt.ui;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.services.UserService;
import br.uffs.chatmqtt.services.GroupService;
import br.uffs.chatmqtt.services.MessageService;

import java.util.Scanner;

public class ConsoleUI {
    private final UserService userService;
    private final GroupService groupService;
    private final MessageService messageService;
    private final MqttService mqtt;
    private final Scanner scanner = new Scanner(System.in);

    // 🎨 Cores ANSI
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    public ConsoleUI(UserService userService, GroupService groupService, MessageService messageService, MqttService mqtt) {
        this.userService = userService;
        this.groupService = groupService;
        this.messageService = messageService;
        this.mqtt = mqtt;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println(CYAN + "\n===== MENU CHAT MQTT =====" + RESET);
            System.out.println("1️⃣  Listar usuários");
            System.out.println("2️⃣  Ir offline");
            System.out.println("3️⃣  Ir online");
            System.out.println("4️⃣  Criar grupo");
            System.out.println("5️⃣  Listar grupos");
            System.out.println("6️⃣  Entrar em grupo");
            System.out.println("7️⃣  Gerenciar solicitações pendentes");
            System.out.println("8️⃣  Solicitar conversa privada");
            System.out.println("0️⃣  Sair");
            System.out.print(YELLOW + "👉 Escolha: " + RESET);

            String option = scanner.nextLine();
            try {
                switch (option) {
                    case "1" -> userService.listUsers();
                    case "2" -> {
                        userService.goOffline();
                        System.out.println(RED + "🔴 Você foi marcado como OFFLINE" + RESET);
                    }
                    case "3" -> {
                        userService.goOnline();
                        System.out.println(GREEN + "🟢 Você foi marcado como ONLINE" + RESET);
                    }
                    case "4" -> {
                        System.out.print("Digite o ID do grupo: ");
                        String groupId = scanner.nextLine();
                        System.out.print("Digite o nome do grupo: ");
                        String groupName = scanner.nextLine();
                        groupService.createGroup(groupId, groupName);
                    }
                    case "5" -> groupService.listGroups();
                    case "6" -> {
                        System.out.print("Digite o ID do grupo: ");
                        String groupId = scanner.nextLine();
                        groupService.requestJoin(groupId);
                    }
                    case "7" -> groupService.processRequests();
                    case "8" -> {
                        System.out.print("Digite o ID do usuário destino: ");
                        String target = scanner.nextLine();
                        messageService.requestChat(target);
                    }
                    case "0" -> {
                        running = false;
                        userService.goOffline();
                        mqtt.disconnect();
                        System.out.println(YELLOW + "👋 Encerrando aplicação..." + RESET);
                    }
                    default -> System.out.println(RED + "⚠️  Opção inválida!" + RESET);
                }
            } catch (Exception e) {
                System.out.println(RED + "❌ Erro: " + e.getMessage() + RESET);
            }
        }
    }
}