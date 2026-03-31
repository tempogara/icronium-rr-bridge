package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class RrLiveState {
    private String eventId;
    private String targetUrl;
    private int pollSec = 5;
    private boolean running;
    private String rrPw;
    private String rrMode;
    private String lastSync = "";
    private List<RrLiveEndpoint> apis = new ArrayList<>();
    private List<RrLiveRow> rows = new ArrayList<>();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public int getPollSec() {
        return pollSec;
    }

    public void setPollSec(int pollSec) {
        this.pollSec = pollSec;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getRrPw() {
        return rrPw;
    }

    public void setRrPw(String rrPw) {
        this.rrPw = rrPw;
    }

    public String getRrMode() {
        return rrMode;
    }

    public void setRrMode(String rrMode) {
        this.rrMode = rrMode;
    }

    public String getLastSync() {
        return lastSync;
    }

    public void setLastSync(String lastSync) {
        this.lastSync = lastSync;
    }

    public List<RrLiveEndpoint> getApis() {
        return apis;
    }

    public void setApis(List<RrLiveEndpoint> apis) {
        this.apis = apis;
    }

    public List<RrLiveRow> getRows() {
        return rows;
    }

    public void setRows(List<RrLiveRow> rows) {
        this.rows = rows;
    }
}
