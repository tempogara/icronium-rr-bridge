package it.icron.icronium.connector.rr.model;

public class RemoteFileRequest {

    private String url;
    private String timingPoint;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTimingPoint() {
        return timingPoint;
    }

    public void setTimingPoint(String timingPoint) {
        this.timingPoint = timingPoint;
    }
}
