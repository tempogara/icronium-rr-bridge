package it.icron.icronium.connector.rr.model;

import java.util.List;

public class GaraDettaglioResponse {

    private String eventId;
    private String rrIdUrl;
    private int bibChipCount;
    private int timingPointCount;
    private int scartiCount;
    private List<GaraDettaglioRow> rows;

    public GaraDettaglioResponse(String eventId, String rrIdUrl, int bibChipCount, int timingPointCount, int scartiCount, List<GaraDettaglioRow> rows) {
        this.eventId = eventId;
        this.rrIdUrl = rrIdUrl;
        this.bibChipCount = bibChipCount;
        this.timingPointCount = timingPointCount;
        this.scartiCount = scartiCount;
        this.rows = rows;
    }

    public String getEventId() {
        return eventId;
    }

    public String getRrIdUrl() {
        return rrIdUrl;
    }

    public int getBibChipCount() {
        return bibChipCount;
    }

    public int getTimingPointCount() {
        return timingPointCount;
    }

    public int getScartiCount() {
        return scartiCount;
    }

    public List<GaraDettaglioRow> getRows() {
        return rows;
    }
}
