package it.icron.icronium.connector.rr.model;

public class ReaderToRaceRequest {

    private String readerId;
    private String filePath;
    private String timingPoint;
    private Integer scaricaOgniSec;
    private String syncOffset;
    private String filterFromTime;
    private String filterToTime;

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTimingPoint() {
        return timingPoint;
    }

    public void setTimingPoint(String timingPoint) {
        this.timingPoint = timingPoint;
    }

    public Integer getScaricaOgniSec() {
        return scaricaOgniSec;
    }

    public void setScaricaOgniSec(Integer scaricaOgniSec) {
        this.scaricaOgniSec = scaricaOgniSec;
    }

    public String getSyncOffset() {
        return syncOffset;
    }

    public void setSyncOffset(String syncOffset) {
        this.syncOffset = syncOffset;
    }

    public String getFilterFromTime() {
        return filterFromTime;
    }

    public void setFilterFromTime(String filterFromTime) {
        this.filterFromTime = filterFromTime;
    }

    public String getFilterToTime() {
        return filterToTime;
    }

    public void setFilterToTime(String filterToTime) {
        this.filterToTime = filterToTime;
    }
}
