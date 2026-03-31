package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class GaraDettaglioSnapshot {

    private String eventId;
    private int bibChipCount;
    private int timingPointCount;
    private int scartiCount;
    private String rrHost;
    private String rrPw;
    private List<BibChipRow> bibChipRows = new ArrayList<>();
    private List<GaraDettaglioRow> rows = new ArrayList<>();

    public GaraDettaglioSnapshot() {
    }

    public GaraDettaglioSnapshot(String eventId, int bibChipCount, int timingPointCount, int scartiCount, List<GaraDettaglioRow> rows) {
        this.eventId = eventId;
        this.bibChipCount = bibChipCount;
        this.timingPointCount = timingPointCount;
        this.scartiCount = scartiCount;
        this.rows = rows;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getBibChipCount() {
        return bibChipCount;
    }

    public void setBibChipCount(int bibChipCount) {
        this.bibChipCount = bibChipCount;
    }

    public int getTimingPointCount() {
        return timingPointCount;
    }

    public void setTimingPointCount(int timingPointCount) {
        this.timingPointCount = timingPointCount;
    }

    public int getScartiCount() {
        return scartiCount;
    }

    public void setScartiCount(int scartiCount) {
        this.scartiCount = scartiCount;
    }

    public String getRrHost() {
        return rrHost;
    }

    public void setRrHost(String rrHost) {
        this.rrHost = rrHost;
    }

    public String getRrPw() {
        return rrPw;
    }

    public void setRrPw(String rrPw) {
        this.rrPw = rrPw;
    }

    public List<BibChipRow> getBibChipRows() {
        return bibChipRows;
    }

    public void setBibChipRows(List<BibChipRow> bibChipRows) {
        this.bibChipRows = bibChipRows;
    }

    public List<GaraDettaglioRow> getRows() {
        return rows;
    }

    public void setRows(List<GaraDettaglioRow> rows) {
        this.rows = rows;
    }
}
