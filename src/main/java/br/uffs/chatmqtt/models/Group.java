package br.uffs.chatmqtt.models;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private final String id;
    private String name;
    private final String ownerId;
    private List<String> members;

    public Group(String id, String name, String ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.members = new ArrayList<>();
        this.members.add(ownerId);
    }

    public String getId() {
        return id;
    }

    public List<String> getMembers() {
        return members;
    }

    public String getName() {
        return name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void addMember(String memberId) {
        if (!this.members.contains(memberId)) {
            this.members.add(memberId);
        }
    }

    public void removeMember(String memberId) {
        this.members.remove(memberId);
    }
}
