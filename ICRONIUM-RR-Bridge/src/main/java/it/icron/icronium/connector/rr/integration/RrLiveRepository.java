package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.RrLiveState;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public class RrLiveRepository {

    private final ObjectMapper objectMapper;
    private final Path storageDir = Paths.get("data", "rr-live");

    public RrLiveRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized Optional<RrLiveState> findByEventId(String eventId) throws IOException {
        Path path = fileForEvent(eventId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(path.toFile(), RrLiveState.class));
    }

    public synchronized void save(RrLiveState state) throws IOException {
        Files.createDirectories(storageDir);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileForEvent(state.getEventId()).toFile(), state);
    }

    public synchronized List<RrLiveState> findAll() throws IOException {
        List<RrLiveState> out = new ArrayList<>();
        if (!Files.exists(storageDir)) {
            return out;
        }
        try (Stream<Path> stream = Files.list(storageDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    out.add(objectMapper.readValue(path.toFile(), RrLiveState.class));
                } catch (IOException ignored) {
                }
            });
        }
        return out;
    }

    private Path fileForEvent(String eventId) {
        return storageDir.resolve(eventId + ".json");
    }
}
