package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class TZeroTimingPointConfigResponse {

    private String eventId;
    private List<TZeroTimingPoint> timingPoints = new ArrayList<>();

    public TZeroTimingPointConfigResponse() {
    }

    public TZeroTimingPointConfigResponse(String eventId, List<TZeroTimingPoint> timingPoints) {
        this.eventId = eventId;
        this.timingPoints = timingPoints == null ? new ArrayList<>() : timingPoints;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public List<TZeroTimingPoint> getTimingPoints() {
        return timingPoints;
    }

    public void setTimingPoints(List<TZeroTimingPoint> timingPoints) {
        this.timingPoints = timingPoints == null ? new ArrayList<>() : timingPoints;
    }
}
