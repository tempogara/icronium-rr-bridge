package it.icron.icronium.connector.rr.model;

public class RrLiveLogMessage {
    private String eventId;
    private String sender;
    private String content;

    public RrLiveLogMessage() {
    }

    public RrLiveLogMessage(String eventId, String sender, String content) {
        this.eventId = eventId;
        this.sender = sender;
        this.content = content;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
