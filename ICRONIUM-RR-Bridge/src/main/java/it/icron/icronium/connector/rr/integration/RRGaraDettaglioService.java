package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.BibChipResponse;
import it.icron.icronium.connector.rr.model.BibChipRow;
import it.icron.icronium.connector.rr.model.FilePassaggiRequest;
import it.icron.icronium.connector.rr.model.GaraDettaglioResponse;
import it.icron.icronium.connector.rr.model.GaraDettaglioRow;
import it.icron.icronium.connector.rr.model.GaraDettaglioSnapshot;
import it.icron.icronium.connector.rr.model.GaraRowUpdateMessage;
import it.icron.icronium.connector.rr.model.GaraSyncResponse;
import it.icron.icronium.connector.rr.model.ChatMessage;
import it.icron.icronium.connector.rr.model.LocalFileRequest;
import it.icron.icronium.connector.rr.model.RemoteFileRequest;
import it.icron.icronium.connector.rr.model.SimulatedFileRequest;
import it.icron.icronium.connector.rr.model.TimingPointResponse;
import it.icron.icronium.connector.rr.model.UpdateTimingPointRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.net.URI;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RRGaraDettaglioService {
    private static final Logger log = LoggerFactory.getLogger(RRGaraDettaglioService.class);

    private final RRSessionService sessionService;
    private final TZeroConfigService tZeroConfigService;
    private final ActiveEventService activeEventService;
    private final ObjectMapper objectMapper;
    private final GaraDettaglioRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Boolean> rowExecutionLocks = new ConcurrentHashMap<>();
    private static final DateTimeFormatter DOWNLOAD_TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SIMULATED_TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter SIMULATED_INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");
    private static final int MAX_DELTA_HISTORY = 10;
    private static final int MAX_CONTENT_PREVIEW_CHARS = 200_000;
    private static final Pattern SYNC_OFFSET_PATTERN = Pattern.compile("^[+-]\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    public RRGaraDettaglioService(RRSessionService sessionService, TZeroConfigService tZeroConfigService, ActiveEventService activeEventService, ObjectMapper objectMapper, GaraDettaglioRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.sessionService = sessionService;
        this.tZeroConfigService = tZeroConfigService;
        this.activeEventService = activeEventService;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    public GaraDettaglioResponse loadDettaglio(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);

        String rrIdUrl = buildEventLandingUrl(eventId);
        Optional<GaraDettaglioSnapshot> maybeSnapshot = repository.findByEventId(eventId);
        if (maybeSnapshot.isPresent()) {
            GaraDettaglioSnapshot snapshot = maybeSnapshot.get();
            enrichSnapshotConnectionFromSession(snapshot);
            repository.save(snapshot);
            return new GaraDettaglioResponse(
                    eventId,
                    rrIdUrl,
                    snapshot.getBibChipCount(),
                    snapshot.getTimingPointCount(),
                    snapshot.getScartiCount(),
                    snapshot.getRows()
            );
        }

        if (sessionService.isTZeroMode()) {
            GaraDettaglioSnapshot snapshot = new GaraDettaglioSnapshot(
                    eventId,
                    0,
                    0,
                    0,
                    new ArrayList<>()
            );
            enrichSnapshotConnectionFromSession(snapshot);
            repository.save(snapshot);
            return new GaraDettaglioResponse(eventId, rrIdUrl, 0, 0, 0, new ArrayList<>());
        }

        String baseApi = buildEventApiUrl(eventId);

        String timingPointsPayload = sessionService.getClient().postData(baseApi + "multirequest", "[\"TimingPoints\"]");
        List<String> timingPoints = parseTimingPoints(timingPointsPayload);

        String participantsCsv = sessionService.getClient().getData(baseApi
                + "data/list?lang=en&fields=%5B%22BIB%22%2C%22CHIP%22%2C%22LASTNAME%22%2C%22FIRSTNAME%22%5D&sort=BIB&listformat=CSV");
        int bibChipCount = countCsvRows(participantsCsv);

        List<GaraDettaglioRow> rows = new ArrayList<>();

        GaraDettaglioSnapshot snapshot = new GaraDettaglioSnapshot(
                eventId,
                bibChipCount,
                timingPoints.size(),
                0,
                rows
        );
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);

        return new GaraDettaglioResponse(
                eventId,
                rrIdUrl,
                bibChipCount,
                timingPoints.size(),
                0,
                rows
        );
    }

    public GaraSyncResponse sync(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);

        if (sessionService.isTZeroMode()) {
            return syncTZero(eventId);
        }

        String baseApi = buildEventApiUrl(eventId);
        String participantsCsv = sessionService.getClient().getData(baseApi
                + "data/list?lang=en&fields=%5B%22BIB%22%2C%22CHIP%22%2C%22LASTNAME%22%2C%22FIRSTNAME%22%5D&sort=BIB&listformat=CSV");

        String tpJson = sessionService.getClient().postData(baseApi + "multirequest", "[\"TimingPoints\"]");
        List<String> timingPoints = parseTimingPoints(tpJson);
        List<BibChipRow> bibChipRows = parseBibChipRows(participantsCsv);

        int participantsCount = bibChipRows.size();
        sessionService.saveSyncData(eventId, new RRGaraSyncData(participantsCount, timingPoints, bibChipRows));

        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));
        snapshot.setBibChipCount(participantsCount);
        snapshot.setTimingPointCount(timingPoints.size());
        snapshot.setBibChipRows(new ArrayList<>(bibChipRows));
        ensureRowIds(snapshot);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);

        String rrIdUrl = buildEventLandingUrl(eventId);
        return new GaraSyncResponse(eventId, rrIdUrl, participantsCount, timingPoints);
    }

    public TimingPointResponse getTimingPoints(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        RRGaraSyncData syncData = sessionService.getSyncData(eventId);
        if (syncData != null) {
            List<String> timingPoints = syncData.getTimingPoints();
            return new TimingPointResponse(eventId, timingPoints.size(), timingPoints);
        }
        Optional<GaraDettaglioSnapshot> snapshot = repository.findByEventId(eventId);
        if (snapshot.isPresent()) {
            List<String> timingPoints = snapshot.get().getRows().stream()
                    .map(GaraDettaglioRow::getTimingPoint)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList();
            return new TimingPointResponse(eventId, timingPoints.size(), timingPoints);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Nessun dato in sessione: eseguire prima SYNC");
    }

    public BibChipResponse getBibChip(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        RRGaraSyncData syncData = sessionService.getSyncData(eventId);
        if (syncData != null) {
            return new BibChipResponse(eventId, syncData.getParticipantsCount(), syncData.getBibChipRows());
        }
        Optional<GaraDettaglioSnapshot> snapshot = repository.findByEventId(eventId);
        if (snapshot.isPresent()) {
            List<BibChipRow> rows = snapshot.get().getBibChipRows() == null ? new ArrayList<>() : snapshot.get().getBibChipRows();
            return new BibChipResponse(eventId, rows.size(), rows);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Nessun dato in sessione: eseguire prima SYNC");
    }

    public List<GaraDettaglioRow> getPersistedRows(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        Optional<GaraDettaglioSnapshot> snapshot = repository.findByEventId(eventId);
        if (snapshot.isEmpty()) {
            return new ArrayList<>();
        }
        GaraDettaglioSnapshot snap = snapshot.get();
        ensureRowIds(snap);
        enrichSnapshotConnectionFromSession(snap);
        repository.save(snap);
        return snap.getRows();
    }

    public String getRowFileContent(String eventId, String rowId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        GaraDettaglioRow row = snapshot.getRows().stream()
                .filter(r -> rowId.equals(r.getRowId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata"));

        String source = row.getSource() == null ? "" : row.getSource().trim();
        if (source.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source vuoto");
        }

        Path path = resolvePreviewPath(source);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File non trovato: " + path);
        }

        StringBuilder sb = new StringBuilder();
        int totalChars = 0;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int needed = line.length() + 1;
                if (totalChars + needed > MAX_CONTENT_PREVIEW_CHARS) {
                    int remaining = MAX_CONTENT_PREVIEW_CHARS - totalChars;
                    if (remaining > 0) {
                        sb.append(line, 0, Math.min(remaining, line.length()));
                    }
                    sb.append(System.lineSeparator()).append("... [truncated]");
                    break;
                }
                sb.append(line).append(System.lineSeparator());
                totalChars += needed;
            }
        }
        return sb.toString();
    }

    public List<GaraDettaglioRow> addRemoteFile(String eventId, RemoteFileRequest request) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);

        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL file remoto obbligatorio");
        }
        if (!sessionService.isTZeroMode() && (request.getTimingPoint() == null || request.getTimingPoint().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timing point obbligatorio");
        }

        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));

        List<GaraDettaglioRow> rows = snapshot.getRows();
        rows.add(new GaraDettaglioRow(
                UUID.randomUUID().toString(),
                request.getUrl().trim(),
                request.getTimingPoint() == null ? "" : request.getTimingPoint().trim(),
                "Stopped",
                60,
                "",
                0,
                0,
                0
        ));
        rows.get(rows.size() - 1).setSourceKind("web");
        rows.get(rows.size() - 1).setSyncOffset("+00:00:00.000");

        snapshot.setRows(rows);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return rows;
    }

    public List<GaraDettaglioRow> addLocalFile(String eventId, LocalFileRequest request) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);

        if (request == null || request.getLocalPath() == null || request.getLocalPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path file locale obbligatorio");
        }
        if (!sessionService.isTZeroMode() && (request.getTimingPoint() == null || request.getTimingPoint().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timing point obbligatorio");
        }

        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));

        List<GaraDettaglioRow> rows = snapshot.getRows();
        rows.add(new GaraDettaglioRow(
                UUID.randomUUID().toString(),
                request.getLocalPath().trim(),
                request.getTimingPoint() == null ? "" : request.getTimingPoint().trim(),
                "Stopped",
                60,
                "",
                0,
                0,
                0
        ));
        rows.get(rows.size() - 1).setSourceKind("local");
        rows.get(rows.size() - 1).setSyncOffset("+00:00:00.000");

        snapshot.setRows(rows);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return rows;
    }

    public List<GaraDettaglioRow> addFilePassaggi(String eventId, FilePassaggiRequest request) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        if (request == null || request.getSource() == null || request.getSource().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source obbligatorio (URL o path locale)");
        }
        if (!sessionService.isTZeroMode() && (request.getTimingPoint() == null || request.getTimingPoint().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timing point obbligatorio");
        }

        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));

        List<GaraDettaglioRow> rows = snapshot.getRows();
        rows.add(new GaraDettaglioRow(
                UUID.randomUUID().toString(),
                request.getSource().trim(),
                request.getTimingPoint() == null ? "" : request.getTimingPoint().trim(),
                "Stopped",
                sanitizeScaricaOgniSec(request.getScaricaOgniSec()),
                "",
                0,
                0,
                0
        ));
        rows.get(rows.size() - 1).setSourceKind(resolveSourceKind(request.getSource().trim(), null));
        rows.get(rows.size() - 1).setSyncOffset(sanitizeSyncOffset(request.getSyncOffset()));

        snapshot.setRows(rows);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return rows;
    }

    public List<GaraDettaglioRow> addSimulatedFile(String eventId, SimulatedFileRequest request) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dati file simulato obbligatori");
        }

        String timingPoint = request.getTimingPoint() == null ? "" : request.getTimingPoint().trim();
        if (!sessionService.isTZeroMode() && timingPoint.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timing point obbligatorio");
        }
        LocalDateTime from = parseSimulatedTimestamp(request.getFromTimestamp(), "Timestamp iniziale obbligatorio");
        LocalDateTime to = parseSimulatedTimestamp(request.getToTimestamp(), "Timestamp finale obbligatorio");
        if (!to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La finestra oraria deve avere fine successiva all'inizio");
        }
        int intervalSeconds = sanitizeScaricaOgniSec(request.getIntervalSeconds());

        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));
        ensureRowIds(snapshot);

        List<BibChipRow> bibChipRows = snapshot.getBibChipRows() == null ? new ArrayList<>() : snapshot.getBibChipRows();
        if (bibChipRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nessun partecipante disponibile: eseguire prima SYNC");
        }

        List<String> chips = bibChipRows.stream()
                .map(BibChipRow::getChip)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        if (chips.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nessun chip disponibile per generare il file simulato");
        }

        String normalizedFileName = normalizeSimulatedFileName(request.getFileName());
        Path simulatedDir;
        if (sessionService.isTZeroMode()) {
            String rootFolder = sessionService.getTZeroRootFolder();
            Path eventFolder = tZeroConfigService.getEventFolder(rootFolder, eventId);
            simulatedDir = eventFolder.resolve("download");
        } else {
            simulatedDir = it.icron.icronium.connector.rr.IcroniumApplication.WORK_DIR
                    .resolve("simulated")
                    .resolve(eventId);
        }
        Files.createDirectories(simulatedDir);
        Path simulatedFile = simulatedDir.resolve(normalizedFileName).normalize();

        List<String> lines = generateSimulatedPassings(chips, from, to, intervalSeconds, eventId + ":" + normalizedFileName);
        Files.write(simulatedFile, lines);

        GaraDettaglioRow row = new GaraDettaglioRow(
                UUID.randomUUID().toString(),
                simulatedFile.toAbsolutePath().toString(),
                timingPoint,
                "Stopped",
                intervalSeconds,
                "",
                0,
                0,
                0
        );
        row.setSourceKind("simulated");
        row.setSyncOffset("+00:00:00.000");

        snapshot.getRows().add(row);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> deleteRow(String eventId, String rowId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);
        GaraDettaglioRow row = snapshot.getRows().stream()
                .filter(r -> rowId.equals(r.getRowId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata"));
        if (!isStopped(row)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancellazione consentita solo in stato Stopped");
        }
        deleteSimulatedFileIfNeeded(row);
        snapshot.getRows().remove(row);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> deleteAllRows(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));
        boolean hasRunning = snapshot.getRows().stream().anyMatch(this::isRunning);
        if (hasRunning) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancellazione totale consentita solo con righe Stopped");
        }
        for (GaraDettaglioRow row : snapshot.getRows()) {
            deleteSimulatedFileIfNeeded(row);
        }
        snapshot.setRows(new ArrayList<>());
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> applyActionToAll(String eventId, String action) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));
        ensureRowIds(snapshot);
        enrichSnapshotConnectionFromSession(snapshot);
        validateGlobalAction(snapshot.getRows(), action);
        for (GaraDettaglioRow row : snapshot.getRows()) {
            applyAction(row, action, false);
        }
        repository.save(snapshot);
        if ("start".equalsIgnoreCase(action)) {
            for (GaraDettaglioRow row : snapshot.getRows()) {
                triggerDownloadNow(snapshot, row);
            }
            repository.save(snapshot);
        }
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> applyActionToRow(String eventId, String rowId, String action) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);
        enrichSnapshotConnectionFromSession(snapshot);
        GaraDettaglioRow actionRow = null;
        for (GaraDettaglioRow row : snapshot.getRows()) {
            if (rowId.equals(row.getRowId())) {
                applyAction(row, action, true);
                actionRow = row;
                break;
            }
        }
        repository.save(snapshot);
        if ("start".equalsIgnoreCase(action) && actionRow != null) {
            triggerDownloadNow(snapshot, actionRow);
            repository.save(snapshot);
        }
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> updateRowTimingPoint(String eventId, String rowId, UpdateTimingPointRequest request) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request obbligatoria");
        }
        if (!sessionService.isTZeroMode() && (request.getTimingPoint() == null || request.getTimingPoint().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timing point obbligatorio");
        }
        if (request.getSource() == null || request.getSource().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source obbligatorio");
        }

        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);
        boolean found = false;
        for (GaraDettaglioRow row : snapshot.getRows()) {
            if (rowId.equals(row.getRowId())) {
                if (!isStopped(row)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Modifica timing point consentita solo in stato Stopped");
                }
                row.setSource(request.getSource().trim());
                row.setTimingPoint(request.getTimingPoint() == null ? "" : request.getTimingPoint().trim());
                row.setScaricaOgniSec(sanitizeScaricaOgniSec(request.getScaricaOgniSec()));
                row.setSyncOffset(sanitizeSyncOffset(request.getSyncOffset()));
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata");
        }
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> duplicateRow(String eventId, String rowId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);
        GaraDettaglioRow sourceRow = snapshot.getRows().stream()
                .filter(r -> rowId.equals(r.getRowId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata"));
        if (!isStopped(sourceRow)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplica consentito solo in stato Stopped");
        }

        GaraDettaglioRow copy = duplicateStoppedRow(sourceRow);
        snapshot.getRows().add(copy);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> moveRow(String eventId, String rowId, String direction) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);

        List<GaraDettaglioRow> rows = snapshot.getRows();
        int index = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rowId.equals(rows.get(i).getRowId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata");
        }

        GaraDettaglioRow row = rows.get(index);
        if (!isStopped(row)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Riordino consentito solo in stato Stopped");
        }

        int targetIndex;
        if ("up".equalsIgnoreCase(direction)) {
            targetIndex = index - 1;
        } else if ("down".equalsIgnoreCase(direction)) {
            targetIndex = index + 1;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direzione non valida");
        }

        if (targetIndex < 0 || targetIndex >= rows.size()) {
            return rows;
        }

        GaraDettaglioRow targetRow = rows.get(targetIndex);
        if (!isStopped(targetRow)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Riordino consentito solo tra righe Stopped");
        }

        Collections.swap(rows, index, targetIndex);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return rows;
    }

    public List<GaraDettaglioRow> relinkRowToLocal(String eventId, String rowId) throws Exception {
        sessionService.requireAuthenticated();
        markEventActive(eventId);
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);

        GaraDettaglioRow row = snapshot.getRows().stream()
                .filter(r -> rowId.equals(r.getRowId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata"));
        if (!isStopped(row)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Relink locale consentito solo in stato Stopped");
        }

        String sourceType = detectSourceType(row.getSource());
        if ("local".equals(sourceType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La riga usa già un file locale");
        }

        String fileName = extractFileName(row.getSource());
        if (fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome file non ricavabile dal source");
        }

        Path localPath = findLocalFileCandidate(fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File non trovato in ~/icronium-work"));

        row.setSource(localPath.toAbsolutePath().normalize().toString());
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> addCodeToBlacklist(String eventId, String rowId, String code) throws Exception {
        sessionService.requireAuthenticated();
        String normalized = normalizeBlacklistCode(code);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codice blacklist obbligatorio");
        }
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);
        GaraDettaglioRow row = snapshot.getRows().stream()
                .filter(r -> rowId.equals(r.getRowId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata"));
        if (!row.getBlacklistedCodes().contains(normalized)) {
            row.getBlacklistedCodes().add(normalized);
        }
        refreshVisibleCount(row);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    public List<GaraDettaglioRow> removeCodeFromBlacklist(String eventId, String rowId, String code) throws Exception {
        sessionService.requireAuthenticated();
        String normalized = normalizeBlacklistCode(code);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codice blacklist obbligatorio");
        }
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessuna riga salvata"));
        ensureRowIds(snapshot);
        GaraDettaglioRow row = snapshot.getRows().stream()
                .filter(r -> rowId.equals(r.getRowId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riga non trovata"));
        row.getBlacklistedCodes().removeIf(codeValue -> normalized.equals(normalizeBlacklistCode(codeValue)));
        refreshVisibleCount(row);
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);
        return snapshot.getRows();
    }

    private List<String> parseTimingPoints(String payload) throws Exception {
        List<String> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(payload);
        JsonNode timingPoints = extractTimingPointNode(root);
        if (timingPoints == null || !timingPoints.isArray()) {
            return result;
        }

        for (JsonNode node : timingPoints) {
            JsonNode nameNode = node.get("Name");
            if (nameNode == null || nameNode.isNull()) {
                nameNode = node.get("name");
            }
            if (nameNode != null && !nameNode.isNull()) {
                String name = nameNode.asText();
                if (name != null && !name.isBlank()) {
                    result.add(name);
                }
            }
        }

        return result;
    }

    private JsonNode extractTimingPointNode(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            for (JsonNode node : root) {
                JsonNode timingPoints = extractTimingPointNode(node);
                if (timingPoints != null) {
                    return timingPoints;
                }
            }
            return null;
        }
        if (root.has("TimingPoints")) {
            return root.get("TimingPoints");
        }
        if (root.has("timingPoints")) {
            return root.get("timingPoints");
        }
        if (root.has("result")) {
            return extractTimingPointNode(root.get("result"));
        }
        if (root.has("data")) {
            return extractTimingPointNode(root.get("data"));
        }
        return null;
    }

    private int countCsvRows(String csv) {
        if (csv == null || csv.isBlank()) {
            return 0;
        }

        String[] lines = csv.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private List<BibChipRow> parseBibChipRows(String csv) {
        List<BibChipRow> rows = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return rows;
        }

        String[] lines = csv.split("\\r?\\n");
        int bibIndex = 0;
        int chipIndex = 1;
        int lastNameIndex = 2;
        int firstNameIndex = 3;
        boolean headerProcessed = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String normalized = trimmed.replace("\"", "");
            String[] parts = normalized.split(";");
            if (parts.length < 2) {
                continue;
            }

            if (!headerProcessed && !isNumeric(parts[0].trim())) {
                bibIndex = findColumnIndex(parts, "BIB", "PETT", "PETTORALE");
                chipIndex = findColumnIndex(parts, "CHIP", "CHIPS", "TRANSPONDER");
                lastNameIndex = findColumnIndex(parts, "LASTNAME", "COGNOME", "SURNAME");
                firstNameIndex = findColumnIndex(parts, "FIRSTNAME", "NOME", "NAME");
                headerProcessed = true;
                continue;
            }
            headerProcessed = true;

            String bibText = getColumnValue(parts, bibIndex);
            String chipsText = getColumnValue(parts, chipIndex);
            String lastName = getColumnValue(parts, lastNameIndex);
            String firstName = getColumnValue(parts, firstNameIndex);
            if (chipsText.isEmpty()) {
                continue;
            }

            int bib;
            try {
                bib = Integer.parseInt(bibText);
            } catch (NumberFormatException e) {
                continue;
            }

            List<String> chips = Arrays.stream(chipsText.split("-"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            for (String chip : chips) {
                rows.add(new BibChipRow(chip, bib, lastName, firstName));
            }
        }

        return rows;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private int findColumnIndex(String[] headers, String... aliases) {
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i] == null ? "" : headers[i].trim().toUpperCase();
            for (String alias : aliases) {
                if (header.equals(alias)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getColumnValue(String[] parts, int index) {
        if (index < 0 || index >= parts.length) {
            return "";
        }
        return parts[index].trim();
    }

    private GaraSyncResponse syncTZero(String eventId) throws Exception {
        String rootFolder = sessionService.getTZeroRootFolder();
        Path eventFolder = tZeroConfigService.getEventFolder(rootFolder, eventId);
        Path participantsFile = eventFolder.resolve("participants.csv");
        if (!Files.exists(participantsFile) || !Files.isRegularFile(participantsFile)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "participants.csv non trovato nella cartella gara TZero");
        }

        String participantsCsv = Files.readString(participantsFile);
        List<BibChipRow> bibChipRows = parseBibChipRows(participantsCsv);
        int participantsCount = bibChipRows.size();

        Path downloadFolder = eventFolder.resolve("download");
        List<Path> filePaths = new ArrayList<>();
        if (Files.isDirectory(downloadFolder)) {
            try (Stream<Path> stream = Files.list(downloadFolder)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith("file-"))
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toUpperCase();
                            return !fileName.contains("MERGED") && !fileName.contains("CORREZIONI");
                        })
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                        .forEach(filePaths::add);
            }
        }

        List<String> timingPoints = new ArrayList<>();
        GaraDettaglioSnapshot snapshot = repository.findByEventId(eventId)
                .orElseGet(() -> createSnapshotFromSession(eventId));
        ensureRowIds(snapshot);

        Map<String, GaraDettaglioRow> existingBySource = new HashMap<>();
        for (GaraDettaglioRow row : snapshot.getRows()) {
            if (row.getSource() != null && !row.getSource().isBlank()) {
                existingBySource.put(Paths.get(row.getSource()).toAbsolutePath().normalize().toString(), row);
            }
        }

        List<GaraDettaglioRow> updatedRows = new ArrayList<>();
        for (Path filePath : filePaths) {
            String normalizedSource = filePath.toAbsolutePath().normalize().toString();
            GaraDettaglioRow existing = existingBySource.get(normalizedSource);
            if (existing != null) {
                String currentTimingPoint = existing.getTimingPoint() == null ? "" : existing.getTimingPoint().trim();
                if (currentTimingPoint.equals(legacyDerivedTZeroTimingPoint(filePath))) {
                    existing.setTimingPoint("");
                    currentTimingPoint = "";
                }
                updatedRows.add(existing);
                if (!currentTimingPoint.isBlank()) {
                    timingPoints.add(currentTimingPoint);
                }
                continue;
            }

            GaraDettaglioRow newRow = new GaraDettaglioRow(
                    UUID.randomUUID().toString(),
                    normalizedSource,
                    "",
                    "Stopped",
                    10,
                    "",
                    0,
                    0,
                    0
            );
            newRow.setSourceKind("local");
            newRow.setSyncOffset("+00:00:00.000");
            updatedRows.add(newRow);
        }

        snapshot.setRows(updatedRows);
        snapshot.setBibChipCount(participantsCount);
        snapshot.setTimingPointCount((int) timingPoints.stream().filter(value -> value != null && !value.isBlank()).distinct().count());
        snapshot.setBibChipRows(new ArrayList<>(bibChipRows));
        enrichSnapshotConnectionFromSession(snapshot);
        repository.save(snapshot);

        List<String> distinctTimingPoints = timingPoints.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        sessionService.saveSyncData(eventId, new RRGaraSyncData(participantsCount, distinctTimingPoints, bibChipRows));

        return new GaraSyncResponse(eventId, buildEventLandingUrl(eventId), participantsCount, distinctTimingPoints);
    }

    private String legacyDerivedTZeroTimingPoint(Path filePath) {
        if (filePath == null || filePath.getFileName() == null) {
            return "";
        }
        String value = filePath.getFileName().toString();
        if (value.startsWith("file-")) {
            value = value.substring(5);
        }
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex > 0) {
            value = value.substring(0, dotIndex);
        }
        return value.trim();
    }

    private GaraDettaglioSnapshot createSnapshotFromSession(String eventId) {
        RRGaraSyncData syncData = sessionService.getSyncData(eventId);
        int participantsCount = syncData != null ? syncData.getParticipantsCount() : 0;
        int timingPointCount = syncData != null ? syncData.getTimingPoints().size() : 0;
        int scartiCount = 0;
        GaraDettaglioSnapshot snapshot = new GaraDettaglioSnapshot(eventId, participantsCount, timingPointCount, scartiCount, new ArrayList<>());
        enrichSnapshotConnectionFromSession(snapshot);
        if (syncData != null && syncData.getBibChipRows() != null) {
            snapshot.setBibChipRows(new ArrayList<>(syncData.getBibChipRows()));
        }
        return snapshot;
    }

    private void ensureRowIds(GaraDettaglioSnapshot snapshot) {
        if (snapshot.getRows() == null) {
            snapshot.setRows(new ArrayList<>());
            return;
        }
        for (GaraDettaglioRow row : snapshot.getRows()) {
            if (row.getRowId() == null || row.getRowId().isBlank()) {
                row.setRowId(UUID.randomUUID().toString());
            }
            if (row.getScaricaOgniSec() <= 0) {
                row.setScaricaOgniSec(60);
            }
            row.setSyncOffset(sanitizeSyncOffset(row.getSyncOffset()));
            if (row.getDownloadDeltas() == null) {
                row.setDownloadDeltas(new ArrayList<>());
            }
            if (row.getSentDeltas() == null) {
                row.setSentDeltas(new ArrayList<>());
            }
            if (row.getDownloadCycleDetails() == null) {
                row.setDownloadCycleDetails(new ArrayList<>());
            }
            if (row.getSentCycleDetails() == null) {
                row.setSentCycleDetails(new ArrayList<>());
            }
            if (row.getDiscardedLines() == null) {
                row.setDiscardedLines(new ArrayList<>());
            }
            if (row.getBlacklistedCodes() == null) {
                row.setBlacklistedCodes(new ArrayList<>());
            }
            if (row.getProcessedLines() < 0) {
                row.setProcessedLines(0);
            }
            if (row.getUploadedLines() < 0) {
                row.setUploadedLines(0);
            }
            // Legacy compatibility: old rows saved with stato=Error are still operationally running.
            if (row.getStato() != null && row.getStato().equalsIgnoreCase("Error")) {
                row.setStato("Running");
                row.setErrorState(true);
                if (row.getLastErrorMessage() == null || row.getLastErrorMessage().isBlank()) {
                    row.setLastErrorMessage("Legacy error status restored");
                }
            }
            trimDeltaHistory(row);
            trimSentHistory(row);
            row.setSourceKind(resolveSourceKind(row.getSource(), row.getSourceKind()));
        }
    }

    private void applyAction(GaraDettaglioRow row, String action, boolean strict) {
        if (action == null) {
            return;
        }
        switch (action.toLowerCase()) {
            case "start":
                if (strict && isRunning(row)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Play non disponibile: riga già Running");
                }
                row.setStato("Running");
                break;
            case "stop":
                if (strict && !isRunning(row) && !isLegacyErrorStatus(row)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Stop non disponibile: riga non Running");
                }
                row.setStato("Stopped");
                row.setErrorState(false);
                row.setLastErrorMessage(null);
                break;
            case "rewind":
                if (!isStopped(row)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Rewind consentito solo in stato Stopped");
                }
                row.setStato("Stopped");
                row.setProgresso(0);
                row.setUltimoDownload("");
                row.setRigheFile(0);
                row.setRighePo(0);
                row.setDownloadDeltas(new ArrayList<>());
                row.setSentDeltas(new ArrayList<>());
                row.setDownloadCycleDetails(new ArrayList<>());
                row.setSentCycleDetails(new ArrayList<>());
                row.setDiscardedLines(new ArrayList<>());
                row.setProcessedLines(0);
                row.setUploadedLines(0);
                row.setUniqueCount(0);
                row.setErrorState(false);
                row.setLastErrorMessage(null);
                break;
            default:
                break;
        }
    }

    private boolean isRunning(GaraDettaglioRow row) {
        return row != null && row.getStato() != null && row.getStato().equalsIgnoreCase("Running");
    }

    private boolean isLegacyErrorStatus(GaraDettaglioRow row) {
        return row != null && row.getStato() != null && row.getStato().equalsIgnoreCase("Error");
    }

    private boolean isStopped(GaraDettaglioRow row) {
        return !isRunning(row);
    }

    private String detectSourceType(String source) {
        return resolveSourceKind(source, null);
    }

    private String resolveSourceKind(String source, String sourceKind) {
        String explicit = sourceKind == null ? "" : sourceKind.trim().toLowerCase();
        if ("simulated".equals(explicit)) {
            return "simulated";
        }
        String value = source == null ? "" : source.trim().toLowerCase();
        String simulatedDir = it.icron.icronium.connector.rr.IcroniumApplication.WORK_DIR
                .resolve("simulated")
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace('\\', '/')
                .toLowerCase();
        String normalized = value.replace('\\', '/');
        String fileName = extractFileName(source).toLowerCase();
        if (fileName.startsWith("file-sim-")) {
            return "simulated";
        }
        if (!simulatedDir.isBlank() && normalized.startsWith(simulatedDir)) {
            return "simulated";
        }
        if (value.contains("192.168.")) {
            return "lan";
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return "web";
        }
        return "local";
    }

    private LocalDateTime parseSimulatedTimestamp(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        try {
            return LocalDateTime.parse(value.trim(), SIMULATED_INPUT_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato timestamp non valido: usare yyyy-MM-ddTHH:mm:ss");
        }
    }

    private String normalizeSimulatedFileName(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            value = "file-sim-generated.tags";
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("file-sim-")) {
            // already normalized
        } else if (lower.startsWith("file-")) {
            value = "file-sim-" + value.substring(5);
        } else {
            value = "file-sim-" + value;
        }
        if (!value.toLowerCase().endsWith(".tags")) {
            value = value + ".tags";
        }
        String sanitized = value.replace('\\', '/');
        if (sanitized.contains("/")) {
            sanitized = sanitized.substring(sanitized.lastIndexOf('/') + 1);
        }
        return sanitized;
    }

    private void deleteSimulatedFileIfNeeded(GaraDettaglioRow row) {
        if (row == null || !"simulated".equalsIgnoreCase(resolveSourceKind(row.getSource(), row.getSourceKind()))) {
            return;
        }
        String source = row.getSource() == null ? "" : row.getSource().trim();
        if (source.isBlank()) {
            return;
        }
        try {
            Path path = Paths.get(source).toAbsolutePath().normalize();
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            log.warn("Delete simulated file skipped: source={} message={}", source, ex.getMessage());
        }
    }

    private List<String> generateSimulatedPassings(List<String> chips, LocalDateTime from, LocalDateTime to, int intervalSeconds, String seedSource) {
        List<String> orderedChips = new ArrayList<>(chips);
        Collections.shuffle(orderedChips, new Random(seedSource.hashCode()));

        long totalSeconds = Math.max(1, ChronoUnit.SECONDS.between(from, to));
        int slots = Math.max(1, (int) Math.ceil(totalSeconds / (double) intervalSeconds) + 1);
        double[] weights = new double[slots];
        double sigma = Math.max(1.0, slots / 6.0);
        double mean = (slots - 1) / 2.0;
        double sum = 0.0;
        for (int i = 0; i < slots; i++) {
            double distance = (i - mean) / sigma;
            weights[i] = Math.exp(-0.5 * distance * distance) + 0.08d;
            sum += weights[i];
        }

        int[] counts = new int[slots];
        double[] fractions = new double[slots];
        int allocated = 0;
        for (int i = 0; i < slots; i++) {
            double exact = (weights[i] / sum) * orderedChips.size();
            counts[i] = (int) Math.floor(exact);
            fractions[i] = exact - counts[i];
            allocated += counts[i];
        }
        while (allocated < orderedChips.size()) {
            int bestIndex = 0;
            for (int i = 1; i < slots; i++) {
                if (fractions[i] > fractions[bestIndex]) {
                    bestIndex = i;
                }
            }
            counts[bestIndex]++;
            fractions[bestIndex] = 0;
            allocated++;
        }

        List<String> lines = new ArrayList<>(orderedChips.size());
        Random random = new Random((seedSource + ":ts").hashCode());
        int chipIndex = 0;
        for (int slot = 0; slot < slots && chipIndex < orderedChips.size(); slot++) {
            int count = counts[slot];
            LocalDateTime slotStart = from.plusSeconds((long) slot * intervalSeconds);
            if (slotStart.isAfter(to)) {
                slotStart = to;
            }
            int spreadMillis = Math.max(1, intervalSeconds * 1000);
            for (int i = 0; i < count && chipIndex < orderedChips.size(); i++) {
                int offsetMillis = count <= 1 ? 0 : Math.min(spreadMillis - 1, (int) Math.round((i / (double) count) * spreadMillis));
                offsetMillis = Math.min(spreadMillis - 1, Math.max(0, offsetMillis + random.nextInt(Math.max(1, spreadMillis / 6)) - Math.max(1, spreadMillis / 12)));
                LocalDateTime ts = slotStart.plusNanos(offsetMillis * 1_000_000L);
                if (ts.isAfter(to)) {
                    ts = to;
                }
                lines.add(orderedChips.get(chipIndex++) + ";" + ts.format(SIMULATED_TS_FORMATTER));
            }
        }
        Collections.sort(lines, Comparator.comparing(line -> {
            String[] parts = line.split(";", 2);
            return parts.length > 1 ? parts[1] : "";
        }));
        return lines;
    }

    private String extractFileName(String source) {
        String value = source == null ? "" : source.trim();
        if (value.isBlank()) {
            return "";
        }
        String noQuery = value.split("\\?", 2)[0];
        String normalized = noQuery.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < normalized.length()) {
            return normalized.substring(lastSlash + 1);
        }
        return normalized;
    }

    private Optional<Path> findLocalFileCandidate(String fileName) throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.home"), "icronium-work");
        if (!Files.isDirectory(baseDir)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.walk(baseDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> fileName.equals(path.getFileName().toString()))
                    .findFirst();
        }
    }

    private void validateGlobalAction(List<GaraDettaglioRow> rows, String action) {
        if (rows == null || action == null) {
            return;
        }
        switch (action.toLowerCase()) {
            case "start":
                boolean hasRunning = rows.stream().anyMatch(this::isRunning);
                if (hasRunning) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Start generale non disponibile: esiste una riga Running");
                }
                break;
            case "rewind":
                boolean hasRunningForRewind = rows.stream().anyMatch(this::isRunning);
                if (hasRunningForRewind) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Rewind generale consentito solo con righe Stopped");
                }
                break;
            default:
                break;
        }
    }

    private int sanitizeScaricaOgniSec(Integer value) {
        if (value == null || value <= 0) {
            return 60;
        }
        return value;
    }

    private String sanitizeSyncOffset(String value) {
        if (value == null || value.isBlank()) {
            return "+00:00:00.000";
        }
        String trimmed = value.trim();
        if (!SYNC_OFFSET_PATTERN.matcher(trimmed).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sync offset format must be +/- hh:mm:ss.sss");
        }
        return trimmed;
    }

    private String normalizeBlacklistCode(String code) {
        if (code == null) {
            return "";
        }
        String normalized = code.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.replace("\"", "").trim();
    }

    private GaraDettaglioRow duplicateStoppedRow(GaraDettaglioRow sourceRow) throws Exception {
        GaraDettaglioRow copy = objectMapper.readValue(
                objectMapper.writeValueAsBytes(sourceRow),
                GaraDettaglioRow.class
        );
        copy.setRowId(UUID.randomUUID().toString());
        copy.setStato("Stopped");
        copy.setUltimoDownload("");
        copy.setProgresso(0);
        copy.setRigheFile(0);
        copy.setRighePo(0);
        copy.setDownloadDeltas(new ArrayList<>());
        copy.setSentDeltas(new ArrayList<>());
        copy.setDownloadCycleDetails(new ArrayList<>());
        copy.setSentCycleDetails(new ArrayList<>());
        copy.setDiscardedLines(new ArrayList<>());
        copy.setProcessedLines(0);
        copy.setUploadedLines(0);
        copy.setErrorState(false);
        copy.setLastErrorMessage(null);
        return copy;
    }

    private Path resolvePreviewPath(String source) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            try {
                String pathPart = URI.create(source).getPath();
                String fileName = Paths.get(pathPart).getFileName() != null
                        ? Paths.get(pathPart).getFileName().toString()
                        : "";
                if (fileName.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome file non valido in source remoto");
                }
                return it.icron.icronium.connector.rr.IcroniumApplication.WORK_DIR.resolve(fileName);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL source non valido");
            }
        }
        return Paths.get(source);
    }

    private String buildEventApiUrl(String eventId) {
        if (sessionService.isTZeroMode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API RR non disponibile in modalità TZero");
        }
        if (sessionService.isLocalMode()) {
            return "http://localhost/_" + eventId + "/api/";
        }
        return "https://events.raceresult.com/_" + eventId + "/api/";
    }

    private String buildEventLandingUrl(String eventId) {
        if (sessionService.isTZeroMode()) {
            return eventId;
        }
        String host = sessionService.isLocalMode() ? "http://localhost" : "https://events.raceresult.com";
        String pw = sessionService.isLocalMode() ? "0" : sessionService.getRrPw();
        return host + "/_" + eventId + "/?lang=en-it&pw=" + pw;
    }

    private String buildEventLandingUrl(String host, String eventId, String pw) {
        return host + "/_" + eventId + "/?lang=en-it&pw=" + pw;
    }

    private void enrichSnapshotConnectionFromSession(GaraDettaglioSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (sessionService.isTZeroMode()) {
            snapshot.setRrHost("");
            snapshot.setRrPw("");
            return;
        }
        String host = sessionService.isLocalMode() ? "http://localhost" : "https://events.raceresult.com";
        String pw = sessionService.isLocalMode() ? "0" : sessionService.getRrPw();
        if (pw == null || pw.isBlank()) {
            pw = "0";
        }
        snapshot.setRrHost(host);
        snapshot.setRrPw(pw);
    }

    @Scheduled(fixedDelay = 1000)
    public void scheduledDownloadTick() {
        try {
            String activeEventId = activeEventService.getActiveEventId();
            if (activeEventId == null || activeEventId.isBlank()) {
                return;
            }
            GaraDettaglioSnapshot snapshot = repository.findByEventId(activeEventId).orElse(null);
            if (snapshot == null || snapshot.getRows() == null || snapshot.getRows().isEmpty()) {
                return;
            }

            boolean snapshotChanged = false;
            for (GaraDettaglioRow row : snapshot.getRows()) {
                if (!isRunning(row)) {
                    continue;
                }
                if (!isDueForDownload(row)) {
                    continue;
                }
                boolean rowChanged = executeDownload(snapshot.getEventId(), row);
                snapshotChanged = snapshotChanged || rowChanged;
            }

            if (snapshotChanged) {
                reconcileStoppedRows(snapshot);
                repository.save(snapshot);
            }
        } catch (Exception ignored) {
        }
    }

    private void markEventActive(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        activeEventService.setActiveEventId(eventId);
    }

    private void triggerDownloadNow(GaraDettaglioSnapshot snapshot, GaraDettaglioRow row) {
        boolean changed = executeDownload(snapshot.getEventId(), row);
        if (changed) {
            try {
                repository.save(snapshot);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isDueForDownload(GaraDettaglioRow row) {
        if (row.getUltimoDownload() == null || row.getUltimoDownload().isBlank()) {
            return true;
        }
        try {
            LocalDateTime lastDownload = LocalDateTime.parse(row.getUltimoDownload(), DOWNLOAD_TS_FORMATTER);
            long secondsElapsed = Duration.between(lastDownload, LocalDateTime.now()).getSeconds();
            return secondsElapsed >= row.getScaricaOgniSec();
        } catch (DateTimeParseException ignored) {
            return true;
        }
    }

    private boolean executeDownload(String eventId, GaraDettaglioRow row) {
        String rowKey = eventId + ":" + row.getRowId();
        if (rowExecutionLocks.putIfAbsent(rowKey, Boolean.TRUE) != null) {
            return false;
        }

        try {
            if (!isRunning(row)) {
                return false;
            }
            String source = row.getSource() == null ? "" : row.getSource().trim();
            if (source.isBlank()) {
                log.warn("[{}/{}] Download skipped: source vuoto", eventId, row.getRowId());
                row.setErrorState(true);
                row.setLastErrorMessage("Download skipped: empty source");
                notifyDownload(eventId, row.getRowId(), "Download saltato: source vuoto");
                notifyRowUpdated(eventId, row);
                return false;
            }

            log.info("[{}/{}] Download start source={}", eventId, row.getRowId(), source);
            Path downloadedPath;
            if (source.startsWith("http://") || source.startsWith("https://")) {
                downloadedPath = FileDownloader.downloadHttp(source);
            } else {
                downloadedPath = FileDownloader.download(source);
            }

            List<String> visibleLines = loadVisibleLines(downloadedPath, row);
            long lines = visibleLines.size();
            row.setUniqueCount(countUniqueCodes(visibleLines));
            int previousLines = row.getRigheFile();
            List<String> downloadCycleLines = extractCycleLines(visibleLines, previousLines);
            row.setRigheFile((int) lines);
            row.setProgresso(100);
            row.setUltimoDownload(LocalDateTime.now().format(DOWNLOAD_TS_FORMATTER));
            row.setErrorState(false);
            row.setLastErrorMessage(null);
            appendDelta(row, (int) Math.max(0, lines - previousLines), downloadCycleLines);
            PostBatchResult postResult = sendDeltaToExternal(eventId, row, downloadedPath);
            int sentDelta = postResult.getSentLines() == null ? 0 : postResult.getSentLines().size();
            appendSentDelta(row, sentDelta, postResult.getSentLines());
            row.setRighePo((int) postResult.getUploadedLines());
            alignWithPersistedState(eventId, row);
            log.info(
                    "[{}/{}] Download completed file={} righeScaricate={} delta={} sentDelta={}",
                    eventId,
                    row.getRowId(),
                    downloadedPath.toAbsolutePath(),
                    lines,
                    row.getDownloadDeltas().isEmpty() ? 0 : row.getDownloadDeltas().get(row.getDownloadDeltas().size() - 1),
                    sentDelta
            );
            notifyDownload(eventId, row.getRowId(), "Download completato: " + downloadedPath.getFileName());
            notifyRowUpdated(eventId, row);
            return true;
        } catch (Exception ex) {
            row.setErrorState(true);
            row.setLastErrorMessage(ex.getMessage());
            alignWithPersistedState(eventId, row);
            log.error("[{}/{}] Download error: {}", eventId, row.getRowId(), ex.getMessage(), ex);
            notifyDownload(eventId, row.getRowId(), "Errore download: " + ex.getMessage());
            notifyRowUpdated(eventId, row);
            return false;
        } finally {
            rowExecutionLocks.remove(rowKey);
        }
    }

    private List<String> loadVisibleLines(Path downloadedPath, GaraDettaglioRow row) throws Exception {
        List<String> visibleLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(downloadedPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isIgnoredPassingLine(line, row)) {
                    continue;
                }
                visibleLines.add(line.trim());
            }
        }
        return visibleLines;
    }

    private long countVisibleLines(Path downloadedPath, GaraDettaglioRow row) throws Exception {
        return loadVisibleLines(downloadedPath, row).size();
    }

    private List<String> extractCycleLines(List<String> visibleLines, int previousLines) {
        if (visibleLines == null || visibleLines.isEmpty()) {
            return new ArrayList<>();
        }
        int safePrevious = Math.max(0, previousLines);
        if (safePrevious >= visibleLines.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(visibleLines.subList(safePrevious, visibleLines.size()));
    }

    private int countUniqueCodes(List<String> visibleLines) {
        if (visibleLines == null || visibleLines.isEmpty()) {
            return 0;
        }
        java.util.Set<String> unique = new java.util.HashSet<>();
        for (String line : visibleLines) {
            String code = extractPassingCode(line);
            if (code != null && !code.isBlank()) {
                unique.add(code);
            }
        }
        return unique.size();
    }

    private boolean isIgnoredPassingLine(String line, GaraDettaglioRow row) {
        if (line == null) {
            return true;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.contains("*")) {
            return true;
        }
        String code = extractPassingCode(trimmed);
        return code != null && row != null && row.getBlacklistedCodes() != null && row.getBlacklistedCodes().contains(code);
    }

    public static String extractPassingCode(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String separator = line.contains("\t") ? "\t" : ";";
        String[] parts = line.split(separator);
        if (parts.length == 0) {
            return null;
        }
        String code = parts[0].replace("\"", "").trim();
        return code.isEmpty() ? null : code;
    }

    private void refreshVisibleCount(GaraDettaglioRow row) {
        try {
            String source = row.getSource() == null ? "" : row.getSource().trim();
            if (source.isBlank()) {
                return;
            }
            Path currentPath = resolvePreviewPath(source);
            if (!Files.exists(currentPath)) {
                return;
            }
            row.setRigheFile((int) countVisibleLines(currentPath, row));
        } catch (Exception ignored) {
        }
    }

    private void notifyDownload(String eventId, String rowId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSender("scheduler");
        message.setContent("[" + eventId + "/" + rowId + "] " + content);
        messagingTemplate.convertAndSend("/topic/messages", message);
    }

    private void notifyRowUpdated(String eventId, GaraDettaglioRow row) {
        messagingTemplate.convertAndSend("/topic/gare-updates", new GaraRowUpdateMessage(eventId, row));
    }

    private PostBatchResult sendDeltaToExternal(String eventId, GaraDettaglioRow row, Path downloadedPath) {
        try {
            Optional<GaraDettaglioSnapshot> snapshotOpt = repository.findByEventId(eventId);
            if (snapshotOpt.isEmpty() || snapshotOpt.get().getBibChipRows() == null || snapshotOpt.get().getBibChipRows().isEmpty()) {
                log.warn("[{}/{}] Invio esterno saltato: mappa BIB-CHIP non disponibile (eseguire SYNC)", eventId, row.getRowId());
                return new PostBatchResult(row.getUploadedLines(), new ArrayList<>());
            }

            GaraDettaglioSnapshot snapshot = snapshotOpt.get();
            Map<String, Integer> chipBibMap = new HashMap<>();
            snapshot.getBibChipRows().forEach(entry -> chipBibMap.put(entry.getChip(), entry.getBib()));

            String host = snapshot.getRrHost();
            String pw = snapshot.getRrPw();
            if (host == null || host.isBlank() || pw == null || pw.isBlank()) {
                log.warn("[{}/{}] Invio esterno saltato: connessione RR non inizializzata nello snapshot", eventId, row.getRowId());
                return new PostBatchResult(row.getUploadedLines(), new ArrayList<>());
            }

            ConnectorContext ctx = new ConnectorContext();
            ctx.setRrId(buildEventLandingUrl(host, eventId, pw));
            ctx.setChipBibMap(chipBibMap);

            PostBatchResult result = PostDowloader.postDeltaBatch(downloadedPath, row, ctx);
            int sentDelta = result.getSentLines() == null ? 0 : result.getSentLines().size();
            notifyDownload(eventId, row.getRowId(), "Invio esterno completato: " + sentDelta + " passaggi");
            return result;
        } catch (Exception ex) {
            log.error(
                    "[{}/{}] POST failed verso sistema esterno: source={} processedOffset={} uploadedTot={} errore={}",
                    eventId,
                    row.getRowId(),
                    row.getSource(),
                    row.getProcessedLines(),
                    row.getUploadedLines(),
                    ex.getMessage(),
                    ex
            );
            log.error("[{}/{}] Errore invio esterno: {}", eventId, row.getRowId(), ex.getMessage(), ex);
            notifyDownload(eventId, row.getRowId(), "Errore invio esterno: " + ex.getMessage());
            return new PostBatchResult(row.getUploadedLines(), new ArrayList<>());
        }
    }

    private void appendDelta(GaraDettaglioRow row, int delta, List<String> cycleLines) {
        if (row.getDownloadDeltas() == null) {
            row.setDownloadDeltas(new ArrayList<>());
        }
        if (row.getDownloadCycleDetails() == null) {
            row.setDownloadCycleDetails(new ArrayList<>());
        }
        row.getDownloadDeltas().add(Math.max(0, delta));
        row.getDownloadCycleDetails().add(cycleLines == null ? new ArrayList<>() : new ArrayList<>(cycleLines));
        trimDeltaHistory(row);
    }

    private void trimDeltaHistory(GaraDettaglioRow row) {
        if (row.getDownloadDeltas() == null) {
            return;
        }
        while (row.getDownloadDeltas().size() > MAX_DELTA_HISTORY) {
            row.getDownloadDeltas().remove(0);
        }
        if (row.getDownloadCycleDetails() == null) {
            return;
        }
        while (row.getDownloadCycleDetails().size() > MAX_DELTA_HISTORY) {
            row.getDownloadCycleDetails().remove(0);
        }
        while (row.getDownloadCycleDetails().size() < row.getDownloadDeltas().size()) {
            row.getDownloadCycleDetails().add(0, new ArrayList<>());
        }
        while (row.getDownloadCycleDetails().size() > row.getDownloadDeltas().size()) {
            row.getDownloadCycleDetails().remove(0);
        }
    }

    private void appendSentDelta(GaraDettaglioRow row, int sent, List<String> cycleLines) {
        if (row.getSentDeltas() == null) {
            row.setSentDeltas(new ArrayList<>());
        }
        if (row.getSentCycleDetails() == null) {
            row.setSentCycleDetails(new ArrayList<>());
        }
        row.getSentDeltas().add(Math.max(0, sent));
        row.getSentCycleDetails().add(cycleLines == null ? new ArrayList<>() : new ArrayList<>(cycleLines));
        trimSentHistory(row);
    }

    private void trimSentHistory(GaraDettaglioRow row) {
        if (row.getSentDeltas() == null) {
            return;
        }
        while (row.getSentDeltas().size() > MAX_DELTA_HISTORY) {
            row.getSentDeltas().remove(0);
        }
        if (row.getSentCycleDetails() == null) {
            return;
        }
        while (row.getSentCycleDetails().size() > MAX_DELTA_HISTORY) {
            row.getSentCycleDetails().remove(0);
        }
        while (row.getSentCycleDetails().size() < row.getSentDeltas().size()) {
            row.getSentCycleDetails().add(0, new ArrayList<>());
        }
        while (row.getSentCycleDetails().size() > row.getSentDeltas().size()) {
            row.getSentCycleDetails().remove(0);
        }
    }

    private void reconcileStoppedRows(GaraDettaglioSnapshot schedulerSnapshot) {
        try {
            Optional<GaraDettaglioSnapshot> persistedOpt = repository.findByEventId(schedulerSnapshot.getEventId());
            if (persistedOpt.isEmpty() || persistedOpt.get().getRows() == null) {
                return;
            }
            Map<String, GaraDettaglioRow> persistedById = new HashMap<>();
            for (GaraDettaglioRow persistedRow : persistedOpt.get().getRows()) {
                if (persistedRow.getRowId() != null) {
                    persistedById.put(persistedRow.getRowId(), persistedRow);
                }
            }
            for (GaraDettaglioRow row : schedulerSnapshot.getRows()) {
                if (row == null || row.getRowId() == null) {
                    continue;
                }
                GaraDettaglioRow persisted = persistedById.get(row.getRowId());
                if (persisted == null) {
                    continue;
                }
                if (!isRunning(persisted) && isRunning(row)) {
                    row.setStato(persisted.getStato());
                    row.setErrorState(persisted.isErrorState());
                    row.setLastErrorMessage(persisted.getLastErrorMessage());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void alignWithPersistedState(String eventId, GaraDettaglioRow row) {
        try {
            Optional<GaraDettaglioSnapshot> persistedOpt = repository.findByEventId(eventId);
            if (persistedOpt.isEmpty() || persistedOpt.get().getRows() == null || row == null || row.getRowId() == null) {
                return;
            }
            for (GaraDettaglioRow persistedRow : persistedOpt.get().getRows()) {
                if (!row.getRowId().equals(persistedRow.getRowId())) {
                    continue;
                }
                if (!isRunning(persistedRow)) {
                    row.setStato(persistedRow.getStato());
                    row.setErrorState(persistedRow.isErrorState());
                    row.setLastErrorMessage(persistedRow.getLastErrorMessage());
                }
                return;
            }
        } catch (Exception ignored) {
        }
    }
}
