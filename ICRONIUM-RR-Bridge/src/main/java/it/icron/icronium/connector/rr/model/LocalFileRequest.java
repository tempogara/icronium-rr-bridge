package it.icron.icronium.connector.rr.model;

public class LocalFileRequest {

    private String localPath;
    private String timingPoint;

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getTimingPoint() {
        return timingPoint;
    }

    public void setTimingPoint(String timingPoint) {
        this.timingPoint = timingPoint;
    }
}
