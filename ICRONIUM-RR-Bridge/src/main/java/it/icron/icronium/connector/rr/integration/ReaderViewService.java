package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.ChatMessage;
import it.icron.icronium.connector.rr.model.GaraDettaglioRow;
import it.icron.icronium.connector.rr.model.GaraDettaglioSnapshot;
import it.icron.icronium.connector.rr.model.LanDiscoveryResponse;
import it.icron.icronium.connector.rr.model.ReaderConfig;
import it.icron.icronium.connector.rr.model.ReaderConfigRequest;
import it.icron.icronium.connector.rr.model.ReaderFileContentResponse;
import it.icron.icronium.connector.rr.model.ReaderFileEntry;
import it.icron.icronium.connector.rr.model.ReaderViewState;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.net.URI;
import java.net.URLEncoder;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Service
public class ReaderViewService {

    private static final DateTimeFormatter FILE_TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ICRON_WAN_INDEX_URL = "https://www.icron.it/services/live/icronium.php?reader=";
    private static final String ICRON_WAN_BASE_URL = "https://www.icron.it";

    private final RRSessionService sessionService;
    private final ActiveEventService activeEventService;
    private final ReaderViewRepository repository;
    private final GaraDettaglioRepository garaDettaglioRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final HttpClient httpClient;

    public ReaderViewService(RRSessionService sessionService, ActiveEventService activeEventService, ReaderViewRepository repository, GaraDettaglioRepository garaDettaglioRepository, ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.sessionService = sessionService;
        this.activeEventService = activeEventService;
        this.repository = repository;
        this.garaDettaglioRepository = garaDettaglioRepository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public ReaderViewState loadState(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);
        ReaderViewState state = repository.findByEventId(eventId)
                .orElseGet(() -> new ReaderViewState(eventId, new ArrayList<>()));
        ensureReaderIds(state);
        enrichLinks(eventId, state);
        repository.save(state);
        return state;
    }

