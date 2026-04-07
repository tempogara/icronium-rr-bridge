package it.icron.icronium.connector.rr.model;

public class ReaderFileEntry {

    private String name;
    private String path;
    private String sourceUrl;
    private String modifiedAt;
    private long sizeBytes;
    private boolean linkedToRace;

    public ReaderFileEntry() {
    }

    public ReaderFileEntry(String name, String path, String modifiedAt, long sizeBytes) {
        this.name = name;
        this.path = path;
        this.modifiedAt = modifiedAt;
        this.sizeBytes = sizeBytes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean isLinkedToRace() {
        return linkedToRace;
    }

    public void setLinkedToRace(boolean linkedToRace) {
        this.linkedToRace = linkedToRace;
    }
}
