package it.icron.icronium.connector.rr.model;

public class RrLiveUpdateMessage {
    private String eventId;
    private RrLiveState state;

    public RrLiveUpdateMessage() {
    }

    public RrLiveUpdateMessage(String eventId, RrLiveState state) {
        this.eventId = eventId;
        this.state = state;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public RrLiveState getState() {
        return state;
    }

    public void setState(RrLiveState state) {
        this.state = state;
    }
}
