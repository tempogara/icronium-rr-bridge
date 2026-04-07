package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class ReaderViewState {

    private String eventId;
    private List<ReaderConfig> readers = new ArrayList<>();

    public ReaderViewState() {
    }

    public ReaderViewState(String eventId, List<ReaderConfig> readers) {
        this.eventId = eventId;
        this.readers = readers;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public List<ReaderConfig> getReaders() {
        return readers;
    }

    public void setReaders(List<ReaderConfig> readers) {
        this.readers = readers;
    }
}
