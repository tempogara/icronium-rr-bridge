package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class ReaderConfig {

    private String readerId;
    private String name;
    private String location;
    private String tipologia;
    private String lastScanAt;
    private String lastScanMessage;
    private String lastScanStatus;
    private List<ReaderFileEntry> todayFiles = new ArrayList<>();

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTipologia() {
        return tipologia;
    }

    public void setTipologia(String tipologia) {
        this.tipologia = tipologia;
    }

    public String getLastScanAt() {
        return lastScanAt;
    }

    public void setLastScanAt(String lastScanAt) {
        this.lastScanAt = lastScanAt;
    }

    public String getLastScanMessage() {
        return lastScanMessage;
    }

    public void setLastScanMessage(String lastScanMessage) {
        this.lastScanMessage = lastScanMessage;
    }

    public String getLastScanStatus() {
        return lastScanStatus;
    }

    public void setLastScanStatus(String lastScanStatus) {
        this.lastScanStatus = lastScanStatus;
    }

    public List<ReaderFileEntry> getTodayFiles() {
        return todayFiles;
    }

    public void setTodayFiles(List<ReaderFileEntry> todayFiles) {
        this.todayFiles = todayFiles;
    }
}
