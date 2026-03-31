package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class GaraSyncResponse {

    private String eventId;
    private String rrIdUrl;
    private int participantsCount;
    private List<String> timingPoints = new ArrayList<>();

    public GaraSyncResponse(String eventId, String rrIdUrl, int participantsCount, List<String> timingPoints) {
        this.eventId = eventId;
        this.rrIdUrl = rrIdUrl;
        this.participantsCount = participantsCount;
        this.timingPoints = timingPoints;
    }

    public String getEventId() {
        return eventId;
    }

    public String getRrIdUrl() {
        return rrIdUrl;
    }

    public int getParticipantsCount() {
        return participantsCount;
    }

    public List<String> getTimingPoints() {
        return timingPoints;
    }
}
