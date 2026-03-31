package it.icron.icronium.connector.rr.integration;

import java.util.ArrayList;
import java.util.List;

public class PostBatchResult {

    private long uploadedLines;
    private List<String> sentLines = new ArrayList<>();

    public PostBatchResult() {
    }

    public PostBatchResult(long uploadedLines, List<String> sentLines) {
        this.uploadedLines = uploadedLines;
        this.sentLines = sentLines;
    }

    public long getUploadedLines() {
        return uploadedLines;
    }

    public void setUploadedLines(long uploadedLines) {
        this.uploadedLines = uploadedLines;
    }

    public List<String> getSentLines() {
        return sentLines;
    }

    public void setSentLines(List<String> sentLines) {
        this.sentLines = sentLines;
    }
}
