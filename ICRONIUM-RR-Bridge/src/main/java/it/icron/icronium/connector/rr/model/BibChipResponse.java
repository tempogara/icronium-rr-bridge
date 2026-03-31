package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class BibChipResponse {

    private String eventId;
    private int participantsCount;
    private List<BibChipRow> rows = new ArrayList<>();

    public BibChipResponse(String eventId, int participantsCount, List<BibChipRow> rows) {
        this.eventId = eventId;
        this.participantsCount = participantsCount;
        this.rows = rows;
    }

    public String getEventId() {
        return eventId;
    }

    public int getParticipantsCount() {
        return participantsCount;
    }

    public List<BibChipRow> getRows() {
        return rows;
    }
}
