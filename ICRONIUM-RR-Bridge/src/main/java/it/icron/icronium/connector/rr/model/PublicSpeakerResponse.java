package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class PublicSpeakerResponse {

    private String eventId;
    private String timingPoint;
    private String mode;
    private int sourceCount;
    private int read;
    private int sent;
    private int discarded;
    private int unique;
    private boolean showSent;
    private List<PublicSpeakerEntry> lastPassings = new ArrayList<>();
    private List<PublicSpeakerMinuteBucket> minuteBuckets = new ArrayList<>();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTimingPoint() {
        return timingPoint;
    }

    public void setTimingPoint(String timingPoint) {
        this.timingPoint = timingPoint;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public int getSent() {
        return sent;
    }

    public void setSent(int sent) {
        this.sent = sent;
    }

    public int getDiscarded() {
        return discarded;
    }

    public void setDiscarded(int discarded) {
        this.discarded = discarded;
    }

    public int getUnique() {
        return unique;
    }

    public void setUnique(int unique) {
        this.unique = unique;
    }

    public boolean isShowSent() {
        return showSent;
    }

    public void setShowSent(boolean showSent) {
        this.showSent = showSent;
    }

    public List<PublicSpeakerEntry> getLastPassings() {
        return lastPassings;
    }

    public void setLastPassings(List<PublicSpeakerEntry> lastPassings) {
        this.lastPassings = lastPassings;
    }

    public List<PublicSpeakerMinuteBucket> getMinuteBuckets() {
        return minuteBuckets;
    }

    public void setMinuteBuckets(List<PublicSpeakerMinuteBucket> minuteBuckets) {
        this.minuteBuckets = minuteBuckets;
    }
}
