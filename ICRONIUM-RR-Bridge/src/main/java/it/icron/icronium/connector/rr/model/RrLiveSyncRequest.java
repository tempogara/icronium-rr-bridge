package it.icron.icronium.connector.rr.model;

public class RrLiveSyncRequest {
    private String eventId;
    private String targetUrl;
    private Integer pollSec;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Integer getPollSec() {
        return pollSec;
    }

    public void setPollSec(Integer pollSec) {
        this.pollSec = pollSec;
    }
}
