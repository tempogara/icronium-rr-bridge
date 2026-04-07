package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.Gara;
import it.icron.icronium.connector.rr.model.GaraDettaglioRow;
import it.icron.icronium.connector.rr.model.TZeroSourceConfig;
import it.icron.icronium.connector.rr.model.TZeroTimingPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TZeroConfigService {
    private static final DateTimeFormatter FOLDER_MODIFIED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final Path configFile = Paths.get("data", "tzero", "config.json");

    public TZeroConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized String getRootFolder() {
        if (!Files.exists(configFile)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(configFile.toFile());
            JsonNode pathNode = root.get("rootFolder");
            if (pathNode == null || pathNode.isNull()) {
                return null;
            }
            String value = pathNode.asText();
            return value == null || value.isBlank() ? null : value.trim();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile leggere la configurazione TZero");
        }
    }

    public synchronized String saveRootFolder(String rootFolder) {
        Path validRoot = validateRootFolder(rootFolder);
        try {
            Files.createDirectories(configFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(
                    configFile.toFile(),
                    Map.of("rootFolder", validRoot.toAbsolutePath().normalize().toString())
            );
            return validRoot.toAbsolutePath().normalize().toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile salvare la configurazione TZero");
        }
    }

    public Path validateRootFolder(String rootFolder) {
        if (rootFolder == null || rootFolder.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cartella root TZero è obbligatoria");
        }
        Path rootPath = Paths.get(rootFolder.trim()).toAbsolutePath().normalize();
        if (!Files.exists(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cartella TZero non esiste");
        }
        if (!Files.isDirectory(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il path TZero deve essere una cartella");
        }
        return rootPath;
    }

    public List<Gara> loadGareFromRoot(String rootFolder) {
        Path rootPath = validateRootFolder(rootFolder);
        try {
            List<Gara> result = new ArrayList<>();
            try (var stream = Files.list(rootPath)) {
                stream
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(this::safeLastModifiedTime).reversed())
                        .forEach(path -> {
                            String folderName = path.getFileName().toString();
                            result.add(new Gara(folderName, folderName, formatLastModified(path)));
                        });
            }
            return result;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile leggere le cartelle TZero");
        }
    }

    private FileTime safeLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0L);
        }
    }

    private String formatLastModified(Path path) {
        return FOLDER_MODIFIED_FORMATTER.format(
                safeLastModifiedTime(path).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );
    }

    public Path getEventFolder(String rootFolder, String eventId) {
        Path rootPath = validateRootFolder(rootFolder);
        if (eventId == null || eventId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId obbligatorio");
        }
        Path eventFolder = rootPath.resolve(eventId.trim()).normalize();
        if (!Files.exists(eventFolder) || !Files.isDirectory(eventFolder)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartella gara TZero non trovata");
        }
        return eventFolder;
    }

    public synchronized List<TZeroTimingPoint> loadTimingPoints(String rootFolder, String eventId) {
        Path configPath = getTimingPointsConfigPath(rootFolder, eventId);
        if (!Files.exists(configPath)) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = objectMapper.readTree(configPath.toFile());
            JsonNode timingPointsNode = root == null ? null : root.get("timingPoints");
            List<TZeroTimingPoint> result = new ArrayList<>();
            if (timingPointsNode != null && timingPointsNode.isArray()) {
                for (JsonNode node : timingPointsNode) {
                    String name = textValue(node.get("name"));
                    if (name.isBlank()) {
                        continue;
                    }
                    String description = textValue(node.get("description"));
                    List<String> sources = new ArrayList<>();
                    JsonNode sourcesNode = node.get("sources");
                    if (sourcesNode != null && sourcesNode.isArray()) {
                        for (JsonNode sourceNode : sourcesNode) {
                            String source = sourceNode == null ? "" : sourceNode.asText("");
                            if (source != null && !source.isBlank()) {
                                sources.add(source.trim());
                            }
                        }
                    }
                    result.add(new TZeroTimingPoint(name.trim(), description.trim(), sources));
                }
            }
            result.sort(Comparator.comparing(tp -> tp.getName().toLowerCase()));
            return result;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile leggere timingpoints.json");
        }
    }

    public synchronized List<TZeroTimingPoint> createOrUpdateTimingPoint(String rootFolder, String eventId, String name, String description) {
        String normalizedName = normalizeTimingPointName(name);
        List<TZeroTimingPoint> timingPoints = loadTimingPoints(rootFolder, eventId);
        TZeroTimingPoint existing = timingPoints.stream()
                .filter(tp -> normalizedName.equalsIgnoreCase(tp.getName()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            timingPoints.add(new TZeroTimingPoint(normalizedName, normalizeDescription(description), new ArrayList<>()));
        } else {
            existing.setDescription(normalizeDescription(description));
        }
        saveTimingPoints(rootFolder, eventId, timingPoints);
        return loadTimingPoints(rootFolder, eventId);
    }

    public synchronized List<TZeroTimingPoint> deleteTimingPoint(String rootFolder, String eventId, String name) {
        String normalizedName = normalizeTimingPointName(name);
        List<TZeroTimingPoint> timingPoints = loadTimingPoints(rootFolder, eventId);
        boolean removed = timingPoints.removeIf(tp -> normalizedName.equalsIgnoreCase(tp.getName()));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Timing point TZero non trovato");
        }
        saveTimingPoints(rootFolder, eventId, timingPoints);
        return loadTimingPoints(rootFolder, eventId);
    }

    public synchronized List<TZeroTimingPoint> assignSourceToTimingPoint(String rootFolder, String eventId, String sourceFileName, String timingPointName) {
        String normalizedSource = normalizeSourceFileName(sourceFileName);
        if (normalizedSource.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source file obbligatorio");
        }
        List<TZeroTimingPoint> timingPoints = loadTimingPoints(rootFolder, eventId);
        timingPoints.forEach(tp -> tp.getSources().removeIf(source -> normalizedSource.equalsIgnoreCase(normalizeSourceFileName(source))));

        String normalizedTimingPoint = timingPointName == null ? "" : timingPointName.trim();
        if (!normalizedTimingPoint.isBlank()) {
            TZeroTimingPoint target = timingPoints.stream()
                    .filter(tp -> normalizedTimingPoint.equalsIgnoreCase(tp.getName()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timing point TZero non definito"));
            if (target.getSources().stream().noneMatch(source -> normalizedSource.equalsIgnoreCase(normalizeSourceFileName(source)))) {
                target.getSources().add(normalizedSource);
            }
        }
        saveTimingPoints(rootFolder, eventId, timingPoints);
        return loadTimingPoints(rootFolder, eventId);
    }

    public synchronized Map<String, String> buildTimingPointBySourceMap(String rootFolder, String eventId) {
        Map<String, String> result = new LinkedHashMap<>();
        for (TZeroTimingPoint timingPoint : loadTimingPoints(rootFolder, eventId)) {
            for (String source : timingPoint.getSources()) {
                String normalizedSource = normalizeSourceFileName(source);
                if (!normalizedSource.isBlank()) {
                    result.put(normalizedSource.toLowerCase(), timingPoint.getName());
                }
            }
        }
        return result;
    }

    public synchronized List<TZeroSourceConfig> loadSourceConfigs(String rootFolder, String eventId) {
        Path configPath = getSourcesConfigPath(rootFolder, eventId);
        if (!Files.exists(configPath)) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = objectMapper.readTree(configPath.toFile());
            JsonNode sourcesNode = root == null ? null : root.get("sources");
            List<TZeroSourceConfig> result = new ArrayList<>();
            if (sourcesNode != null && sourcesNode.isArray()) {
                for (JsonNode node : sourcesNode) {
                    String source = textValue(node.get("source")).trim();
                    if (source.isBlank()) {
                        continue;
                    }
                    TZeroSourceConfig cfg = new TZeroSourceConfig();
                    cfg.setSource(source);
                    cfg.setTimingPoint(textValue(node.get("timingPoint")).trim());
                    cfg.setScaricaOgniSec(node.hasNonNull("scaricaOgniSec") ? node.get("scaricaOgniSec").asInt(10) : 10);
                    cfg.setSyncOffset(textValue(node.get("syncOffset")).trim());
                    cfg.setFilterFromTime(textValue(node.get("filterFromTime")).trim());
                    cfg.setFilterToTime(textValue(node.get("filterToTime")).trim());
                    cfg.setSourceKind(textValue(node.get("sourceKind")).trim());
                    result.add(cfg);
                }
            }
            return result;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile leggere sources.json");
        }
    }

    public synchronized void upsertSourceConfig(String rootFolder, String eventId, GaraDettaglioRow row) {
        if (row == null || row.getSource() == null || row.getSource().isBlank()) {
            return;
        }
        List<TZeroSourceConfig> configs = loadSourceConfigs(rootFolder, eventId);
        String normalizedSource = normalizeSourceKey(row.getSource());
        TZeroSourceConfig target = configs.stream()
                .filter(cfg -> normalizedSource.equalsIgnoreCase(normalizeSourceKey(cfg.getSource())))
                .findFirst()
                .orElse(null);
        if (target == null) {
            target = new TZeroSourceConfig();
            configs.add(target);
        }
        target.setSource(row.getSource().trim());
        target.setTimingPoint(row.getTimingPoint() == null ? "" : row.getTimingPoint().trim());
        target.setScaricaOgniSec(row.getScaricaOgniSec());
        target.setSyncOffset(row.getSyncOffset() == null ? "" : row.getSyncOffset().trim());
        target.setFilterFromTime(row.getFilterFromTime() == null ? "" : row.getFilterFromTime().trim());
        target.setFilterToTime(row.getFilterToTime() == null ? "" : row.getFilterToTime().trim());
        target.setSourceKind(row.getSourceKind() == null ? "" : row.getSourceKind().trim());
        saveSourceConfigs(rootFolder, eventId, configs);
    }

    public synchronized void removeSourceConfig(String rootFolder, String eventId, String source) {
        String normalizedSource = normalizeSourceKey(source);
        if (normalizedSource.isBlank()) {
            return;
        }
        List<TZeroSourceConfig> configs = loadSourceConfigs(rootFolder, eventId);
        configs.removeIf(cfg -> normalizedSource.equalsIgnoreCase(normalizeSourceKey(cfg.getSource())));
        saveSourceConfigs(rootFolder, eventId, configs);
    }

    private void saveTimingPoints(String rootFolder, String eventId, List<TZeroTimingPoint> timingPoints) {
        Path configPath = getTimingPointsConfigPath(rootFolder, eventId);
        try {
            Files.createDirectories(configPath.getParent());
            List<Map<String, Object>> serialized = timingPoints.stream()
                    .filter(Objects::nonNull)
                    .map(tp -> Map.<String, Object>of(
                            "name", tp.getName() == null ? "" : tp.getName().trim(),
                            "description", tp.getDescription() == null ? "" : tp.getDescription().trim(),
                            "sources", tp.getSources() == null ? List.of() : tp.getSources().stream()
                                    .map(this::normalizeSourceFileName)
                                    .filter(source -> !source.isBlank())
                                    .distinct()
                                    .toList()
                    ))
                    .toList();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), Map.of("timingPoints", serialized));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile salvare timingpoints.json");
        }
    }

    private void saveSourceConfigs(String rootFolder, String eventId, List<TZeroSourceConfig> configs) {
        Path configPath = getSourcesConfigPath(rootFolder, eventId);
        try {
            Files.createDirectories(configPath.getParent());
            List<Map<String, Object>> serialized = configs.stream()
                    .filter(Objects::nonNull)
                    .filter(cfg -> cfg.getSource() != null && !cfg.getSource().isBlank())
                    .map(cfg -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("source", cfg.getSource().trim());
                        row.put("timingPoint", cfg.getTimingPoint() == null ? "" : cfg.getTimingPoint().trim());
                        row.put("scaricaOgniSec", cfg.getScaricaOgniSec() == null ? 10 : cfg.getScaricaOgniSec());
                        row.put("syncOffset", cfg.getSyncOffset() == null ? "" : cfg.getSyncOffset().trim());
                        row.put("filterFromTime", cfg.getFilterFromTime() == null ? "" : cfg.getFilterFromTime().trim());
                        row.put("filterToTime", cfg.getFilterToTime() == null ? "" : cfg.getFilterToTime().trim());
                        row.put("sourceKind", cfg.getSourceKind() == null ? "" : cfg.getSourceKind().trim());
                        return row;
                    })
                    .toList();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), Map.of("sources", serialized));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile salvare sources.json");
        }
    }

    private Path getTimingPointsConfigPath(String rootFolder, String eventId) {
        Path eventFolder = getEventFolder(rootFolder, eventId);
        return eventFolder.resolve("config").resolve("timingpoints.json").normalize();
    }

    private Path getSourcesConfigPath(String rootFolder, String eventId) {
        Path eventFolder = getEventFolder(rootFolder, eventId);
        return eventFolder.resolve("config").resolve("sources.json").normalize();
    }

    private String normalizeTimingPointName(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome timing point obbligatorio");
        }
        return value.trim();
    }

    private String normalizeDescription(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSourceFileName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized.trim();
    }

    private String normalizeSourceKey(String value) {
        return value == null ? "" : value.trim();
    }

    private String textValue(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("");
    }
}