    public ReaderViewState saveReader(String eventId, String readerId, ReaderConfigRequest request) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);
        ReaderViewState state = repository.findByEventId(eventId)
                .orElseGet(() -> new ReaderViewState(eventId, new ArrayList<>()));
        ensureReaderIds(state);

        String name = normalizeRequired(request == null ? null : request.getName(), "Reader name is required");
        String location = normalizeEnum(request == null ? null : request.getLocation(), List.of("LAN", "WAN", "FS"), "Location is not valid");
        String tipologia = normalizeEnum(request == null ? null : request.getTipologia(), List.of("ICRON", "UBIDIUM"), "Reader type is not valid");

        ReaderConfig reader = null;
        if (readerId != null && !readerId.isBlank()) {
            reader = state.getReaders().stream()
                    .filter(item -> readerId.equals(item.getReaderId()))
                    .findFirst()
                    .orElse(null);
        }
        if (reader == null) {
            reader = new ReaderConfig();
            reader.setReaderId(UUID.randomUUID().toString());
            state.getReaders().add(reader);
        }
        reader.setName(name);
        reader.setLocation(location);
        reader.setTipologia(tipologia);
        scanReader(eventId, reader);
        enrichLinks(eventId, state);
        repository.save(state);
        notify(eventId, "Reader saved: " + name);
        return state;
    }

    public ReaderViewState deleteReader(String eventId, String readerId) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);
        ReaderViewState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader view not found"));
        ensureReaderIds(state);
        boolean removed = state.getReaders().removeIf(item -> readerId.equals(item.getReaderId()));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader not found");
        }
        repository.save(state);
        notify(eventId, "Reader deleted");
        return state;
    }

    public ReaderViewState moveReader(String eventId, String readerId, String direction) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);
        ReaderViewState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader view not found"));
        ensureReaderIds(state);
        int index = -1;
        for (int i = 0; i < state.getReaders().size(); i++) {
            if (readerId.equals(state.getReaders().get(i).getReaderId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader not found");
        }
        int targetIndex = "up".equalsIgnoreCase(direction) ? index - 1 : "down".equalsIgnoreCase(direction) ? index + 1 : -1;
        if (targetIndex < 0 || targetIndex >= state.getReaders().size()) {
            return state;
        }
        java.util.Collections.swap(state.getReaders(), index, targetIndex);
        repository.save(state);
        return state;
    }

    public ReaderViewState scanNow(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);
        ReaderViewState state = repository.findByEventId(eventId)
                .orElseGet(() -> new ReaderViewState(eventId, new ArrayList<>()));
        ensureReaderIds(state);
        int discoveredReaders = autoRegisterLanReaders(state);
        for (ReaderConfig reader : state.getReaders()) {
            scanReader(eventId, reader);
        }
        enrichLinks(eventId, state);
        repository.save(state);
        if (discoveredReaders > 0) {
            notify(eventId, "Reader scan completed (" + discoveredReaders + " new LAN reader" + (discoveredReaders == 1 ? "" : "s") + " registered)");
        } else {
            notify(eventId, "Reader scan completed");
        }
        return state;
    }

    public LanDiscoveryResponse discoverLanReaders(String eventId) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);

        return discoverLanReadersInternal();
    }

    public ReaderFileContentResponse loadFileContent(String eventId, String readerId, String encodedPath) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);
        ReaderViewState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader view not found"));
        ensureReaderIds(state);

        ReaderConfig reader = state.getReaders().stream()
                .filter(item -> readerId.equals(item.getReaderId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader not found"));

        String normalizedPath = safe(encodedPath).trim();
        if (normalizedPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File path is required");
        }

        ReaderFileEntry fileEntry = reader.getTodayFiles().stream()
                .filter(item -> normalizedPath.equals(safe(item.getPath())))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found in reader"));

        ReaderFileContentResponse response = new ReaderFileContentResponse();
        response.setReaderId(reader.getReaderId());
        response.setReaderName(reader.getName());
        response.setFileName(fileEntry.getName());
        response.setFilePath(fileEntry.getPath());
        response.setContent(readReaderFileContent(fileEntry));
        return response;
    }

    public String materializeReaderFileForRace(String eventId, String readerId, String encodedPath) throws Exception {
        sessionService.requireAuthenticated();
        activeEventService.setActiveEventId(eventId);
        ReaderViewState state = repository.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader view not found"));
        ensureReaderIds(state);

        ReaderConfig reader = state.getReaders().stream()
                .filter(item -> readerId.equals(item.getReaderId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader not found"));

        String normalizedPath = safe(encodedPath).trim();
        if (normalizedPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File path is required");
        }

        ReaderFileEntry fileEntry = reader.getTodayFiles().stream()
                .filter(item -> normalizedPath.equals(safe(item.getPath())))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found in reader"));

        if (sessionService.isTZeroMode()) {
            return copyReaderFileIntoTZeroDownload(eventId, fileEntry);
        }
        String sourceUrl = safe(fileEntry.getSourceUrl()).trim();
        return sourceUrl.isBlank() ? fileEntry.getPath() : sourceUrl;
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledScan() {
        String eventId = activeEventService.getActiveEventId();
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        try {
            ReaderViewState state = repository.findByEventId(eventId).orElse(null);
            if (state == null || state.getReaders() == null || state.getReaders().isEmpty()) {
                return;
            }
            boolean changed = false;
            for (ReaderConfig reader : state.getReaders()) {
                changed = scanReader(eventId, reader) || changed;
            }
            changed = enrichLinks(eventId, state) || changed;
            if (changed) {
                repository.save(state);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean scanReader(String eventId, ReaderConfig reader) {
        List<ReaderFileEntry> files = new ArrayList<>();
        String status = "WARN";
        String message;
        try {
            if ("FS".equalsIgnoreCase(reader.getLocation())) {
                files = scanFileSystemReader(reader);
                status = "OK";
                message = files.isEmpty() ? "No files found for today" : files.size() + " files found";
            } else if ("WAN".equalsIgnoreCase(reader.getLocation()) && "ICRON".equalsIgnoreCase(reader.getTipologia())) {
                files = scanIcronWanReader(reader);
                status = "OK";
                message = files.isEmpty() ? "No files found for today" : files.size() + " files found";
            } else {
                message = "Scanner not configured yet for " + reader.getTipologia() + " / " + reader.getLocation();
            }
        } catch (Exception ex) {
            status = "ERROR";
            message = ex.getMessage();
            files = new ArrayList<>();
        }
        boolean changed = !sameFiles(reader.getTodayFiles(), files)
                || !safe(reader.getLastScanStatus()).equals(status)
                || !safe(reader.getLastScanMessage()).equals(message);
        reader.setTodayFiles(files);
        reader.setLastScanStatus(status);
        reader.setLastScanMessage(message);
        reader.setLastScanAt(LocalDateTime.now().format(FILE_TS_FORMATTER));
        if (changed) {
            messagingTemplate.convertAndSend("/topic/messages", buildMessage(eventId, "[" + reader.getName() + "] " + message));
        }
        return changed;
    }

    private List<ReaderFileEntry> scanFileSystemReader(ReaderConfig reader) throws IOException {
        Path root = Paths.get(reader.getName()).toAbsolutePath().normalize();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IOException("FS reader path not found");
        }
        LocalDate today = LocalDate.now();
        try (Stream<Path> stream = Files.list(root)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> toEntry(path, today))
                    .filter(item -> item != null && isSupportedReaderFile(item.getName()))
                    .sorted(Comparator.comparing(ReaderFileEntry::getModifiedAt).reversed())
                    .toList();
        }
    }

    private List<ReaderFileEntry> scanIcronWanReader(ReaderConfig reader) throws Exception {
        String readerKey = safe(reader.getName()).trim();
        if (readerKey.isBlank()) {
            throw new IOException("Reader name is required");
        }
        String url = ICRON_WAN_INDEX_URL + URLEncoder.encode(readerKey, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("WAN reader request failed: HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (!root.path("ok").asBoolean(false)) {
            throw new IOException(root.path("error").asText("WAN reader response error"));
        }

        List<ReaderFileEntry> files = new ArrayList<>();
        JsonNode array = root.path("files");
        if (array.isArray()) {
            for (JsonNode node : array) {
                ReaderFileEntry entry = new ReaderFileEntry();
                entry.setName(node.path("name").asText(""));
                entry.setPath(node.path("path").asText(""));
                entry.setModifiedAt(node.path("modifiedAt").asText(""));
                entry.setSizeBytes(node.path("sizeBytes").asLong(0));
                String relativeUrl = node.path("url").asText("");
                if (!relativeUrl.isBlank()) {
                    entry.setSourceUrl(relativeUrl.startsWith("http") ? relativeUrl : ICRON_WAN_BASE_URL + relativeUrl);
                }
                if (isSupportedReaderFile(entry.getName())) {
                    files.add(entry);
                }
            }
        }
        files.sort(Comparator.comparing(ReaderFileEntry::getModifiedAt).reversed());
        return files;
    }

    private ReaderFileEntry toEntry(Path path, LocalDate today) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            LocalDateTime dateTime = LocalDateTime.ofInstant(lastModified.toInstant(), ZoneId.systemDefault());
            if (!dateTime.toLocalDate().equals(today)) {
                return null;
            }
            return new ReaderFileEntry(
                    path.getFileName().toString(),
                    path.toAbsolutePath().normalize().toString(),
                    dateTime.format(FILE_TS_FORMATTER),
                    Files.size(path)
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean sameFiles(List<ReaderFileEntry> left, List<ReaderFileEntry> right) {
        List<ReaderFileEntry> a = left == null ? List.of() : left;
        List<ReaderFileEntry> b = right == null ? List.of() : right;
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!safe(a.get(i).getName()).equals(safe(b.get(i).getName()))
                    || !safe(a.get(i).getPath()).equals(safe(b.get(i).getPath()))
                    || !safe(a.get(i).getSourceUrl()).equals(safe(b.get(i).getSourceUrl()))
                    || !safe(a.get(i).getModifiedAt()).equals(safe(b.get(i).getModifiedAt()))
                    || a.get(i).getSizeBytes() != b.get(i).getSizeBytes()
                    || a.get(i).isLinkedToRace() != b.get(i).isLinkedToRace()) {
                return false;
            }
        }
        return true;
    }

    private boolean enrichLinks(String eventId, ReaderViewState state) throws IOException {
        Set<String> sourceNames = new HashSet<>();
        java.util.Optional<GaraDettaglioSnapshot> snapshotOpt = garaDettaglioRepository.findByEventId(eventId);
        if (snapshotOpt.isPresent() && snapshotOpt.get().getRows() != null) {
            for (GaraDettaglioRow row : snapshotOpt.get().getRows()) {
                String source = safe(row.getSource()).trim();
                if (source.isBlank()) {
                    continue;
                }
                try {
                    Path fileName = Paths.get(source).getFileName();
                    if (fileName != null) {
                        sourceNames.add(fileName.toString().toLowerCase());
                    }
                } catch (Exception ignored) {
                }
            }
        }

        boolean changed = false;
        for (ReaderConfig reader : state.getReaders()) {
            if (reader.getTodayFiles() == null) {
                continue;
            }
            for (ReaderFileEntry file : reader.getTodayFiles()) {
                boolean linked = sourceNames.contains(safe(file.getName()).trim().toLowerCase());
                if (file.isLinkedToRace() != linked) {
                    file.setLinkedToRace(linked);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private String readReaderFileContent(ReaderFileEntry fileEntry) throws Exception {
        String sourceUrl = safe(fileEntry.getSourceUrl()).trim();
        if (!sourceUrl.isBlank()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Reader file download failed");
            }
            try (InputStream in = response.body()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        Path filePath = Paths.get(fileEntry.getPath()).toAbsolutePath().normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader file not found");
        }
        return Files.readString(filePath);
    }

    private String copyReaderFileIntoTZeroDownload(String eventId, ReaderFileEntry fileEntry) throws Exception {
        String rootFolder = sessionService.getTZeroRootFolder();
        if (rootFolder == null || rootFolder.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TZero root folder not configured");
        }

        Path downloadDir = Paths.get(rootFolder, eventId, "download").toAbsolutePath().normalize();
        Files.createDirectories(downloadDir);

        String fileName = safe(fileEntry.getName()).trim();
        if (fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reader file name is missing");
        }
        Path target = downloadDir.resolve(fileName).normalize();

        String sourceUrl = safe(fileEntry.getSourceUrl()).trim();
        if (!sourceUrl.isBlank()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Reader file download failed");
            }
            try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(target)) {
                in.transferTo(out);
            }
            return target.toString();
        }

        Path sourcePath = Paths.get(fileEntry.getPath()).toAbsolutePath().normalize();
        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader file not found");
        }
        Files.copy(sourcePath, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    private boolean isSupportedReaderFile(String fileName) {
        String name = safe(fileName).trim().toLowerCase();
        return name.endsWith(".tags") || name.endsWith(".csv");
    }

    private int autoRegisterLanReaders(ReaderViewState state) {
        LanDiscoveryResponse discovery = discoverLanReadersInternal();
        List<String> ips = discovery.getReaderIps() == null ? List.of() : discovery.getReaderIps();
        int added = 0;
        for (String ip : ips) {
            String normalizedIp = safe(ip).trim();
            if (normalizedIp.isBlank()) {
                continue;
            }
            boolean exists = state.getReaders().stream()
                    .anyMatch(reader -> normalizedIp.equalsIgnoreCase(safe(reader.getName()).trim()));
            if (exists) {
                continue;
            }
            ReaderConfig reader = new ReaderConfig();
            reader.setReaderId(UUID.randomUUID().toString());
            reader.setName(normalizedIp);
            reader.setLocation("LAN");
            reader.setTipologia("ICRON");
            reader.setTodayFiles(new ArrayList<>());
            state.getReaders().add(reader);
            added++;
        }
        return added;
    }

    private LanDiscoveryResponse discoverLanReadersInternal() {
        String subnetPrefix = detectLocal192SubnetPrefix();
        if (subnetPrefix == null || subnetPrefix.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No local 192.168.x subnet detected");
        }

        ExecutorService executor = Executors.newFixedThreadPool(24);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 1; i <= 254; i++) {
                final String ip = subnetPrefix + i;
                tasks.add(() -> probeReaderFiles(ip) ? ip : null);
            }
            List<String> ips = new ArrayList<>();
            for (Future<String> future : executor.invokeAll(tasks)) {
                try {
                    String ip = future.get();
                    if (ip != null && !ip.isBlank()) {
                        ips.add(ip);
                    }
                } catch (Exception ignored) {
                }
            }
            Collections.sort(ips);
            LanDiscoveryResponse response = new LanDiscoveryResponse();
            response.setSubnetPrefix(subnetPrefix);
            response.setReaderIps(ips);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LAN scan interrupted");
        } finally {
            executor.shutdownNow();
        }
    }

    private String detectLocal192SubnetPrefix() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!(address instanceof Inet4Address)) {
                        continue;
                    }
                    String host = address.getHostAddress();
                    if (!host.startsWith("192.168.")) {
                        continue;
                    }
                    String[] parts = host.split("\\.");
                    if (parts.length == 4) {
                        return parts[0] + "." + parts[1] + "." + parts[2] + ".";
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean probeReaderFiles(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + ip + "/files"))
                    .GET()
                    .timeout(Duration.ofMillis(500))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            return status >= 200 && status < 400;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void ensureReaderIds(ReaderViewState state) {
        if (state.getReaders() == null) {
            state.setReaders(new ArrayList<>());
        }
        for (ReaderConfig reader : state.getReaders()) {
            if (reader.getReaderId() == null || reader.getReaderId().isBlank()) {
                reader.setReaderId(UUID.randomUUID().toString());
            }
            if (reader.getTodayFiles() == null) {
                reader.setTodayFiles(new ArrayList<>());
            }
        }
    }

    private String normalizeRequired(String value, String error) {
        String normalized = safe(value).trim();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        return normalized;
    }

    private String normalizeEnum(String value, List<String> allowed, String error) {
        String normalized = safe(value).trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        return normalized;
    }

    private void notify(String eventId, String text) {
        messagingTemplate.convertAndSend("/topic/messages", buildMessage(eventId, text));
    }

    private ChatMessage buildMessage(String eventId, String text) {
        ChatMessage message = new ChatMessage();
        message.setSender("reader-view");
        message.setContent("[" + eventId + "] " + text);
        return message;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
