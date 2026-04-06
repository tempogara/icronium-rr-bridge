package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.Gara;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TZeroConfigService {

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
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                        .forEach(path -> {
                            String folderName = path.getFileName().toString();
                            result.add(new Gara(folderName, folderName, "-"));
                        });
            }
            return result;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile leggere le cartelle TZero");
        }
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
}
