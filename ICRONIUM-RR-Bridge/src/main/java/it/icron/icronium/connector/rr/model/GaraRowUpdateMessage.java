package it.icron.icronium.connector.rr.model;

public class GaraRowUpdateMessage {

    private String eventId;
    private GaraDettaglioRow row;

    public GaraRowUpdateMessage() {
    }

    public GaraRowUpdateMessage(String eventId, GaraDettaglioRow row) {
        this.eventId = eventId;
        this.row = row;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public GaraDettaglioRow getRow() {
        return row;
    }

    public void setRow(GaraDettaglioRow row) {
        this.row = row;
    }
}
