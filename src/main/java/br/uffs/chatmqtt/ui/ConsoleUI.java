package br.uffs.chatmqtt.ui;

import br.uffs.chatmqtt.services.UserService;

import java.util.Scanner;

public class ConsoleUI {
    private final UserService userService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleUI(UserService userService) {
        this.userService = userService;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== MENU CHAT MQTT =====");
            System.out.println("1. Listar usuários");
            System.out.println("2. Ir offline");
            System.out.println("3. Ir online");
            System.out.println("0. Sair");
            System.out.print("Escolha: ");

            String option = scanner.nextLine();

            try {
                switch (option) {
                    case "1" -> userService.listUsers();
                    case "2" -> userService.goOffline();
                    case "3" -> userService.goOnline();
                    case "0" -> {
                        running = false;
                        System.out.println("Saindo...");
                    }
                    default -> System.out.println("Opção inválida!");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }
}