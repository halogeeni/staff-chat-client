package com.ateamdevelopers.staffchatclient;

public class Message {

    private final long timestamp;
    private final Integer fromUserId, toUserId, toGroupId;
    private final String body;

    // broadcast message
    public Message(Integer fromUserId, String body, long timestamp) {
        this.fromUserId = fromUserId;
        this.body = body;
        this.timestamp = timestamp;
        this.toUserId = null;
        this.toGroupId = null;
    }

    // user-to-user message
    public Message(Integer fromUserId, Integer toUserId, String body, long timestamp) {
        this.fromUserId = fromUserId;
        this.body = body;
        this.timestamp = timestamp;
        this.toUserId = toUserId;
        this.toGroupId = null;
    }

    // group message
    public Message(Integer fromUserId, String body, Integer toGroupId, long timestamp) {
        this.fromUserId = fromUserId;
        this.body = body;
        this.timestamp = timestamp;
        this.toUserId = null;
        this.toGroupId = toGroupId;
    }

    @Override
    public String toString() {
        return "fromUserId: " + fromUserId + " body: " + body + " timestamp: " + timestamp;
    }

    // getters

    public long getTimestamp() {
        return timestamp;
    }

    public Integer getFromUserId() {
        return fromUserId;
    }

    public Integer getToUserId() {
        return toUserId;
    }

    public Integer getToGroupId() {
        return toGroupId;
    }

    public String getBody() {
        return body;
    }

}
