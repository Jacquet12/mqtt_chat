package br.uffs.chatmqtt.ui;

import br.uffs.chatmqtt.mqtt.MqttService;
import br.uffs.chatmqtt.services.ChatService;
import br.uffs.chatmqtt.services.GroupService;
import br.uffs.chatmqtt.services.UserService;

import java.util.Scanner;

public class ConsoleUI {

    private UserService userService;
    private GroupService groupService;
    private MqttService mqtt;
    private ChatService chatService;

    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;
    private boolean chatAtivo = false;

    public ConsoleUI() {}

    public void setServices(UserService u, GroupService g, MqttService m, ChatService c) {
        this.userService = u;
        this.groupService = g;
        this.mqtt = m;
        this.chatService = c;
    }

    public void ativarChat() { chatAtivo = true; }
    public void desativarChat() { chatAtivo = false; }

    public void start() {
        while (running) {

            if (chatAtivo) {
                try { Thread.sleep(150); } catch (Exception ignored) {}
                continue;
            }

            System.out.println("\n===== MENU CHAT MQTT =====");
            System.out.println("1  Listar usuários");
            System.out.println("2  Ir offline");
            System.out.println("3  Ir online");
            System.out.println("4  Criar grupo");
            System.out.println("5  Listar grupos");
            System.out.println("6  Solicitar entrada em grupo");
            System.out.println("7  Gerenciar solicitações de grupo");
            System.out.println("8  Solicitar conversa privada");
            System.out.println("9  Gerenciar solicitações privadas");
            System.out.println("10 Listar conversas privadas");
            System.out.println("11 Enviar mensagem para grupo");
            System.out.println("12 Enviar mensagem privada");
            System.out.println("0  Sair");
            System.out.print("Escolha: ");

            String option = scanner.nextLine().trim();

            if (!option.matches("\\d+")) {
                System.out.println("Opção inválida!");
                continue;
            }

            try {
                switch (option) {
                    case "1" -> userService.listUsers();
                    case "2" -> userService.goOffline();
                    case "3" -> userService.goOnline();
                    case "4" -> createGroup();
                    case "5" -> groupService.listGroups();
                    case "6" -> joinGroup();
                    case "7" -> groupService.processRequests();
                    case "8" -> privateRequest();
                    case "9" -> chatService.processRequests();
                    case "10" -> chatService.listSessions();
                    case "11" -> groupChat();
                    case "12" -> chatService.openPrivateChat(this);
                    case "0" -> {
                        running = false;
                        userService.goOffline();
                        mqtt.disconnect();
                        System.out.println("Encerrando...");
                    }
                    default -> System.out.println("Opção inválida");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private void createGroup() throws Exception {
        System.out.print("ID do grupo: ");
        String id = scanner.nextLine();
        System.out.print("Nome do grupo: ");
        String nm = scanner.nextLine();
        groupService.createGroup(id, nm);
    }

    private void joinGroup() throws Exception {
        System.out.print("ID do grupo: ");
        String idg = scanner.nextLine();
        groupService.requestJoin(idg);
    }

    private void privateRequest() throws Exception {
        System.out.print("ID do usuário destino: ");
        String tgt = scanner.nextLine();
        chatService.requestChat(tgt);
    }

    private void groupChat() throws Exception {
        System.out.print("ID do grupo: ");
        String gid = scanner.nextLine();
        groupService.openGroupChat(gid, this);
    }
}
