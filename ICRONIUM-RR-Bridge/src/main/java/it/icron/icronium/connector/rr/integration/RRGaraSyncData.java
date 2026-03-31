package it.icron.icronium.connector.rr.integration;

import it.icron.icronium.connector.rr.model.BibChipRow;

import java.util.ArrayList;
import java.util.List;

public class RRGaraSyncData {

    private int participantsCount;
    private List<String> timingPoints = new ArrayList<>();
    private List<BibChipRow> bibChipRows = new ArrayList<>();

    public RRGaraSyncData(int participantsCount, List<String> timingPoints, List<BibChipRow> bibChipRows) {
        this.participantsCount = participantsCount;
        this.timingPoints = timingPoints;
        this.bibChipRows = bibChipRows;
    }

    public int getParticipantsCount() {
        return participantsCount;
    }

    public List<String> getTimingPoints() {
        return timingPoints;
    }

    public List<BibChipRow> getBibChipRows() {
        return bibChipRows;
    }
}
