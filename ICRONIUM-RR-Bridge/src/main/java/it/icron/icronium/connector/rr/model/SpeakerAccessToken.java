package it.icron.icronium.connector.rr.model;

public class SpeakerAccessToken {

    private String token;
    private String eventId;
    private String timingPoint;
    private String mode;
    private long createdAt;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTimingPoint() {
        return timingPoint;
    }

    public void setTimingPoint(String timingPoint) {
        this.timingPoint = timingPoint;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
