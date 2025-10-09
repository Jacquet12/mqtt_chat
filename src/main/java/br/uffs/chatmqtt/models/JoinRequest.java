package br.uffs.chatmqtt.models;

public class JoinRequest {
    private final String groupId;
    private final String userId;

    public JoinRequest(String groupId, String userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getUserId() {
        return userId;
    }
}