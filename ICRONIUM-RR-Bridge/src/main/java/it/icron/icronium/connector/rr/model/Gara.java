package it.icron.icronium.connector.rr.model;

public class Gara {

    private String id;
    private String eventName;
    private String eventDate;

    public Gara(String id, String eventName, String eventDate) {
        this.id = id;
        this.eventName = eventName;
        this.eventDate = eventDate;
    }

    public String getId() {
        return id;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventDate() {
        return eventDate;
    }
}
