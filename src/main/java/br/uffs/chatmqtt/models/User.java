package br.uffs.chatmqtt.models;

public class User {
    private final String id;
    private final boolean online;

    public User(String id, boolean online) {
        this.id = id;
        this.online = online;
    }

    public String getId() {
        return id;
    }

    public boolean isOnline() {
        return online;
    }
}