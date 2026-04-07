package it.icron.icronium.connector.rr.model;

public class UpdateTimingPointRequest {

    private String source;
    private String timingPoint;
    private Integer scaricaOgniSec;
    private String syncOffset;
    private String filterFromTime;
    private String filterToTime;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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
