package it.icron.icronium.connector.rr.integration;

import it.icron.icronium.connector.rr.model.GaraDettaglioRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostUploader {
    private static final Logger log = LoggerFactory.getLogger(PostUploader.class);

    private static final int MAX_BATCH = 300;
    private static final int MAX_DISCARDED = 1000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static PostBatchResult postDeltaBatch(Path file, GaraDettaglioRow row, ConnectorContext ctx) throws Exception {
        Map<String, Integer> chipBibMap = ctx.getChipBibMap();
        if (chipBibMap == null || chipBibMap.isEmpty()) {
            log.warn("POST delta skipped: chipBibMap vuota per source={}", row.getSource());
            return new PostBatchResult(row.getUploadedLines(), new ArrayList<>());
        }

        String deviceId = extractDeviceId(file.getFileName().toString());
        String timingPoint = row.getTimingPoint();
        double syncOffsetSeconds = parseSyncOffsetSeconds(row.getSyncOffset());
        List<String> batch = new ArrayList<>();
        List<String> batchSourceLines = new ArrayList<>();
        List<String> sentLines = new ArrayList<>();

        long fileLineIndex = 0;
        long uploaded = row.getUploadedLines();
        long processed = row.getProcessedLines();
        log.info("POST delta start: file={} processedOffset={} uploadedTot={} syncOffset={}", file.toAbsolutePath(), processed, uploaded, row.getSyncOffset());

        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (fileLineIndex++ < processed) {
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String chip = RRGaraDettaglioService.extractPassingCode(line);
                if (chip != null && row.getBlacklistedCodes() != null && row.getBlacklistedCodes().contains(chip)) {
                    continue;
                }

                String separator = line.indexOf('\t') >= 0 ? "\t" : ";";
                String[] parts = line.split(separator);
                if (parts.length < 2) {
                    continue;
                }

                chip = parts[0].trim();
                String tsPart = parts[1].trim();
                String timeStr = tsPart.length() > 12 ? tsPart.substring(11) : tsPart;

                Integer bib = chipBibMap.get(chip);
                if (bib == null) {
                    appendDiscardedLine(row, line);
                    continue;
                }

                LocalTime lt = LocalTime.parse(timeStr, TIME_FMT);
                double seconds = lt.toSecondOfDay() + (lt.getNano() / 1_000_000_000.0) + syncOffsetSeconds;
                String json = buildJson(timingPoint, bib, seconds, deviceId);
                batch.add(json);
                batchSourceLines.add(line);

                if (batch.size() == MAX_BATCH) {
                    sendBatch(ctx, batch);
                    log.info("POST batch sent: size={} timingPoint={}", batch.size(), timingPoint);
                    uploaded += batch.size();
                    sentLines.addAll(batchSourceLines);
                    // Checkpoint immediately after each successful batch:
                    // next retry resumes from the first unsent line.
                    row.setUploadedLines(uploaded);
                    row.setProcessedLines(fileLineIndex);
                    batch.clear();
                    batchSourceLines.clear();
                }
            }

            if (!batch.isEmpty()) {
                sendBatch(ctx, batch);
                log.info("POST batch sent: size={} timingPoint={}", batch.size(), timingPoint);
                uploaded += batch.size();
                sentLines.addAll(batchSourceLines);
                row.setUploadedLines(uploaded);
                row.setProcessedLines(fileLineIndex);
            }
        }

        row.setProcessedLines(fileLineIndex);
        row.setUploadedLines(uploaded);
        log.info("POST delta completed: file={} processedOffset={} uploadedTot={}", file.toAbsolutePath(), fileLineIndex, uploaded);
        return new PostBatchResult(uploaded, sentLines);
    }

    private static String extractDeviceId(String fileName) {
        String s = fileName;
        if (s.startsWith("file-")) {
            s = s.substring(5);
        }
        int dot = s.lastIndexOf('.');
        if (dot > 0) {
            s = s.substring(0, dot);
        }
        return s;
    }

    private static String buildJson(String timingPoint, int bib, double seconds, String deviceId) {
        return "{"
                + "\"TimingPoint\":\"" + timingPoint + "\","
                + "\"ResultID\":0,"
                + "\"Bib\":" + bib + ","
                + "\"Time\":" + seconds + ","
                + "\"InfoText\":\"ICRON1\","
                + "\"Passing\":{"
                + "\"DeviceID\":\"" + deviceId + "\","
                + "\"IsMarker\":false"
                + "}"
                + "}";
    }

    private static void sendBatch(ConnectorContext ctx, List<String> batch) throws Exception {
        if (ctx.isSimulateOnly()) {
            log.info("POST simulated: batchSize={}", batch.size());
            return;
        }
        String url = ctx.getBaseUrl()
                + "times/add?lang=en&contestFilter=0&IgnoreBibToBibAssign=1";
        String body = "[" + String.join(",", batch) + "]";
        log.info("POST request: url={} batchSize={}", url, batch.size());
        ctx.getRr().postData(url, body);
    }

    private static void appendDiscardedLine(GaraDettaglioRow row, String line) {
        if (line != null && line.contains("*")) {
            return;
        }
        if (row.getDiscardedLines() == null) {
            row.setDiscardedLines(new ArrayList<>());
        }
        row.getDiscardedLines().add(line);
        while (row.getDiscardedLines().size() > MAX_DISCARDED) {
            row.getDiscardedLines().remove(0);
        }
    }

    private static double parseSyncOffsetSeconds(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String trimmed = value.trim();
        int sign = trimmed.startsWith("-") ? -1 : 1;
        String raw = trimmed.startsWith("+") || trimmed.startsWith("-") ? trimmed.substring(1) : trimmed;
        String[] hms = raw.split(":");
        if (hms.length != 3) {
            return 0;
        }
        String[] secMillis = hms[2].split("\\.");
        if (secMillis.length != 2) {
            return 0;
        }
        int hours = Integer.parseInt(hms[0]);
        int minutes = Integer.parseInt(hms[1]);
        int seconds = Integer.parseInt(secMillis[0]);
        int millis = Integer.parseInt(secMillis[1]);
        double total = (hours * 3600L) + (minutes * 60L) + seconds + (millis / 1000.0);
        return sign * total;
    }
}
