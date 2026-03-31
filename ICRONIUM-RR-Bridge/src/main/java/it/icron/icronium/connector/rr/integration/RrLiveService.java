package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.RrLiveEndpoint;
import it.icron.icronium.connector.rr.model.RrLiveLogMessage;
import it.icron.icronium.connector.rr.model.RrLiveRow;
import it.icron.icronium.connector.rr.model.RrLiveState;
import it.icron.icronium.connector.rr.model.RrLiveSyncRequest;
import it.icron.icronium.connector.rr.model.RrLiveUpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RrLiveService {
    private static final Logger log = LoggerFactory.getLogger(RrLiveService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RRSessionService sessionService;
    private final RrLiveRepository repository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public RrLiveService(RRSessionService sessionService, RrLiveRepository repository, ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.sessionService = sessionService;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    public RrLiveState sync(RrLiveSyncRequest request) throws Exception {
        sessionService.requireAuthenticated();
        if (request == null || request.getEventId() == null || request.getEventId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId is required");
        }

        String eventId = request.getEventId().trim();
        int pollSec = (request.getPollSec() == null || request.getPollSec() <= 0) ? 5 : request.getPollSec();
        String targetUrl = request.getTargetUrl() == null ? "" : request.getTargetUrl().trim();

        String baseApi = buildEventApiUrl(eventId);
        String syncUrl = baseApi + "simpleapi/get?lang=en";
        String payload;
        if (sessionService.isLocalMode()) {
            payload = sessionService.getClient().getDataRaw(syncUrl + "&pw=0");
        } else {
            payload = sessionService.getClient().getData(syncUrl);
        }
        List<RrLiveEndpoint> apis = parseApis(payload);

        RrLiveState state = repository.findByEventId(eventId).orElseGet(RrLiveState::new);
        state.setEventId(eventId);
        state.setPollSec(pollSec);
        state.setTargetUrl(targetUrl);
        state.setRrMode(sessionService.getMode());
        state.setRrPw(sessionService.isLocalMode() ? "0" : sessionService.getRrPw());
        state.setLastSync(LocalDateTime.now().format(TS));
        state.setApis(apis);
        state.setRows(mapRows(baseApi, apis, state.getRows()));
        state.setRunning(state.getRows().stream().anyMatch(RrLiveRow::isRunning));

        repository.save(state);
        publishState(state);
        publishLog(eventId, "sync", "SYNC completed: " + apis.size() + " endpoints loaded");
        return state;
    }

    public RrLiveState getState(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        return repository.findByEventId(eventId).orElseGet(() -> {
            RrLiveState empty = new RrLiveState();
            empty.setEventId(eventId);
            return empty;
        });
    }

    public RrLiveState start(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        RrLiveState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "State not found. Run SYNC first"));
        state.setRunning(true);
        // refresh auth material on start
        state.setRrMode(sessionService.getMode());
        state.setRrPw(sessionService.isLocalMode() ? "0" : sessionService.getRrPw());
        long now = System.currentTimeMillis();
        for (RrLiveRow row : state.getRows()) {
            row.setRunning(true);
            row.setStatus("Running");
            row.setNextRunAt(now);
        }
        repository.save(state);
        publishState(state);
        publishLog(eventId, "action", "Global PLAY started on " + state.getRows().size() + " rows");
        return state;
    }

    public RrLiveState stop(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        RrLiveState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "State not found"));
        state.setRunning(false);
        for (RrLiveRow row : state.getRows()) {
            row.setRunning(false);
            row.setStatus("Stopped");
        }
        repository.save(state);
        publishState(state);
        publishLog(eventId, "action", "Global STOP applied");
        return state;
    }

    public RrLiveState resetAll(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        RrLiveState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "State not found"));
        for (RrLiveRow row : state.getRows()) {
            row.setRunning(false);
            row.setStatus("Ready");
            clearRowRuntimeMemory(row);
            try {
                LiveUploader.resetCache(safeRowName(row), eventId);
            } catch (Exception ex) {
                log.warn("rr-live global resetCache error [{}]: {}", safeRowName(row), ex.getMessage());
                publishLog(eventId, "scheduler", "Reset cache error on " + safeRowName(row) + ": " + ex.getMessage());
            }
        }
        state.setRunning(false);
        repository.save(state);
        publishState(state);
        publishLog(eventId, "action", "Global RESET applied on " + state.getRows().size() + " rows");
        return state;
    }

    public RrLiveState deleteAll(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        RrLiveState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "State not found"));
        int removed = state.getRows() == null ? 0 : state.getRows().size();
        state.getRows().clear();
        state.setRunning(false);
        repository.save(state);
        publishState(state);
        publishLog(eventId, "action", "Global DELETE removed " + removed + " rows");
        return state;
    }

    public RrLiveState applyRowAction(String eventId, String rowKey, String action) throws Exception {
        sessionService.requireAuthenticated();
        RrLiveState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "State not found"));
        RrLiveRow row = state.getRows().stream()
                .filter(r -> rowKey.equals(r.getKey()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Row not found"));

        String a = action == null ? "" : action.trim().toLowerCase();
        switch (a) {
            case "play":
            case "start":
                row.setRunning(true);
                row.setStatus("Running");
                row.setLastError("");
                row.setNextRunAt(System.currentTimeMillis());
                publishLog(eventId, "action", "Row PLAY: " + safeRowName(row));
                break;
            case "stop":
                row.setRunning(false);
                row.setStatus("Stopped");
                publishLog(eventId, "action", "Row STOP: " + safeRowName(row));
                break;
            case "reset":
            case "rewind":
                row.setRunning(false);
                row.setStatus("Ready");
                clearRowRuntimeMemory(row);
                try {
                    LiveUploader.resetCache(safeRowName(row), eventId);
                } catch (Exception ex) {
                    log.warn("rr-live resetCache error [{}]: {}", safeRowName(row), ex.getMessage());
                    publishLog(eventId, "scheduler", "Reset cache error on " + safeRowName(row) + ": " + ex.getMessage());
                }
                publishLog(eventId, "action", "Row RESET: " + safeRowName(row) + " (runtime memory cleared)");
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported action");
        }

        state.setRunning(state.getRows().stream().anyMatch(RrLiveRow::isRunning));
        repository.save(state);
        publishState(state);
        return state;
    }

    public RrLiveState deleteRow(String eventId, String rowKey) throws Exception {
        sessionService.requireAuthenticated();
        RrLiveState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "State not found"));
        state.getRows().removeIf(r -> rowKey.equals(r.getKey()));
        state.setRunning(state.getRows().stream().anyMatch(RrLiveRow::isRunning));
        repository.save(state);
        publishState(state);
        publishLog(eventId, "action", "Row DELETE: " + rowKey);
        return state;
    }

    @Scheduled(fixedDelay = 1000)
    public void scheduledTick() {
        try {
            List<RrLiveState> states = repository.findAll();
            long now = System.currentTimeMillis();
            for (RrLiveState state : states) {
                if (state.getRows() == null || state.getRows().isEmpty()) {
                    continue;
                }
                boolean changed = false;
                boolean anyRunning = false;
                for (RrLiveRow row : state.getRows()) {
                    if (!row.isRunning()) {
                        continue;
                    }
                    anyRunning = true;
                    if (row.getNextRunAt() > now) {
                        continue;
                    }
                    changed = true;
                    processRow(state, row);
                    row.setNextRunAt(System.currentTimeMillis() + (long) state.getPollSec() * 1000L);
                }
                if (state.isRunning() != anyRunning) {
                    state.setRunning(anyRunning);
                    changed = true;
                }
                if (changed) {
                    repository.save(state);
                    publishState(state);
                }
            }
        } catch (Exception ex) {
            log.warn("rr-live scheduler error: {}", ex.getMessage());
        }
    }

    private void processRow(RrLiveState state, RrLiveRow row) {
        try {
            row.setStatus("Running");
            RaceResultClient client = new RaceResultClient();
            client.setPw(state.getRrPw());

            // RR live row URLs must be used as-is (no pw/cookie/query additions).
            String csv = client.getDataRaw(row.getUrl());
            String hash = sha256(csv);
            int bytes = csv == null ? 0 : csv.getBytes(StandardCharsets.UTF_8).length;

            if (hash.equals(row.getLastHash())) {
                row.setStatus("Running (unchanged)");
                row.setLastError("");
                row.setLastUpdate(LocalDateTime.now().format(TS));
                row.setLastBytes(bytes);
                publishLog(state.getEventId(), "scheduler", "Unchanged: " + safeRowName(row) + " (" + bytes + " bytes)");
                return;
            }

            LiveUploader.send(safeRowName(row), (csv == null ? "" : csv).getBytes(StandardCharsets.UTF_8), state.getEventId());
            row.setStatus("Running (sent)");
            row.setLastError("");
            row.setLastUpdate(LocalDateTime.now().format(TS));
            row.setLastBytes(bytes);
            row.setLastHash(hash);
            publishLog(state.getEventId(), "scheduler", "Sent: " + safeRowName(row) + " (" + bytes + " bytes)");
        } catch (Exception ex) {
            row.setStatus("Running (error)");
            row.setLastError(ex.getMessage());
            row.setLastUpdate(LocalDateTime.now().format(TS));
            log.warn("rr-live row error [{}]: {}", row.getName(), ex.getMessage());
            publishLog(state.getEventId(), "scheduler", "Error on " + safeRowName(row) + ": " + ex.getMessage());
        }
    }

    private List<RrLiveRow> mapRows(String baseApi, List<RrLiveEndpoint> apis, List<RrLiveRow> existingRows) {
        Map<String, RrLiveRow> oldByKey = new HashMap<>();
        if (existingRows != null) {
            for (RrLiveRow row : existingRows) {
                if (row.getKey() != null) {
                    oldByKey.put(row.getKey(), row);
                }
            }
        }

        List<RrLiveRow> out = new ArrayList<>();
        for (RrLiveEndpoint api : apis) {
            String key = api.getKey();
            String label = api.getLabel();
            String url = api.getUrl();
            boolean disabled = api.isDisabled();
            if (disabled) {
                continue;
            }

            String pollingUrl = resolvePollingUrl(baseApi, key, url);
            if (pollingUrl == null || pollingUrl.isBlank()) {
                continue;
            }

            String rowKey = (key == null || key.isBlank()) ? pollingUrl : key;
            RrLiveRow row = oldByKey.getOrDefault(rowKey, new RrLiveRow());
            row.setKey(rowKey);
            row.setName((label == null || label.isBlank()) ? rowKey : label);
            row.setUrl(pollingUrl);
            if (row.getStatus() == null || row.getStatus().isBlank()) {
                row.setStatus("Ready");
            }
            out.add(row);
        }

        out.sort(Comparator.comparing(r -> r.getName() == null ? "" : r.getName().toLowerCase()));
        return out;
    }

    private List<RrLiveEndpoint> parseApis(String payload) throws Exception {
        List<RrLiveEndpoint> out = new ArrayList<>();
        JsonNode endpointArray = resolveEndpointArray(objectMapper.readTree(payload));
        if (endpointArray == null || !endpointArray.isArray()) {
            return out;
        }
        for (JsonNode node : endpointArray) {
            RrLiveEndpoint endpoint = new RrLiveEndpoint();
            endpoint.setKey(text(node, "key", "Key"));
            endpoint.setLabel(text(node, "label", "Label", "name", "Name"));
            endpoint.setUrl(text(node, "url", "Url"));
            endpoint.setDisabled(bool(node, "disabled", "Disabled"));
            out.add(endpoint);
        }
        return out;
    }

    private String resolvePollingUrl(String baseApi, String key, String url) {
        if (key != null && !key.isBlank()) {
            return baseApi + key;
        }
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return baseApi + url;
    }

    private JsonNode resolveEndpointArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("apis")) {
            return resolveEndpointArray(root.get("apis"));
        }
        if (root.has("data")) {
            return resolveEndpointArray(root.get("data"));
        }
        if (root.has("result")) {
            return resolveEndpointArray(root.get("result"));
        }
        return null;
    }

    private String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String t = value.asText();
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private boolean bool(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                return value.asBoolean(false);
            }
        }
        return false;
    }

    private String sha256(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }

    private String buildEventApiUrl(String eventId) {
        if (sessionService.isLocalMode()) {
            return "http://localhost/_" + eventId + "/api/";
        }
        return "https://events.raceresult.com/_" + eventId + "/api/";
    }

    private void publishState(RrLiveState state) {
        if (state == null || state.getEventId() == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/rr-live-updates",
                new RrLiveUpdateMessage(state.getEventId(), state)
        );
    }

    private void publishLog(String eventId, String sender, String content) {
        if (eventId == null || eventId.isBlank() || content == null || content.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/rr-live-messages",
                new RrLiveLogMessage(eventId, sender, content)
        );
    }

    private String safeRowName(RrLiveRow row) {
        if (row == null) {
            return "";
        }
        if (row.getName() != null && !row.getName().isBlank()) {
            return row.getName();
        }
        return row.getKey() == null ? "" : row.getKey();
    }

    private void clearRowRuntimeMemory(RrLiveRow row) {
        row.setLastError("");
        row.setLastBytes(0);
        // Clearing hash forces the next PLAY cycle to send from scratch.
        row.setLastHash("");
        row.setLastUpdate("");
        row.setNextRunAt(0);
    }
}
