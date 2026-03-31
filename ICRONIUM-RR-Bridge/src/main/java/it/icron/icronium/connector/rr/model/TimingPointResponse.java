package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class TimingPointResponse {

    private String eventId;
    private int count;
    private List<String> timingPoints = new ArrayList<>();

    public TimingPointResponse(String eventId, int count, List<String> timingPoints) {
        this.eventId = eventId;
        this.count = count;
        this.timingPoints = timingPoints;
    }

    public String getEventId() {
        return eventId;
    }

    public int getCount() {
        return count;
    }

    public List<String> getTimingPoints() {
        return timingPoints;
    }
}
