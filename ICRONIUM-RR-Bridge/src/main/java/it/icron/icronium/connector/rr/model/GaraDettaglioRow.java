package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class GaraDettaglioRow {

    private String rowId;
    private String source;
    private String sourceKind;
    private String timingPoint;
    private String syncOffset = "+00:00:00.000";
    private String stato;
    private int scaricaOgniSec;
    private String ultimoDownload;
    private int progresso;
    private int righeFile;
    private int righePo;
    private List<Integer> downloadDeltas = new ArrayList<>();
    private List<Integer> sentDeltas = new ArrayList<>();
    private List<List<String>> downloadCycleDetails = new ArrayList<>();
    private List<List<String>> sentCycleDetails = new ArrayList<>();
    private List<String> discardedLines = new ArrayList<>();
    private List<String> blacklistedCodes = new ArrayList<>();
    private long processedLines;
    private long uploadedLines;
    private int uniqueCount;
    private boolean errorState;
    private String lastErrorMessage;

    public GaraDettaglioRow() {
    }

    public GaraDettaglioRow(String rowId, String source, String timingPoint, String stato, int scaricaOgniSec, String ultimoDownload, int progresso, int righeFile, int righePo) {
        this.rowId = rowId;
        this.source = source;
        this.timingPoint = timingPoint;
        this.stato = stato;
        this.scaricaOgniSec = scaricaOgniSec;
        this.ultimoDownload = ultimoDownload;
        this.progresso = progresso;
        this.righeFile = righeFile;
        this.righePo = righePo;
    }

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(String sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getTimingPoint() {
        return timingPoint;
    }

    public void setTimingPoint(String timingPoint) {
        this.timingPoint = timingPoint;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public String getSyncOffset() {
        return syncOffset;
    }

    public void setSyncOffset(String syncOffset) {
        this.syncOffset = syncOffset;
    }

    public int getScaricaOgniSec() {
        return scaricaOgniSec;
    }

    public void setScaricaOgniSec(int scaricaOgniSec) {
        this.scaricaOgniSec = scaricaOgniSec;
    }

    public String getUltimoDownload() {
        return ultimoDownload;
    }

    public void setUltimoDownload(String ultimoDownload) {
        this.ultimoDownload = ultimoDownload;
    }

    public int getProgresso() {
        return progresso;
    }

    public void setProgresso(int progresso) {
        this.progresso = progresso;
    }

    public int getRigheFile() {
        return righeFile;
    }

    public void setRigheFile(int righeFile) {
        this.righeFile = righeFile;
    }

    public int getRighePo() {
        return righePo;
    }

    public void setRighePo(int righePo) {
        this.righePo = righePo;
    }

    public List<Integer> getDownloadDeltas() {
        return downloadDeltas;
    }

    public void setDownloadDeltas(List<Integer> downloadDeltas) {
        this.downloadDeltas = downloadDeltas;
    }

    public List<Integer> getSentDeltas() {
        return sentDeltas;
    }

    public void setSentDeltas(List<Integer> sentDeltas) {
        this.sentDeltas = sentDeltas;
    }

    public List<List<String>> getDownloadCycleDetails() {
        return downloadCycleDetails;
    }

    public void setDownloadCycleDetails(List<List<String>> downloadCycleDetails) {
        this.downloadCycleDetails = downloadCycleDetails;
    }

    public List<List<String>> getSentCycleDetails() {
        return sentCycleDetails;
    }

    public void setSentCycleDetails(List<List<String>> sentCycleDetails) {
        this.sentCycleDetails = sentCycleDetails;
    }

    public List<String> getDiscardedLines() {
        return discardedLines;
    }

    public void setDiscardedLines(List<String> discardedLines) {
        this.discardedLines = discardedLines;
    }

    public List<String> getBlacklistedCodes() {
        return blacklistedCodes;
    }

    public void setBlacklistedCodes(List<String> blacklistedCodes) {
        this.blacklistedCodes = blacklistedCodes;
    }

    public long getProcessedLines() {
        return processedLines;
    }

    public void setProcessedLines(long processedLines) {
        this.processedLines = processedLines;
    }

    public long getUploadedLines() {
        return uploadedLines;
    }

    public void setUploadedLines(long uploadedLines) {
        this.uploadedLines = uploadedLines;
    }

    public int getUniqueCount() {
        return uniqueCount;
    }

    public void setUniqueCount(int uniqueCount) {
        this.uniqueCount = uniqueCount;
    }

    public boolean isErrorState() {
        return errorState;
    }

    public void setErrorState(boolean errorState) {
        this.errorState = errorState;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }
}
